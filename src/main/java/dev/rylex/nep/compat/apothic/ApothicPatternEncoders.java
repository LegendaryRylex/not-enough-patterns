package dev.rylex.nep.compat.apothic;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.pattern.EnchantingPattern;
import dev.rylex.nep.pattern.PatternStacks;
import dev.rylex.nep.pattern.encoding.PatternConverters;
import dev.rylex.nep.pattern.encoding.PatternFallback;
import dev.rylex.nep.util.Recipes;
import dev.shadowsoffire.apothic_enchanting.table.infusion.InfusionRecipe;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.Identifier;
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
        ItemStack output = recipe.getOutput().create();
        if (output.isEmpty() || InfusionRecipeResolver.statsFor(recipe) == null) {
            return null;
        }
        GenericStack expected = GenericStack.fromItemStack(output);
        GenericStack result = PatternStacks.singleResult(encoded);
        if (expected == null || result == null || !result.equals(expected)) {
            return null;
        }
        List<GenericStack> inputs = PatternStacks.condensedInputs(encoded);
        if (inputs == null) {
            return null;
        }
        AEItemKey input = chooseInput(inputs, recipe);
        if (input == null) {
            return null;
        }
        return encode(Recipes.idOf(holder), recipe, input, paymentIn(inputs), expected);
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
        AEKey payment = InfusionPayments.paymentIn(plan.expected().keySet());
        return PatternFallback.Result.of(
                encode(Recipes.idOf(plan.holder()), plan.holder().value(), plan.input(), payment, plan.result()));
    }

    private static ItemStack encode(
            Identifier recipe, InfusionRecipe infusion, AEItemKey input, @Nullable AEKey payment, GenericStack result) {
        return EnchantingPattern.encode(recipe, ApothicRecipeIngredients.chosen(infusion, input, payment), result);
    }

    @Nullable
    private static AEKey paymentIn(List<GenericStack> inputs) {
        List<AEKey> keys = new ArrayList<>(inputs.size());
        for (GenericStack stack : inputs) {
            keys.add(stack.what());
        }
        return InfusionPayments.paymentIn(keys);
    }

    @Nullable
    private static AEItemKey chooseInput(List<GenericStack> inputs, InfusionRecipe recipe) {
        for (GenericStack stack : inputs) {
            if (stack.what() instanceof AEItemKey key && recipe.getInput().test(key.toStack())) {
                return key;
            }
        }
        return null;
    }
}
