package dev.rylex.nep.compat.mysticalagriculture;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import com.blakebr0.mysticalagriculture.api.crafting.IAwakeningRecipe;
import com.blakebr0.mysticalagriculture.api.crafting.IInfusionRecipe;
import dev.rylex.nep.machine.ManualRequirement;
import dev.rylex.nep.pattern.encoding.EncodedIngredients;
import dev.rylex.nep.pattern.encoding.IngredientMatching;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import org.jetbrains.annotations.Nullable;

final class MysticalRecipeIngredients {
    private MysticalRecipeIngredients() {}

    static final int INFUSION_PEDESTALS = 8;
    static final int AWAKENING_PEDESTALS = 4;
    static final int AWAKENING_VESSELS = 4;

    @Nullable
    static EncodedIngredients infusion(IInfusionRecipe recipe, Level level) {
        GenericStack result = IngredientMatching.resultOf(MysticalRecipeResolver.resultOf(recipe));
        if (result == null) {
            return null;
        }
        List<List<GenericStack>> inputs = new ArrayList<>();
        if (!addSingle(inputs, recipe.getAltarIngredient(), level)) {
            return null;
        }
        for (Ingredient ingredient : recipe.getPedestalIngredients()) {
            if (ingredient.isEmpty()) {
                continue;
            }
            if (!addSingle(inputs, ingredient, level)) {
                return null;
            }
        }
        if (inputs.size() < 2 || inputs.size() > INFUSION_PEDESTALS + 1) {
            return null;
        }
        return new EncodedIngredients(List.copyOf(inputs), List.of(result));
    }

    @Nullable
    static EncodedIngredients awakening(IAwakeningRecipe recipe, Level level) {
        GenericStack result = IngredientMatching.resultOf(MysticalRecipeResolver.resultOf(recipe));
        if (result == null) {
            return null;
        }
        List<Ingredient> pedestals = pedestalIngredients(recipe);
        List<SizedIngredient> essences = recipe.getEssenceIngredients();
        if (pedestals.size() != AWAKENING_PEDESTALS || essences.size() != AWAKENING_VESSELS) {
            return null;
        }

        List<List<GenericStack>> inputs = new ArrayList<>();
        if (!addSingle(inputs, recipe.getAltarIngredient(), level)) {
            return null;
        }
        for (Ingredient ingredient : pedestals) {
            if (!addSingle(inputs, ingredient, level)) {
                return null;
            }
        }
        for (SizedIngredient essence : essences) {
            List<AEItemKey> options = IngredientMatching.itemOptions(essence.ingredient(), level);
            if (options.isEmpty() || essence.count() <= 0) {
                return null;
            }
            inputs.add(IngredientMatching.options(options, essence.count()));
        }
        return new EncodedIngredients(List.copyOf(inputs), List.of(result));
    }

    static List<Ingredient> pedestalIngredients(IAwakeningRecipe recipe) {
        List<Ingredient> pedestals = new ArrayList<>(AWAKENING_PEDESTALS);
        for (Ingredient ingredient : recipe.getPedestalIngredients()) {
            if (!ingredient.isEmpty()) {
                pedestals.add(ingredient);
            }
        }
        return List.copyOf(pedestals);
    }

    static List<Ingredient> infusionDemands(IInfusionRecipe recipe) {
        List<Ingredient> demands = new ArrayList<>();
        demands.add(recipe.getAltarIngredient());
        for (Ingredient ingredient : recipe.getPedestalIngredients()) {
            if (!ingredient.isEmpty()) {
                demands.add(ingredient);
            }
        }
        return List.copyOf(demands);
    }

    static List<Ingredient> awakeningDemands(IAwakeningRecipe recipe) {
        List<Ingredient> demands = new ArrayList<>();
        demands.add(recipe.getAltarIngredient());
        demands.addAll(pedestalIngredients(recipe));
        return List.copyOf(demands);
    }

    static List<ManualRequirement> manualInfusionRequirements(IInfusionRecipe recipe) {
        return requirementsOf(infusionDemands(recipe), List.of());
    }

    static List<ManualRequirement> manualAwakeningRequirements(IAwakeningRecipe recipe) {
        return requirementsOf(awakeningDemands(recipe), recipe.getEssenceIngredients());
    }

    private static List<ManualRequirement> requirementsOf(List<Ingredient> demands, List<SizedIngredient> essences) {
        List<ManualRequirement> requirements = new ArrayList<>(demands.size() + essences.size());
        for (Ingredient demand : demands) {
            requirements.add(new ManualRequirement(demand, 1, true));
        }
        for (SizedIngredient essence : essences) {
            if (essence.count() > 0) {
                requirements.add(new ManualRequirement(essence.ingredient(), essence.count(), true));
            }
        }
        return List.copyOf(requirements);
    }

    private static boolean addSingle(List<List<GenericStack>> inputs, Ingredient ingredient, Level level) {
        List<AEItemKey> options = IngredientMatching.itemOptions(ingredient, level);
        if (options.isEmpty()) {
            return false;
        }
        inputs.add(IngredientMatching.options(options, 1));
        return true;
    }
}
