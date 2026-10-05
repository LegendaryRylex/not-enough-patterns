package dev.rylex.nep.compat.malum;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.core.definitions.AEBlocks;
import com.sammy.malum.common.recipe.RuneworkingRecipe;
import dev.rylex.nep.Nep;
import dev.rylex.nep.pattern.RuneworkingPattern;
import dev.rylex.nep.pattern.encoding.EncodedIngredients;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

@GameTestHolder(Nep.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FocusedSpiritMatrixRuneworkingGameTest {

    private static final String TEMPLATE = "empty_5x5x5";
    private static final String BATCH = "nep_focused_spirit_runeworking";

    private static final BlockPos MATRIX = new BlockPos(2, 1, 2);
    private static final BlockPos ME_CONTROLLER = new BlockPos(2, 1, 3);
    private static final BlockPos ENERGY_CELL = ME_CONTROLLER.above();

    private FocusedSpiritMatrixRuneworkingGameTest() {}

    private static FocusedSpiritMatrixBlockEntity placeMatrix(GameTestHelper helper) {
        RuneworkingResolver.clearCache();
        helper.setBlock(ME_CONTROLLER, AEBlocks.CONTROLLER.block());
        helper.setBlock(ENERGY_CELL, AEBlocks.CREATIVE_ENERGY_CELL.block().defaultBlockState());
        helper.setBlock(MATRIX, NepMalumContent.MATRIX.get().defaultBlockState());
        FocusedSpiritMatrixBlockEntity matrix =
                helper.getBlockEntity(MATRIX) instanceof FocusedSpiritMatrixBlockEntity be ? be : null;
        helper.assertTrue(matrix != null, "the Focused Spirit Matrix has no block entity");
        return matrix;
    }

    private static RecipeHolder<RuneworkingRecipe> spiritFedRecipe(GameTestHelper helper) {
        RecipeHolder<RuneworkingRecipe> best = null;
        for (RecipeHolder<RuneworkingRecipe> holder : RuneworkingResolver.candidates(helper.getLevel())) {
            ItemStack[] secondary = holder.value().secondaryInput.ingredient().getItems();
            if (!holder.id().getNamespace().equals("malum")
                    || MalumRecipeIngredients.runeworking(holder.value()) == null
                    || secondary.length == 0
                    || !SpiritBank.hasDedicatedSlot(secondary[0])) {
                continue;
            }
            if (best == null || holder.id().compareTo(best.id()) < 0) {
                best = holder;
            }
        }
        helper.assertTrue(best != null, "no runeworking recipe pays in spirits, so the bank path is untested");
        return best;
    }

    private static IPatternDetails patternFor(GameTestHelper helper, RecipeHolder<RuneworkingRecipe> holder) {
        EncodedIngredients expected = MalumRecipeIngredients.runeworking(holder.value());
        helper.assertTrue(expected != null, "the runeworking recipe produced no ingredient list");

        List<GenericStack> inputs = new ArrayList<>();
        for (List<GenericStack> options : expected.inputs()) {
            inputs.add(options.get(0));
        }
        ItemStack encoded = RuneworkingPattern.encode(
                holder.id(), inputs, expected.outputs().get(0));
        IPatternDetails details = PatternDetailsHelper.decodePattern(encoded, helper.getLevel());
        helper.assertTrue(details != null, "the runeworking pattern did not decode");
        return details;
    }

    private static KeyCounter[] inputsOf(IPatternDetails details) {
        KeyCounter[] inputs = new KeyCounter[details.getInputs().length];
        for (int slot = 0; slot < inputs.length; slot++) {
            inputs[slot] = new KeyCounter();
            IPatternDetails.IInput input = details.getInputs()[slot];
            inputs[slot].add(input.getPossibleInputs()[0].what(), input.getMultiplier());
        }
        return inputs;
    }

    private static int countIn(IItemHandler handler, Item item) {
        int total = 0;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 400)
    public static void aRuneworkingPatternRunsToCompletionInTheMatrix(GameTestHelper helper) {
        FocusedSpiritMatrixBlockEntity matrix = placeMatrix(helper);

        RecipeHolder<RuneworkingRecipe> holder = spiritFedRecipe(helper);
        IPatternDetails details = patternFor(helper, holder);
        helper.assertTrue(
                matrix.pushMatrixPattern(details, inputsOf(details), Direction.UP),
                "the Matrix refused a runeworking pattern");

        ItemStack result = holder.value().output;
        helper.succeedWhen(() -> helper.assertTrue(
                countIn(matrix.getOutputBuffer(), result.getItem()) >= result.getCount(),
                "the Matrix never finished the rune"));
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void aRunesSpiritsAreStagedInTheBankNotTheBuffer(GameTestHelper helper) {
        FocusedSpiritMatrixBlockEntity matrix = placeMatrix(helper);

        RecipeHolder<RuneworkingRecipe> holder = spiritFedRecipe(helper);
        ItemStack spirit = holder.value().secondaryInput.ingredient().getItems()[0];
        IPatternDetails details = patternFor(helper, holder);
        helper.assertTrue(
                matrix.pushMatrixPattern(details, inputsOf(details), Direction.UP),
                "the Matrix refused a runeworking pattern");

        helper.assertTrue(
                countIn(matrix.getSpiritBank(), spirit.getItem())
                        == holder.value().secondaryInput.count(),
                "a rune's spirits did not go into the spirit bank the way an infusion's do");
        helper.assertTrue(
                countIn(matrix.getInputBuffer(), spirit.getItem()) == 0,
                "a rune's spirits were staged in the input buffer as ordinary items as well");
        matrix.clearPending();
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void obelisksCutTheRuneTimeTheWayTheyCutAnInfusion(GameTestHelper helper) {
        FocusedSpiritMatrixBlockEntity matrix = placeMatrix(helper);
        int bare = matrix.craftTicks();

        int max = FocusedSpiritMatrixUpgrades.maxObelisks();
        helper.assertTrue(
                matrix.getUpgradeSlot()
                        .insertItem(
                                0,
                                new ItemStack(
                                        com.sammy.malum.registry.common.block.MalumBlocks.RUNEWOOD_OBELISK.get(), max),
                                false)
                        .isEmpty(),
                "the upgrade slot would not take a full stack of Runewood Obelisks");
        helper.assertTrue(matrix.craftTicks() < bare, "Runewood Obelisks did not cut the craft time");
        helper.succeed();
    }
}
