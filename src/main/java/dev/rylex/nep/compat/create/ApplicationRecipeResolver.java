package dev.rylex.nep.compat.create;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.kinetics.deployer.DeployerApplicationRecipe;
import com.simibubi.create.content.kinetics.deployer.ItemApplicationRecipe;
import com.simibubi.create.content.kinetics.deployer.ManualApplicationRecipe;
import dev.rylex.nep.pattern.AndesiteCraftingPattern;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;
import org.jetbrains.annotations.Nullable;

final class ApplicationRecipeResolver {
    private ApplicationRecipeResolver() {}

    record Plan(
            AEItemKey depotItem,
            @Nullable AEItemKey consumedTool,
            @Nullable Ingredient keptTool,
            Map<AEItemKey, Long> expectedItems) {}

    private static final Map<AEItemKey, Optional<Plan>> CACHE = new HashMap<>();

    static void clearCache() {
        CACHE.clear();
    }

    @Nullable
    static Plan resolve(IPatternDetails pattern, Level level) {
        return CACHE.computeIfAbsent(pattern.getDefinition(), def -> Optional.ofNullable(compute(pattern, level)))
                .orElse(null);
    }

    @Nullable
    private static Plan compute(IPatternDetails pattern, Level level) {
        List<GenericStack> outputs = pattern.getOutputs();
        if (outputs.size() != 1 || !(outputs.get(0).what() instanceof AEItemKey outputKey)) {
            return null;
        }
        long outputCount = outputs.get(0).amount();

        Map<AEItemKey, Long> available = CreateRecipeIngredients.flattenItemInputs(pattern);
        if (available == null) {
            return null;
        }

        ItemStack expected = outputKey.toStack();
        if (pattern instanceof AndesiteCraftingPattern andesite) {
            RecipeHolder<?> holder =
                    level.getRecipeManager().byKey(andesite.recipe()).orElse(null);
            if (holder == null || !(holder.value() instanceof ItemApplicationRecipe recipe)) {
                return null;
            }
            return planFor(holder, recipe, available, expected, outputCount);
        }

        for (RecipeHolder<? extends ItemApplicationRecipe> holder : candidates(level)) {
            Plan plan = planFor(holder, holder.value(), available, expected, outputCount);
            if (plan != null) {
                return plan;
            }
        }
        return null;
    }

    @Nullable
    private static Plan planFor(
            RecipeHolder<?> holder,
            ItemApplicationRecipe recipe,
            Map<AEItemKey, Long> available,
            ItemStack expected,
            long outputCount) {
        if (!AllRecipeTypes.CAN_BE_AUTOMATED.test(holder)) {
            return null;
        }
        if (!CreateRecipeIngredients.matchesSingleResult(recipe.getRollableResults(), expected, outputCount)) {
            return null;
        }
        return match(available, recipe.getProcessedItem(), recipe.getRequiredHeldItem(), recipe.shouldKeepHeldItem());
    }

    static List<RecipeHolder<? extends ItemApplicationRecipe>> candidates(Level level) {
        List<RecipeHolder<? extends ItemApplicationRecipe>> all = new ArrayList<>();
        all.addAll(level.getRecipeManager()
                .getAllRecipesFor(AllRecipeTypes.DEPLOYING.<RecipeWrapper, DeployerApplicationRecipe>getType()));
        all.addAll(level.getRecipeManager()
                .getAllRecipesFor(AllRecipeTypes.ITEM_APPLICATION.<RecipeWrapper, ManualApplicationRecipe>getType()));
        return all;
    }

    @Nullable
    private static Plan match(Map<AEItemKey, Long> available, Ingredient depot, Ingredient tool, boolean keepTool) {
        long total = 0;
        for (long count : available.values()) {
            total += count;
        }

        if (keepTool) {
            if (total != 1) {
                return null;
            }
            AEItemKey key = available.keySet().iterator().next();
            if (!depot.test(key.toStack())) {
                return null;
            }
            return new Plan(key, null, tool, Map.of(key, 1L));
        }

        if (total != 2) {
            return null;
        }
        List<AEItemKey> keys = List.copyOf(available.keySet());
        if (keys.size() == 1) {
            AEItemKey key = keys.get(0);
            ItemStack stack = key.toStack();
            if (depot.test(stack) && tool.test(stack)) {
                return new Plan(key, key, null, Map.of(key, 2L));
            }
            return null;
        }
        AEItemKey a = keys.get(0);
        AEItemKey b = keys.get(1);
        if (available.get(a) != 1 || available.get(b) != 1) {
            return null;
        }
        if (depot.test(a.toStack()) && tool.test(b.toStack())) {
            return new Plan(a, b, null, Map.of(a, 1L, b, 1L));
        }
        if (depot.test(b.toStack()) && tool.test(a.toStack())) {
            return new Plan(b, a, null, Map.of(a, 1L, b, 1L));
        }
        return null;
    }
}
