/*
 * Copyright 2014 Midokura SARL
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.midonet.midolman.datapath

import java.nio.channels._
import java.nio.channels.spi.SelectorProvider
import java.nio.ByteBuffer

import org.midonet.midolman.services.MidolmanActorsService

import scala.util.control.NonFatal

import com.lmax.disruptor.{Sequencer, LifecycleAware, EventPoller}
import rx.Observer

import org.slf4j.LoggerFactory
import com.typesafe.scalalogging.Logger

import org.midonet.midolman.datapath.DisruptorDatapathChannel._
import org.midonet.midolman.PacketWorkflow.DuplicateFlow
import org.midonet.midolman.PacketsEntryPoint
import org.midonet.netlink._
import org.midonet.netlink.exceptions.NetlinkException
import org.midonet.odp.{FlowMatch, OvsNetlinkFamilies, OvsProtocol}
import org.midonet.{ErrorCode, Util}
import org.midonet.util.concurrent.{NanoClock, Backchannel}

object FlowProcessor {
    private val unsafe = Util.getUnsafe

    /**
     * Used for unsafe access to the lastSequence field, so we can do a volatile
     * read on the writer thread while avoiding doing a volatile write to it
     * from the producer thread.
     */
    private val sequenceAddress = unsafe.objectFieldOffset(
        classOf[FlowProcessor].getDeclaredField("lastSequence"))
}

