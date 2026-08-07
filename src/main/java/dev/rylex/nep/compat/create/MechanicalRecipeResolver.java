package dev.rylex.nep.compat.create;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.kinetics.crafter.MechanicalCraftingRecipe;
import com.simibubi.create.content.kinetics.crafter.RecipeGridHandler;
import com.simibubi.create.content.kinetics.crafter.RecipeGridHandler.GroupedItems;
import com.simibubi.create.infrastructure.config.AllConfigs;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.pattern.GridPlan;
import dev.rylex.nep.util.CellAssigner;
import dev.rylex.nep.util.RecipeCache;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.createmod.catnip.math.Pointing;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

final class MechanicalRecipeResolver {
    private MechanicalRecipeResolver() {}

    private record CacheKey(AEItemKey definition, boolean regularCrafting) {}

    private static final RecipeCache<Map<CacheKey, Optional<GridPlan>>> CACHE =
            RecipeCache.of(level -> Collections.synchronizedMap(new HashMap<>()));

    static void clearCache() {
        CACHE.clear();
    }

    private record Demand(ItemStack expected, long outputCount, Map<AEItemKey, Long> available) {}

    @Nullable
    static GridPlan resolve(IPatternDetails pattern, Level level) {
        CacheKey key = new CacheKey(pattern.getDefinition(), regularCraftingAllowed());
        return CACHE.get(level)
                .computeIfAbsent(key, k -> Optional.ofNullable(compute(pattern, level)))
                .orElse(null);
    }

    @Nullable
    static GridPlan resolveRecipe(IPatternDetails pattern, ResourceLocation recipeId, Level level) {
        Demand demand = demandOf(pattern);
        if (demand == null) {
            return null;
        }
        RecipeHolder<?> holder = level.getRecipeManager().byKey(recipeId).orElse(null);
        if (holder == null) {
            return null;
        }
        if (!(holder.value() instanceof MechanicalCraftingRecipe) && AllRecipeTypes.shouldIgnoreInAutomation(holder)) {
            return null;
        }
        return planFor(holder.value(), demand, level);
    }

    @Nullable
    private static GridPlan compute(IPatternDetails pattern, Level level) {
        Demand demand = demandOf(pattern);
        if (demand == null) {
            return null;
        }
        for (RecipeHolder<MechanicalCraftingRecipe> holder :
                level.getRecipeManager().getAllRecipesFor(recipeType())) {
            GridPlan plan = planFor(holder.value(), demand, level);
            if (plan != null) {
                return plan;
            }
        }
        return null;
    }

    @Nullable
    private static Demand demandOf(IPatternDetails pattern) {
        List<GenericStack> outputs = pattern.getOutputs();
        if (outputs.size() != 1 || !(outputs.get(0).what() instanceof AEItemKey outputKey)) {
            return null;
        }

        Map<AEItemKey, Long> available = CreateRecipeIngredients.flattenItemInputs(pattern);
        if (available == null) {
            return null;
        }
        return new Demand(outputKey.toStack(), outputs.get(0).amount(), available);
    }

    @Nullable
    private static GridPlan planFor(Recipe<?> recipe, Demand demand, Level level) {
        ItemStack result = recipe.getResultItem(level.registryAccess());
        if (result.getCount() != demand.outputCount()
                || !ItemStack.isSameItemSameComponents(result, demand.expected())) {
            return null;
        }
        GridPlan plan =
                switch (recipe) {
                    case MechanicalCraftingRecipe mechanical ->
                        tryAssignShaped(
                                mechanical.getWidth(),
                                mechanical.getHeight(),
                                mechanical.getIngredients(),
                                demand.available());
                    case ShapedRecipe shaped ->
                        regularCraftingAllowed()
                                ? tryAssignShaped(
                                        shaped.getWidth(),
                                        shaped.getHeight(),
                                        shaped.getIngredients(),
                                        demand.available())
                                : null;
                    case ShapelessRecipe shapeless ->
                        regularCraftingAllowed()
                                ? tryAssignShapeless(shapeless.getIngredients(), demand.available())
                                : null;
                    default -> null;
                };
        return plan != null && validate(plan, result, level) ? plan : null;
    }

