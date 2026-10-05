package dev.rylex.nep.compat.compactcrafting;

import dev.compactmods.crafting.api.components.IRecipeBlockComponent;
import dev.compactmods.crafting.recipes.MiniaturizationRecipe;

final class MiniaturizationComponents {
    private MiniaturizationComponents() {}

    static boolean isEmpty(MiniaturizationRecipe recipe, String component) {
        if (recipe.getComponents().isEmptyBlock(component)) {
            return true;
        }
        IRecipeBlockComponent block = recipe.getComponents().getBlock(component).orElse(null);
        return block != null && block.getBlock().defaultBlockState().isAir();
    }
}
