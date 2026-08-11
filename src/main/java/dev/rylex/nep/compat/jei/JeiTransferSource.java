package dev.rylex.nep.compat.jei;

import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;

public interface JeiTransferSource {

    void addTransfers(TransferCollector collector);

    default void addCatalysts(IRecipeCatalystRegistration registration) {}

    interface TransferCollector {
        <R extends Recipe<?>> void add(
                IRecipeType<RecipeHolder<R>> recipeType, PatternTransferHandler.Extractor<RecipeHolder<R>> extractor);

        /** For categories JEI exposes as the bare recipe rather than its {@link RecipeHolder}. */
        <T> void addUnwrapped(
                IRecipeType<T> recipeType,
                PatternTransferHandler.Extractor<T> extractor,
                PatternTransferHandler.RecipeId<T> identifier);
    }
}
