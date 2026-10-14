package dev.rylex.nep.compat.mysticalagriculture;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.GenericStack;
import com.blakebr0.mysticalagriculture.api.crafting.IAwakeningRecipe;
import com.blakebr0.mysticalagriculture.api.crafting.IInfusionRecipe;
import dev.rylex.nep.pattern.AwakeningPattern;
import dev.rylex.nep.pattern.InfusionPattern;
import dev.rylex.nep.pattern.encoding.EncodedIngredients;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;

final class MysticalFixtures {
    private MysticalFixtures() {}

    @FunctionalInterface
    private interface PatternEncoder {
        ItemStack encode(Identifier id, List<GenericStack> inputs, GenericStack output);
    }

    static <R extends Recipe<?>> RecipeHolder<R> firstById(
            GameTestHelper helper,
            Iterable<RecipeHolder<R>> candidates,
            Predicate<RecipeHolder<R>> keep,
            String failure) {
        RecipeHolder<R> best = null;
        for (RecipeHolder<R> holder : candidates) {
            if (!keep.test(holder)) {
                continue;
            }
            if (best == null || holder.id().identifier().compareTo(best.id().identifier()) < 0) {
                best = holder;
            }
        }
        helper.assertTrue(best != null, failure);
        return best;
    }

    static RecipeHolder<IInfusionRecipe> simplestInfusion(GameTestHelper helper) {
        return firstById(
                helper,
                MysticalRecipeResolver.infusionCandidates(helper.getLevel()),
                holder -> holder.id().identifier().getNamespace().equals("mysticalagriculture")
                        && MysticalRecipeIngredients.infusion(holder.value(), helper.getLevel()) != null,
                "no infusion recipe was loaded");
    }

    static RecipeHolder<IAwakeningRecipe> simplestAwakening(GameTestHelper helper) {
        return firstById(
                helper,
                MysticalRecipeResolver.awakeningCandidates(helper.getLevel()),
                holder -> MysticalRecipeIngredients.awakening(holder.value(), helper.getLevel()) != null,
                "no awakening recipe was loaded");
    }

    static IPatternDetails awakeningPattern(GameTestHelper helper, RecipeHolder<IAwakeningRecipe> holder) {
        EncodedIngredients expected = MysticalRecipeIngredients.awakening(holder.value(), helper.getLevel());
        helper.assertTrue(expected != null, "the recipe produced no ingredient list");
        return decoded(helper, holder.id().identifier(), expected, AwakeningPattern::encode, "awakening");
    }

    static IPatternDetails infusionPattern(GameTestHelper helper, RecipeHolder<IInfusionRecipe> holder) {
        EncodedIngredients expected = MysticalRecipeIngredients.infusion(holder.value(), helper.getLevel());
        helper.assertTrue(expected != null, "the recipe produced no ingredient list");
        return decoded(helper, holder.id().identifier(), expected, InfusionPattern::encode, "infusion");
    }

    private static IPatternDetails decoded(
            GameTestHelper helper, Identifier id, EncodedIngredients expected, PatternEncoder encoder, String kind) {
        List<GenericStack> inputs = new ArrayList<>();
        for (List<GenericStack> options : expected.inputs()) {
            inputs.add(options.get(0));
        }
        ItemStack encoded = encoder.encode(id, inputs, expected.outputs().get(0));
        IPatternDetails details = PatternDetailsHelper.decodePattern(encoded, helper.getLevel());
        helper.assertTrue(details != null, "the " + kind + " pattern did not decode");
        return details;
    }
}
