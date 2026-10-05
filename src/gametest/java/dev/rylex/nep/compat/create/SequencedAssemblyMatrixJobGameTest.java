package dev.rylex.nep.compat.create;

import appeng.api.AECapabilities;
import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.core.definitions.AEBlocks;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import dev.rylex.nep.Nep;
import dev.rylex.nep.pattern.AndesiteCraftingPattern;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Nep.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SequencedAssemblyMatrixJobGameTest {

    private static final String TEMPLATE = "empty_9x6x9";
    private static final String BATCH = "nep_matrix_jobs";

    private static final BlockPos MATRIX = new BlockPos(4, 1, 4);
    private static final BlockPos MOTOR = new BlockPos(4, 1, 3);
    private static final BlockPos ME_CONTROLLER = new BlockPos(5, 1, 4);
    private static final BlockPos ENERGY = ME_CONTROLLER.above();

    private static final Direction EJECTION = Direction.WEST;
    private static final int WATER_PER_CRAFT = 250;

    private static final ResourceLocation FILLING = ResourceLocation.fromNamespaceAndPath("test", "matrix_filling");
    private static final ResourceLocation DEPLOYING = ResourceLocation.fromNamespaceAndPath("test", "matrix_deploying");
    private static final ResourceLocation KEPT_TOOL =
            ResourceLocation.fromNamespaceAndPath("test", "matrix_deploying_kept_tool");
    private static final ResourceLocation WORN_TOOL =
            ResourceLocation.fromNamespaceAndPath("test", "matrix_deploying_worn_tool");
    private static final ResourceLocation COMPONENT_FILLING =
            ResourceLocation.fromNamespaceAndPath("test", "matrix_filling_components");

    private static final int PICKAXE_DAMAGE = 7;

    private SequencedAssemblyMatrixJobGameTest() {}

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

    private static IPatternDetails decode(GameTestHelper helper, ItemStack encoded) {
        IPatternDetails details = PatternDetailsHelper.decodePattern(encoded, helper.getLevel());
        helper.assertTrue(details != null, "the test pattern did not decode");
        return details;
    }

    private static IPatternDetails fillingPattern(GameTestHelper helper) {
        return decode(
                helper,
                AndesiteCraftingPattern.encode(
                        FILLING,
                        List.of(
                                new GenericStack(AEItemKey.of(Items.BRICK), 1),
                                new GenericStack(AEFluidKey.of(Fluids.WATER), WATER_PER_CRAFT)),
                        new GenericStack(AEItemKey.of(Items.QUARTZ), 1)));
    }

    private static IPatternDetails deployingPattern(GameTestHelper helper) {
        return decode(
                helper,
                AndesiteCraftingPattern.encode(
                        DEPLOYING,
                        List.of(
                                new GenericStack(AEItemKey.of(Items.IRON_INGOT), 1),
                                new GenericStack(AEItemKey.of(Items.GOLD_INGOT), 1)),
                        new GenericStack(AEItemKey.of(Items.EMERALD), 1)));
    }

    private static IPatternDetails keptToolPattern(GameTestHelper helper) {
        return decode(
                helper,
                AndesiteCraftingPattern.encode(
                        KEPT_TOOL,
                        List.of(new GenericStack(AEItemKey.of(Items.COPPER_INGOT), 1)),
                        List.of(new GenericStack(AEItemKey.of(Items.DIAMOND), 1)),
                        new GenericStack(AEItemKey.of(Items.AMETHYST_SHARD), 1)));
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

    private static void push(GameTestHelper helper, IPatternDetails details, String what) {
        helper.assertTrue(
                machine(helper).pushPattern(details, inputsOf(details), EJECTION),
                "the matrix refused the " + what + " pattern");
    }

    private static ItemStack damagedPickaxe() {
        ItemStack stack = new ItemStack(Items.IRON_PICKAXE);
        stack.setDamageValue(PICKAXE_DAMAGE);
        return stack;
    }

    private static int outputCount(SequencedAssemblyMatrixBlockEntity matrix, ItemStack wanted) {
        int total = 0;
        for (int slot = 0; slot < matrix.getOutputBuffer().getSlots(); slot++) {
            ItemStack stack = matrix.getOutputBuffer().getStackInSlot(slot);
            if (ItemStack.isSameItemSameComponents(stack, wanted)) {
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

    private static String describe(SequencedAssemblyMatrixBlockEntity matrix) {
        return "(flags=" + matrix.statusFlags() + " progress=" + matrix.craftProgress() + " in0="
                + matrix.getInputBuffer().getStackInSlot(0) + " out0="
                + matrix.getOutputBuffer().getStackInSlot(0) + " tank0="
                + matrix.fluidHandler().getFluidInTank(0) + ")";
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 400)
    public static void theMatrixFillsAnItemWithoutASpout(GameTestHelper helper) {
        SequencedAssemblyMatrixBlockEntity matrix = powered(helper);
        push(helper, fillingPattern(helper), "filling");

        helper.succeedWhen(() -> {
            helper.assertTrue(outputCount(matrix, Items.QUARTZ) == 1, "the matrix filled nothing " + describe(matrix));
            helper.assertTrue(
                    matrix.fluidHandler().getFluidInTank(0).isEmpty(),
                    "the matrix kept the fluid the filling recipe consumed " + describe(matrix));
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 400)
    public static void theMatrixDeploysWithoutADeployer(GameTestHelper helper) {
        SequencedAssemblyMatrixBlockEntity matrix = powered(helper);
        push(helper, deployingPattern(helper), "deploying");

        helper.succeedWhen(() -> {
            helper.assertTrue(
                    outputCount(matrix, Items.EMERALD) == 1, "the matrix deployed nothing " + describe(matrix));
            helper.assertTrue(
                    matrix.getInputBuffer().getStackInSlot(0).isEmpty(),
                    "the matrix left its deploying inputs staged " + describe(matrix));
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 400)
    public static void aKeptToolIsHandedBackAlongsideTheResult(GameTestHelper helper) {
        SequencedAssemblyMatrixBlockEntity matrix = powered(helper);
        push(helper, keptToolPattern(helper), "kept tool deploying");

        helper.succeedWhen(() -> {
            helper.assertTrue(
                    outputCount(matrix, Items.AMETHYST_SHARD) == 1, "the matrix deployed nothing " + describe(matrix));
            helper.assertTrue(
                    outputCount(matrix, Items.DIAMOND) == 1,
                    "the matrix swallowed the tool this recipe keeps; the network is owed it back " + describe(matrix));
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void aToolCreateWouldWearDownIsRefused(GameTestHelper helper) {
        powered(helper);
        IPatternDetails details = decode(
                helper,
                AndesiteCraftingPattern.encode(
                        WORN_TOOL,
                        List.of(new GenericStack(AEItemKey.of(Items.OAK_PLANKS), 1)),
                        List.of(),
                        List.of(new GenericStack(AEItemKey.of(Items.WOODEN_SHOVEL), 1)),
                        new GenericStack(AEItemKey.of(Items.STICK), 1)));

        helper.assertTrue(
                !machine(helper).pushPattern(details, inputsOf(details), EJECTION),
                "the matrix accepted a deploying recipe whose tool a Deployer only damages; running it would eat a "
                        + "whole shovel per craft and hand the network back an item its pattern never named");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 400)
    public static void theMatrixFillsAnOutputThatCarriesDataComponents(GameTestHelper helper) {
        SequencedAssemblyMatrixBlockEntity matrix = powered(helper);
        IPatternDetails details = decode(
                helper,
                AndesiteCraftingPattern.encode(
                        COMPONENT_FILLING,
                        List.of(
                                new GenericStack(AEItemKey.of(Items.APPLE), 1),
                                new GenericStack(AEFluidKey.of(Fluids.WATER), WATER_PER_CRAFT)),
                        new GenericStack(AEItemKey.of(damagedPickaxe()), 1)));
        push(helper, details, "component-carrying filling");

        helper.succeedWhen(() -> {
            helper.assertTrue(
                    outputCount(matrix, damagedPickaxe()) == 1,
                    "the matrix produced no pickaxe at the damage its pattern named " + describe(matrix));
            helper.assertTrue(
                    !matrix.hasPending(),
                    "the matrix still owes the component-carrying output it already made " + describe(matrix));
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 400)
    public static void aPlainProcessingPatternRunsWhenOneRecipeFitsIt(GameTestHelper helper) {
        SequencedAssemblyMatrixBlockEntity matrix = powered(helper);
        IPatternDetails details = decode(
                helper,
                PatternDetailsHelper.encodeProcessingPattern(
                        List.of(
                                new GenericStack(AEItemKey.of(Items.BRICK), 1),
                                new GenericStack(AEFluidKey.of(Fluids.WATER), WATER_PER_CRAFT)),
                        List.of(new GenericStack(AEItemKey.of(Items.QUARTZ), 1))));
        push(helper, details, "processing");

        helper.succeedWhen(() -> helper.assertTrue(
                outputCount(matrix, Items.QUARTZ) == 1,
                "the matrix took a processing pattern one filling recipe fits and then never ran it "
                        + describe(matrix)));
    }
}
