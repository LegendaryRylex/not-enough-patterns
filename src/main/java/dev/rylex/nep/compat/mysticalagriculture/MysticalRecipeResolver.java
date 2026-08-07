package dev.rylex.nep.compat.mysticalagriculture;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import com.blakebr0.mysticalagriculture.api.crafting.IAwakeningRecipe;
import com.blakebr0.mysticalagriculture.api.crafting.IInfusionRecipe;
import com.blakebr0.mysticalagriculture.init.ModRecipeTypes;
import dev.rylex.nep.pattern.AwakeningPattern;
import dev.rylex.nep.pattern.InfusionPattern;
import dev.rylex.nep.pattern.PatternStacks;
import dev.rylex.nep.pattern.encoding.EncodedIngredients;
import dev.rylex.nep.pattern.encoding.IngredientMatching;
import dev.rylex.nep.util.RecipeCache;
import dev.rylex.nep.util.Uniqueness;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

final class MysticalRecipeResolver {
    private MysticalRecipeResolver() {}

    private static final RecipeCache<List<RecipeHolder<IInfusionRecipe>>> INFUSION_CACHE =
            RecipeCache.of(level -> List.copyOf(level.getRecipeManager().getAllRecipesFor(infusionType())));

    private static final RecipeCache<List<RecipeHolder<IAwakeningRecipe>>> AWAKENING_CACHE =
            RecipeCache.of(level -> List.copyOf(level.getRecipeManager().getAllRecipesFor(awakeningType())));

    record InfusionPlan(
            RecipeHolder<IInfusionRecipe> holder,
            GenericStack altar,
            List<GenericStack> pedestals,
            Map<AEItemKey, Long> expectedItems,
            GenericStack result) {}

    record AwakeningPlan(
            RecipeHolder<IAwakeningRecipe> holder,
            GenericStack altar,
            List<GenericStack> pedestals,
            List<GenericStack> essences,
            Map<AEItemKey, Long> expectedItems,
            GenericStack result) {}

    static void clearCache() {
        INFUSION_CACHE.clear();
        AWAKENING_CACHE.clear();
    }

    static List<RecipeHolder<IInfusionRecipe>> infusionCandidates(Level level) {
        return INFUSION_CACHE.get(level);
    }

    static List<RecipeHolder<IAwakeningRecipe>> awakeningCandidates(Level level) {
        return AWAKENING_CACHE.get(level);
    }

    static RecipeType<IInfusionRecipe> infusionType() {
        return ModRecipeTypes.INFUSION.get();
    }

    static RecipeType<IAwakeningRecipe> awakeningType() {
        return ModRecipeTypes.AWAKENING.get();
    }

    @Nullable
    static RecipeHolder<IInfusionRecipe> infusionById(Level level, ResourceLocation recipe) {
        RecipeHolder<?> holder = level.getRecipeManager().byKey(recipe).orElse(null);
        return holder != null && holder.value() instanceof IInfusionRecipe ? castInfusion(holder) : null;
    }

    @Nullable
    static RecipeHolder<IAwakeningRecipe> awakeningById(Level level, ResourceLocation recipe) {
        RecipeHolder<?> holder = level.getRecipeManager().byKey(recipe).orElse(null);
        return holder != null && holder.value() instanceof IAwakeningRecipe ? castAwakening(holder) : null;
    }

    @Nullable
    static RecipeHolder<IInfusionRecipe> infusionByOutputItem(Level level, Item result) {
        return Uniqueness.onlyMatch(
                infusionCandidates(level),
                candidate ->
                        candidate.value().getResultItem(level.registryAccess()).getItem() == result ? candidate : null);
    }

    @Nullable
    static RecipeHolder<IAwakeningRecipe> awakeningByOutputItem(Level level, Item result) {
        return Uniqueness.onlyMatch(
                awakeningCandidates(level),
                candidate ->
                        candidate.value().getResultItem(level.registryAccess()).getItem() == result ? candidate : null);
    }

    @Nullable
    static InfusionPlan resolveInfusion(IPatternDetails pattern, Level level) {
        List<GenericStack> inputs = PatternStacks.condensedInputs(pattern);
        GenericStack result = PatternStacks.singleResult(pattern);
        if (inputs == null || result == null) {
            return null;
        }
        if (pattern instanceof InfusionPattern infusion) {
            RecipeHolder<IInfusionRecipe> holder = infusionById(level, infusion.recipe());
            return holder == null ? null : infusionPlan(holder, level, inputs, result);
        }
        return Uniqueness.onlyMatch(
                infusionCandidates(level), candidate -> infusionPlan(candidate, level, inputs, result));
    }

    @Nullable
    static AwakeningPlan resolveAwakening(IPatternDetails pattern, Level level) {
        List<GenericStack> inputs = PatternStacks.condensedInputs(pattern);
        GenericStack result = PatternStacks.singleResult(pattern);
        if (inputs == null || result == null) {
            return null;
        }
        if (pattern instanceof AwakeningPattern awakening) {
            RecipeHolder<IAwakeningRecipe> holder = awakeningById(level, awakening.recipe());
            return holder == null ? null : awakeningPlan(holder, level, inputs, result);
        }
        return Uniqueness.onlyMatch(
                awakeningCandidates(level), candidate -> awakeningPlan(candidate, level, inputs, result));
    }

    @Nullable
    static InfusionPlan infusionPlan(
            RecipeHolder<IInfusionRecipe> holder, Level level, List<GenericStack> inputs, GenericStack result) {
        GenericStack[] chosen = match(MysticalRecipeIngredients.infusion(holder.value(), level), inputs, result);
        if (chosen == null) {
            return null;
        }
        Map<AEItemKey, Long> expected = expectedItems(inputs);
        if (expected == null) {
            return null;
        }
        List<GenericStack> pedestals = new ArrayList<>(chosen.length - 1);
        pedestals.addAll(List.of(chosen).subList(1, chosen.length));
        return new InfusionPlan(holder, chosen[0], List.copyOf(pedestals), expected, result);
    }

    @Nullable
    static AwakeningPlan awakeningPlan(
            RecipeHolder<IAwakeningRecipe> holder, Level level, List<GenericStack> inputs, GenericStack result) {
        GenericStack[] chosen = match(MysticalRecipeIngredients.awakening(holder.value(), level), inputs, result);
        if (chosen == null) {
            return null;
        }
        Map<AEItemKey, Long> expected = expectedItems(inputs);
        if (expected == null) {
            return null;
        }
        int vessels = MysticalRecipeIngredients.AWAKENING_VESSELS;
        int firstVessel = chosen.length - vessels;
        List<GenericStack> pedestals = List.copyOf(List.of(chosen).subList(1, firstVessel));
        List<GenericStack> essences = List.copyOf(List.of(chosen).subList(firstVessel, chosen.length));
        return new AwakeningPlan(holder, chosen[0], pedestals, essences, expected, result);
    }

    private static GenericStack @Nullable [] match(
            @Nullable EncodedIngredients expected, List<GenericStack> inputs, GenericStack result) {
        return IngredientMatching.matchAssign(expected, inputs, result);
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

    @SuppressWarnings("unchecked")
    private static RecipeHolder<IInfusionRecipe> castInfusion(RecipeHolder<?> holder) {
        return (RecipeHolder<IInfusionRecipe>) holder;
    }

    @SuppressWarnings("unchecked")
    private static RecipeHolder<IAwakeningRecipe> castAwakening(RecipeHolder<?> holder) {
        return (RecipeHolder<IAwakeningRecipe>) holder;
    }
}
