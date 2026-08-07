package dev.rylex.nep.compat.draconic;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.GenericStack;
import com.brandon3055.draconicevolution.api.crafting.IFusionRecipe;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.pattern.FusionCraftingPattern;
import dev.rylex.nep.pattern.encoding.EncodedIngredients;
import dev.rylex.nep.pattern.encoding.IngredientMatching;
import dev.rylex.nep.pattern.encoding.PatternConverters;
import dev.rylex.nep.pattern.encoding.PatternFallback;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

final class DraconicPatternEncoders {
    private DraconicPatternEncoders() {}

    static void register() {
        PatternConverters.register(IFusionRecipe.class, DraconicPatternEncoders::fusion);
        PatternConverters.registerFallback(DraconicPatternEncoders::fusionByResult);
    }

    @Nullable
    private static ItemStack fusion(IPatternDetails encoded, RecipeHolder<IFusionRecipe> holder, Level level) {
        if (!NepConfig.draconicFusionCrafting()) {
            return null;
        }
        List<GenericStack> inputs = FusionResults.canonical(FusionRecipeResolver.condensedInputs(encoded), level);
        GenericStack result = FusionRecipeResolver.singleResult(encoded);
        if (inputs == null || result == null) {
            return null;
        }
        result = FusionResults.canonical(result, level);
        Split split = split(holder, level, inputs, result);
        return split == null ? null : FusionCraftingPattern.encode(holder.id(), split.consumed(), split.kept(), result);
    }

    @Nullable
    private static PatternFallback.Result fusionByResult(IPatternDetails encoded, Level level) {
        if (!NepConfig.draconicFusionCrafting()) {
            return null;
        }
        List<GenericStack> inputs = FusionResults.canonical(FusionRecipeResolver.condensedInputs(encoded), level);
        GenericStack result = FusionRecipeResolver.singleResult(encoded);
        if (inputs == null || result == null) {
            return null;
        }
        result = FusionResults.canonical(result, level);
        if (FusionRecipeResolver.hasUnrepeatableResult(level, result)) {
            return PatternFallback.Result.feedback(Component.translatable("nep.encoding.fusion_item_data"));
        }

        RecipeHolder<IFusionRecipe> only = null;
        Split split = null;
        for (RecipeHolder<IFusionRecipe> candidate : FusionRecipeResolver.candidates(level)) {
            Split candidateSplit = split(candidate, level, inputs, result);
            if (candidateSplit != null) {
                if (only != null) {
                    return null;
                }
                only = candidate;
                split = candidateSplit;
            }
        }
        return only == null
                ? null
                : PatternFallback.Result.of(
                        FusionCraftingPattern.encode(only.id(), split.consumed(), split.kept(), result));
    }

    private record Split(List<GenericStack> consumed, List<GenericStack> kept) {}

    @Nullable
    private static Split split(
            RecipeHolder<IFusionRecipe> holder, Level level, List<GenericStack> inputs, GenericStack result) {
        Split supplied = splitUsing(DraconicRecipeIngredients.fusion(holder, level, true), inputs, result);
        return supplied != null
                ? supplied
                : splitUsing(DraconicRecipeIngredients.fusion(holder, level, false), inputs, result);
    }

    @Nullable
    private static Split splitUsing(
            @Nullable EncodedIngredients expected, List<GenericStack> inputs, GenericStack result) {
        GenericStack[] chosen = IngredientMatching.matchAssign(expected, inputs, result);
        if (chosen == null) {
            return null;
        }
        List<GenericStack> consumed = new ArrayList<>();
        List<GenericStack> kept = new ArrayList<>();
        for (int slot = 0; slot < chosen.length; slot++) {
            (expected.isRetained(slot) ? kept : consumed).add(chosen[slot]);
        }
        return new Split(List.copyOf(consumed), List.copyOf(kept));
    }
}
