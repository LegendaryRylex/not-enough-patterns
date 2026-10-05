package dev.rylex.nep.compat.draconic;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.core.definitions.AEBlocks;
import com.brandon3055.draconicevolution.api.crafting.IFusionRecipe;
import dev.rylex.nep.Nep;
import dev.rylex.nep.machine.ManualCraftOutcome;
import dev.rylex.nep.machine.ManualCraftResult;
import dev.rylex.nep.pattern.encoding.EncodedIngredients;
import dev.rylex.nep.pattern.encoding.PatternConverters;
import dev.rylex.nep.pattern.encoding.PatternOrigin;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

@GameTestHolder(Nep.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FusionMatrixManualCraftGameTest {

    private static final String TEMPLATE = "empty_5x5x5";
    private static final String BATCH = "nep_fusion_matrix_manual";

    private static final BlockPos MATRIX = new BlockPos(2, 1, 2);
    private static final BlockPos ME_CONTROLLER = new BlockPos(2, 1, 3);
    private static final BlockPos ENERGY_CELL = ME_CONTROLLER.above();

    private static final ResourceLocation RECIPE =
            ResourceLocation.fromNamespaceAndPath("test", "kept_ingredient_matrix");

    private FusionMatrixManualCraftGameTest() {}

    private static FusionMatrixBlockEntity place(GameTestHelper helper) {
        helper.setBlock(MATRIX, NepDraconicContent.MATRIX.get().defaultBlockState());
        BlockEntity be = helper.getBlockEntity(MATRIX);
        helper.assertTrue(be instanceof FusionMatrixBlockEntity, "the matrix did not create its block entity");
        return (FusionMatrixBlockEntity) be;
    }

    private static void powerUp(GameTestHelper helper) {
        helper.setBlock(ME_CONTROLLER, AEBlocks.CONTROLLER.block());
        helper.setBlock(ENERGY_CELL, AEBlocks.CREATIVE_ENERGY_CELL.block().defaultBlockState());
    }

    private static void keepCharged(GameTestHelper helper, FusionMatrixBlockEntity matrix, int ticks) {
        for (int tick = 0; tick <= ticks; tick++) {
            helper.runAtTickTime(tick, () -> matrix.energyStorage().modify(matrix.energyCapacity()));
        }
    }

    private static RecipeHolder<IFusionRecipe> recipe(GameTestHelper helper) {
        RecipeHolder<IFusionRecipe> holder = FusionRecipeResolver.resolveById(helper.getLevel(), RECIPE);
        helper.assertTrue(holder != null, "the kept-ingredient matrix test recipe did not load");
        return holder;
    }

    private static Player playerHolding(GameTestHelper helper, Item item, int count) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getInventory().add(new ItemStack(item, count));
        return player;
    }

    private static int countIn(IItemHandler handler, Item item) {
        int total = 0;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack held = handler.getStackInSlot(slot);
            if (held.is(item)) {
                total += held.getCount();
            }
        }
        return total;
    }

    private static int carried(Player player, Item item) {
        int total = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void aManualCraftTakesOneKeptItemAndOneCatalystPerBatch(GameTestHelper helper) {
        FusionMatrixBlockEntity matrix = place(helper);
        recipe(helper);
        Player player = playerHolding(helper, Items.IRON_INGOT, 8);

        ManualCraftOutcome outcome = matrix.startManualCraft(player, RECIPE, 2);

        helper.assertTrue(
                outcome.status() == ManualCraftResult.STARTED,
                "a manual craft the player could pay for was refused as " + outcome.status());
        helper.assertTrue(outcome.batches() == 2, "the matrix queued " + outcome.batches() + " crafts instead of 2");
        helper.assertTrue(
                countIn(matrix.getInputBuffer(), Items.IRON_INGOT) == 3,
                "two catalysts and one kept ingot make 3 staged, but the buffer holds "
                        + countIn(matrix.getInputBuffer(), Items.IRON_INGOT));
        helper.assertTrue(
                carried(player, Items.IRON_INGOT) == 5,
                "the player was left with " + carried(player, Items.IRON_INGOT) + " ingots out of 8; exactly the 3"
                        + " staged may leave the inventory");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void aManualCraftTheInventoryCannotPayForMovesNothing(GameTestHelper helper) {
        FusionMatrixBlockEntity matrix = place(helper);
        recipe(helper);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        ManualCraftOutcome outcome = matrix.startManualCraft(player, RECIPE, 1);

        helper.assertTrue(
                outcome.status() == ManualCraftResult.MISSING_ITEMS,
                "an empty inventory started a craft anyway, reporting " + outcome.status());
        helper.assertTrue(
                countIn(matrix.getInputBuffer(), Items.IRON_INGOT) == 0, "a refused manual craft staged items anyway");
        helper.assertTrue(!matrix.hasPending(), "a refused manual craft left a job queued");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void onlyTheBatchesTheInventoryCoversAreQueued(GameTestHelper helper) {
        FusionMatrixBlockEntity matrix = place(helper);
        recipe(helper);
        Player player = playerHolding(helper, Items.IRON_INGOT, 4);

        ManualCraftOutcome outcome = matrix.startManualCraft(player, RECIPE, 64);

        helper.assertTrue(
                outcome.status() == ManualCraftResult.STARTED,
                "a manual craft the player could partly pay for was refused as " + outcome.status());
        helper.assertTrue(
                outcome.batches() == 3,
                "4 ingots cover 3 crafts plus the kept one, but " + outcome.batches() + " were queued");
        helper.assertTrue(carried(player, Items.IRON_INGOT) == 0, "the player kept ingots the craft had claimed");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 400)
    public static void aManualCraftLeavesItsResultInTheOutputBuffer(GameTestHelper helper) {
        FusionMatrixBlockEntity matrix = place(helper);
        powerUp(helper);
        recipe(helper);
        Player player = playerHolding(helper, Items.IRON_INGOT, 8);

        helper.assertTrue(
                matrix.startManualCraft(player, RECIPE, 2).status() == ManualCraftResult.STARTED,
                "the manual craft was refused before it could run");
        keepCharged(helper, matrix, 320);

        helper.runAfterDelay(300, () -> {
            helper.assertTrue(
                    countIn(matrix.getOutputBuffer(), Items.EMERALD) == 2,
                    "the output buffer holds " + countIn(matrix.getOutputBuffer(), Items.EMERALD)
                            + " results out of 2; a hand-started craft has no network to hand them to");
            helper.assertTrue(
                    countIn(matrix.getInputBuffer(), Items.IRON_INGOT) == 1,
                    "the kept ingot did not survive the job in the input buffer");
            helper.assertTrue(!matrix.hasPending(), "the matrix still owes a manual craft it has already run");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void aPatternForAManuallyQueuedResultIsRefused(GameTestHelper helper) {
        FusionMatrixBlockEntity matrix = place(helper);
        RecipeHolder<IFusionRecipe> holder = recipe(helper);
        Player player = playerHolding(helper, Items.IRON_INGOT, 8);

        helper.assertTrue(
                matrix.startManualCraft(player, RECIPE, 1).status() == ManualCraftResult.STARTED,
                "the manual craft was refused, so this test proves nothing");

        IPatternDetails details = patternFor(helper, holder);
        helper.assertTrue(
                !matrix.pushMatrixPattern(details, inputsOf(details), Direction.UP),
                "a pattern was accepted for a result a hand-started craft is already queued for; the matrix cannot"
                        + " tell whose item the next result is");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void aManualCraftForAResultAPatternOwnsIsRefused(GameTestHelper helper) {
        FusionMatrixBlockEntity matrix = place(helper);
        RecipeHolder<IFusionRecipe> holder = recipe(helper);
        IPatternDetails details = patternFor(helper, holder);

        helper.assertTrue(
                matrix.pushMatrixPattern(details, inputsOf(details), Direction.UP),
                "the pattern was refused, so this test proves nothing");
        Player player = playerHolding(helper, Items.IRON_INGOT, 8);

        ManualCraftOutcome outcome = matrix.startManualCraft(player, RECIPE, 1);

        helper.assertTrue(
                outcome.status() == ManualCraftResult.BUSY,
                "a manual craft was queued behind a pattern for the same result, reporting " + outcome.status());
        helper.assertTrue(carried(player, Items.IRON_INGOT) == 8, "a refused manual craft took the player's items");
        helper.succeed();
    }

    private static IPatternDetails patternFor(GameTestHelper helper, RecipeHolder<IFusionRecipe> holder) {
        EncodedIngredients encoded = DraconicRecipeIngredients.fusion(holder, helper.getLevel());
        helper.assertTrue(encoded != null, "the test recipe produced no ingredient list");
        List<GenericStack> inputs = new ArrayList<>();
        for (List<GenericStack> slot : encoded.inputs()) {
            inputs.add(slot.get(0));
        }
        ItemStack processing = PatternDetailsHelper.encodeProcessingPattern(
                inputs, List.of(encoded.outputs().get(0)));
        ItemStack converted = PatternConverters.convert(
                PatternOrigin.ofRecipe(holder.id()), processing, helper.makeMockPlayer(GameType.SURVIVAL));
        helper.assertTrue(converted != null && !converted.isEmpty(), "the test recipe would not encode as a pattern");
        IPatternDetails details = PatternDetailsHelper.decodePattern(converted, helper.getLevel());
        helper.assertTrue(details != null, "the encoded pattern did not decode");
        return details;
    }

    private static KeyCounter[] inputsOf(IPatternDetails details) {
        KeyCounter[] inputs = new KeyCounter[details.getInputs().length];
        for (int index = 0; index < inputs.length; index++) {
            IPatternDetails.IInput input = details.getInputs()[index];
            KeyCounter counter = new KeyCounter();
            GenericStack template = input.getPossibleInputs()[0];
            counter.add((AEItemKey) template.what(), template.amount() * input.getMultiplier());
            inputs[index] = counter;
        }
        return inputs;
    }
}
