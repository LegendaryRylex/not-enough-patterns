package dev.rylex.nep.compat.actuallyadditions;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import de.ellpeck.actuallyadditions.mod.crafting.EmpowererRecipe;
import de.ellpeck.actuallyadditions.mod.crafting.LaserRecipe;
import dev.rylex.nep.pattern.encoding.EncodedIngredients;
import dev.rylex.nep.pattern.encoding.IngredientMatching;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

final class ActuallyAdditionsRecipeIngredients {
    private ActuallyAdditionsRecipeIngredients() {}

    @Nullable
    static EncodedIngredients empowering(EmpowererRecipe recipe) {
        GenericStack result = IngredientMatching.resultOf(recipe.getOutput());
        if (result == null) {
            return null;
        }

        List<List<GenericStack>> inputs = new ArrayList<>(5);
        for (Ingredient ingredient : List.of(
                recipe.getInput(),
                recipe.getStandOne(),
                recipe.getStandTwo(),
                recipe.getStandThree(),
                recipe.getStandFour())) {
            List<AEItemKey> options = IngredientMatching.itemOptions(ingredient);
            if (options.isEmpty()) {
                return null;
            }
            inputs.add(IngredientMatching.options(options, 1));
        }
        return new EncodedIngredients(List.copyOf(inputs), List.of(result));
    }

    @Nullable
    static EncodedIngredients laser(LaserRecipe recipe, Level level) {
        GenericStack result = IngredientMatching.resultOf(
                recipe.getResultItem(level.registryAccess()).copy());
        if (result == null) {
            return null;
        }
        List<AEItemKey> options = IngredientMatching.itemOptions(recipe.getInput());
        if (options.isEmpty()) {
            return null;
        }
        return new EncodedIngredients(List.of(IngredientMatching.options(options, 1)), List.of(result));
    }

    record Demand(Ingredient ingredient, int count) {}

    static List<Demand> empoweringDemand(EmpowererRecipe recipe) {
        return List.of(
                new Demand(recipe.getInput(), 1),
                new Demand(recipe.getStandOne(), 1),
                new Demand(recipe.getStandTwo(), 1),
                new Demand(recipe.getStandThree(), 1),
                new Demand(recipe.getStandFour(), 1));
    }

    static List<Demand> laserDemand(LaserRecipe recipe) {
        return List.of(new Demand(recipe.getInput(), 1));
    }
}
