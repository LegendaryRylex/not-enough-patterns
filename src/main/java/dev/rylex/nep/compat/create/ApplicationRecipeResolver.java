package dev.rylex.nep.compat.create;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.kinetics.deployer.DeployerApplicationRecipe;
import com.simibubi.create.content.kinetics.deployer.ItemApplicationRecipe;
import com.simibubi.create.content.kinetics.deployer.ManualApplicationRecipe;
import dev.rylex.nep.pattern.AndesiteCraftingPattern;
import dev.rylex.nep.util.RecipeCache;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
            @Nullable AEItemKey suppliedTool,
            @Nullable Ingredient keptTool,
            boolean wornTool,
            Map<AEItemKey, Long> expectedItems) {
        static Plan returningTool(AEItemKey base, @Nullable AEItemKey supplied, Ingredient tool, boolean worn) {
            Map<AEItemKey, Long> expected = new LinkedHashMap<>();
            expected.merge(base, 1L, Long::sum);
            if (supplied != null) {
                expected.merge(supplied, 1L, Long::sum);
            }
            return new Plan(base, null, supplied, tool, worn, Map.copyOf(expected));
        }

        static Plan sameItem(AEItemKey key) {
            return Plan.sameItem(key);
        }

        static Plan twoItem(AEItemKey depot, AEItemKey consumedTool) {
            return new Plan(depot, consumedTool, null, null, false, Map.of(depot, 1L, consumedTool, 1L));
        }
    }

    private record Split(Map<AEItemKey, Long> consumed, Map<AEItemKey, Long> retained) {
        static Split empty() {
            return new Split(new LinkedHashMap<>(), new LinkedHashMap<>());
        }

        void add(boolean handedBack, AEItemKey key, long amount) {
            (handedBack ? retained : consumed).merge(key, amount, Long::sum);
        }

        @Nullable
        Split orNull() {
            return consumed.isEmpty() ? null : this;
        }
    }

    private static final RecipeCache<Map<AEItemKey, Optional<Plan>>> CACHE =
            RecipeCache.of(level -> Collections.synchronizedMap(new HashMap<>()));

    static void clearCache() {
        CACHE.clear();
    }

    @Nullable
    static Plan resolve(IPatternDetails pattern, Level level) {
        return CACHE.get(level)
                .computeIfAbsent(
                        pattern.getDefinition(),
                        def -> Optional.ofNullable(compute(pattern, level, splitInputs(pattern))))
                .orElse(null);
    }

    /**
     * The plan for the keys AE2 actually handed over, which differ from the pattern templates as soon as a worn tool is
     * drawn from stock at some other damage value.
     */
    @Nullable
    static Plan resolveProvided(IPatternDetails pattern, Level level, KeyCounter[] provided) {
        return compute(pattern, level, splitProvided(pattern, provided));
    }

    @Nullable
    private static Plan compute(IPatternDetails pattern, Level level, @Nullable Split available) {
        List<GenericStack> outputs = pattern.getOutputs();
        if (outputs.size() != 1 || !(outputs.get(0).what() instanceof AEItemKey outputKey)) {
            return null;
        }
        long outputCount = outputs.get(0).amount();

        if (available == null) {
            return null;
        }

        ItemStack expected = outputKey.toStack();
        if (pattern instanceof AndesiteCraftingPattern andesite) {
            RecipeHolder<?> holder = SandPaperPolishing.byId(andesite.recipe(), level);
            if (holder == null) {
                holder = level.getRecipeManager().byKey(andesite.recipe()).orElse(null);
            }
            if (holder == null) {
                holder = LogStripping.byId(andesite.recipe(), level);
            }
            if (holder == null || !(holder.value() instanceof ItemApplicationRecipe recipe)) {
                return null;
            }
            return planFor(holder, recipe, available, expected, outputCount, level);
        }

        for (RecipeHolder<? extends ItemApplicationRecipe> holder : candidates(level)) {
            Plan plan = planFor(holder, holder.value(), available, expected, outputCount, level);
            if (plan != null) {
                return plan;
            }
        }
        return null;
    }

    @Nullable
    private static Split splitInputs(IPatternDetails pattern) {
        Split split = Split.empty();
        for (IPatternDetails.IInput input : pattern.getInputs()) {
            GenericStack primary = input.getPossibleInputs()[0];
            if (!(primary.what() instanceof AEItemKey key)) {
                return null;
            }
            long amount = primary.amount() * input.getMultiplier();
            split.add(handedBack(input), key, amount);
        }
        return split.orNull();
    }

    @Nullable
    private static Split splitProvided(IPatternDetails pattern, KeyCounter[] provided) {
        IPatternDetails.IInput[] inputs = pattern.getInputs();
        if (provided.length != inputs.length) {
            return null;
        }
        Split split = Split.empty();
        for (int slot = 0; slot < inputs.length; slot++) {
            boolean back = handedBack(inputs[slot]);
            for (Object2LongMap.Entry<AEKey> entry : provided[slot]) {
                long amount = entry.getLongValue();
                if (amount == 0) {
                    continue;
                }
                if (amount < 0 || !(entry.getKey() instanceof AEItemKey key)) {
                    return null;
                }
                split.add(back, key, amount);
            }
        }
        return split.orNull();
    }

    /**
     * Asked of the template rather than of the pushed key, so a tool on its last point of durability still counts as
     * the recipe tool even though this one craft breaks it and leaves nothing to hand back.
     */
    private static boolean handedBack(IPatternDetails.IInput input) {
        GenericStack primary = input.getPossibleInputs()[0];
        return input.getRemainingKey(primary.what()) != null;
    }

    @Nullable
    private static Plan planFor(
            RecipeHolder<?> holder,
            ItemApplicationRecipe recipe,
            Split available,
            ItemStack expected,
            long outputCount,
            Level level) {
        if (!AllRecipeTypes.CAN_BE_AUTOMATED.test(holder)) {
            return null;
        }
        if (!CreateRecipeIngredients.matchesSingleResult(recipe.getRollableResults(), expected, outputCount)) {
            return null;
        }
        if (recipe.getIngredients().size() < 2) {
            return null;
        }
        return match(
                available,
                recipe.getProcessedItem(),
                recipe.getRequiredHeldItem(),
                DeployerToolFate.of(holder.id(), recipe, level));
    }

    static List<RecipeHolder<? extends ItemApplicationRecipe>> candidates(Level level) {
        List<RecipeHolder<? extends ItemApplicationRecipe>> all = new ArrayList<>();
        all.addAll(level.getRecipeManager()
                .getAllRecipesFor(AllRecipeTypes.DEPLOYING.<RecipeWrapper, DeployerApplicationRecipe>getType()));
        all.addAll(level.getRecipeManager()
                .getAllRecipesFor(AllRecipeTypes.ITEM_APPLICATION.<RecipeWrapper, ManualApplicationRecipe>getType()));
        all.addAll(LogStripping.recipes(level));
        all.addAll(SandPaperPolishing.recipes(level));
        return all;
    }

    @Nullable
    private static Plan match(Split split, Ingredient depot, Ingredient tool, DeployerToolFate fate) {
        Map<AEItemKey, Long> available = split.consumed();
        Map<AEItemKey, Long> retained = split.retained();
        long total = 0;
        for (long count : available.values()) {
            total += count;
        }

        if (fate.returnsTool()) {
            return matchReturningTool(available, retained, total, depot, tool, fate == DeployerToolFate.WORN);
        }
        return matchConsumedPair(available, retained, total, depot, tool);
    }

    @Nullable
    private static Plan matchReturningTool(
            Map<AEItemKey, Long> available,
            Map<AEItemKey, Long> retained,
            long total,
            Ingredient depot,
            Ingredient tool,
            boolean worn) {
        if (total != 1) {
            return null;
        }
        AEItemKey base = available.keySet().iterator().next();
        if (!depot.test(base.toStack())) {
            return null;
        }
        if (retained.isEmpty()) {
            return Plan.returningTool(base, null, tool, worn);
        }
        if (retained.size() != 1) {
            return null;
        }
        Map.Entry<AEItemKey, Long> kept = retained.entrySet().iterator().next();
        if (kept.getValue() != 1L || !tool.test(kept.getKey().toStack())) {
            return null;
        }
        return Plan.returningTool(base, kept.getKey(), tool, worn);
    }

    @Nullable
    private static Plan matchConsumedPair(
            Map<AEItemKey, Long> available,
            Map<AEItemKey, Long> retained,
            long total,
            Ingredient depot,
            Ingredient tool) {
        if (total != 2 || !retained.isEmpty()) {
            return null;
        }
        List<AEItemKey> keys = List.copyOf(available.keySet());
        if (keys.size() == 1) {
            AEItemKey key = keys.get(0);
            ItemStack stack = key.toStack();
            if (depot.test(stack) && tool.test(stack)) {
                return new Plan(key, key, null, null, false, Map.of(key, 2L));
            }
            return null;
        }
        AEItemKey a = keys.get(0);
        AEItemKey b = keys.get(1);
        if (available.get(a) != 1 || available.get(b) != 1) {
            return null;
        }
        if (depot.test(a.toStack()) && tool.test(b.toStack())) {
            return Plan.twoItem(a, b);
        }
        if (depot.test(b.toStack()) && tool.test(a.toStack())) {
            return Plan.twoItem(b, a);
        }
        return null;
    }
}
