package dev.rylex.nep.compat.mysticalagriculture;

import appeng.api.AECapabilities;
import com.blakebr0.mysticalagriculture.init.ModTileEntities;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.guide.NepModules;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;

public final class MysticalAgricultureCompat {
    private MysticalAgricultureCompat() {}

    public static void init(IEventBus modBus, Dist dist) {
        NepModules.register("mysticalagriculture", NepConfig::mysticalOverride);
        NepModules.register("infusion_altar", NepConfig::mysticalInfusion);
        NepModules.register("awakening_altar", NepConfig::mysticalAwakening);
        NepModules.register("infused_awakening_matrix", NepConfig::mysticalInfusedAwakeningMatrix);
        NepMysticalContent.register(modBus);
        MysticalPatternEncoders.register();
        modBus.addListener(MysticalAgricultureCompat::registerCapabilities);
        NeoForge.EVENT_BUS.addListener(MysticalAgricultureCompat::onReload);
        NeoForge.EVENT_BUS.addListener(MysticalAgricultureCompat::onDatapackSync);
        if (dist.isClient()) {
            MysticalClientCompat.init(modBus);
        }
    }

    private static void onDatapackSync(OnDatapackSyncEvent event) {
        event.sendRecipes(MysticalRecipeResolver.infusionType(), MysticalRecipeResolver.awakeningType());
    }

    private static void onReload(AddServerReloadListenersEvent event) {
        MysticalRecipeResolver.clearCache();
        InfusionAltarCraftingMachine.clearCache();
        AwakeningAltarCraftingMachine.clearCache();
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                AECapabilities.CRAFTING_MACHINE,
                ModTileEntities.INFUSION_ALTAR.get(),
                (be, side) -> new InfusionAltarCraftingMachine(be));
        event.registerBlockEntity(
                AECapabilities.CRAFTING_MACHINE,
                ModTileEntities.AWAKENING_ALTAR.get(),
                (be, side) -> new AwakeningAltarCraftingMachine(be));
        event.registerBlockEntity(
                AECapabilities.CRAFTING_MACHINE,
                NepMysticalContent.MATRIX_BLOCK_ENTITY.get(),
                (be, side) -> new InfusedAwakeningMatrixCraftingMachine(be));
        event.registerBlockEntity(
                AECapabilities.IN_WORLD_GRID_NODE_HOST,
                NepMysticalContent.MATRIX_BLOCK_ENTITY.get(),
                (be, context) -> be.gridNodeHost());
        event.registerBlockEntity(
                Capabilities.Item.BLOCK,
                NepMysticalContent.MATRIX_BLOCK_ENTITY.get(),
                (be, side) -> be.itemHandlerForSide());
    }
}
