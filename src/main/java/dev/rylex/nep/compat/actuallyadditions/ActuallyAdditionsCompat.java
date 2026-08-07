package dev.rylex.nep.compat.actuallyadditions;

import appeng.api.AECapabilities;
import de.ellpeck.actuallyadditions.mod.blocks.ActuallyBlocks;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.guide.NepModules;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

public final class ActuallyAdditionsCompat {
    private ActuallyAdditionsCompat() {}

    public static void init(IEventBus modBus, Dist dist) {
        NepModules.register("actuallyadditions", NepConfig::actuallyAdditionsOverride);
        NepModules.register("empowering", NepConfig::actuallyAdditionsEmpowering);
        NepModules.register("atomic_reconstruction", NepConfig::actuallyAdditionsAtomicReconstruction);
        NepModules.register("atomic_empowering_matrix", NepConfig::actuallyAdditionsMatrix);
        NepActuallyAdditionsContent.register(modBus);
        ActuallyAdditionsPatternEncoders.register();
        modBus.addListener(ActuallyAdditionsCompat::registerCapabilities);
        NeoForge.EVENT_BUS.addListener(ActuallyAdditionsCompat::onReload);
        if (dist.isClient()) {
            ActuallyAdditionsClientCompat.init(modBus);
        }
    }

    private static void onReload(AddReloadListenerEvent event) {
        ActuallyAdditionsRecipeResolver.clearCache();
        EmpowererCraftingMachine.clearCache();
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                AECapabilities.CRAFTING_MACHINE,
                ActuallyBlocks.EMPOWERER.getTileEntityType(),
                (be, side) -> new EmpowererCraftingMachine(be));
        event.registerBlockEntity(
                AECapabilities.CRAFTING_MACHINE,
                NepActuallyAdditionsContent.MATRIX_BLOCK_ENTITY.get(),
                (be, side) -> new AtomicEmpoweringMatrixCraftingMachine(be));
        event.registerBlockEntity(
                AECapabilities.IN_WORLD_GRID_NODE_HOST,
                NepActuallyAdditionsContent.MATRIX_BLOCK_ENTITY.get(),
                (be, context) -> be.gridNodeHost());
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                NepActuallyAdditionsContent.MATRIX_BLOCK_ENTITY.get(),
                (be, side) -> be.itemHandlerForSide());
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                NepActuallyAdditionsContent.MATRIX_BLOCK_ENTITY.get(),
                (be, side) -> be.energyStorage());
    }
}
