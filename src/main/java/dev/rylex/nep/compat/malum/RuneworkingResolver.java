package dev.rylex.nep.compat.malum;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import com.sammy.malum.common.recipe.RuneworkingRecipe;
import com.sammy.malum.registry.common.recipe.MalumRecipeTypes;
import dev.rylex.nep.pattern.PatternStacks;
import dev.rylex.nep.pattern.RuneworkingPattern;
import dev.rylex.nep.pattern.encoding.EncodedIngredients;
import dev.rylex.nep.pattern.encoding.IngredientMatching;
import dev.rylex.nep.util.RecipeCache;
import dev.rylex.nep.util.Uniqueness;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

final class RuneworkingResolver {
    private RuneworkingResolver() {}

    private static final RecipeCache<List<RecipeHolder<RuneworkingRecipe>>> CANDIDATE_CACHE = RecipeCache.of(
            level -> List.copyOf(level.getRecipeManager().getAllRecipesFor(MalumRecipeTypes.RUNEWORKING.get())));

    record Plan(
            RecipeHolder<RuneworkingRecipe> holder,
            GenericStack primary,
            GenericStack secondary,
            Map<AEItemKey, Long> expectedItems,
            GenericStack result) {}

    static void clearCache() {
        CANDIDATE_CACHE.clear();
    }

    static List<RecipeHolder<RuneworkingRecipe>> candidates(Level level) {
        return CANDIDATE_CACHE.get(level);
    }

    @Nullable
    static Plan resolve(IPatternDetails pattern, Level level) {
        List<GenericStack> inputs = PatternStacks.condensedInputs(pattern);
        GenericStack result = PatternStacks.singleResult(pattern);
        if (inputs == null || result == null) {
            return null;
        }
        if (pattern instanceof RuneworkingPattern runeworking) {
            RecipeHolder<RuneworkingRecipe> holder = byId(level, runeworking.recipe());
            return holder == null ? null : plan(holder, inputs, result);
        }
        return Uniqueness.onlyMatch(candidates(level), candidate -> plan(candidate, inputs, result));
    }

    @Nullable
    static RecipeHolder<RuneworkingRecipe> byId(Level level, ResourceLocation recipe) {
        for (RecipeHolder<RuneworkingRecipe> candidate : candidates(level)) {
            if (candidate.id().equals(recipe)) {
                return candidate;
            }
        }
        return null;
    }

    @Nullable
    static Plan plan(RecipeHolder<RuneworkingRecipe> holder, List<GenericStack> inputs, GenericStack result) {
        EncodedIngredients expected = MalumRecipeIngredients.runeworking(holder.value());
        GenericStack[] chosen = IngredientMatching.matchAssign(expected, inputs, result);
        if (chosen == null) {
            return null;
        }
        Map<AEItemKey, Long> expectedItems = expectedItems(inputs);
        if (expectedItems == null) {
            return null;
        }
        return new Plan(holder, chosen[0], chosen[1], expectedItems, result);
    }

    @Nullable
    private static Map<AEItemKey, Long> expectedItems(List<GenericStack> inputs) {
        Map<AEItemKey, Long> expected = new LinkedHashMap<>();
        for (GenericStack stack : inputs) {
            if (!(stack.what() instanceof AEItemKey key)) {
                return null;
            }
            expected.merge(key, stack.amount(), Long::sum);
        }
        return Map.copyOf(expected);
    }
}
