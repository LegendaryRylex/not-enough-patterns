package dev.rylex.nep.compat.thunderbolt.mixin;

import dev.rylex.nep.mixin.ModGatedMixinPlugin;

public class ThunderboltMixinPlugin extends ModGatedMixinPlugin {

    public ThunderboltMixinPlugin() {
        super("ae2lt", "BorrowedCapacityCalculatorMixin", "PathingServiceChannelCountMixin");
    }
}
