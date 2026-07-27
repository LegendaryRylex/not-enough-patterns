package dev.rylex.nep.compat.jei;

import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;

public interface JeiTransferSource {

    void addTransfers(TransferCollector collector);

    default void addCatalysts(IRecipeCatalystRegistration registration) {}

    interface TransferCollector {
        <R extends Recipe<?>> void add(
                RecipeType<RecipeHolder<R>> recipeType, PatternTransferHandler.Extractor<R> extractor);
    }
}
