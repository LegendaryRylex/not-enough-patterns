package dev.rylex.nep.compat.draconic;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import com.brandon3055.brandonscore.api.TechLevel;
import com.brandon3055.draconicevolution.api.DraconicAPI;
import com.brandon3055.draconicevolution.api.crafting.IFusionRecipe;
import dev.rylex.nep.pattern.FusionCraftingPattern;
import dev.rylex.nep.pattern.PatternStacks;
import dev.rylex.nep.pattern.encoding.EncodedIngredients;
import dev.rylex.nep.pattern.encoding.IngredientMatching;
import dev.rylex.nep.util.RecipeCache;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

final class FusionRecipeResolver {
    private FusionRecipeResolver() {}

    private static final RecipeCache<List<RecipeHolder<IFusionRecipe>>> CANDIDATE_CACHE =
            RecipeCache.of(level -> List.copyOf(level.getRecipeManager().getAllRecipesFor(recipeType())));

    record Plan(
            RecipeHolder<IFusionRecipe> holder,
            TechLevel tier,
            GenericStack catalyst,
            List<GenericStack> injectorItems,
            List<GenericStack> retainedItems,
            List<Ingredient> preloaded,
            Map<AEItemKey, Long> expectedItems,
            GenericStack result) {

        int injectorsNeeded() {
            return injectorItems.size() + retainedItems.size() + preloaded.size();
        }
    }

    static void clearCache() {
        CANDIDATE_CACHE.clear();
        FusionResults.clearCache();
    }

    static List<RecipeHolder<IFusionRecipe>> candidates(Level level) {
        return CANDIDATE_CACHE.get(level);
    }

    @SuppressWarnings("unchecked")
    private static RecipeType<IFusionRecipe> recipeType() {
        return (RecipeType<IFusionRecipe>) DraconicAPI.FUSION_RECIPE_TYPE.get();
    }

    @Nullable
    static RecipeHolder<IFusionRecipe> resolveById(Level level, ResourceLocation recipe) {
        RecipeHolder<?> holder = level.getRecipeManager().byKey(recipe).orElse(null);
        return holder != null && holder.value() instanceof IFusionRecipe ? cast(holder) : null;
    }

