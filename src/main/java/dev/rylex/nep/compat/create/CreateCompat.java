package dev.rylex.nep.compat.create;

import appeng.api.AECapabilities;
import com.simibubi.create.AllBlockEntityTypes;
import dev.rylex.nep.Nep;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.guide.NepModules;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class CreateCompat {
    private CreateCompat() {}

    public static void init(IEventBus modBus, Dist dist) {
        NepModules.register("create", NepConfig::createOverride);
        NepModules.register("mechanical_crafting", NepConfig::createMechanicalCrafting);
        NepModules.register("deploying", NepConfig::createDeploying);
        NepModules.register("filling", NepConfig::createFilling);
        NepModules.register("sequenced_assembly", NepConfig::createSequencedAssembly);
        NepModules.register("sequenced_assembly_matrix", NepConfig::createSequencedAssemblyMatrix);
        NepCreateContent.register(modBus);
        CreatePatternEncoders.register();
        modBus.addListener(CreateCompat::registerCapabilities);
        modBus.addListener(CreateCompat::registerPayloads);
        NeoForge.EVENT_BUS.addListener(PatternEncodingHandler::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(LogStripping::onDeployerRecipeSearch);
        NeoForge.EVENT_BUS.addListener(CreateCompat::onReload);
        NeoForge.EVENT_BUS.addListener(CreateCompat::onTagsUpdated);
        NeoForge.EVENT_BUS.addListener(CreateCompat::onServerStarted);
        if (dist.isClient()) {
            SequencedAssemblyClientCompat.init(modBus);
        }
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1")
                .playToClient(
                        SequencedAssemblyState.TYPE,
                        SequencedAssemblyState.STREAM_CODEC,
                        (payload, context) -> context.enqueueWork(() -> SeqAssemblyStateClient.handle(payload)));
    }

    private static void onServerStarted(ServerStartedEvent event) {
        int configured = NepConfig.createSequencedAssemblyMatrixMinimumSpeed();
        float peakSpeed = SequencedAssemblyMatrixBlockEntity.peakSpeed();
        if (configured > peakSpeed) {
            Nep.LOGGER.warn(
                    "Assembly Matrix minimumSpeed ({} RPM) is above Create's maxRotationSpeed ({} RPM), which"
                            + " no shaft can reach; treating {} RPM as the threshold so the Matrix stays usable.",
                    configured,
                    Math.round(peakSpeed),
                    Math.round(peakSpeed));
        }
    }

    private static void onReload(AddReloadListenerEvent event) {
        MechanicalRecipeResolver.clearCache();
        ApplicationRecipeResolver.clearCache();
        FillingRecipeResolver.clearCache();
        SequencedAssemblyResolver.clearCache();
        DepotCraftingMachine.clearCache();
        LogStripping.clearCache();
        SandPaperPolishing.clearCache();
    }

    private static void onTagsUpdated(TagsUpdatedEvent event) {
        LogStripping.clearCache();
        SandPaperPolishing.clearCache();
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                AECapabilities.CRAFTING_MACHINE,
                AllBlockEntityTypes.MECHANICAL_CRAFTER.get(),
                (be, side) -> new MechanicalCrafterCraftingMachine(be));
        event.registerBlockEntity(
                AECapabilities.CRAFTING_MACHINE,
                AllBlockEntityTypes.DEPOT.get(),
                (be, side) -> new DepotCraftingMachine(be));
        event.registerBlockEntity(
                AECapabilities.CRAFTING_MACHINE,
                NepCreateContent.CONTROLLER_BLOCK_ENTITY.get(),
                (be, side) -> new SequencedAssemblyCraftingMachine(be));
        event.registerBlockEntity(
                AECapabilities.IN_WORLD_GRID_NODE_HOST,
                NepCreateContent.CONTROLLER_BLOCK_ENTITY.get(),
                (be, context) -> be.gridNodeHost());
        event.registerBlockEntity(
                AECapabilities.CRAFTING_MACHINE,
                NepCreateContent.MATRIX_BLOCK_ENTITY.get(),
                (be, side) -> new SequencedAssemblyMatrixCraftingMachine(be));
        event.registerBlockEntity(
                AECapabilities.IN_WORLD_GRID_NODE_HOST,
                NepCreateContent.MATRIX_BLOCK_ENTITY.get(),
                (be, context) -> be.gridNodeHost());
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                NepCreateContent.CONTROLLER_BLOCK_ENTITY.get(),
                (be, side) -> be.itemHandlerForSide());
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                NepCreateContent.MATRIX_BLOCK_ENTITY.get(),
                (be, side) -> be.itemHandlerForSide());
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                NepCreateContent.MATRIX_BLOCK_ENTITY.get(),
                (be, side) -> be.fluidHandler());
        if (SequencedAssemblyMatrixBlockEntity.NEW_AGE_LOADED) {
            event.registerBlockEntity(
                    Capabilities.EnergyStorage.BLOCK,
                    NepCreateContent.MATRIX_BLOCK_ENTITY.get(),
                    (be, side) -> be.energyStorage());
        }
    }
}
