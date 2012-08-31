package com.midokura.midonet.client.dto;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import java.net.InetAddress;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author Mihai Claudiu Toader <mtoader@midokura.com>
 *         Date: 1/31/12
 */
@XmlRootElement
public class DtoInterface {

    public enum Type {
        Physical, Virtual, Tunnel, Unknown
    }

    public enum StatusType {
        Up(0x01), Carrier(0x02);

        private int mask;

        private StatusType(int mask) {
            this.mask = mask;
        }

        public int getMask() {
            return mask;
        }
    }

    public enum PropertyKeys {
        midonet_port_id,
    }

    private UUID id;
    private UUID hostId;
    private String name;
    private String mac;
    private int mtu;
    private int status;
    private Type type;
    private String endpoint;
    private String portType;
    private InetAddress[] addresses;
    private Map<String, String> properties = new HashMap<String, String>();

    @XmlTransient
    private URI uri;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public UUID getHostId() {
        return hostId;
    }

    public void setHostId(UUID hostId) {
        this.hostId = hostId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public int getMtu() {
        return mtu;
    }

    public void setMtu(int mtu) {
        this.mtu = mtu;
    }

    public int getStatus() {
        return status;
    }

    public boolean getStatusField(StatusType statusType) {
        return (status & statusType.getMask()) != 0;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public void setStatusField(StatusType statusType) {
        setStatus(getStatus() & statusType.getMask());
    }

    public void clearStatusField(StatusType statusType) {
        setStatus(getStatus() & ~statusType.getMask());
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public InetAddress[] getAddresses() {
        return addresses;
    }

    public void setAddresses(InetAddress[] addresses) {
        this.addresses = addresses;
    }

    public String getPortType() {
        return portType;
    }

    public void setPortType(String portType) {
        this.portType = portType;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    public void setProperty(PropertyKeys property, String value) {
        getProperties().put(property.name(), value);
    }

    public String getProperty(PropertyKeys property) {
        return getProperties().get(property.name());
    }

    @Override
    public String toString() {
        return "DtoInterface{" +
                "name=" + name +
                ", hostId=" + hostId +
                ", mac='" + mac + '\'' +
                ", mtu=" + mtu +
                ", status=" + status +
                ", type=" + type +
                ", endpoint='" + endpoint + '\'' +
                ", porttype='" + portType + '\'' +
                ", addresses=" + (addresses == null ? null : Arrays.asList(
                addresses)) +
                ", properties=" + properties +
                ", uri=" + uri +
                '}';
    }
}
