package dev.rylex.nep.compat.draconic;

import static dev.rylex.nep.GameTestFixtures.countIn;
import static dev.rylex.nep.GameTestFixtures.inputsOf;
import static dev.rylex.nep.compat.draconic.DraconicFixtures.assertCompletesWithin;
import static dev.rylex.nep.compat.draconic.DraconicFixtures.keepCharged;
import static dev.rylex.nep.compat.draconic.DraconicFixtures.keptIngredientRecipe;
import static dev.rylex.nep.compat.draconic.DraconicFixtures.patternFor;
import static dev.rylex.nep.compat.draconic.DraconicFixtures.placeMatrix;
import static dev.rylex.nep.compat.draconic.DraconicFixtures.powerUp;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import com.brandon3055.draconicevolution.api.crafting.IFusionRecipe;
import dev.rylex.nep.Nep;
import dev.rylex.nep.pattern.FusionCraftingPattern;
import dev.rylex.nep.pattern.encoding.EncodedIngredients;
import dev.rylex.nep.pattern.encoding.PatternConverters;
import dev.rylex.nep.pattern.encoding.PatternOrigin;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

@GameTestHolder(Nep.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FusionMatrixKeptIngredientGameTest {

    private static final String TEMPLATE = "empty_5x5x5";
    private static final String BATCH = "nep_fusion_matrix";

    private FusionMatrixKeptIngredientGameTest() {}

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void aJeiTransferredKeptIngredientEncodesAsAFusionPattern(GameTestHelper helper) {
        RecipeHolder<IFusionRecipe> holder = keptIngredientRecipe(helper);
        EncodedIngredients expected = DraconicRecipeIngredients.fusion(holder, helper.getLevel());
        helper.assertTrue(expected != null, "the recipe produced no ingredient list");

        List<GenericStack> shown = new ArrayList<>();
        for (List<GenericStack> slot : expected.inputs()) {
            shown.add(slot.get(0));
        }
        ItemStack processing = PatternDetailsHelper.encodeProcessingPattern(
                shown, List.of(expected.outputs().get(0)));

        ItemStack converted = PatternConverters.convert(
                PatternOrigin.ofRecipe(holder.id()), processing, helper.makeMockPlayer(GameType.SURVIVAL));
        helper.assertTrue(
                converted != null && !converted.isEmpty(),
                "a pattern carrying the kept ingredient, as JEI transfers it, would not encode as a fusion pattern");

        IPatternDetails details = PatternDetailsHelper.decodePattern(converted, helper.getLevel());
        helper.assertTrue(details instanceof FusionCraftingPattern, "the converted pattern is not a fusion pattern");
        List<GenericStack> retained = ((FusionCraftingPattern) details).retained();
        helper.assertTrue(
                retained.size() == 1
                        && retained.get(0).what() instanceof AEItemKey key
                        && key.getItem() == Items.DIAMOND,
                "the kept ingredient was not encoded as retained, so the network would treat it as consumed");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void aKeptIngredientIsEncodedAsARemainingKey(GameTestHelper helper) {
        RecipeHolder<IFusionRecipe> holder = keptIngredientRecipe(helper);

        IPatternDetails details = patternFor(helper, holder);
        IPatternDetails.IInput kept = null;
        for (IPatternDetails.IInput input : details.getInputs()) {
            if (input.getPossibleInputs()[0].what() instanceof AEItemKey key && key.getItem() == Items.DIAMOND) {
                kept = input;
            }
        }
        helper.assertTrue(kept != null, "the kept ingredient was left out of the pattern, so JEI cannot fill it in");
        helper.assertTrue(
                kept.getRemainingKey(kept.getPossibleInputs()[0].what()) != null,
                "the kept ingredient is not marked as remaining, so a job for N crafts would demand N of it");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 400)
    public static void aSuppliedKeptIngredientIsHandedBackNotConsumed(GameTestHelper helper) {
        RecipeHolder<IFusionRecipe> holder = keptIngredientRecipe(helper);
        assertCompletesWithin(helper, holder, 300);

        FusionMatrixBlockEntity matrix = placeMatrix(helper);
        powerUp(helper);
        IPatternDetails details = patternFor(helper, holder);

        helper.assertTrue(
                matrix.pushMatrixPattern(details, inputsOf(details), Direction.UP), "a fusion pattern was rejected");
        helper.assertTrue(
                countIn(matrix.getInputBuffer(), Items.DIAMOND) == 1,
                "the pattern did not stage the kept ingredient, so nothing supplied it");
        keepCharged(helper, matrix, 320);

        helper.runAfterDelay(300, () -> {
            helper.assertTrue(
                    countIn(matrix.getOutputBuffer(), Items.GOLD_INGOT) > 0, "the kept-ingredient recipe never ran");
            helper.assertTrue(
                    countIn(matrix.getInputBuffer(), Items.DIAMOND) == 0,
                    "the kept ingredient was left stranded in the input buffer instead of going back to the network");
            helper.assertTrue(
                    countIn(matrix.getOutputBuffer(), Items.DIAMOND) == 1,
                    "the kept ingredient was consumed; the crafting CPU expects it handed back every craft");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 400)
    public static void aPatternWithoutTheKeptIngredientStillRunsFromAHandLoadedCopy(GameTestHelper helper) {
        RecipeHolder<IFusionRecipe> holder = keptIngredientRecipe(helper);
        assertCompletesWithin(helper, holder, 300);

        FusionMatrixBlockEntity matrix = placeMatrix(helper);
        powerUp(helper);
        IPatternDetails details = patternFor(helper, holder, false);

        helper.assertTrue(
                matrix.pushMatrixPattern(details, inputsOf(details), Direction.UP),
                "a fusion pattern that omits its kept ingredient was rejected");
        ItemStack kept = new ItemStack(Items.DIAMOND);
        helper.assertTrue(
                matrix.manualDemandFor(kept) > 0,
                "the matrix would not let a kept ingredient be loaded by hand, so the recipe can never run");
        matrix.getInputBuffer().insertItem(matrix.getInputBuffer().getSlots() - 1, kept.copy(), false);
        keepCharged(helper, matrix, 320);

        helper.runAfterDelay(300, () -> {
            helper.assertTrue(
                    countIn(matrix.getOutputBuffer(), Items.GOLD_INGOT) > 0, "the kept-ingredient recipe never ran");
            helper.assertTrue(
                    countIn(matrix.getInputBuffer(), Items.DIAMOND) == 1,
                    "a hand-loaded kept ingredient must stay put; the pattern never asked the network for it");
            helper.succeed();
        });
    }

    private static void fillOutputBuffer(FusionMatrixBlockEntity matrix) {
        IItemHandler output = matrix.getOutputBuffer();
        for (int slot = 0; slot < output.getSlots(); slot++) {
            output.insertItem(slot, new ItemStack(Items.NETHERITE_BLOCK, 64), false);
        }
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 400)
    public static void aFullOutputBufferMakesTheMatrixWaitInsteadOfLosingTheKeptItem(GameTestHelper helper) {
        RecipeHolder<IFusionRecipe> holder = keptIngredientRecipe(helper);
        assertCompletesWithin(helper, holder, 150);

        FusionMatrixBlockEntity matrix = placeMatrix(helper);
        powerUp(helper);
        IPatternDetails details = patternFor(helper, holder);
        helper.assertTrue(
                matrix.pushMatrixPattern(details, inputsOf(details), Direction.UP), "a fusion pattern was rejected");
        fillOutputBuffer(matrix);
        keepCharged(helper, matrix, 380);

        helper.runAfterDelay(60, () -> {
            helper.assertTrue(
                    matrix.activeResult().isEmpty(),
                    "the matrix claimed a craft it has nowhere to put; finishing it would strand the kept item");
            helper.assertTrue(
                    matrix.stall() == FusionMatrixBlockEntity.Stall.OUTPUT_FULL,
                    "a matrix blocked on a full output buffer reported " + matrix.stall() + " instead of OUTPUT_FULL");
            helper.assertTrue(
                    countIn(matrix.getInputBuffer(), Items.DIAMOND) == 1,
                    "the kept item left the input buffer while the craft was waiting on output room");

            matrix.getOutputBuffer().extractItem(0, 64, false);
            matrix.getOutputBuffer().extractItem(1, 64, false);
            helper.runAfterDelay(80, () -> {
                helper.assertTrue(
                        countIn(matrix.getOutputBuffer(), Items.GOLD_INGOT) > 0,
                        "the craft never resumed once the output buffer had room again");
                helper.assertTrue(
                        countIn(matrix.getOutputBuffer(), Items.DIAMOND) == 1,
                        "the kept item was lost while the craft waited for output room");
                helper.succeed();
            });
        });
    }

    private static int totalIn(IItemHandler handler) {
        int total = 0;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            total += handler.getStackInSlot(slot).getCount();
        }
        return total;
    }

    private static FusionMatrixBlockEntity matrixStagingAKeptItem(GameTestHelper helper, int pushes) {
        RecipeHolder<IFusionRecipe> holder = keptIngredientRecipe(helper);

        FusionMatrixBlockEntity matrix = placeMatrix(helper);
        IPatternDetails details = patternFor(helper, holder);
        for (int push = 0; push < pushes; push++) {
            helper.assertTrue(
                    matrix.pushMatrixPattern(details, inputsOf(details), Direction.UP),
                    "a fusion pattern was rejected on push " + push);
        }
        helper.assertTrue(
                countIn(matrix.getInputBuffer(), Items.DIAMOND) > 0,
                "no push staged the kept item, so nothing is at risk");
        return matrix;
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void clearingPendingRecipesLeavesAStagedKeptItemAlone(GameTestHelper helper) {
        FusionMatrixBlockEntity matrix = matrixStagingAKeptItem(helper, 1);
        int staged = totalIn(matrix.getInputBuffer());
        int kept = countIn(matrix.getInputBuffer(), Items.DIAMOND);

        matrix.clearPending();

        helper.assertTrue(!matrix.hasPending(), "clearing left the matrix still owing a craft");
        helper.assertTrue(
                totalIn(matrix.getInputBuffer()) == staged,
                "clearing pending recipes changed the input buffer from " + staged + " to "
                        + totalIn(matrix.getInputBuffer()) + " items; it must eat nothing and duplicate nothing");
        helper.assertTrue(
                countIn(matrix.getInputBuffer(), Items.DIAMOND) == kept, "clearing pending recipes ate the kept item");
        helper.assertTrue(matrix.missingInputs().isEmpty(), "clearing left a stale missing-ingredients readout behind");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void emptyingTheBuffersHandsAStagedKeptItemBackWhole(GameTestHelper helper) {
        FusionMatrixBlockEntity matrix = matrixStagingAKeptItem(helper, 1);
        int staged = totalIn(matrix.getInputBuffer()) + totalIn(matrix.getOutputBuffer());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        matrix.clearBufferTo(player);

        helper.assertTrue(
                totalIn(matrix.getInputBuffer()) == 0 && totalIn(matrix.getOutputBuffer()) == 0,
                "emptying the buffers left items behind");
        int handed = 0;
        int keptBack = 0;
        for (ItemStack stack : player.getInventory().items) {
            handed += stack.getCount();
            if (stack.is(Items.DIAMOND)) {
                keptBack += stack.getCount();
            }
        }
        helper.assertTrue(
                handed == staged,
                "emptying the buffers handed the player " + handed + " items out of " + staged
                        + " staged; nothing may be eaten and nothing duplicated");
        helper.assertTrue(keptBack == 1, "the staged kept item was not handed back");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void theKeptItemIsAskedForOnceNoMatterHowManyCraftsAreQueued(GameTestHelper helper) {
        FusionMatrixBlockEntity matrix = matrixStagingAKeptItem(helper, 3);
        IItemHandler input = matrix.getInputBuffer();
        for (int slot = 0; slot < input.getSlots(); slot++) {
            input.extractItem(slot, Integer.MAX_VALUE, false);
        }

        helper.runAfterDelay(40, () -> {
            long asked = 0;
            for (GenericStack missing : matrix.missingInputs()) {
                if (missing.what() instanceof AEItemKey key && key.getItem() == Items.DIAMOND) {
                    asked = missing.amount();
                }
            }
            helper.assertTrue(
                    asked == 1,
                    "the readout asked the network for " + asked
                            + " copies of the kept item to cover 3 queued crafts; it is handed back after every"
                            + " execution, so one copy covers the whole job");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void anItemBothConsumedAndKeptIsNotHighlightedAsRetained(GameTestHelper helper) {
        RecipeHolder<IFusionRecipe> holder = FusionRecipeResolver.resolveById(
                helper.getLevel(), ResourceLocation.fromNamespaceAndPath("test", "kept_ingredient_matrix"));
        helper.assertTrue(holder != null, "the consumed-and-kept test recipe did not load");

        FusionMatrixBlockEntity matrix = placeMatrix(helper);
        IPatternDetails details = patternFor(helper, holder);
        helper.assertTrue(
                matrix.pushMatrixPattern(details, inputsOf(details), Direction.UP), "a fusion pattern was rejected");

        for (AEItemKey key : matrix.retainedInputKeys()) {
            helper.assertTrue(
                    key.getItem() != Items.IRON_INGOT,
                    "an iron ingot is consumed as the catalyst and kept as an ingredient, so it is not fully"
                            + " retained and must not be outlined as an item the network gets back");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void clearingWithNothingPendingChangesNothing(GameTestHelper helper) {
        FusionMatrixBlockEntity matrix = placeMatrix(helper);
        helper.assertTrue(!matrix.hasPending(), "a freshly placed matrix already owed a craft");

        matrix.clearPending();
        matrix.clearPending();

        helper.assertTrue(!matrix.hasPending(), "clearing an idle matrix left it owing something");
        helper.assertTrue(matrix.missingInputs().isEmpty(), "clearing an idle matrix invented a missing ingredient");
        helper.assertTrue(
                matrix.stall() == FusionMatrixBlockEntity.Stall.NONE,
                "clearing an idle matrix left it reporting " + matrix.stall());
        helper.succeed();
    }
}
