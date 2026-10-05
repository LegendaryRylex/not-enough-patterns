package dev.rylex.nep.compat.malum;

import appeng.api.AECapabilities;
import com.sammy.malum.registry.common.block.MalumBlockEntities;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.guide.NepModules;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

public final class MalumCompat {
    private MalumCompat() {}

    public static void init(IEventBus modBus, Dist dist) {
        NepModules.register("malum", NepConfig::malumOverride);
        NepModules.register("spirit_infusion", NepConfig::malumSpiritInfusion);
        NepModules.register("spirit_focusing", NepConfig::malumSpiritFocusing);
        NepModules.register("runeworking", NepConfig::malumRuneworking);
        NepModules.register("focused_spirit_matrix", NepConfig::malumFocusedSpiritMatrix);
        NepSpirits.register(modBus);
        NepMalumContent.register(modBus);
        NepRites.register(modBus);
        MalumPatternEncoders.register();
        modBus.addListener(MalumCompat::registerCapabilities);
        NeoForge.EVENT_BUS.addListener(MalumCompat::onReload);
        if (dist.isClient()) {
            MalumClientCompat.init(modBus);
        }
    }

    private static void onReload(AddReloadListenerEvent event) {
        SpiritInfusionResolver.clearCache();
        SpiritFocusingResolver.clearCache();
        RuneworkingResolver.clearCache();
        SpiritAltarCraftingMachine.clearCache();
        SpiritCrucibleCraftingMachine.clearCache();
        RunicWorkbenchCraftingMachine.clearCache();
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                AECapabilities.CRAFTING_MACHINE,
                MalumBlockEntities.SPIRIT_ALTAR.get(),
                (be, side) -> new SpiritAltarCraftingMachine(be));
        event.registerBlockEntity(
                AECapabilities.CRAFTING_MACHINE,
                MalumBlockEntities.SPIRIT_CRUCIBLE.get(),
                (be, side) -> new SpiritCrucibleCraftingMachine(be));
        event.registerBlockEntity(
                AECapabilities.CRAFTING_MACHINE,
                MalumBlockEntities.RUNIC_WORKBENCH.get(),
                (be, side) -> new RunicWorkbenchCraftingMachine(be));
        event.registerBlockEntity(
                AECapabilities.CRAFTING_MACHINE,
                NepMalumContent.MATRIX_BLOCK_ENTITY.get(),
                (be, side) -> new FocusedSpiritMatrixCraftingMachine(be));
        event.registerBlockEntity(
                AECapabilities.IN_WORLD_GRID_NODE_HOST,
                NepMalumContent.MATRIX_BLOCK_ENTITY.get(),
                (be, context) -> be.gridNodeHost());
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                NepMalumContent.MATRIX_BLOCK_ENTITY.get(),
                (be, side) -> be.itemHandlerForSide());
    }
}
