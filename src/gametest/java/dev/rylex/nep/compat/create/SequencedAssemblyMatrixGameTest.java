package dev.rylex.nep.compat.create;

import dev.rylex.nep.Nep;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

@GameTestHolder(Nep.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SequencedAssemblyMatrixGameTest {

    private static final String TEMPLATE = "empty_5x5x5";
    private static final String BATCH = "nep_matrix";
    private static final BlockPos MATRIX = new BlockPos(2, 1, 2);

    private SequencedAssemblyMatrixGameTest() {}

    private static SequencedAssemblyMatrixBlockEntity place(GameTestHelper helper) {
        helper.setBlock(MATRIX, NepCreateContent.MATRIX.get().defaultBlockState());
        BlockEntity be = helper.getBlockEntity(MATRIX);
        helper.assertTrue(
                be instanceof SequencedAssemblyMatrixBlockEntity, "the matrix did not create its block entity");
        return (SequencedAssemblyMatrixBlockEntity) be;
    }

    private static int comparator(GameTestHelper helper) {
        BlockPos absolute = helper.absolutePos(MATRIX);
        return helper.getLevel().getBlockState(absolute).getAnalogOutputSignal(helper.getLevel(), absolute);
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void idlesWithoutKineticPower(GameTestHelper helper) {
        SequencedAssemblyMatrixBlockEntity matrix = place(helper);

        helper.assertTrue(matrix.getSpeed() == 0, "an unpowered matrix reported rotational speed");
        helper.assertTrue(matrix.stressDraw() == 0, "an unpowered matrix drew stress");
        helper.assertTrue(matrix.craftProgress() == 0.0F, "an unpowered matrix made craft progress");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void unpoweredMatrixNeverConsumesItsInput(GameTestHelper helper) {
        SequencedAssemblyMatrixBlockEntity matrix = place(helper);
        matrix.getInputBuffer().insertItem(0, new ItemStack(Items.OBSIDIAN, 8), false);

        helper.runAfterDelay(40, () -> {
            helper.assertTrue(
                    matrix.getInputBuffer().getStackInSlot(0).getCount() == 8,
                    "an unpowered matrix consumed its staged input");
            helper.assertTrue(
                    matrix.getOutputBuffer().getStackInSlot(0).isEmpty(), "an unpowered matrix produced an output");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void hoppersAndPipesCannotStageIngredientsForNoJob(GameTestHelper helper) {
        SequencedAssemblyMatrixBlockEntity matrix = place(helper);
        IItemHandler handler =
                helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(MATRIX), null);
        helper.assertTrue(handler != null, "the matrix exposed no item handler");

        int inputSlots = matrix.getInputBuffer().getSlots();
        for (int slot = 0; slot < inputSlots; slot++) {
            helper.assertTrue(
                    handler.insertItem(slot, new ItemStack(Items.OBSIDIAN, 4), false)
                                    .getCount()
                            == 4,
                    "a hopper or pipe staged ingredients in slot " + slot + " for a matrix that owes nothing; loose "
                            + "ingredients name no recipe, so an unrelated one sharing the same base could claim them");
            helper.assertTrue(
                    !handler.isItemValid(slot, new ItemStack(Items.OBSIDIAN)),
                    "input slot " + slot + " advertised itself as insertable");
        }
        helper.assertTrue(
                matrix.getInputBuffer().getStackInSlot(0).isEmpty(), "a refused insert still reached the input buffer");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void stagedInputCannotBePulledBackOut(GameTestHelper helper) {
        SequencedAssemblyMatrixBlockEntity matrix = place(helper);
        matrix.getInputBuffer().insertItem(0, new ItemStack(Items.OBSIDIAN, 4), false);

        IItemHandler handler =
                helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(MATRIX), null);
        helper.assertTrue(handler != null, "the matrix exposed no item handler");

        for (int slot = 0; slot < handler.getSlots(); slot++) {
            helper.assertTrue(
                    handler.extractItem(slot, 64, true).getItem() != Items.OBSIDIAN,
                    "a pipe drained staged ingredients back out of slot " + slot);
        }
        helper.assertTrue(matrix.getInputBuffer().getStackInSlot(0).getCount() == 4, "staged ingredients went missing");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void resultSlotsRefuseInserts(GameTestHelper helper) {
        SequencedAssemblyMatrixBlockEntity matrix = place(helper);
        IItemHandler handler =
                helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(MATRIX), null);
        helper.assertTrue(handler != null, "the matrix exposed no item handler");

        int inputSlots = matrix.getInputBuffer().getSlots();
        for (int slot = inputSlots; slot < handler.getSlots(); slot++) {
            helper.assertTrue(
                    !handler.insertItem(slot, new ItemStack(Items.DIAMOND, 1), false)
                            .isEmpty(),
                    "an adjacent inventory stuffed items into result slot " + slot);
            helper.assertTrue(
                    !handler.isItemValid(slot, new ItemStack(Items.DIAMOND)),
                    "result slot " + slot + " advertised itself as insertable");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void finishedOutputCanBeExtractedThroughTheExposedHandler(GameTestHelper helper) {
        SequencedAssemblyMatrixBlockEntity matrix = place(helper);
        matrix.getOutputBuffer().insertItem(0, new ItemStack(Items.DIAMOND, 3), false);

        IItemHandler handler =
                helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(MATRIX), null);
        helper.assertTrue(handler != null, "the matrix exposed no item handler");

        boolean found = false;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            if (handler.extractItem(slot, 3, true).getCount() == 3) {
                found = true;
                break;
            }
        }
        helper.assertTrue(found, "a finished result could not be extracted from the matrix");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void comparatorReadsTheOutputBufferAndNothingElse(GameTestHelper helper) {
        SequencedAssemblyMatrixBlockEntity matrix = place(helper);

        helper.assertTrue(comparator(helper) == 0, "an empty matrix emitted a signal");

        matrix.getInputBuffer().insertItem(0, new ItemStack(Items.OBSIDIAN, 64), false);
        helper.assertTrue(comparator(helper) == 0, "the input buffer reached the comparator");

        matrix.getOutputBuffer().insertItem(0, new ItemStack(Items.DIAMOND, 64), false);
        helper.assertTrue(comparator(helper) > 0, "a stocked output buffer emitted no signal");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void breakingTheMatrixDropsItsStagedItems(GameTestHelper helper) {
        SequencedAssemblyMatrixBlockEntity matrix = place(helper);
        matrix.getInputBuffer().insertItem(0, new ItemStack(Items.OBSIDIAN, 5), false);
        matrix.getOutputBuffer().insertItem(0, new ItemStack(Items.DIAMOND, 2), false);

        helper.destroyBlock(MATRIX);

        helper.assertItemEntityPresent(Items.OBSIDIAN, MATRIX, 2.0);
        helper.assertItemEntityPresent(Items.DIAMOND, MATRIX, 2.0);
        helper.succeed();
    }
}
