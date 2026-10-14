package dev.rylex.nep.compat.occultism;

import appeng.api.AECapabilities;
import com.klikli_dev.occultism.registry.OccultismDataComponents;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.guide.NepModules;
import dev.rylex.nep.machine.IncidentalComponents;
import dev.rylex.nep.machine.MatrixCraftingMachine;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;

public final class OccultismCompat {
    private OccultismCompat() {}

    public static void init(IEventBus modBus, Dist dist) {
        NepModules.register("occultism", NepConfig::occultismOverride);
        NepModules.register("spirit_provider", NepConfig::occultismSpiritProvider);
        NepModules.register("bound_spirit_matrix", NepConfig::occultismMatrix);
        NepModules.register("ritual_controller", NepConfig::occultismRitualController);
        NepOccultismContent.register(modBus);
        OccultismPatternEncoders.register();
        modBus.addListener(OccultismCompat::registerCapabilities);
        modBus.addListener(OccultismCompat::commonSetup);
        NeoForge.EVENT_BUS.addListener(SpiritProviders::onJobResult);
        if (dist.isClient()) {
            OccultismClientCompat.init(modBus);
        }
    }

    private static void commonSetup(FMLCommonSetupEvent event) {
        IncidentalComponents.register(OccultismDataComponents.SPIRIT_NAME.get());
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                AECapabilities.CRAFTING_MACHINE,
                NepOccultismContent.SPIRIT_PROVIDER_BLOCK_ENTITY.get(),
                (be, side) -> new SpiritProviderCraftingMachine(be));
        event.registerBlockEntity(
                AECapabilities.CRAFTING_MACHINE,
                NepOccultismContent.MATRIX_BLOCK_ENTITY.get(),
                (be, side) -> new MatrixCraftingMachine(
                        be::readyForPatterns,
                        be::pushMatrixPattern,
                        NepOccultismContent.MATRIX_ITEM,
                        NepOccultismContent.MATRIX));
        event.registerBlockEntity(
                AECapabilities.IN_WORLD_GRID_NODE_HOST,
                NepOccultismContent.MATRIX_BLOCK_ENTITY.get(),
                (be, context) -> be.gridNodeHost());
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                NepOccultismContent.MATRIX_BLOCK_ENTITY.get(),
                (be, side) -> be.itemHandlerForSide());
        event.registerBlockEntity(
                AECapabilities.CRAFTING_MACHINE,
                NepOccultismContent.CONTROLLER_BLOCK_ENTITY.get(),
                (be, side) -> new RitualControllerCraftingMachine(be));
        event.registerBlockEntity(
                AECapabilities.IN_WORLD_GRID_NODE_HOST,
                NepOccultismContent.CONTROLLER_BLOCK_ENTITY.get(),
                (be, context) -> be.gridNodeHost());
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                NepOccultismContent.CONTROLLER_BLOCK_ENTITY.get(),
                (be, side) -> be.itemHandlerForSide());
    }
}
