package dev.rylex.nep.client;

import dev.rylex.nep.Nep;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddPackFindersEvent;

@EventBusSubscriber(modid = Nep.MOD_ID, value = Dist.CLIENT)
public final class NepGuidePacks {
    private static final List<String> GUIDED_MODS =
            List.of("mysticalagriculture", "apothic_enchanting", "occultism", "oritech");

    private NepGuidePacks() {}

    @SubscribeEvent
    static void addPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.CLIENT_RESOURCES) {
            return;
        }
        for (String modId : GUIDED_MODS) {
            hidePagesWithout(event, modId);
        }
    }

    /**
     * GuideMe has no conditional frontmatter and no page filter API, so pages stay in {@code assets/nep/ae2guide} where
     * the dev live preview can see them and a filter-only pack takes them back out.
     */
    private static void hidePagesWithout(AddPackFindersEvent event, String modId) {
        if (ModList.get().isLoaded(modId)) {
            return;
        }
        event.addPackFinders(
                Identifier.fromNamespaceAndPath(Nep.MOD_ID, "guide_filters/no_" + modId),
                PackType.CLIENT_RESOURCES,
                Component.literal("nep guide pages for " + modId),
                PackSource.BUILT_IN,
                true,
                Pack.Position.TOP);
    }
}
