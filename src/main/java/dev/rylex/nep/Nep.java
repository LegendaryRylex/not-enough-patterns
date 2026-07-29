package dev.rylex.nep;

import appeng.api.upgrades.Upgrades;
import appeng.core.definitions.AEBlocks;
import dev.rylex.nep.client.NepClient;
import dev.rylex.nep.compat.create.CreateCompat;
import dev.rylex.nep.guide.NepRecipes;
import dev.rylex.nep.net.NepNetwork;
import net.minecraft.resources.ResourceLocation;
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
        NepRecipes.init(modBus);
        modBus.addListener(NepNetwork::registerPayloads);
        modBus.addListener(this::commonSetup);
        if (ModList.get().isLoaded("create")) {
            CreateCompat.init(modBus, dist);
            LOGGER.info("Create integration enabled");
        }
        if (dist.isClient()) {
            NepClient.init(container);
        }
        LOGGER.info("{} initialized", MOD_ID);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> Upgrades.add(NepItems.IMPORT_CARD, AEBlocks.PATTERN_PROVIDER.block(), 1));
    }
}
