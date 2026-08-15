package dev.rylex.nep.machine;

import appeng.api.networking.IGridNode;

public interface ChannelDemand {

    int channelDemand();

    static int of(IGridNode node) {
        return node.getOwner() instanceof ChannelDemand demand ? Math.max(0, demand.channelDemand()) : 1;
    }
}