    static boolean hasUnrepeatableResult(Level level, GenericStack result) {
        if (!(result.what() instanceof AEItemKey key)) {
            return false;
        }
        for (RecipeHolder<IFusionRecipe> candidate : candidates(level)) {
            ItemStack produced = candidate.value().getResultItem(level.registryAccess());
            if (produced.getItem() == key.getItem()
                    && !FusionResults.producesAStableResult(candidate.value(), candidate.id(), level)) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    static RecipeHolder<IFusionRecipe> resolveByOutputItem(Level level, Item result) {
        RecipeHolder<IFusionRecipe> only = null;
        for (RecipeHolder<IFusionRecipe> candidate : candidates(level)) {
            if (FusionResults.expectedResult(candidate.value(), level).getItem() != result) {
                continue;
            }
            if (only != null) {
                return null;
            }
            only = candidate;
        }
        return only;
    }

    @Nullable
    static Plan resolve(IPatternDetails pattern, Level level) {
        List<GenericStack> inputs = condensedInputs(pattern);
        GenericStack result = singleResult(pattern);
        if (inputs == null || result == null) {
            return null;
        }

        if (pattern instanceof FusionCraftingPattern fusion) {
            RecipeHolder<?> holder =
                    level.getRecipeManager().byKey(fusion.recipe()).orElse(null);
            if (holder == null || !(holder.value() instanceof IFusionRecipe)) {
                return null;
            }
            return plan(cast(holder), level, inputs, result);
        }

        Plan only = null;
        for (RecipeHolder<IFusionRecipe> candidate : candidates(level)) {
            Plan plan = plan(candidate, level, inputs, result);
            if (plan != null) {
                if (only != null) {
                    return null;
                }
                only = plan;
            }
        }
        return only;
    }

    @Nullable
    private static Plan plan(
            RecipeHolder<IFusionRecipe> holder, Level level, List<GenericStack> inputs, GenericStack result) {
        EncodedIngredients expected = DraconicRecipeIngredients.fusion(holder, level, true);
        GenericStack[] chosen = match(expected, inputs, result);
        if (chosen == null) {
            expected = DraconicRecipeIngredients.fusion(holder, level, false);
            chosen = match(expected, inputs, result);
        }
        if (chosen == null) {
            return null;
        }

        List<GenericStack> injectorItems = new ArrayList<>(chosen.length - 1);
        List<GenericStack> retainedItems = new ArrayList<>();
        for (int slot = 1; slot < chosen.length; slot++) {
            (expected.isRetained(slot) ? retainedItems : injectorItems).add(chosen[slot]);
        }

        List<Ingredient> preloaded = new ArrayList<>();
        if (retainedItems.isEmpty()) {
            for (IFusionRecipe.IFusionIngredient ingredient : holder.value().fusionIngredients()) {
                if (!ingredient.consume()) {
                    preloaded.add(ingredient.get());
                }
            }
        }

        Map<AEItemKey, Long> expectedItems = new LinkedHashMap<>();
        for (GenericStack stack : inputs) {
            if (!(stack.what() instanceof AEItemKey key)) {
                return null;
            }
            expectedItems.merge(key, stack.amount(), Long::sum);
        }

        return new Plan(
                holder,
                holder.value().getRecipeTier(),
                chosen[0],
                List.copyOf(injectorItems),
                List.copyOf(retainedItems),
                List.copyOf(preloaded),
                Map.copyOf(expectedItems),
                result);
    }

    @Nullable
    static Plan planFromProvided(Plan plan, Map<AEItemKey, Long> provided, Level level) {
        List<DraconicRecipeIngredients.Demand> demands =
                DraconicRecipeIngredients.demandOf(plan.holder().value());
        if (demands == null) {
            return null;
        }

        Map<AEItemKey, Long> left = new LinkedHashMap<>(provided);
        GenericStack catalyst = null;
        List<GenericStack> injectorItems = new ArrayList<>();
        List<GenericStack> retainedItems = new ArrayList<>();
        List<Ingredient> preloaded = new ArrayList<>();
        for (int index = 0; index < demands.size(); index++) {
            DraconicRecipeIngredients.Demand demand = demands.get(index);
            AEItemKey chosen = take(left, demand);
            if (chosen == null) {
                if (index == 0 || demand.consume()) {
                    return null;
                }
                preloaded.add(demand.ingredient());
            } else if (index == 0) {
                catalyst = new GenericStack(chosen, demand.count());
            } else {
                (demand.consume() ? injectorItems : retainedItems).add(new GenericStack(chosen, demand.count()));
            }
        }
        if (catalyst == null || !left.isEmpty()) {
            return null;
        }

        AEItemKey produced =
                FusionResults.resultFor(plan.holder().value(), level, List.of((AEItemKey) catalyst.what()));
        if (produced == null) {
            return null;
        }
        return new Plan(
                plan.holder(),
                plan.tier(),
                catalyst,
                List.copyOf(injectorItems),
                List.copyOf(retainedItems),
                List.copyOf(preloaded),
                Map.copyOf(provided),
                new GenericStack(produced, plan.result().amount()));
    }

    @Nullable
    private static AEItemKey take(Map<AEItemKey, Long> left, DraconicRecipeIngredients.Demand demand) {
        for (AEItemKey key : List.copyOf(left.keySet())) {
            long held = left.get(key);
            if (held < demand.count() || !demand.ingredient().test(key.toStack(demand.count()))) {
                continue;
            }
            long remaining = held - demand.count();
            if (remaining > 0) {
                left.put(key, remaining);
            } else {
                left.remove(key);
            }
            return key;
        }
        return null;
    }

    private static GenericStack @Nullable [] match(
            @Nullable EncodedIngredients expected, List<GenericStack> inputs, GenericStack result) {
        return IngredientMatching.matchAssign(expected, inputs, result);
    }

    @SuppressWarnings("unchecked")
    private static RecipeHolder<IFusionRecipe> cast(RecipeHolder<?> holder) {
        return (RecipeHolder<IFusionRecipe>) holder;
    }

    @Nullable
    static GenericStack singleResult(IPatternDetails pattern) {
        return PatternStacks.singleResult(pattern);
    }

    @Nullable
    static List<GenericStack> condensedInputs(IPatternDetails pattern) {
        return PatternStacks.condensedInputs(pattern);
    }
}