    private static boolean regularCraftingAllowed() {
        return NepConfig.createMechanicalCraftingAllowRegular()
                && AllConfigs.server().recipes.allowRegularCraftingInCrafter.get();
    }

    private static RecipeType<MechanicalCraftingRecipe> recipeType() {
        return AllRecipeTypes.MECHANICAL_CRAFTING.<CraftingInput, MechanicalCraftingRecipe>getType();
    }

    @Nullable
    private static GridPlan tryAssignShapeless(NonNullList<Ingredient> ingredients, Map<AEItemKey, Long> available) {
        if (ingredients.isEmpty()) {
            return null;
        }
        int width = (int) Math.ceil(Math.sqrt(ingredients.size()));
        int height = (ingredients.size() + width - 1) / width;
        NonNullList<Ingredient> padded = NonNullList.withSize(width * height, Ingredient.EMPTY);
        for (int i = 0; i < ingredients.size(); i++) {
            padded.set(i, ingredients.get(i));
        }
        return tryAssignShaped(width, height, padded, available);
    }

    @Nullable
    private static GridPlan tryAssignShaped(
            int width, int height, NonNullList<Ingredient> ingredients, Map<AEItemKey, Long> available) {
        if (width < 1 || height < 1 || ingredients.size() != width * height) {
            return null;
        }

        List<AEItemKey> keys = List.copyOf(available.keySet());
        long[] capacities = new long[keys.size()];
        ItemStack[] keyStacks = new ItemStack[keys.size()];
        for (int i = 0; i < keys.size(); i++) {
            capacities[i] = available.get(keys.get(i));
            keyStacks[i] = keys.get(i).toStack();
        }

        List<int[]> cellCompatible = new ArrayList<>();
        List<Integer> filledCells = new ArrayList<>();
        for (int cell = 0; cell < ingredients.size(); cell++) {
            Ingredient ingredient = ingredients.get(cell);
            if (ingredient.isEmpty()) {
                continue;
            }
            List<Integer> compatible = new ArrayList<>();
            for (int key = 0; key < keys.size(); key++) {
                if (ingredient.test(keyStacks[key])) {
                    compatible.add(key);
                }
            }
            if (compatible.isEmpty()) {
                return null;
            }
            cellCompatible.add(compatible.stream().mapToInt(Integer::intValue).toArray());
            filledCells.add(cell);
        }
        if (filledCells.isEmpty()) {
            return null;
        }

        int[] assignment = CellAssigner.assign(capacities, cellCompatible);
        if (assignment == null) {
            return null;
        }

        List<AEItemKey> cells = new ArrayList<>(Collections.nCopies(width * height, (AEItemKey) null));
        for (int i = 0; i < filledCells.size(); i++) {
            cells.set(filledCells.get(i), keys.get(assignment[i]));
        }
        return new GridPlan(width, height, cells);
    }

    private static boolean validate(GridPlan plan, ItemStack expected, Level level) {
        ItemStack result = RecipeGridHandler.tryToApplyRecipe(level, buildGrid(plan));
        return result != null
                && result.getCount() == expected.getCount()
                && ItemStack.isSameItemSameComponents(result, expected);
    }

    private static GroupedItems buildGrid(GridPlan plan) {
        GroupedItems rows = null;
        for (int row = 0; row < plan.height(); row++) {
            GroupedItems rowItems = null;
            for (int col = plan.width() - 1; col >= 0; col--) {
                AEItemKey key = plan.cells().get(row * plan.width() + col);
                GroupedItems cell = new GroupedItems(key == null ? ItemStack.EMPTY : key.toStack());
                if (rowItems != null) {
                    rowItems.mergeOnto(cell, Pointing.LEFT);
                }
                rowItems = cell;
            }
            if (rows != null) {
                rows.mergeOnto(rowItems, Pointing.DOWN);
            }
            rows = rowItems;
        }
        return rows;
    }
}
