package dev.rylex.nep;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddPackFindersEvent;

@EventBusSubscriber(modid = Nep.MOD_ID)
public final class NepDataPacks {
    private NepDataPacks() {}

    @SubscribeEvent
    static void addPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.SERVER_DATA) {
            return;
        }
        hideDataWithout(event, "malum", "no_malum");
    }

    /** NeoForge warns about a data map file for an unregistered type before it reads the file's conditions. */
    private static void hideDataWithout(AddPackFindersEvent event, String modId, String pack) {
        if (ModList.get().isLoaded(modId)) {
            return;
        }
        event.addPackFinders(
                ResourceLocation.fromNamespaceAndPath(Nep.MOD_ID, "data_filters/" + pack),
                PackType.SERVER_DATA,
                Component.literal("nep data for " + modId),
                PackSource.BUILT_IN,
                true,
                Pack.Position.TOP);
    }
}
