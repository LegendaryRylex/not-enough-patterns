package dev.rylex.nep.pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

class EmpoweringPatternTest {

    private static final ResourceLocation RECIPE = ResourceLocation.parse("actuallyadditions:empowering/diamatine");

    private static EmpoweringPattern pattern(List<GenericStack> inputs) {
        ItemStack encoded = EmpoweringPattern.encode(RECIPE, inputs, new GenericStack(AEItemKey.of(Items.DIAMOND), 1));
        return new EmpoweringPattern(AEItemKey.of(encoded), null);
    }

    private static GenericStack one(net.minecraft.world.item.Item item) {
        return new GenericStack(AEItemKey.of(item), 1);
    }

    @Test
    void aDuplicateModifierCondensesIntoOneInputWithTwoOfIt() {
        EmpoweringPattern pattern = pattern(List.of(
                one(Items.DIAMOND_BLOCK),
                one(Items.LIGHT_BLUE_DYE),
                one(Items.CLAY_BALL),
                one(Items.CLAY_BALL),
                one(Items.CLAY)));

        IPatternDetails.IInput[] inputs = pattern.getInputs();

        assertEquals(4, inputs.length, "the two clay balls must condense into a single input");
        long clay = 0;
        for (IPatternDetails.IInput input : inputs) {
            if (input.getPossibleInputs()[0].what().equals(AEItemKey.of(Items.CLAY_BALL))) {
                clay = input.getMultiplier();
            }
        }
        assertEquals(2, clay, "the condensed clay ball input must ask for two");
    }

    @Test
    void everyIngredientIsConsumed() {
        EmpoweringPattern pattern = pattern(List.of(
                one(Items.DIAMOND_BLOCK),
                one(Items.LIGHT_BLUE_DYE),
                one(Items.CLAY_BALL),
                one(Items.CLAY_BALL),
                one(Items.CLAY)));

        assertTrue(pattern.retained().isEmpty(), "empowering keeps nothing back");
        for (IPatternDetails.IInput input : pattern.getInputs()) {
            assertNull(
                    input.getRemainingKey(input.getPossibleInputs()[0].what()),
                    "no empowering ingredient is returned to the network");
        }
    }

    @Test
    void theRecipeIdSurvivesEncoding() {
        assertEquals(RECIPE, pattern(List.of(one(Items.DIAMOND_BLOCK))).recipe());
    }
}
