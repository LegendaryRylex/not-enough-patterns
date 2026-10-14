package dev.rylex.nep.compat.malum.mixin;

import dev.rylex.nep.mixin.ModGatedMixinPlugin;

public class MalumMixinPlugin extends ModGatedMixinPlugin {

    public MalumMixinPlugin() {
        super(
                "malum",
                "MalumLogBlockMixin",
                "MalumSpiritRiteTypesMixin",
                "RunicWorkbenchBlockEntityMixin",
                "SpiritAltarBlockEntityMixin",
                "SpiritCrucibleCoreBlockEntityMixin",
                "SoulHarvestHandlerMixin",
                "SpiritHolderMixin",
                "SpiritTypePropertyMixin");
    }
}
