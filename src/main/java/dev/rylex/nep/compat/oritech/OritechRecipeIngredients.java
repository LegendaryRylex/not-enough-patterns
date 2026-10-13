package dev.rylex.nep.compat.oritech;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import dev.rylex.nep.machine.ManualRequirement;
import dev.rylex.nep.pattern.encoding.EncodedIngredients;
import dev.rylex.nep.pattern.encoding.IngredientMatching;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.init.recipes.OritechRecipe;

final class OritechRecipeIngredients {
    private OritechRecipeIngredients() {}

    static final int PARTICLES = 2;

    /**
     * The result is always a single item, because the 1.21.1 accelerator grows its output slot by one per collision
     * whatever count the recipe names.
     */
    @Nullable
    static EncodedIngredients collision(OritechRecipe recipe) {
        if (recipe.getInputs().size() != PARTICLES || recipe.getResults().isEmpty()) {
            return null;
        }
        GenericStack result =
                IngredientMatching.resultOf(recipe.getResults().get(0).copyWithCount(1));
        if (result == null) {
            return null;
        }

        List<List<GenericStack>> inputs = new ArrayList<>(PARTICLES);
        for (Ingredient ingredient : recipe.getInputs()) {
            List<AEItemKey> options = IngredientMatching.itemOptions(ingredient);
            if (options.isEmpty()) {
                return null;
            }
            inputs.add(IngredientMatching.options(options, 1));
        }
        return new EncodedIngredients(List.copyOf(inputs), List.of(result));
    }

    static ItemStack fullResult(OritechRecipe recipe) {
        return recipe.getResults().isEmpty()
                ? ItemStack.EMPTY
                : recipe.getResults().get(0).copy();
    }

    @Nullable
    static List<ManualRequirement> manualRequirements(OritechRecipe recipe) {
        if (recipe.getInputs().size() != PARTICLES) {
            return null;
        }
        List<ManualRequirement> requirements = new ArrayList<>(PARTICLES);
        for (Ingredient ingredient : recipe.getInputs()) {
            requirements.add(new ManualRequirement(ingredient, 1, true));
        }
        return List.copyOf(requirements);
    }
}
