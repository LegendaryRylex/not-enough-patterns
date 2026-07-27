package dev.rylex.nep.compat.create;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import dev.rylex.nep.pattern.encoding.EncodedIngredients;
import java.util.List;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

class CreateRecipeIngredientsTest {

    private static GenericStack stack(Item item, long amount) {
        return new GenericStack(AEItemKey.of(item), amount);
    }

    private static List<GenericStack> slot(GenericStack... options) {
        return List.of(options);
    }

    @Test
    void satisfiesMatchesADirectOneToOneAssignment() {
        EncodedIngredients expected = new EncodedIngredients(
                List.of(slot(stack(Items.IRON_INGOT, 2)), slot(stack(Items.GOLD_INGOT, 1))),
                List.of(stack(Items.DIAMOND, 1)));

        assertTrue(CreateRecipeIngredients.satisfies(
                expected, List.of(stack(Items.IRON_INGOT, 2), stack(Items.GOLD_INGOT, 1))));
    }

    @Test
    void satisfiesBacktracksWhenTheGreedyChoiceWouldFail() {
        Item x = Items.IRON_INGOT;
        Item y = Items.GOLD_INGOT;
        Item z = Items.COPPER_INGOT;
        EncodedIngredients expected = new EncodedIngredients(
                List.of(slot(stack(x, 1), stack(z, 1)), slot(stack(x, 1), stack(y, 1)), slot(stack(x, 1), stack(y, 1))),
                List.of(stack(Items.DIAMOND, 1)));

        assertTrue(
                CreateRecipeIngredients.satisfies(expected, List.of(stack(x, 1), stack(y, 1), stack(z, 1))),
                "the assignment z/x/y exists; a greedy first-option pick must not lose it");
    }

    @Test
    void satisfiesRejectsLeftoverInputs() {
        EncodedIngredients expected =
                new EncodedIngredients(List.of(slot(stack(Items.IRON_INGOT, 1))), List.of(stack(Items.DIAMOND, 1)));

        assertFalse(CreateRecipeIngredients.satisfies(
                expected, List.of(stack(Items.IRON_INGOT, 1), stack(Items.GOLD_INGOT, 1))));
    }

    @Test
    void satisfiesRejectsAShortfall() {
        EncodedIngredients expected =
                new EncodedIngredients(List.of(slot(stack(Items.IRON_INGOT, 4))), List.of(stack(Items.DIAMOND, 1)));

        assertFalse(CreateRecipeIngredients.satisfies(expected, List.of(stack(Items.IRON_INGOT, 3))));
    }
}
