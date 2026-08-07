package dev.rylex.nep.compat.apothic;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.pattern.EnchantingPattern;
import dev.rylex.nep.pattern.PatternStacks;
import dev.rylex.nep.pattern.encoding.PatternConverters;
import dev.rylex.nep.pattern.encoding.PatternFallback;
import dev.shadowsoffire.apothic_enchanting.table.infusion.InfusionRecipe;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

final class ApothicPatternEncoders {
    private ApothicPatternEncoders() {}

    static void register() {
        PatternConverters.register(InfusionRecipe.class, ApothicPatternEncoders::infusion);
        PatternConverters.registerFallback(ApothicPatternEncoders::infusionByResult);
    }

    @Nullable
    private static ItemStack infusion(IPatternDetails encoded, RecipeHolder<InfusionRecipe> holder, Level level) {
        if (!NepConfig.apothicInfusion()) {
            return null;
        }
        InfusionRecipe recipe = holder.value();
        ItemStack output = recipe.getOutput();
        if (output.isEmpty() || InfusionRecipeResolver.statsFor(recipe) == null) {
            return null;
        }
        GenericStack expected = GenericStack.fromItemStack(output);
        GenericStack result = PatternStacks.singleResult(encoded);
        if (expected == null || result == null || !result.equals(expected)) {
            return null;
        }
        AEItemKey input = chooseInput(encoded, recipe);
        if (input == null) {
            return null;
        }
        return encode(holder.id(), recipe, input, expected);
    }

    @Nullable
    private static PatternFallback.Result infusionByResult(IPatternDetails encoded, Level level) {
        if (!NepConfig.apothicInfusion()) {
            return null;
        }
        InfusionRecipeResolver.Plan plan =
                InfusionRecipeResolver.resolve(encoded, level, InfusionCosts.Rates.fromConfig());
        if (plan == null) {
            return null;
        }
        return PatternFallback.Result.of(
                encode(plan.holder().id(), plan.holder().value(), plan.input(), plan.result()));
    }

    private static ItemStack encode(
            ResourceLocation recipe, InfusionRecipe infusion, AEItemKey input, GenericStack result) {
        return EnchantingPattern.encode(recipe, ApothicRecipeIngredients.chosen(infusion, input), result);
    }

    @Nullable
    private static AEItemKey chooseInput(IPatternDetails encoded, InfusionRecipe recipe) {
        List<GenericStack> inputs = PatternStacks.condensedInputs(encoded);
        if (inputs == null) {
            return null;
        }
        for (GenericStack stack : inputs) {
            if (stack.what() instanceof AEItemKey key && recipe.getInput().test(key.toStack())) {
                return key;
            }
        }
        return null;
    }
}
