package dev.rylex.nep.compat.malum;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import com.sammy.malum.common.recipe.SpiritFocusingRecipe;
import com.sammy.malum.registry.common.recipe.MalumRecipeTypes;
import dev.rylex.nep.pattern.PatternStacks;
import dev.rylex.nep.pattern.SpiritFocusingPattern;
import dev.rylex.nep.pattern.encoding.EncodedIngredients;
import dev.rylex.nep.pattern.encoding.IngredientMatching;
import dev.rylex.nep.util.RecipeCache;
import dev.rylex.nep.util.Uniqueness;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

final class SpiritFocusingResolver {
    private SpiritFocusingResolver() {}

    private static final RecipeCache<List<RecipeHolder<SpiritFocusingRecipe>>> CANDIDATE_CACHE = RecipeCache.of(
            level -> List.copyOf(level.getRecipeManager().getAllRecipesFor(MalumRecipeTypes.SPIRIT_FOCUSING.get())));

    record Plan(
            RecipeHolder<SpiritFocusingRecipe> holder,
            List<GenericStack> spirits,
            Map<AEItemKey, Long> expectedItems,
            GenericStack result) {}

    static void clearCache() {
        CANDIDATE_CACHE.clear();
    }

    static List<RecipeHolder<SpiritFocusingRecipe>> candidates(Level level) {
        return CANDIDATE_CACHE.get(level);
    }

    @Nullable
    static Plan resolve(IPatternDetails pattern, Level level) {
        List<GenericStack> inputs = PatternStacks.condensedInputs(pattern);
        GenericStack result = PatternStacks.singleResult(pattern);
        if (inputs == null || result == null) {
            return null;
        }
        if (pattern instanceof SpiritFocusingPattern focusing) {
            RecipeHolder<SpiritFocusingRecipe> holder = byId(level, focusing.recipe());
            return holder == null ? null : plan(holder, inputs, result);
        }
        return Uniqueness.onlyMatch(candidates(level), candidate -> plan(candidate, inputs, result));
    }

    @Nullable
    static RecipeHolder<SpiritFocusingRecipe> byId(Level level, ResourceLocation recipe) {
        for (RecipeHolder<SpiritFocusingRecipe> candidate : candidates(level)) {
            if (candidate.id().equals(recipe)) {
                return candidate;
            }
        }
        return null;
    }

    @Nullable
    static Plan plan(RecipeHolder<SpiritFocusingRecipe> holder, List<GenericStack> inputs, GenericStack result) {
        SpiritFocusingRecipe recipe = holder.value();
        EncodedIngredients expected = MalumRecipeIngredients.spiritFocusing(recipe);
        GenericStack[] chosen = IngredientMatching.matchAssign(expected, inputs, result);
        if (chosen == null) {
            return null;
        }
        Map<AEItemKey, Long> expectedItems = expectedItems(inputs);
        if (expectedItems == null) {
            return null;
        }
        return new Plan(holder, List.of(chosen), expectedItems, result);
    }

    static boolean acceptsImpetus(SpiritFocusingRecipe recipe, ItemStack impetus) {
        return !impetus.isEmpty() && recipe.input.test(impetus);
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
