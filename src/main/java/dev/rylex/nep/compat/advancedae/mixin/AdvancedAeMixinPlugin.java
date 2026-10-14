package dev.rylex.nep.compat.advancedae.mixin;

import dev.rylex.nep.mixin.ModGatedMixinPlugin;

public class AdvancedAeMixinPlugin extends ModGatedMixinPlugin {

    public AdvancedAeMixinPlugin() {
        super(
                "advanced_ae",
                "AdvPatternProviderLogicMixin",
                "AdvPatternProviderLogicHostMixin",
                "AdvPatternProviderMenuMixin",
                "AdvPatternProviderScreenMixin",
                "SmallAdvPatternProviderScreenMixin");
    }
}
