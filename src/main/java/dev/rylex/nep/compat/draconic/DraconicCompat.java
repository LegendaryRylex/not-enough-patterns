package dev.rylex.nep.compat.draconic;

import appeng.api.AECapabilities;
import com.brandon3055.brandonscore.capability.CapabilityOP;
import com.brandon3055.draconicevolution.blocks.tileentity.TileFusionCraftingCore;
import com.brandon3055.draconicevolution.init.DEContent;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.guide.NepModules;
import dev.rylex.nep.pattern.FusionCraftingPattern;
import dev.rylex.nep.provider.MachineImports;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

public final class DraconicCompat {
    private DraconicCompat() {}

    public static void init(IEventBus modBus, Dist dist) {
        NepModules.register("draconicevolution", NepConfig::draconicOverride);
        NepModules.register("fusion_crafting", NepConfig::draconicFusionCrafting);
        NepModules.register("fusion_matrix", NepConfig::draconicFusionMatrix);
        NepDraconicContent.register(modBus);
        DraconicPatternEncoders.register();
        FusionCraftingPattern.canonicalizeWith(FusionResults::canonical);
        FusionCraftingPattern.substituteInputsWith(FusionInputSubstitution::of);
        MachineImports.register(MachineImports.DRACONIC_FUSION_CORE, FusionCoreImport::create);
        modBus.addListener(DraconicCompat::registerCapabilities);
        NeoForge.EVENT_BUS.addListener(DraconicCompat::onReload);
        NeoForge.EVENT_BUS.addListener(DraconicCompat::onBlockBroken);
        if (dist.isClient()) {
            DraconicClientCompat.init(modBus);
        }
    }

    private static void onBlockBroken(BlockEvent.BreakEvent event) {
        if (event.getLevel().getBlockEntity(event.getPos()) instanceof TileFusionCraftingCore core) {
            FusionReclaimer.onCraftCancelled(core);
        }
    }

    private static void onReload(AddReloadListenerEvent event) {
        FusionRecipeResolver.clearCache();
        FusionCraftingMachine.clearCache();
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                AECapabilities.CRAFTING_MACHINE,
                DEContent.TILE_CRAFTING_CORE.get(),
                (be, side) -> new FusionCraftingMachine(be));
        event.registerBlockEntity(
                AECapabilities.CRAFTING_MACHINE,
                NepDraconicContent.MATRIX_BLOCK_ENTITY.get(),
                (be, side) -> new FusionMatrixCraftingMachine(be));
        event.registerBlockEntity(
                AECapabilities.IN_WORLD_GRID_NODE_HOST,
                NepDraconicContent.MATRIX_BLOCK_ENTITY.get(),
                (be, context) -> be.gridNodeHost());
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                NepDraconicContent.MATRIX_BLOCK_ENTITY.get(),
                (be, side) -> be.itemHandlerForSide());
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                NepDraconicContent.MATRIX_BLOCK_ENTITY.get(),
                (be, side) -> be.energyStorage());
        event.registerBlockEntity(
                CapabilityOP.BLOCK, NepDraconicContent.MATRIX_BLOCK_ENTITY.get(), (be, side) -> be.opStorage());
    }
}
