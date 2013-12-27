/*
 * Copyright (c) 2014 Midokura Europe SARL, All Rights Reserved.
 */
package org.midonet.midolman.simulation

import java.util.UUID
import akka.event.LoggingBus

/**
 * LoadBalancer.
 *
 * Placeholder class.
 */
class LoadBalancer(val id: UUID, val adminStateUp: Boolean,
                    val vips: Set[VIP], val loggingBus: LoggingBus)
