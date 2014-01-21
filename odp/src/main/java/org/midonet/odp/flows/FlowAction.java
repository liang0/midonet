/*
 * Copyright (c) 2012 Midokura Europe SARL, All Rights Reserved.
 */
package org.midonet.odp.flows;

import org.midonet.netlink.NetlinkMessage;
import org.midonet.netlink.messages.BuilderAware;
import org.midonet.odp.OpenVSwitch;

public interface FlowAction
        extends BuilderAware, NetlinkMessage.Attr<FlowAction> {

    public interface FlowActionAttr {

        /** u32 port number. */
        NetlinkMessage.AttrKey<FlowActionOutput> OUTPUT =
            NetlinkMessage.AttrKey.attr(OpenVSwitch.FlowAction.Attr.Output);

        /** Nested OVS_USERSPACE_ATTR_*. */
        NetlinkMessage.AttrKey<FlowActionUserspace> USERSPACE =
            NetlinkMessage.AttrKey.attrNested(OpenVSwitch.FlowAction.Attr.Userspace);

        /** One nested OVS_KEY_ATTR_*. */
        NetlinkMessage.AttrKey<FlowActionSetKey> SET =
            NetlinkMessage.AttrKey.attrNested(OpenVSwitch.FlowAction.Attr.Set);

        /** struct ovs_action_push_vlan. */
        NetlinkMessage.AttrKey<FlowActionPushVLAN> PUSH_VLAN =
            NetlinkMessage.AttrKey.attr(OpenVSwitch.FlowAction.Attr.PushVLan);

        /** No argument. */
        NetlinkMessage.AttrKey<FlowActionPopVLAN> POP_VLAN =
            NetlinkMessage.AttrKey.attr(OpenVSwitch.FlowAction.Attr.PopVLan);

        /** Nested OVS_SAMPLE_ATTR_*. */
        NetlinkMessage.AttrKey<FlowActionSample> SAMPLE =
            NetlinkMessage.AttrKey.attrNested(OpenVSwitch.FlowAction.Attr.Sample);
    }

    static NetlinkMessage.CustomBuilder<FlowAction> Builder =
        new NetlinkMessage.CustomBuilder<FlowAction>() {
            @Override
            public FlowAction newInstance(short type) {
                switch (type) {

                    case OpenVSwitch.FlowAction.Attr.Output:
                        return new FlowActionOutput();

                    case OpenVSwitch.FlowAction.Attr.Userspace:
                        return new FlowActionUserspace();

                    case OpenVSwitch.FlowAction.Attr.Set:
                        return new FlowActionSetKey();

                    case OpenVSwitch.FlowAction.Attr.PushVLan:
                        return new FlowActionPushVLAN();

                    case OpenVSwitch.FlowAction.Attr.PopVLan:
                        return new FlowActionPopVLAN();

                    case OpenVSwitch.FlowAction.Attr.Sample:
                        return new FlowActionSample();

                    default: return null;
                }
            }
        };
}
