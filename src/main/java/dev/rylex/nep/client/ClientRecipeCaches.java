package dev.rylex.nep.client;

import dev.rylex.nep.Nep;
import dev.rylex.nep.util.RecipeCache;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RecipesUpdatedEvent;

@EventBusSubscriber(modid = Nep.MOD_ID, value = Dist.CLIENT)
public final class ClientRecipeCaches {
    private ClientRecipeCaches() {}

    @SubscribeEvent
    static void onRecipesUpdated(RecipesUpdatedEvent event) {
        RecipeCache.clearClient();
    }

    @SubscribeEvent
    static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        RecipeCache.clearClient();
    }
}
