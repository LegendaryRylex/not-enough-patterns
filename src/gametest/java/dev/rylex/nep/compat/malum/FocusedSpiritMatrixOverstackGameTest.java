package dev.rylex.nep.compat.malum;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.core.definitions.AEBlocks;
import com.sammy.malum.common.recipe.SpiritInfusionRecipe;
import com.sammy.malum.registry.common.item.MalumItems;
import dev.rylex.nep.Nep;
import dev.rylex.nep.machine.OverstackedItemHandler;
import dev.rylex.nep.pattern.SpiritInfusionPattern;
import dev.rylex.nep.pattern.encoding.EncodedIngredients;
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
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

@GameTestHolder(Nep.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FocusedSpiritMatrixOverstackGameTest {

    private static final String TEMPLATE = "empty_5x5x5";
    private static final String BATCH = "nep_focused_spirit_overstack";

    private static final BlockPos MATRIX = new BlockPos(2, 1, 2);
    private static final BlockPos ME_CONTROLLER = new BlockPos(2, 1, 3);
    private static final BlockPos ENERGY_CELL = ME_CONTROLLER.above();

    private static final ResourceLocation INPUT_OVERSTACK =
            ResourceLocation.fromNamespaceAndPath("test", "matrix_input_overstack");
    private static final ResourceLocation INPUT_FLOOD =
            ResourceLocation.fromNamespaceAndPath("test", "matrix_input_flood");
    private static final ResourceLocation SPIRIT_HEAVY =
            ResourceLocation.fromNamespaceAndPath("test", "matrix_spirit_heavy");

    private static final int SPIRITS_PER_CRAFT = 10;
    private static final int UMBRAL_SLOT = 4;

    /** Saddles do not stack, so returning more of them than this would outrun a player's own inventory. */
    private static final int STAGED_BY_HAND = 20;

    private FocusedSpiritMatrixOverstackGameTest() {}

    private static FocusedSpiritMatrixBlockEntity placeMatrix(GameTestHelper helper) {
        SpiritInfusionResolver.clearCache();
        helper.setBlock(ME_CONTROLLER, AEBlocks.CONTROLLER.block());
        helper.setBlock(ENERGY_CELL, AEBlocks.CREATIVE_ENERGY_CELL.block().defaultBlockState());
        helper.setBlock(MATRIX, NepMalumContent.MATRIX.get().defaultBlockState());
        FocusedSpiritMatrixBlockEntity matrix =
                helper.getBlockEntity(MATRIX) instanceof FocusedSpiritMatrixBlockEntity be ? be : null;
        helper.assertTrue(matrix != null, "the Focused Spirit Matrix has no block entity");
        return matrix;
    }

    private static IPatternDetails patternFor(GameTestHelper helper, ResourceLocation recipe) {
        RecipeHolder<SpiritInfusionRecipe> holder = SpiritInfusionResolver.byId(helper.getLevel(), recipe);
        helper.assertTrue(holder != null, "the test recipe " + recipe + " did not load");
        EncodedIngredients expected = MalumRecipeIngredients.spiritInfusion(holder.value());
        helper.assertTrue(expected != null, "the test recipe " + recipe + " produced no ingredient list");

        List<GenericStack> inputs = new ArrayList<>();
        for (List<GenericStack> options : expected.inputs()) {
            inputs.add(options.get(0));
        }
        ItemStack encoded =
                SpiritInfusionPattern.encode(recipe, inputs, expected.outputs().get(0));
        IPatternDetails details = PatternDetailsHelper.decodePattern(encoded, helper.getLevel());
        helper.assertTrue(details != null, "the pattern for " + recipe + " did not decode");
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

    private static int bankCapacity(IItemHandler bank) {
        return bank.getSlotLimit(UMBRAL_SLOT);
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
    public static void anInputDemandBeyondTheSlotCountIsAcceptedAndOverstacked(GameTestHelper helper) {
        FocusedSpiritMatrixBlockEntity matrix = placeMatrix(helper);
        IPatternDetails details = patternFor(helper, INPUT_OVERSTACK);

        helper.assertTrue(
                matrix.pushMatrixPattern(details, inputsOf(details), Direction.UP),
                "the Matrix refused a pattern whose unstackable demand exceeds one saddle per input slot");

        boolean overstacked = false;
        for (int slot = 0; slot < matrix.getInputBuffer().getSlots(); slot++) {
            ItemStack stack = matrix.getInputBuffer().getStackInSlot(slot);
            if (stack.is(Items.SADDLE) && stack.getCount() > stack.getMaxStackSize()) {
                overstacked = true;
            }
        }
        helper.assertTrue(overstacked, "the staged saddles were not overstacked past their max stack size");

        helper.succeedWhen(() -> {
            helper.assertTrue(
                    countIn(matrix.getOutputBuffer(), Items.GOLD_NUGGET) >= 1,
                    "the Matrix never finished a craft staged in overstacked slots");
            helper.assertTrue(
                    countIn(matrix.getInputBuffer(), Items.SADDLE) == 0, "the Matrix left consumed saddles staged");
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void anInputDemandBeyondEvenTheOverstackedCapacityIsRefusedCleanly(GameTestHelper helper) {
        FocusedSpiritMatrixBlockEntity matrix = placeMatrix(helper);
        int capacity = matrix.getInputBuffer().getSlots() * OverstackedItemHandler.SLOT_LIMIT;

        RecipeHolder<SpiritInfusionRecipe> holder = SpiritInfusionResolver.byId(helper.getLevel(), INPUT_FLOOD);
        helper.assertTrue(holder != null, "the test recipe " + INPUT_FLOOD + " did not load");
        helper.assertTrue(
                holder.value().input.count() > capacity,
                "the flood recipe asks for " + holder.value().input.count() + " saddles, which the buffer's " + capacity
                        + " slot-limited spaces can hold, so it proves nothing");

        IPatternDetails details = patternFor(helper, INPUT_FLOOD);
        helper.assertTrue(
                !matrix.pushMatrixPattern(details, inputsOf(details), Direction.UP),
                "the Matrix accepted a pattern demanding more than its buffer can ever hold");
        helper.assertTrue(
                matrix.refusal() == FocusedSpiritMatrixBlockEntity.Refusal.BUFFER_FULL,
                "the Matrix refused for " + matrix.refusal() + " rather than the full buffer");
        helper.assertTrue(
                countIn(matrix.getInputBuffer(), Items.SADDLE) == 0,
                "a refused push stranded saddles in the input buffer");
        helper.assertTrue(
                countIn(matrix.getSpiritBank(), MalumItems.ARCANE_SPIRIT.get()) == 0,
                "a refused push banked the recipe's spirits anyway");
        helper.assertTrue(matrix.pendingJobs() == 0, "a refused push still queued a job");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void spiritsBeyondTheirOwnSlotAreRefusedRatherThanDestroyed(GameTestHelper helper) {
        FocusedSpiritMatrixBlockEntity matrix = placeMatrix(helper);
        IItemHandler bank = matrix.getSpiritBank();

        int limit = bankCapacity(bank);
        int prefill = limit - SPIRITS_PER_CRAFT + 6;
        helper.assertTrue(
                bank.insertItem(UMBRAL_SLOT, new ItemStack(MalumItems.UMBRAL_SPIRIT.get(), prefill), false)
                        .isEmpty(),
                "the bank would not take " + prefill + " Umbral Spirits");

        IPatternDetails details = patternFor(helper, SPIRIT_HEAVY);
        helper.assertTrue(
                !matrix.pushMatrixPattern(details, inputsOf(details), Direction.UP),
                "the Matrix accepted a craft whose spirits overflow the one bank slot that can hold them; the "
                        + "overflow has nowhere to go and is destroyed on staging");
        helper.assertTrue(
                matrix.refusal() == FocusedSpiritMatrixBlockEntity.Refusal.BUFFER_FULL,
                "the Matrix refused for " + matrix.refusal() + " rather than the full spirit bank");
        helper.assertTrue(
                countIn(bank, MalumItems.UMBRAL_SPIRIT.get()) == prefill,
                "a refused push changed what the spirit bank was holding");
        helper.assertTrue(
                countIn(matrix.getInputBuffer(), Items.CLAY_BALL) == 0,
                "a push refused over its spirits still staged its item input");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void spiritsFillingTheirSlotExactlyAreAccepted(GameTestHelper helper) {
        FocusedSpiritMatrixBlockEntity matrix = placeMatrix(helper);
        IItemHandler bank = matrix.getSpiritBank();

        int limit = bankCapacity(bank);
        int prefill = limit - SPIRITS_PER_CRAFT;
        helper.assertTrue(
                bank.insertItem(UMBRAL_SLOT, new ItemStack(MalumItems.UMBRAL_SPIRIT.get(), prefill), false)
                        .isEmpty(),
                "the bank would not take " + prefill + " Umbral Spirits");

        IPatternDetails details = patternFor(helper, SPIRIT_HEAVY);
        helper.assertTrue(
                matrix.pushMatrixPattern(details, inputsOf(details), Direction.UP),
                "the Matrix refused a craft whose spirits fill the bank slot exactly");
        helper.assertTrue(
                countIn(bank, MalumItems.UMBRAL_SPIRIT.get()) == limit,
                "the staged spirits did not bring the bank slot up to its limit");
        matrix.clearPending();
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void emptyBuffersReturnsAnOverstackedSlotAndLeavesTheSpiritBank(GameTestHelper helper) {
        FocusedSpiritMatrixBlockEntity matrix = placeMatrix(helper);
        int staged = STAGED_BY_HAND;
        int banked = 12;

        matrix.getInputBuffer().insertItem(0, new ItemStack(Items.SADDLE, staged), false);
        helper.assertTrue(
                matrix.getInputBuffer().getStackInSlot(0).getCount() == staged,
                "the input buffer did not overstack the staged saddles");
        matrix.getSpiritBank().insertItem(UMBRAL_SLOT, new ItemStack(MalumItems.UMBRAL_SPIRIT.get(), banked), false);

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        matrix.clearBufferTo(player);

        helper.assertTrue(
                countIn(matrix.getInputBuffer(), Items.SADDLE) == 0, "Empty Buffers left saddles in the input buffer");
        helper.assertTrue(
                countIn(matrix.getSpiritBank(), MalumItems.UMBRAL_SPIRIT.get()) == banked,
                "Empty Buffers took spirits out of the bank");
        helper.assertTrue(
                player.getInventory().countItem(Items.SADDLE) == staged,
                "Empty Buffers returned only part of an overstacked slot");
        helper.assertTrue(
                player.getInventory().countItem(MalumItems.UMBRAL_SPIRIT.get()) == 0,
                "Empty Buffers handed the banked spirits to the player");
        helper.succeed();
    }
}
