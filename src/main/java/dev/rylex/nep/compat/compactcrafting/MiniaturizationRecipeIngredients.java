package dev.rylex.nep.compat.compactcrafting;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import dev.compactmods.crafting.api.components.IRecipeBlockComponent;
import dev.compactmods.crafting.recipes.MiniaturizationRecipe;
import dev.rylex.nep.machine.ManualRequirement;
import dev.rylex.nep.pattern.encoding.EncodedIngredients;
import dev.rylex.nep.pattern.encoding.IngredientMatching;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

final class MiniaturizationRecipeIngredients {
    private MiniaturizationRecipeIngredients() {}

    record Demand(Ingredient ingredient, int count) {}

    @Nullable
    static EncodedIngredients miniaturization(RecipeHolder<MiniaturizationRecipe> holder, Level level) {
        return miniaturization(holder.value(), level);
    }

    @Nullable
    static EncodedIngredients miniaturization(MiniaturizationRecipe recipe, Level level) {
        GenericStack result = singleOutput(recipe);
        Map<Item, Integer> costs = itemCosts(recipe);
        if (result == null || costs == null) {
            return null;
        }
        List<List<GenericStack>> inputs = new ArrayList<>(costs.size());
        for (Map.Entry<Item, Integer> entry : costs.entrySet()) {
            AEItemKey key = AEItemKey.of(new ItemStack(entry.getKey()));
            if (key == null) {
                return null;
            }
            inputs.add(IngredientMatching.options(List.of(key), entry.getValue()));
        }
        return new EncodedIngredients(List.copyOf(inputs), List.of(result));
    }

    @Nullable
    static List<Demand> demandOf(MiniaturizationRecipe recipe) {
        Map<Item, Integer> costs = itemCosts(recipe);
        if (costs == null) {
            return null;
        }
        List<Demand> demands = new ArrayList<>(costs.size());
        costs.forEach((item, count) -> demands.add(new Demand(Ingredient.of(item), count)));
        return List.copyOf(demands);
    }

    @Nullable
    static List<ManualRequirement> manualRequirements(MiniaturizationRecipe recipe) {
        List<Demand> demands = demandOf(recipe);
        if (demands == null) {
            return null;
        }
        List<ManualRequirement> requirements = new ArrayList<>(demands.size());
        for (Demand demand : demands) {
            requirements.add(new ManualRequirement(demand.ingredient(), demand.count(), true));
        }
        return List.copyOf(requirements);
    }

    @Nullable
    private static Map<Item, Integer> itemCosts(MiniaturizationRecipe recipe) {
        Map<Item, Integer> costs = new LinkedHashMap<>();
        ItemStack catalyst = recipe.catalyst();
        if (catalyst.isEmpty()) {
            return null;
        }
        costs.merge(catalyst.getItem(), Math.max(1, catalyst.getCount()), Integer::sum);

        boolean anyComponent = false;
        for (Map.Entry<String, Integer> entry : new TreeMap<>(recipe.getComponentTotals()).entrySet()) {
            int needed = entry.getValue() == null ? 0 : entry.getValue();
            if (needed <= 0 || MiniaturizationComponents.isEmpty(recipe, entry.getKey())) {
                continue;
            }
            IRecipeBlockComponent component =
                    recipe.getComponents().getBlock(entry.getKey()).orElse(null);
            if (component == null) {
                return null;
            }
            Block block = component.getBlock();
            Item item = block.asItem();
            if (item == Items.AIR) {
                return null;
            }
            costs.merge(item, needed, Integer::sum);
            anyComponent = true;
        }
        return anyComponent ? costs : null;
    }

    @Nullable
    static GenericStack singleOutput(MiniaturizationRecipe recipe) {
        ItemStack[] outputs = recipe.getOutputs();
        if (outputs.length != 1) {
            return null;
        }
        return IngredientMatching.resultOf(outputs[0]);
    }
}
