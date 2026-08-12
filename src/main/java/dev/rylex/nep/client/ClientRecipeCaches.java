package dev.rylex.nep.client;

import dev.rylex.nep.Nep;
import dev.rylex.nep.util.RecipeCache;
import dev.rylex.nep.util.Recipes;
import net.minecraft.world.item.crafting.RecipeMap;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RecipesReceivedEvent;

@EventBusSubscriber(modid = Nep.MOD_ID, value = Dist.CLIENT)
public final class ClientRecipeCaches {
    private ClientRecipeCaches() {}

    @SubscribeEvent
    static void onRecipesReceived(RecipesReceivedEvent event) {
        Recipes.setClientRecipes(event.getRecipeMap());
    }

    @SubscribeEvent
    static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        Recipes.setClientRecipes(RecipeMap.EMPTY);
        RecipeCache.clearClient();
    }
}
