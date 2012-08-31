/*
 * Copyright (c) 2012 Midokura Pte.Ltd.
 */

package com.midokura.midonet.client.dto;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.UUID;

/**
 * MetricTarget represent an object for which we can collect metrics.
 * Date: 5/25/12
 */

@XmlRootElement
public class DtoMetricTarget {
    UUID targetIdentifier;
    String type;

    public UUID getTargetIdentifier() {
        return targetIdentifier;
    }

    public void setTargetIdentifier(UUID targetIdentifier) {
        this.targetIdentifier = targetIdentifier;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
