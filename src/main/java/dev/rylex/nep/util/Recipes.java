package dev.rylex.nep.util;

import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Recipe lookup for both logical sides.
 *
 * <p>A client no longer holds the loaded recipes: only the types a mod asked for through {@code OnDatapackSyncEvent}
 * reach it, and they arrive as a standalone {@link RecipeMap} rather than through the level. The two sides therefore
 * resolve differently, and every nep lookup goes through here so a caller never has to know which side it is on. Every
 * type nep reads client-side must be requested from the server, or the client sees no candidates at all.
 *
 * <p>A sync replaces the client map wholesale and a datapack reload replaces the server's, so the map instance itself
 * identifies a generation of recipes: {@link RecipeCache} keys on it and needs no invalidation event.
 */
public final class Recipes {
    private Recipes() {}

    private static volatile RecipeMap clientRecipes = RecipeMap.EMPTY;

    public static void setClientRecipes(RecipeMap recipes) {
        clientRecipes = recipes;
    }

    public static RecipeMap of(Level level) {
        return level instanceof ServerLevel server ? server.recipeAccess().recipeMap() : clientRecipes;
    }

    public static <I extends RecipeInput, T extends Recipe<I>> List<RecipeHolder<T>> allOf(
            Level level, RecipeType<T> type) {
        return List.copyOf(of(level).byType(type));
    }

    @Nullable
    public static RecipeHolder<?> byId(Level level, Identifier recipe) {
        return of(level).byKey(key(recipe));
    }

    public static ResourceKey<Recipe<?>> key(Identifier recipe) {
        return ResourceKey.create(Registries.RECIPE, recipe);
    }

    public static Identifier idOf(RecipeHolder<?> holder) {
        return holder.id().identifier();
    }
}
