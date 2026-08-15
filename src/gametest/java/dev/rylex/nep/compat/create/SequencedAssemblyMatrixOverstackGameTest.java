package dev.rylex.nep.compat.create;

import appeng.api.AECapabilities;
import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.core.definitions.AEBlocks;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import dev.rylex.nep.Nep;
import dev.rylex.nep.pattern.SequencedAssemblyPattern;
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
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Nep.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SequencedAssemblyMatrixOverstackGameTest {

    private static final String TEMPLATE = "empty_9x6x9";
    private static final String BATCH = "nep_matrix_overstack";

    private static final BlockPos MATRIX = new BlockPos(4, 1, 4);
    private static final BlockPos MOTOR = new BlockPos(4, 1, 3);
    private static final BlockPos ME_CONTROLLER = new BlockPos(5, 1, 4);
    private static final BlockPos ENERGY = ME_CONTROLLER.above();

    private static final Direction EJECTION = Direction.WEST;
    private static final int SADDLES_PER_CRAFT = 60;
    private static final int SADDLES_STAGED_BY_HAND = 20;

    private static final ResourceLocation ASSEMBLY =
            ResourceLocation.fromNamespaceAndPath("test", "matrix_overstack_assembly");

    private SequencedAssemblyMatrixOverstackGameTest() {}

    private static SequencedAssemblyMatrixBlockEntity powered(GameTestHelper helper) {
        helper.setBlock(
                MATRIX,
                NepCreateContent.MATRIX
                        .get()
                        .defaultBlockState()
                        .setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.Z));
        BlockEntity be = helper.getBlockEntity(MATRIX);
        helper.assertTrue(
                be instanceof SequencedAssemblyMatrixBlockEntity, "the matrix did not create its block entity");

        helper.setBlock(
                MOTOR,
                AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(DirectionalKineticBlock.FACING, Direction.SOUTH));
        BlockEntity motor = helper.getBlockEntity(MOTOR);
        helper.assertTrue(
                motor instanceof CreativeMotorBlockEntity, "the creative motor did not create its block entity");
        ((CreativeMotorBlockEntity) motor)
                .generatedSpeed.setValue(Math.round(SequencedAssemblyMatrixBlockEntity.peakSpeed()));

        helper.setBlock(ME_CONTROLLER, AEBlocks.CONTROLLER.block());
        helper.setBlock(ENERGY, AEBlocks.CREATIVE_ENERGY_CELL.block().defaultBlockState());
        return (SequencedAssemblyMatrixBlockEntity) be;
    }

    private static ICraftingMachine machine(GameTestHelper helper) {
        ICraftingMachine machine =
                helper.getLevel().getCapability(AECapabilities.CRAFTING_MACHINE, helper.absolutePos(MATRIX), null);
        helper.assertTrue(machine != null, "the matrix exposed no crafting machine capability");
        return machine;
    }

    private static IPatternDetails assemblyPattern(GameTestHelper helper) {
        ItemStack encoded = SequencedAssemblyPattern.encode(
                ASSEMBLY,
                List.of(
                        new GenericStack(AEItemKey.of(Items.CLAY_BALL), 1),
                        new GenericStack(AEItemKey.of(Items.SADDLE), SADDLES_PER_CRAFT)),
                new GenericStack(AEItemKey.of(Items.NETHERITE_SCRAP), 1));
        IPatternDetails details = PatternDetailsHelper.decodePattern(encoded, helper.getLevel());
        helper.assertTrue(details != null, "the test pattern did not decode");
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

    private static int countIn(SequencedAssemblyMatrixBlockEntity matrix, Item item) {
        int total = 0;
        for (int slot = 0; slot < matrix.getInputBuffer().getSlots(); slot++) {
            ItemStack stack = matrix.getInputBuffer().getStackInSlot(slot);
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static int outputCount(SequencedAssemblyMatrixBlockEntity matrix, Item item) {
        int total = 0;
        for (int slot = 0; slot < matrix.getOutputBuffer().getSlots(); slot++) {
            ItemStack stack = matrix.getOutputBuffer().getStackInSlot(slot);
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 400)
    public static void unstackableDemandBeyondTheSlotCountIsAcceptedAndAssembled(GameTestHelper helper) {
        SequencedAssemblyMatrixBlockEntity matrix = powered(helper);
        IPatternDetails details = assemblyPattern(helper);

        helper.assertTrue(
                machine(helper).pushPattern(details, inputsOf(details), EJECTION),
                "the matrix refused a pattern whose unstackable demand exceeds its slot count chunked by max "
                        + "stack size");

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
                    outputCount(matrix, Items.NETHERITE_SCRAP) == 1, "the matrix never finished the assembly");
            helper.assertTrue(countIn(matrix, Items.SADDLE) == 0, "the matrix left consumed saddles staged");
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void emptyBuffersReturnsAnOverstackedSlotInFull(GameTestHelper helper) {
        SequencedAssemblyMatrixBlockEntity matrix = powered(helper);
        matrix.getInputBuffer().insertItem(0, new ItemStack(Items.SADDLE, SADDLES_STAGED_BY_HAND), false);
        helper.assertTrue(
                matrix.getInputBuffer().getStackInSlot(0).getCount() == SADDLES_STAGED_BY_HAND,
                "the input buffer did not overstack the staged saddles");

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        matrix.clearBufferTo(player);

        helper.assertTrue(countIn(matrix, Items.SADDLE) == 0, "Empty Buffers left saddles behind");
        helper.assertTrue(
                player.getInventory().countItem(Items.SADDLE) == SADDLES_STAGED_BY_HAND,
                "Empty Buffers returned only part of an overstacked slot");
        helper.succeed();
    }
}
