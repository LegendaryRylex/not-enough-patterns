package dev.rylex.nep.compat.malum;

import static dev.rylex.nep.GameTestFixtures.countIn;
import static dev.rylex.nep.GameTestFixtures.inputsOf;

import appeng.api.crafting.IPatternDetails;
import com.sammy.malum.common.recipe.RuneworkingRecipe;
import com.sammy.malum.registry.common.block.MalumBlocks;
import dev.rylex.nep.Nep;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

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
        return MalumFixtures.placeMatrix(helper, ME_CONTROLLER, ENERGY_CELL, MATRIX);
    }

    private static RecipeHolder<RuneworkingRecipe> spiritFedRecipe(GameTestHelper helper) {
        return MalumFixtures.pickBest(
                helper,
                RuneworkingResolver.candidates(helper.getLevel()),
                holder -> {
                    ItemStack[] secondary =
                            holder.value().secondaryInput.ingredient().getItems();
                    return holder.id().getNamespace().equals("malum")
                            && MalumRecipeIngredients.runeworking(holder.value()) != null
                            && secondary.length != 0
                            && SpiritBank.hasDedicatedSlot(secondary[0]);
                },
                MalumFixtures.byId(),
                "no runeworking recipe pays in spirits, so the bank path is untested");
    }

    private static IPatternDetails patternFor(GameTestHelper helper, RecipeHolder<RuneworkingRecipe> holder) {
        return MalumFixtures.runeworkingPattern(helper, holder);
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
                        .insertItem(0, new ItemStack(MalumBlocks.RUNEWOOD_OBELISK.get(), max), false)
                        .isEmpty(),
                "the upgrade slot would not take a full stack of Runewood Obelisks");
        helper.assertTrue(matrix.craftTicks() < bare, "Runewood Obelisks did not cut the craft time");
        helper.succeed();
    }
}
