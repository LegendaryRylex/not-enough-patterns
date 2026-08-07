package dev.rylex.nep.compat.compactcrafting;

import appeng.api.AECapabilities;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.guide.NepModules;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

public final class CompactCraftingCompat {
    private CompactCraftingCompat() {}

    public static void init(IEventBus modBus, Dist dist) {
        NepModules.register("compactcrafting", NepConfig::compactCraftingOverride);
        NepModules.register("miniaturization_matrix", NepConfig::compactCraftingMiniaturizationMatrix);
        NepModules.register("miniaturization_controller", NepConfig::compactCraftingMiniaturizationController);
        NepCompactCraftingContent.register(modBus);
        CompactCraftingPatternEncoders.register();
        modBus.addListener(CompactCraftingCompat::registerCapabilities);
        NeoForge.EVENT_BUS.addListener(CompactCraftingCompat::onReload);
        if (dist.isClient()) {
            CompactCraftingClientCompat.init(modBus);
        }
    }

    private static void onReload(AddReloadListenerEvent event) {
        MiniaturizationRecipeResolver.clearCache();
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                AECapabilities.CRAFTING_MACHINE,
                NepCompactCraftingContent.MATRIX_BLOCK_ENTITY.get(),
                (be, side) -> new MiniaturizationMatrixCraftingMachine(be));
        event.registerBlockEntity(
                AECapabilities.IN_WORLD_GRID_NODE_HOST,
                NepCompactCraftingContent.MATRIX_BLOCK_ENTITY.get(),
                (be, context) -> be.gridNodeHost());
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                NepCompactCraftingContent.MATRIX_BLOCK_ENTITY.get(),
                (be, side) -> be.itemHandlerForSide());
        event.registerBlockEntity(
                AECapabilities.CRAFTING_MACHINE,
                NepCompactCraftingContent.CONTROLLER_BLOCK_ENTITY.get(),
                (be, side) -> new MiniaturizationControllerCraftingMachine(be));
        event.registerBlockEntity(
                AECapabilities.IN_WORLD_GRID_NODE_HOST,
                NepCompactCraftingContent.CONTROLLER_BLOCK_ENTITY.get(),
                (be, context) -> be.gridNodeHost());
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                NepCompactCraftingContent.CONTROLLER_BLOCK_ENTITY.get(),
                (be, side) -> be.itemHandlerForSide());
    }
}
