package dev.rylex.nep.compat.mysticalagriculture;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.GenericStack;
import com.blakebr0.mysticalagriculture.api.crafting.IAwakeningRecipe;
import com.blakebr0.mysticalagriculture.api.crafting.IInfusionRecipe;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.pattern.AwakeningPattern;
import dev.rylex.nep.pattern.InfusionPattern;
import dev.rylex.nep.pattern.PatternStacks;
import dev.rylex.nep.pattern.encoding.EncodedIngredients;
import dev.rylex.nep.pattern.encoding.IngredientMatching;
import dev.rylex.nep.pattern.encoding.PatternConverters;
import dev.rylex.nep.pattern.encoding.PatternFallback;
import dev.rylex.nep.util.Recipes;
import dev.rylex.nep.util.Uniqueness;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

final class MysticalPatternEncoders {
    private MysticalPatternEncoders() {}

    static void register() {
        PatternConverters.register(IInfusionRecipe.class, MysticalPatternEncoders::infusion);
        PatternConverters.register(IAwakeningRecipe.class, MysticalPatternEncoders::awakening);
        PatternConverters.registerFallback(MysticalPatternEncoders::infusionByResult);
        PatternConverters.registerFallback(MysticalPatternEncoders::awakeningByResult);
    }

    @Nullable
    private static ItemStack infusion(IPatternDetails encoded, RecipeHolder<IInfusionRecipe> holder, Level level) {
        if (!NepConfig.mysticalInfusion()) {
            return null;
        }
        return encodeInfusion(holder, level, encoded);
    }

    @Nullable
    private static ItemStack awakening(IPatternDetails encoded, RecipeHolder<IAwakeningRecipe> holder, Level level) {
        if (!NepConfig.mysticalAwakening()) {
            return null;
        }
        return encodeAwakening(holder, level, encoded);
    }

    @Nullable
    private static PatternFallback.Result infusionByResult(IPatternDetails encoded, Level level) {
        if (!NepConfig.mysticalInfusion()) {
            return null;
        }
        ItemStack only = Uniqueness.onlyMatch(
                MysticalRecipeResolver.infusionCandidates(level),
                candidate -> encodeInfusion(candidate, level, encoded));
        return only == null ? null : PatternFallback.Result.of(only);
    }

    @Nullable
    private static PatternFallback.Result awakeningByResult(IPatternDetails encoded, Level level) {
        if (!NepConfig.mysticalAwakening()) {
            return null;
        }
        ItemStack only = Uniqueness.onlyMatch(
                MysticalRecipeResolver.awakeningCandidates(level),
                candidate -> encodeAwakening(candidate, level, encoded));
        return only == null ? null : PatternFallback.Result.of(only);
    }

    @Nullable
    private static ItemStack encodeInfusion(
            RecipeHolder<IInfusionRecipe> holder, Level level, IPatternDetails encoded) {
        List<GenericStack> chosen = assign(MysticalRecipeIngredients.infusion(holder.value(), level), encoded);
        if (chosen == null) {
            return null;
        }
        return InfusionPattern.encode(Recipes.idOf(holder), chosen, PatternStacks.singleResult(encoded));
    }

    @Nullable
    private static ItemStack encodeAwakening(
            RecipeHolder<IAwakeningRecipe> holder, Level level, IPatternDetails encoded) {
        List<GenericStack> chosen = assign(MysticalRecipeIngredients.awakening(holder.value(), level), encoded);
        if (chosen == null) {
            return null;
        }
        return AwakeningPattern.encode(Recipes.idOf(holder), chosen, PatternStacks.singleResult(encoded));
    }

    @Nullable
    private static List<GenericStack> assign(@Nullable EncodedIngredients expected, IPatternDetails encoded) {
        GenericStack[] chosen = IngredientMatching.matchAssign(
                expected, PatternStacks.condensedInputs(encoded), PatternStacks.singleResult(encoded));
        return chosen == null ? null : List.of(chosen);
    }
}
