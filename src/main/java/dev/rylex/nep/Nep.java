package dev.rylex.nep;

import appeng.core.definitions.AEBlocks;
import dev.rylex.nep.client.NepClient;
import dev.rylex.nep.compat.actuallyadditions.ActuallyAdditionsCompat;
import dev.rylex.nep.compat.apothic.ApothicCompat;
import dev.rylex.nep.compat.ars.ArsCompat;
import dev.rylex.nep.compat.compactcrafting.CompactCraftingCompat;
import dev.rylex.nep.compat.create.CreateCompat;
import dev.rylex.nep.compat.draconic.DraconicCompat;
import dev.rylex.nep.compat.extendedae.ExtendedAeCompat;
import dev.rylex.nep.compat.malum.MalumCompat;
import dev.rylex.nep.compat.mysticalagriculture.MysticalAgricultureCompat;
import dev.rylex.nep.compat.occultism.OccultismCompat;
import dev.rylex.nep.compat.oritech.OritechCompat;
import dev.rylex.nep.decoder.DecoderContent;
import dev.rylex.nep.guide.NepRecipes;
import dev.rylex.nep.hub.NepContent;
import dev.rylex.nep.net.NepNetwork;
import dev.rylex.nep.provider.ImportCardUpgrades;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(Nep.MOD_ID)
public final class Nep {
    public static final String MOD_ID = "nep";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public Nep(IEventBus modBus, ModContainer container, Dist dist) {
        NepSlotSemantics.init();
        container.registerConfig(ModConfig.Type.SERVER, NepConfig.SPEC);
        modBus.addListener(ModConfigEvent.Loading.class, NepConfig::onConfigLoaded);
        modBus.addListener(ModConfigEvent.Reloading.class, NepConfig::onConfigLoaded);
        NepComponents.COMPONENTS.register(modBus);
        NepItems.ITEMS.register(modBus);
        NepCreativeTabs.TABS.register(modBus);
        NepContent.register(modBus);
        DecoderContent.register(modBus);
        NepRecipes.init(modBus);
        modBus.addListener(NepNetwork::registerPayloads);
        modBus.addListener(this::commonSetup);
        integrate("create", "Create", () -> CreateCompat.init(modBus, dist));
        integrate("draconicevolution", "Draconic Evolution", () -> DraconicCompat.init(modBus, dist));
        integrate("compactcrafting", "Compact Crafting", () -> CompactCraftingCompat.init(modBus, dist));
        integrate("apothic_enchanting", "Apothic Enchanting", () -> ApothicCompat.init(modBus));
        integrate("actuallyadditions", "Actually Additions", () -> ActuallyAdditionsCompat.init(modBus, dist));
        integrate("mysticalagriculture", "Mystical Agriculture", () -> MysticalAgricultureCompat.init(modBus, dist));
        integrate("malum", "Malum", () -> MalumCompat.init(modBus, dist));
        integrate("ars_nouveau", "Ars Nouveau", () -> ArsCompat.init(modBus, dist));
        integrate("occultism", "Occultism", () -> OccultismCompat.init(modBus, dist));
        integrate("oritech", "Oritech", () -> OritechCompat.init(modBus, dist));
        integrate("extendedae", "ExtendedAE", () -> ExtendedAeCompat.init(modBus, dist));
        if (dist.isClient()) {
            NepClient.init(container, modBus);
        }
        LOGGER.info("{} initialized", MOD_ID);
    }

    private static void integrate(String modId, String label, Runnable init) {
        if (ModList.get().isLoaded(modId)) {
            init.run();
            LOGGER.info("{} integration enabled", label);
        }
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            var providers = new ArrayList<Item>();
            providers.add(AEBlocks.PATTERN_PROVIDER.asItem());
            addProviders(providers, "advanced_ae", "adv_pattern_provider", "small_adv_pattern_provider");
            addProviders(providers, "extendedae", "ex_pattern_provider");
            addProviders(providers, "expandedae", "exp_pattern_provider");
            addProviders(providers, "megacells", "mega_pattern_provider");
            addProviders(providers, "appliedcreate", "andesite_pattern_provider", "brass_pattern_provider");
            addProviders(
                    providers,
                    "ae2_draconic_fusion_autocrafter",
                    "me_draconic_pattern_provider",
                    "me_draconic_pattern_provider_panel");
            ImportCardUpgrades.register(providers);
        });
    }

    private static void addProviders(List<Item> providers, String modId, String... itemPaths) {
        if (!ModList.get().isLoaded(modId)) {
            return;
        }
        for (var path : itemPaths) {
            BuiltInRegistries.ITEM
                    .getOptional(ResourceLocation.fromNamespaceAndPath(modId, path))
                    .ifPresent(providers::add);
        }
    }
}
