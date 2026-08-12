package dev.rylex.nep.compat.jei;

import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IIngredientAliasRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;

public interface JeiTransferSource {

    void addTransfers(TransferCollector collector);

    default void addCatalysts(IRecipeCatalystRegistration registration) {}

    default void addRecipes(IRecipeRegistration registration, Level level) {}

    default void addAliases(IIngredientAliasRegistration registration) {}

    interface TransferCollector {
        <R extends Recipe<?>> void add(
                RecipeType<RecipeHolder<R>> recipeType, PatternTransferHandler.Extractor<RecipeHolder<R>> extractor);

        <T> void addUnwrapped(
                RecipeType<T> recipeType,
                PatternTransferHandler.Extractor<T> extractor,
                PatternTransferHandler.Identifier<T> identifier);
    }
}
