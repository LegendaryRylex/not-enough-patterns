package dev.rylex.nep.compat.apothic;

import appeng.api.AECapabilities;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.guide.NepModules;
import dev.shadowsoffire.apothic_enchanting.Ench;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;

public final class ApothicCompat {
    private ApothicCompat() {}

    public static void init(IEventBus modBus) {
        NepModules.register("apothic_enchanting", NepConfig::apothicOverride);
        NepModules.register("enchantment_infusion", NepConfig::apothicInfusion);
        ApothicPatternEncoders.register();
        modBus.addListener(ApothicCompat::registerCapabilities);
        modBus.addListener(ModConfigEvent.Reloading.class, ApothicCompat::onConfigReload);
        NeoForge.EVENT_BUS.addListener(ApothicCompat::onReload);
        NeoForge.EVENT_BUS.addListener(ApothicCompat::onDatapackSync);
    }

    private static void onReload(AddServerReloadListenersEvent event) {
        InfusionRecipeResolver.clearCache();
        InfusionCraftingMachine.clearCache();
    }

    private static void onDatapackSync(OnDatapackSyncEvent event) {
        event.sendRecipes(Ench.RecipeTypes.INFUSION);
    }

    private static void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == NepConfig.SPEC) {
            InfusionCraftingMachine.clearCache();
        }
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                AECapabilities.CRAFTING_MACHINE,
                BlockEntityType.ENCHANTING_TABLE,
                (be, side) -> be.getBlockState().is(Ench.Blocks.RAVEN_ENCHANTING_TABLE.value())
                        ? new InfusionCraftingMachine(be)
                        : null);
    }
}
