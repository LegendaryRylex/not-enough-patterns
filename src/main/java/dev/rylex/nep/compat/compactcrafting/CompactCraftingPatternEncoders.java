package dev.rylex.nep.compat.compactcrafting;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.GenericStack;
import dev.compactmods.crafting.recipes.MiniaturizationRecipe;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.pattern.MiniaturizationPattern;
import dev.rylex.nep.pattern.encoding.EncodedIngredients;
import dev.rylex.nep.pattern.encoding.IngredientMatching;
import dev.rylex.nep.pattern.encoding.PatternConverters;
import dev.rylex.nep.pattern.encoding.PatternFallback;
import dev.rylex.nep.util.Uniqueness;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

final class CompactCraftingPatternEncoders {
    private CompactCraftingPatternEncoders() {}

    static void register() {
        PatternConverters.register(MiniaturizationRecipe.class, CompactCraftingPatternEncoders::miniaturization);
        PatternConverters.registerFallback(CompactCraftingPatternEncoders::miniaturizationByResult);
    }

    @Nullable
    private static ItemStack miniaturization(
            IPatternDetails encoded, RecipeHolder<MiniaturizationRecipe> holder, Level level) {
        if (!NepConfig.compactCraftingMiniaturizationMatrix()
                && !NepConfig.compactCraftingMiniaturizationController()) {
            return null;
        }
        List<GenericStack> inputs = MiniaturizationRecipeResolver.condensedInputs(encoded);
        GenericStack result = MiniaturizationRecipeResolver.singleResult(encoded);
        if (inputs == null || result == null) {
            return null;
        }
        List<GenericStack> chosen =
                match(MiniaturizationRecipeIngredients.miniaturization(holder, level), inputs, result);
        return chosen == null ? null : MiniaturizationPattern.encode(holder.id(), chosen, result);
    }

    @Nullable
    private static PatternFallback.Result miniaturizationByResult(IPatternDetails encoded, Level level) {
        if (!NepConfig.compactCraftingMiniaturizationMatrix()
                && !NepConfig.compactCraftingMiniaturizationController()) {
            return null;
        }
        List<GenericStack> inputs = MiniaturizationRecipeResolver.condensedInputs(encoded);
        GenericStack result = MiniaturizationRecipeResolver.singleResult(encoded);
        if (inputs == null || result == null) {
            return null;
        }

        ItemStack only = Uniqueness.onlyMatch(MiniaturizationRecipeResolver.candidates(level), candidate -> {
            List<GenericStack> chosen =
                    match(MiniaturizationRecipeIngredients.miniaturization(candidate, level), inputs, result);
            return chosen == null ? null : MiniaturizationPattern.encode(candidate.id(), chosen, result);
        });
        return only == null ? null : PatternFallback.Result.of(only);
    }

    @Nullable
    private static List<GenericStack> match(
            @Nullable EncodedIngredients expected, List<GenericStack> inputs, GenericStack result) {
        GenericStack[] chosen = IngredientMatching.matchAssign(expected, inputs, result);
        return chosen == null ? null : List.of(chosen);
    }
}
