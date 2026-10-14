package dev.rylex.nep.machine;

import appeng.api.networking.IGridNode;

public final class WholeChannelDemand {

    private WholeChannelDemand() {}

    public static int of(IGridNode node) {
        return Math.max(1, ChannelDemand.of(node));
    }
}
