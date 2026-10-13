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
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.init.recipes.OritechRecipe;

final class OritechRecipeIngredients {
    private OritechRecipeIngredients() {}

    static final int PARTICLES = 2;

    @Nullable
    static EncodedIngredients collision(OritechRecipe recipe, Level level) {
        if (recipe.itemInputs().size() != PARTICLES || recipe.itemResults().isEmpty()) {
            return null;
        }
        GenericStack result = IngredientMatching.resultOf(fullResult(recipe));
        if (result == null) {
            return null;
        }

        List<List<GenericStack>> inputs = new ArrayList<>(PARTICLES);
        for (Ingredient ingredient : recipe.itemInputs()) {
            List<AEItemKey> options = IngredientMatching.itemOptions(ingredient, level);
            if (options.isEmpty()) {
                return null;
            }
            inputs.add(IngredientMatching.options(options, 1));
        }
        return new EncodedIngredients(List.copyOf(inputs), List.of(result));
    }

    static ItemStack fullResult(OritechRecipe recipe) {
        return recipe.itemResults().isEmpty()
                ? ItemStack.EMPTY
                : recipe.itemResults().get(0).create();
    }

    @Nullable
    static List<ManualRequirement> manualRequirements(OritechRecipe recipe) {
        if (recipe.itemInputs().size() != PARTICLES) {
            return null;
        }
        List<ManualRequirement> requirements = new ArrayList<>(PARTICLES);
        for (Ingredient ingredient : recipe.itemInputs()) {
            requirements.add(new ManualRequirement(ingredient, 1, true));
        }
        return List.copyOf(requirements);
    }
}