class FlowProcessor(families: OvsNetlinkFamilies,
                    maxPendingRequests: Int,
                    maxRequestSize: Int,
                    channelFactory: NetlinkChannelFactory,
                    selectorProvider: SelectorProvider,
                    clock: NanoClock)
    extends EventPoller.Handler[DatapathEvent]
    with Backchannel
    with LifecycleAware {

    import FlowProcessor._

    private val log = Logger(LoggerFactory.getLogger(
        "org.midonet.datapath.flow-processor"))

    private var writeBuf = BytesUtil.instance.allocateDirect(64 * 1024)
    private val selector = selectorProvider.openSelector()
    private val createChannel = channelFactory.create(blocking = false)
    private val createChannelPid = createChannel.getLocalAddress.getPid
    private val createProtocol = new OvsProtocol(createChannelPid, families)
    private val brokerChannel = channelFactory.create(blocking = false)
    private val brokerChannelPid = brokerChannel.getLocalAddress.getPid
    private val brokerProtocol = new OvsProtocol(brokerChannelPid, families)

    {
        log.debug(s"Created write channel with pid $createChannelPid")
        log.debug(s"Created delete channel with pid $brokerChannelPid")
    }

    private val writer = new NetlinkBlockingWriter(createChannel)
    private val broker = new NetlinkRequestBroker(
        new NetlinkBlockingWriter(brokerChannel),
        new NetlinkReader(brokerChannel),
        maxPendingRequests,
        maxRequestSize,
        BytesUtil.instance.allocateDirect(64 * 1024),
        clock)
    private val timeoutMillis = broker.timeout.toMillis

    private var lastSequence = Sequencer.INITIAL_CURSOR_VALUE

    override def onEvent(event: DatapathEvent, sequence: Long,
                         endOfBatch: Boolean): Boolean = {
        if (event.op == FLOW_CREATE) {
            try {
                event.bb.putInt(NetlinkMessage.NLMSG_PID_OFFSET, createChannelPid)
                writer.write(event.bb)
                log.debug(s"Created flow #$sequence")
            } catch { case t: Throwable =>
                log.error(s"Failed to create flow #$sequence", t)
            }
            lastSequence = sequence
        }
        true
    }

    def capacity = broker.capacity

    /**
     * Tries to eject a flow only if the corresponding Disruptor sequence is
     * greater than the one specified, meaning that the corresponding flow
     * create operation hasn't been completed yet.
     */
    def tryEject(sequence: Long, datapathId: Int, flowMatch: FlowMatch,
                 obs: Observer[ByteBuffer]): Boolean = {
        var brokerSeq = 0L
        val disruptorSeq = unsafe.getLongVolatile(this, sequenceAddress)
        if (disruptorSeq >= sequence && { brokerSeq = broker.nextSequence()
                                          brokerSeq } != NetlinkRequestBroker.FULL) {
            try {
                brokerProtocol.prepareFlowDelete(
                    datapathId, flowMatch.getKeys, broker.get(brokerSeq))
                broker.publishRequest(brokerSeq, obs)
            } catch { case e: Throwable =>
                obs.onError(e)
            }
            true
        } else {
            false
        }
    }

    def tryGet(datapathId: Int, flowMatch: FlowMatch,
               obs: Observer[ByteBuffer]): Boolean = {
        var seq = 0L
        if ({ seq = broker.nextSequence(); seq } != NetlinkRequestBroker.FULL) {
            try {
                brokerProtocol.prepareFlowGet(
                    datapathId, flowMatch, broker.get(seq))
                broker.publishRequest(seq, obs)
            } catch { case e: Throwable =>
                obs.onError(e)
            }
            true
        } else {
            false
        }
    }

    override def shouldProcess(): Boolean =
        broker.hasRequestsToWrite

    override def process(): Unit = {
        if (broker.hasRequestsToWrite) {
            val bytes = broker.writePublishedRequests()
            log.debug(s"Wrote flow deletion requests ($bytes bytes)")
        }
    }

    val handleDeleteError = new Observer[ByteBuffer] {
        override def onCompleted(): Unit =
            log.warn("Unexpected reply to flow deletion; probably the late " +
                     "answer of a request that timed out")

        override def onError(e: Throwable): Unit = e match {
            case ne: NetlinkException =>
                log.warn(s"Unexpected flow deletion error ${ne.getErrorCodeEnum}; " +
                         "probably the late answer of a request that timed out")
            case NonFatal(nf) =>
                log.error("Unexpected error when deleting a flow", nf)
        }

        override def onNext(t: ByteBuffer): Unit = { }
    }

    private def handleCreateError(reader: NetlinkReader, buf: ByteBuffer): Unit =
        try {
            reader.read(buf)
        } catch {
            case ne: NetlinkException if ne.getErrorCodeEnum == ErrorCode.EEXIST =>
                log.debug("Tried to add duplicate DP flow")
                PacketsEntryPoint.getRef()(MidolmanActorsService.actorSystem) !
                    DuplicateFlow(ne.seq)
            case e: NetlinkException =>
                log.warn("Failed to create flow", e)
            case NonFatal(e) =>
                log.error("Unexpected error when creating a flow")
        } finally {
            buf.clear()
        }

    private val replies = new Thread("flow-processor-errors") {
        override def run(): Unit = {
            val reader = new NetlinkReader(createChannel)
            val createErrorsBuffer = BytesUtil.instance.allocateDirect(64 * 1024)
            while (createChannel.isOpen && brokerChannel.isOpen) {
                val createKey = createChannel.register(selector, SelectionKey.OP_READ)
                val deleteKey = brokerChannel.register(selector, SelectionKey.OP_READ)
                try {
                    if (selector.select(timeoutMillis) > 0) {
                        if (createKey.isReadable)
                            handleCreateError(reader, createErrorsBuffer)
                        if (deleteKey.isReadable)
                            broker.readReply(handleDeleteError)
                        selector.selectedKeys().clear()
                    } else {
                        broker.timeoutExpiredRequests()
                    }
                } catch {
                    case ignored @ (_: InterruptedException |
                                    _: ClosedChannelException |
                                    _: ClosedByInterruptException|
                                    _: AsynchronousCloseException) =>
                    case NonFatal(t) =>
                        log.debug("Error while reading replies", t)
                }
            }
        }
    }

    override def onStart(): Unit = {
        replies.setDaemon(true)
        replies.start()
    }

    override def onShutdown(): Unit = {
        createChannel.close()
        brokerChannel.close()
        selector.wakeup()
    }
}
