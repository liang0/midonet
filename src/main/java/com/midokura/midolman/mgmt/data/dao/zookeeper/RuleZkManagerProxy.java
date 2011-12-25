/*
 * @(#)RuleZkManagerProxy        1.6 11/09/05
 *
 * Copyright 2011 Midokura KK
 */
package com.midokura.midolman.mgmt.data.dao.zookeeper;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.midokura.midolman.mgmt.data.dao.OwnerQueryable;
import com.midokura.midolman.mgmt.data.dao.RuleDao;
import com.midokura.midolman.mgmt.data.dto.Rule;
import com.midokura.midolman.state.Directory;
import com.midokura.midolman.state.RuleIndexOutOfBoundsException;
import com.midokura.midolman.state.RuleZkManager;
import com.midokura.midolman.state.StateAccessException;
import com.midokura.midolman.state.ZkNodeEntry;
import com.midokura.midolman.state.ZkStateSerializationException;

/**
 * Data access class for rules.
 *
 * @version 1.6 08 Sept 2011
 * @author Ryu Ishimoto
 */
public class RuleZkManagerProxy extends ZkMgmtManager implements RuleDao,
        OwnerQueryable {

    private RuleZkManager zkManager = null;

    /**
     * Constructor
     *
     * @param zkConn
     *            Zookeeper connection string
     */
    public RuleZkManagerProxy(Directory zk, String basePath, String mgmtBasePath) {
        super(zk, basePath, mgmtBasePath);
        zkManager = new RuleZkManager(zk, basePath);
    }

    /**
     * Add rule object to Zookeeper directories.
     *
     * @param rule
     *            Rule object to add.
     * @throws StateAccessException
     * @throws ZkStateSerializationException
     * @throws RuleIndexOutOfBoundsException
     * @throws Exception
     *             Error adding data to Zookeeper.
     */
    @Override
    public UUID create(Rule rule) throws RuleIndexOutOfBoundsException,
            StateAccessException {
        return zkManager.create(rule.toZkRule());
    }

    @Override
    public void delete(UUID id) throws StateAccessException {
        zkManager.delete(id);
    }

    /**
     * Get a Rule for the given ID.
     *
     * @param id
     *            Rule ID to search.
     * @return Rule object with the given ID.
     * @throws StateAccessException
     * @throws ZkStateSerializationException
     * @throws Exception
     *             Error getting data to Zookeeper.
     */
    @Override
    public Rule get(UUID id) throws StateAccessException {
        return new Rule(id, zkManager.get(id).value);
    }

    /**
     * Get a list of rules for a chain.
     *
     * @param chainId
     *            UUID of chain.
     * @return A Set of Rules
     * @throws StateAccessException
     * @throws ZkStateSerializationException
     * @throws Exception
     *             Zookeeper(or any) error.
     */
    @Override
    public List<Rule> list(UUID chainId) throws StateAccessException {
        List<Rule> rules = new ArrayList<Rule>();
        List<ZkNodeEntry<UUID, com.midokura.midolman.rules.Rule>> entries = zkManager
                .list(chainId);
        for (ZkNodeEntry<UUID, com.midokura.midolman.rules.Rule> entry : entries) {
            rules.add(new Rule(entry.key, entry.value));
        }
        return rules;
    }

    @Override
    public String getOwner(UUID id) throws StateAccessException {
        Rule rule = get(id);
        OwnerQueryable manager = new ChainZkManagerProxy(zk,
                pathManager.getBasePath(), mgmtPathManager.getBasePath());
        return manager.getOwner(rule.getChainId());
    }
}
