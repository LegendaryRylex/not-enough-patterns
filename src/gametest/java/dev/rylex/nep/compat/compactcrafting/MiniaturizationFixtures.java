package dev.rylex.nep.compat.compactcrafting;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.GenericStack;
import dev.compactmods.crafting.recipes.MiniaturizationRecipe;
import dev.rylex.nep.machine.ManualCraftFixtures;
import dev.rylex.nep.machine.ManualRequirement;
import dev.rylex.nep.pattern.MiniaturizationPattern;
import dev.rylex.nep.pattern.encoding.EncodedIngredients;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.Nullable;

final class MiniaturizationFixtures {
    private MiniaturizationFixtures() {}

    static IPatternDetails patternFor(GameTestHelper helper, RecipeHolder<MiniaturizationRecipe> holder) {
        EncodedIngredients expected = MiniaturizationRecipeIngredients.miniaturization(holder.value());
        helper.assertTrue(expected != null, "the recipe produced no ingredient list");
        ItemStack encoded = MiniaturizationPattern.encode(
                holder.id(), firstOptions(expected), expected.outputs().get(0));
        IPatternDetails details = PatternDetailsHelper.decodePattern(encoded, helper.getLevel());
        helper.assertTrue(details != null, "the miniaturization pattern did not decode");
        return details;
    }

    static List<GenericStack> firstOptions(EncodedIngredients expected) {
        List<GenericStack> inputs = new ArrayList<>();
        for (List<GenericStack> slot : expected.inputs()) {
            inputs.add(slot.get(0));
        }
        return inputs;
    }

    @Nullable
    static RecipeHolder<MiniaturizationRecipe> shortestRecipe(
            GameTestHelper helper, Predicate<MiniaturizationRecipe> accepts) {
        RecipeHolder<MiniaturizationRecipe> shortest = null;
        for (RecipeHolder<MiniaturizationRecipe> candidate :
                MiniaturizationRecipeResolver.candidates(helper.getLevel())) {
            if (MiniaturizationRecipeIngredients.miniaturization(candidate.value()) == null
                    || !accepts.test(candidate.value())) {
                continue;
            }
            if (shortest == null
                    || candidate.value().getCraftingTime() < shortest.value().getCraftingTime()) {
                shortest = candidate;
            }
        }
        return shortest;
    }

    static Player manualCraftPlayer(GameTestHelper helper, RecipeHolder<MiniaturizationRecipe> holder, String failure) {
        List<ManualRequirement> requirements = MiniaturizationRecipeIngredients.manualRequirements(holder.value());
        helper.assertTrue(requirements != null, failure);
        return ManualCraftFixtures.playerWith(helper, requirements);
    }
}
