/*
 * Copyright (c) 2012. Midokura Japan K.K.
 */

package com.midokura.midonet.client.resource;

import com.midokura.midonet.client.VendorMediaType;
import com.midokura.midonet.client.WebResource;
import com.midokura.midonet.client.dto.DtoPortGroup;

import java.net.URI;
import java.util.UUID;

public class PortGroup extends ResourceBase<PortGroup, DtoPortGroup> {


    public PortGroup(WebResource resource, URI uriForCreation,
                     DtoPortGroup pg) {
        super(resource, uriForCreation, pg, VendorMediaType.APPLICATION_PORTGROUP_JSON);
    }

    /**
     * Gets URI for this port groups
     *
     * @return URI of this port group
     */
    @Override
    public URI getUri() {
        return principalDto.getUri();
    }

    /**
     * Gets ID of the port Groups
     *
     * @return UUID of this port groups
     */
    public UUID getId() {
        return principalDto.getId();
    }

    /**
     * Gets name of this port group
     *
     * @return name of this port group
     */
    public String getName() {
        return principalDto.getName();
    }

    /**
     * Gets tenant ID string of this port group
     *
     * @return tenant ID string
     */
    public String getTenantId() {
        return principalDto.getTenantId();
    }

    /**
     * Sets name of this portgroup for creation
     *
     * @param name name of the port group
     * @return this
     */
    public PortGroup name(String name) {
        principalDto.setName(name);
        return this;
    }
}