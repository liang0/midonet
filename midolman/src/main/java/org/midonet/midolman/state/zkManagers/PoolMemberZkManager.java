/*
 * Copyright (c) 2014 Midokura SARL, All Rights Reserved.
 */
package org.midonet.midolman.state.zkManagers;

import java.util.List;
import java.util.UUID;

import com.google.common.base.Objects;

import org.apache.zookeeper.Op;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.midonet.cluster.data.neutron.loadbalancer.Member;
import org.midonet.midolman.serialization.SerializationException;
import org.midonet.midolman.serialization.Serializer;
import org.midonet.midolman.state.AbstractZkManager;
import org.midonet.midolman.state.PathBuilder;
import org.midonet.midolman.state.ZkManager;
import org.midonet.midolman.state.l4lb.LBStatus;

import static java.util.Arrays.asList;

/**
 * Class to manage the PoolMember ZooKeeper data.
 */
public class PoolMemberZkManager extends
        AbstractZkManager<UUID, PoolMemberZkManager.PoolMemberConfig> {

    private final static Logger log = LoggerFactory
            .getLogger(PoolMemberZkManager.class);

    public static class PoolMemberConfig extends BaseConfig {

        public UUID poolId;
        public String address;
        public int protocolPort;
        public int weight;
        public boolean adminStateUp;
        public LBStatus status;

        public PoolMemberConfig() {
            super();
        }

        public PoolMemberConfig(UUID poolId,
                                String address,
                                int protocolPort,
                                int weight,
                                boolean adminStateUp,
                                LBStatus status) {
            this.poolId = poolId;
            this.address = address;
            this.protocolPort = protocolPort;
            this.weight = weight;
            this.adminStateUp = adminStateUp;
            this.status = status;
        }

        public PoolMemberConfig(Member member) {
            this.poolId = member.poolId;
            this.address = member.address;
            this.protocolPort = member.protocolPort;
            this.weight = member.weight;
            this.adminStateUp = member.adminStateUp;
            // The default status is active. Only the health monitor can
            // change this to INACTIVE.
            this.status = LBStatus.ACTIVE;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(poolId, address, protocolPort, weight,
                    adminStateUp, status);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || !getClass().equals(o.getClass()))
                return false;

            PoolMemberConfig that = (PoolMemberConfig) o;

            return Objects.equal(poolId, that.poolId) &&
                    Objects.equal(address, that.address) &&
                    protocolPort == that.protocolPort &&
                    weight == that.weight &&
                    adminStateUp == that.adminStateUp &&
                    status == that.status;
        }
    }

    public PoolMemberZkManager(ZkManager zk, PathBuilder paths,
                               Serializer serializer) {
        super(zk, paths, serializer);
    }

    @Override
    protected String getConfigPath(UUID id) {
        return paths.getPoolMemberPath(id);
    }

    @Override
    protected Class<PoolMemberConfig> getConfigClass() {
        return PoolMemberConfig.class;
    }

    public List<Op> prepareCreate(UUID id, PoolMemberConfig config)
            throws SerializationException {
        return asList(simpleCreateOp(id, config));
    }

    public List<Op> prepareUpdate(UUID id, PoolMemberConfig config)
            throws SerializationException {
        return asList(simpleUpdateOp(id, config));
    }

    public List<Op> prepareDelete(UUID id) {
        return asList(Op.delete(paths.getPoolMemberPath(id), -1));
    }
}
