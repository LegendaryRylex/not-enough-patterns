package dev.rylex.nep;

import appeng.api.upgrades.Upgrades;
import appeng.core.definitions.AEBlocks;
import dev.rylex.nep.client.NepClient;
import dev.rylex.nep.compat.apothic.ApothicCompat;
import dev.rylex.nep.compat.mysticalagriculture.MysticalAgricultureCompat;
import dev.rylex.nep.guide.NepRecipes;
import dev.rylex.nep.hub.NepContent;
import dev.rylex.nep.net.NepNetwork;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(Nep.MOD_ID)
public final class Nep {
    public static final String MOD_ID = "nep";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    public Nep(IEventBus modBus, ModContainer container, Dist dist) {
        NepSlotSemantics.init();
        container.registerConfig(ModConfig.Type.SERVER, NepConfig.SPEC);
        NepComponents.COMPONENTS.register(modBus);
        NepItems.ITEMS.register(modBus);
        NepCreativeTabs.TABS.register(modBus);
        NepContent.register(modBus);
        NepRecipes.init(modBus);
        modBus.addListener(NepNetwork::registerPayloads);
        modBus.addListener(this::commonSetup);
        if (ModList.get().isLoaded("apothic_enchanting")) {
            ApothicCompat.init(modBus);
            LOGGER.info("Apothic Enchanting integration enabled");
        }
        if (ModList.get().isLoaded("mysticalagriculture")) {
            MysticalAgricultureCompat.init(modBus, dist);
            LOGGER.info("Mystical Agriculture integration enabled");
        }
        if (dist.isClient()) {
            NepClient.init(container, modBus);
        }
        LOGGER.info("{} initialized", MOD_ID);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            Upgrades.add(NepItems.IMPORT_CARD, AEBlocks.PATTERN_PROVIDER.block(), 1);
            registerProviderUpgrade("advanced_ae", "adv_pattern_provider", "small_adv_pattern_provider");
            registerProviderUpgrade("extendedae", "ex_pattern_provider");
            registerProviderUpgrade("megacells", "mega_pattern_provider");
        });
    }

    private static void registerProviderUpgrade(String modId, String... itemPaths) {
        if (!ModList.get().isLoaded(modId)) {
            return;
        }
        for (var path : itemPaths) {
            BuiltInRegistries.ITEM
                    .getOptional(Identifier.fromNamespaceAndPath(modId, path))
                    .ifPresent(item -> Upgrades.add(NepItems.IMPORT_CARD, item, 1));
        }
    }
}
