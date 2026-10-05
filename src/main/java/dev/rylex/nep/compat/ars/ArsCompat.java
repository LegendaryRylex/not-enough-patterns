package dev.rylex.nep.compat.ars;

import appeng.api.AECapabilities;
import com.hollingsworth.arsnouveau.setup.registry.BlockRegistry;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.guide.NepModules;
import dev.rylex.nep.pattern.ApparatusPattern;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ArsCompat {
    private ArsCompat() {}

    public static void init(IEventBus modBus, Dist dist) {
        NepModules.register("ars_nouveau", NepConfig::arsOverride);
        NepModules.register("enchanting_apparatus", NepConfig::arsEnchantingApparatus);
        NepModules.register("imbuement_chamber", NepConfig::arsImbuementChamber);
        NepModules.register("arcane_lectern", NepConfig::arsArcaneLectern);
        NepModules.register("ritual_conductor", NepConfig::arsRitualConductor);
        NepModules.register("arcane_enchanting_matrix", NepConfig::arsMatrix);
        NepArsContent.register(modBus);
        ArsPatternEncoders.register();
        ApparatusPattern.substituteInputsWith(ArmorUpgradeSubstitution::of);
        modBus.addListener(ArsCompat::registerCapabilities);
        modBus.addListener(ArsCompat::registerPayloads);
        NeoForge.EVENT_BUS.addListener(ArsCompat::onReload);
        if (dist.isClient()) {
            ArsClientCompat.init(modBus);
        }
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1")
                .playToClient(
                        RitualConductorState.TYPE,
                        RitualConductorState.STREAM_CODEC,
                        (payload, context) -> context.enqueueWork(() -> RitualConductorStateClient.handle(payload)))
                .playToServer(
                        SelectRitualPayload.TYPE,
                        SelectRitualPayload.STREAM_CODEC,
                        (payload, context) -> context.enqueueWork(() -> selectRitual(payload, context)));
    }

    private static void selectRitual(SelectRitualPayload payload, IPayloadContext context) {
        if (context.player().containerMenu instanceof RitualConductorMenu menu) {
            menu.selectRitual(payload.ritual().orElse(null));
        }
    }

    private static void onReload(AddReloadListenerEvent event) {
        ArsRecipeResolver.clearCache();
        EnchantingApparatusCraftingMachine.clearCache();
        ImbuementCraftingMachine.clearCache();
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                AECapabilities.CRAFTING_MACHINE,
                BlockRegistry.ENCHANTING_APP_TILE.get(),
                (be, side) -> new EnchantingApparatusCraftingMachine(be));
        event.registerBlockEntity(
                AECapabilities.CRAFTING_MACHINE,
                BlockRegistry.ARCANE_CORE_TILE.get(),
                (be, side) -> EnchantingApparatusCraftingMachine.forCore(be));
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                BlockRegistry.ARCANE_CORE_TILE.get(),
                (be, side) -> new ArcaneCoreItemHandler(be, side));
        event.registerBlockEntity(
                AECapabilities.CRAFTING_MACHINE,
                BlockRegistry.IMBUEMENT_TILE.get(),
                (be, side) -> new ImbuementCraftingMachine(be));
        event.registerBlockEntity(
                AECapabilities.CRAFTING_MACHINE,
                NepArsContent.MATRIX_BLOCK_ENTITY.get(),
                (be, side) -> new ArcaneEnchantingMatrixCraftingMachine(be));
        event.registerBlockEntity(
                AECapabilities.IN_WORLD_GRID_NODE_HOST,
                NepArsContent.MATRIX_BLOCK_ENTITY.get(),
                (be, context) -> be.gridNodeHost());
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                NepArsContent.MATRIX_BLOCK_ENTITY.get(),
                (be, side) -> be.itemHandlerForSide());
        event.registerBlockEntity(
                CapabilityRegistry.SOURCE_CAPABILITY,
                NepArsContent.MATRIX_BLOCK_ENTITY.get(),
                (be, side) -> be.sourceStorage());
        event.registerBlockEntity(
                AECapabilities.IN_WORLD_GRID_NODE_HOST,
                NepArsContent.ARCANE_LECTERN_BLOCK_ENTITY.get(),
                (be, context) -> be);
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                NepArsContent.ARCANE_LECTERN_BLOCK_ENTITY.get(),
                (be, side) -> be.itemHandler());
        event.registerBlockEntity(
                AECapabilities.IN_WORLD_GRID_NODE_HOST,
                NepArsContent.RITUAL_CONDUCTOR_BLOCK_ENTITY.get(),
                (be, context) -> be);
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                NepArsContent.RITUAL_CONDUCTOR_BLOCK_ENTITY.get(),
                (be, side) -> be.itemHandler());
    }
}
