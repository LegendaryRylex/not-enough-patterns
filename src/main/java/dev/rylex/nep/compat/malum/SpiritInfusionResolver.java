package dev.rylex.nep.compat.malum;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import com.sammy.malum.common.recipe.SpiritInfusionRecipe;
import com.sammy.malum.registry.common.recipe.MalumRecipeTypes;
import dev.rylex.nep.pattern.PatternStacks;
import dev.rylex.nep.pattern.SpiritInfusionPattern;
import dev.rylex.nep.pattern.encoding.EncodedIngredients;
import dev.rylex.nep.pattern.encoding.IngredientMatching;
import dev.rylex.nep.util.RecipeCache;
import dev.rylex.nep.util.Uniqueness;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

final class SpiritInfusionResolver {
    private SpiritInfusionResolver() {}

    private static final RecipeCache<List<RecipeHolder<SpiritInfusionRecipe>>> CANDIDATE_CACHE = RecipeCache.of(
            level -> List.copyOf(level.getRecipeManager().getAllRecipesFor(MalumRecipeTypes.SPIRIT_INFUSION.get())));

    record Plan(
            RecipeHolder<SpiritInfusionRecipe> holder,
            GenericStack input,
            List<GenericStack> spirits,
            List<GenericStack> extras,
            Map<AEItemKey, Long> expectedItems,
            GenericStack result) {}

    static void clearCache() {
        CANDIDATE_CACHE.clear();
    }

    static List<RecipeHolder<SpiritInfusionRecipe>> candidates(Level level) {
        return CANDIDATE_CACHE.get(level);
    }

    @Nullable
    static Plan resolve(IPatternDetails pattern, Level level) {
        List<GenericStack> inputs = PatternStacks.condensedInputs(pattern);
        GenericStack result = PatternStacks.singleResult(pattern);
        if (inputs == null || result == null) {
            return null;
        }
        if (pattern instanceof SpiritInfusionPattern infusion) {
            RecipeHolder<SpiritInfusionRecipe> holder = byId(level, infusion.recipe());
            return holder == null ? null : plan(holder, inputs, result, level);
        }
        return Uniqueness.onlyMatch(candidates(level), candidate -> plan(candidate, inputs, result, level));
    }

    @Nullable
    static RecipeHolder<SpiritInfusionRecipe> byId(Level level, ResourceLocation recipe) {
        for (RecipeHolder<SpiritInfusionRecipe> candidate : candidates(level)) {
            if (candidate.id().equals(recipe)) {
                return candidate;
            }
        }
        return null;
    }

    @Nullable
    static Plan plan(
            RecipeHolder<SpiritInfusionRecipe> holder, List<GenericStack> inputs, GenericStack result, Level level) {
        SpiritInfusionRecipe recipe = holder.value();
        EncodedIngredients expected = MalumRecipeIngredients.spiritInfusion(recipe);
        GenericStack[] chosen = IngredientMatching.matchAssign(expected, inputs, result);
        if (chosen == null) {
            chosen = carriedAssign(recipe, expected, inputs, result, level);
        }
        if (chosen == null) {
            return null;
        }
        Map<AEItemKey, Long> expectedItems = expectedItems(inputs);
        if (expectedItems == null) {
            return null;
        }
        int firstExtra = 1 + MalumRecipeIngredients.spiritCount(recipe);
        List<GenericStack> spirits = List.copyOf(List.of(chosen).subList(1, firstExtra));
        List<GenericStack> extras = List.copyOf(List.of(chosen).subList(firstExtra, chosen.length));
        return new Plan(holder, chosen[0], spirits, extras, expectedItems, result);
    }

    /**
     * A carrying recipe's pattern names the component-bearing item it was encoded around rather than the plain one the
     * ingredient lists, so the encoded input stands in for slot one once it produces exactly the declared output.
     */
    private static GenericStack @Nullable [] carriedAssign(
            SpiritInfusionRecipe recipe,
            @Nullable EncodedIngredients expected,
            List<GenericStack> inputs,
            GenericStack result,
            Level level) {
        if (expected == null
                || !recipe.carryOverComponentData
                || !(level instanceof ServerLevel server)
                || !(result.what() instanceof AEItemKey declared)) {
            return null;
        }
        for (GenericStack candidate : inputs) {
            if (!(candidate.what() instanceof AEItemKey key) || candidate.amount() != recipe.input.count()) {
                continue;
            }
            ItemStack stack = key.toStack();
            if (!recipe.input.ingredient().test(stack)) {
                continue;
            }
            ItemStack carried = recipe.getOutput(server, stack);
            if (carried.isEmpty() || carried.getCount() != result.amount() || !declared.matches(carried)) {
                continue;
            }
            List<List<GenericStack>> slots = new ArrayList<>(expected.inputs());
            slots.set(0, List.of(new GenericStack(key, recipe.input.count())));
            GenericStack[] chosen = IngredientMatching.matchAssign(
                    new EncodedIngredients(List.copyOf(slots), List.of(result)), inputs, result);
            if (chosen != null) {
                return chosen;
            }
        }
        return null;
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
