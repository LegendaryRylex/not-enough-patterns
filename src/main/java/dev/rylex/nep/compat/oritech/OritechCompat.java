package dev.rylex.nep.compat.oritech;

import appeng.api.AECapabilities;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.guide.NepModules;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;

public final class OritechCompat {
    private OritechCompat() {}

    public static void init(IEventBus modBus, Dist dist) {
        NepModules.register("oritech", NepConfig::oritechOverride);
        NepModules.register("particle_collision", NepConfig::oritechParticleCollision);
        NepModules.register("particle_collision_matrix", NepConfig::oritechParticleCollisionMatrix);
        NepModules.register("particle_collision_controller", NepConfig::oritechParticleCollisionController);
        NepOritechContent.register(modBus);
        OritechPatternEncoders.register();
        modBus.addListener(OritechCompat::registerCapabilities);
        NeoForge.EVENT_BUS.addListener(OritechCompat::onReload);
        if (dist.isClient()) {
            OritechClientCompat.init(modBus);
        }
    }

    private static void onReload(AddServerReloadListenersEvent event) {
        OritechRecipeResolver.clearCache();
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                AECapabilities.CRAFTING_MACHINE,
                NepOritechContent.MATRIX_BLOCK_ENTITY.get(),
                (be, side) -> new ParticleCollisionMatrixCraftingMachine(be));
        event.registerBlockEntity(
                AECapabilities.IN_WORLD_GRID_NODE_HOST,
                NepOritechContent.MATRIX_BLOCK_ENTITY.get(),
                (be, context) -> be.gridNodeHost());
        event.registerBlockEntity(
                Capabilities.Item.BLOCK,
                NepOritechContent.MATRIX_BLOCK_ENTITY.get(),
                (be, side) -> be.itemHandlerForSide());
        event.registerBlockEntity(
                Capabilities.Energy.BLOCK,
                NepOritechContent.MATRIX_BLOCK_ENTITY.get(),
                (be, side) -> be.energyStorage());
        event.registerBlockEntity(
                AECapabilities.CRAFTING_MACHINE,
                NepOritechContent.CONTROLLER_BLOCK_ENTITY.get(),
                (be, side) -> new ParticleCollisionControllerCraftingMachine(be));
        event.registerBlockEntity(
                AECapabilities.IN_WORLD_GRID_NODE_HOST,
                NepOritechContent.CONTROLLER_BLOCK_ENTITY.get(),
                (be, context) -> be.gridNodeHost());
        event.registerBlockEntity(
                Capabilities.Item.BLOCK,
                NepOritechContent.CONTROLLER_BLOCK_ENTITY.get(),
                (be, side) -> be.itemHandlerForSide());
    }
}
