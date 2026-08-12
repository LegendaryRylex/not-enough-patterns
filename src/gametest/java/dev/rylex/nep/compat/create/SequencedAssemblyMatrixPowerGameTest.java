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
import com.simibubi.create.AllItems;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import dev.rylex.nep.Nep;
import dev.rylex.nep.NepItems;
import dev.rylex.nep.pattern.SequencedAssemblyPattern;
import dev.rylex.nep.pattern.encoding.PatternConverters;
import dev.rylex.nep.pattern.encoding.PatternOrigin;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;

@GameTestHolder(Nep.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SequencedAssemblyMatrixPowerGameTest {

    private static final String TEMPLATE = "empty_9x6x9";
    private static final String BATCH = "nep_matrix_powered";

    private static final BlockPos MATRIX = new BlockPos(4, 1, 4);
    private static final BlockPos MOTOR = new BlockPos(4, 1, 3);
    private static final BlockPos ENERGY = new BlockPos(5, 1, 4);

    private static final Direction EJECTION = Direction.WEST;
    private static final int FILL_PER_STEP = 500;

    private SequencedAssemblyMatrixPowerGameTest() {}

    private static SequencedAssemblyMatrixBlockEntity placeMatrix(GameTestHelper helper) {
        helper.setBlock(
                MATRIX,
                NepCreateContent.MATRIX
                        .get()
                        .defaultBlockState()
                        .setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.Z));
        BlockEntity be = helper.getBlockEntity(MATRIX);
        helper.assertTrue(
                be instanceof SequencedAssemblyMatrixBlockEntity, "the matrix did not create its block entity");
        return (SequencedAssemblyMatrixBlockEntity) be;
    }

    private static void placeMotor(GameTestHelper helper, int rpm) {
        helper.setBlock(
                MOTOR,
                AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(DirectionalKineticBlock.FACING, Direction.SOUTH));
        BlockEntity be = helper.getBlockEntity(MOTOR);
        helper.assertTrue(be instanceof CreativeMotorBlockEntity, "the creative motor did not create its block entity");
        ((CreativeMotorBlockEntity) be).generatedSpeed.setValue(rpm);
    }

    private static void placeEnergy(GameTestHelper helper) {
        helper.setBlock(ENERGY, AEBlocks.CREATIVE_ENERGY_CELL.block().defaultBlockState());
    }

    private static ResourceLocation plateRecipe(GameTestHelper helper) {
        for (RecipeHolder<SequencedAssemblyRecipe> holder : helper.getLevel()
                .getRecipeManager()
                .getAllRecipesFor(
                        AllRecipeTypes.SEQUENCED_ASSEMBLY.<RecipeWrapper, SequencedAssemblyRecipe>getType())) {
            for (ProcessingOutput output : holder.value().resultPool) {
                if (output.getStack().is(NepCreateContent.HARDENED_OBSIDIAN_PLATE.get())) {
                    return holder.id();
                }
            }
        }
        helper.fail("no sequenced assembly recipe produces the hardened obsidian plate");
        return null;
    }

    private static IPatternDetails platePattern(GameTestHelper helper) {
        ItemStack encoded = SequencedAssemblyPattern.encode(
                plateRecipe(helper),
                List.of(
                        new GenericStack(AEItemKey.of(AllItems.POWDERED_OBSIDIAN.get()), 1),
                        new GenericStack(AEFluidKey.of(Fluids.LAVA), FILL_PER_STEP),
                        new GenericStack(AEFluidKey.of(Fluids.WATER), FILL_PER_STEP)),
                new GenericStack(AEItemKey.of(NepCreateContent.HARDENED_OBSIDIAN_PLATE.get()), 1));
        IPatternDetails details = PatternDetailsHelper.decodePattern(encoded, helper.getLevel());
        helper.assertTrue(details != null, "the hardened obsidian plate pattern did not decode");
        return details;
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void encodingWithoutJeiStillYieldsAnAssemblyPattern(GameTestHelper helper) {
        placeMatrix(helper);
        ItemStack processing = PatternDetailsHelper.encodeProcessingPattern(
                List.of(
                        new GenericStack(AEItemKey.of(AllItems.POWDERED_OBSIDIAN.get()), 1),
                        new GenericStack(AEFluidKey.of(Fluids.LAVA), FILL_PER_STEP),
                        new GenericStack(AEFluidKey.of(Fluids.WATER), FILL_PER_STEP)),
                List.of(new GenericStack(AEItemKey.of(NepCreateContent.HARDENED_OBSIDIAN_PLATE.get()), 1)));

        ItemStack converted =
                PatternConverters.convert(PatternOrigin.MANUAL, processing, helper.makeMockPlayer(GameType.SURVIVAL));
        helper.assertTrue(
                converted != null && converted.is(NepItems.SEQUENCED_ASSEMBLY_PATTERN.get()),
                "a hand-encoded processing pattern was not converted without a JEI recipe intent; sequenced assembly "
                        + "would be unautomatable without JEI");

        IPatternDetails details = PatternDetailsHelper.decodePattern(converted, helper.getLevel());
        helper.assertTrue(details != null, "the converted pattern did not decode");
        ICraftingMachine machine =
                helper.getLevel().getCapability(AECapabilities.CRAFTING_MACHINE, helper.absolutePos(MATRIX), null);
        helper.assertTrue(machine != null, "the matrix exposed no crafting machine capability");
        helper.assertTrue(
                machine.pushPattern(details, inputsOf(details), EJECTION),
                "the matrix refused the pattern the fallback produced");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void aRecipeViewerTransferIsLeftAsAProcessingPattern(GameTestHelper helper) {
        placeMatrix(helper);
        ItemStack processing = PatternDetailsHelper.encodeProcessingPattern(
                List.of(
                        new GenericStack(AEItemKey.of(AllItems.POWDERED_OBSIDIAN.get()), 1),
                        new GenericStack(AEFluidKey.of(Fluids.LAVA), FILL_PER_STEP),
                        new GenericStack(AEFluidKey.of(Fluids.WATER), FILL_PER_STEP)),
                List.of(new GenericStack(AEItemKey.of(NepCreateContent.HARDENED_OBSIDIAN_PLATE.get()), 1)));

        ItemStack converted = PatternConverters.convert(
                PatternOrigin.RECIPE_VIEWER, processing, helper.makeMockPlayer(GameType.SURVIVAL));
        helper.assertTrue(
                converted == null,
                "a recipe viewer filled these slots for a machine NEP does not drive, and NEP still claimed them as "
                        + converted + "; the pattern belongs to whichever machine the open category was for");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void theMatrixRefusesAProcessingPattern(GameTestHelper helper) {
        placeMatrix(helper);
        ICraftingMachine machine =
                helper.getLevel().getCapability(AECapabilities.CRAFTING_MACHINE, helper.absolutePos(MATRIX), null);
        helper.assertTrue(machine != null, "the matrix exposed no crafting machine capability");

        ItemStack encoded = PatternDetailsHelper.encodeProcessingPattern(
                List.of(
                        new GenericStack(AEItemKey.of(AllItems.POWDERED_OBSIDIAN.get()), 1),
                        new GenericStack(AEFluidKey.of(Fluids.LAVA), FILL_PER_STEP),
                        new GenericStack(AEFluidKey.of(Fluids.WATER), FILL_PER_STEP)),
                List.of(new GenericStack(AEItemKey.of(NepCreateContent.HARDENED_OBSIDIAN_PLATE.get()), 1)));
        IPatternDetails details = PatternDetailsHelper.decodePattern(encoded, helper.getLevel());
        helper.assertTrue(details != null, "the processing pattern did not decode");
        helper.assertTrue(
                !machine.pushPattern(details, inputsOf(details), EJECTION),
                "the matrix accepted a plain processing pattern; sequenced assembly takes its own pattern only");
        helper.succeed();
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

    private static void pushPlatePattern(GameTestHelper helper) {
        ICraftingMachine machine =
                helper.getLevel().getCapability(AECapabilities.CRAFTING_MACHINE, helper.absolutePos(MATRIX), null);
        helper.assertTrue(machine != null, "the matrix exposed no crafting machine capability");
        IPatternDetails details = platePattern(helper);
        helper.assertTrue(
                machine.pushPattern(details, inputsOf(details), EJECTION),
                "the matrix refused the hardened obsidian plate pattern");
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
        StringBuilder tanks = new StringBuilder();
        IFluidHandler fluids = matrix.fluidHandler();
        for (int tank = 0; tank < fluids.getTanks(); tank++) {
            tanks.append(fluids.getFluidInTank(tank)).append(' ');
        }
        return "(flags=" + matrix.statusFlags() + " speed=" + matrix.getSpeed() + " stress=" + matrix.stressDraw()
                + " progress=" + matrix.craftProgress() + " input0="
                + matrix.getInputBuffer().getStackInSlot(0) + " out0="
                + matrix.getOutputBuffer().getStackInSlot(0) + " tanks="
                + tanks.toString().trim() + ")";
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 400)
    public static void aFullyPoweredMatrixAssemblesThePatternItWasPushed(GameTestHelper helper) {
        SequencedAssemblyMatrixBlockEntity matrix = placeMatrix(helper);
        placeMotor(helper, Math.round(SequencedAssemblyMatrixBlockEntity.peakSpeed()));
        placeEnergy(helper);
        pushPlatePattern(helper);

        helper.succeedWhen(() -> {
            helper.assertTrue(
                    matrix.getSpeed() != 0, "the creative motor never drove the matrix through its shaft face");
            helper.assertTrue(
                    (matrix.statusFlags() & SequencedAssemblyMatrixBlockEntity.FLAG_POWERED) != 0,
                    "the matrix never joined the creative energy cell's grid");
            helper.assertTrue(
                    matrix.getInputBuffer().getStackInSlot(0).isEmpty(),
                    "the matrix did not consume its base item " + describe(matrix));
            ItemStack produced = matrix.getOutputBuffer().getStackInSlot(0);
            helper.assertTrue(!produced.isEmpty(), "the matrix finished no craft " + describe(matrix));
            helper.assertTrue(
                    produced.is(NepCreateContent.HARDENED_OBSIDIAN_PLATE.get()),
                    "the matrix produced " + produced + " instead of the plate its pattern named; another sequenced "
                            + "assembly recipe sharing the obsidian dust base won the scan " + describe(matrix));
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 600)
    public static void aFullOutputBufferBlocksTheFinishUntilSpaceFrees(GameTestHelper helper) {
        SequencedAssemblyMatrixBlockEntity matrix = placeMatrix(helper);
        placeMotor(helper, Math.round(SequencedAssemblyMatrixBlockEntity.peakSpeed()));
        placeEnergy(helper);
        for (int slot = 1; slot < matrix.getOutputBuffer().getSlots(); slot++) {
            matrix.getOutputBuffer().insertItem(slot, new ItemStack(Items.DIRT, 64), false);
        }
        pushPlatePattern(helper);

        int[] phase = {0};
        helper.succeedWhen(() -> {
            if (phase[0] == 0) {
                helper.assertTrue(matrix.craftProgress() > 0.0F, "the craft has not started yet " + describe(matrix));
                matrix.getOutputBuffer().insertItem(0, new ItemStack(Items.DIRT, 64), false);
                phase[0] = 1;
            }
            if (phase[0] == 1) {
                helper.assertTrue(
                        (matrix.statusFlags() & SequencedAssemblyMatrixBlockEntity.FLAG_OUTPUT_BLOCKED) != 0,
                        "a full output buffer never reported as blocked " + describe(matrix));
                matrix.getOutputBuffer().extractItem(0, 64, false);
                phase[0] = 2;
            }
            helper.assertTrue(
                    outputCount(matrix, NepCreateContent.HARDENED_OBSIDIAN_PLATE.get()) == 1,
                    "the finished plate never landed after space was freed " + describe(matrix));
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 400)
    public static void breakingTheMatrixMidCraftDropsTheIngredientsNotTheResult(GameTestHelper helper) {
        SequencedAssemblyMatrixBlockEntity matrix = placeMatrix(helper);
        placeMotor(helper, Math.round(SequencedAssemblyMatrixBlockEntity.peakSpeed()));
        placeEnergy(helper);
        pushPlatePattern(helper);

        boolean[] broken = {false};
        helper.succeedWhen(() -> {
            if (!broken[0]) {
                helper.assertTrue(
                        matrix.craftProgress() > 0.0F && matrix.craftProgress() < 1.0F,
                        "the craft has not started yet " + describe(matrix));
                broken[0] = true;
                helper.destroyBlock(MATRIX);
            }
            helper.assertItemEntityPresent(AllItems.POWDERED_OBSIDIAN.get(), MATRIX, 2.0);
            helper.assertItemEntityNotPresent(NepCreateContent.HARDENED_OBSIDIAN_PLATE.get(), MATRIX, 2.0);
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 400)
    public static void theKineticNetworkSeesTheMatrixStress(GameTestHelper helper) {
        SequencedAssemblyMatrixBlockEntity matrix = placeMatrix(helper);
        placeMotor(helper, Math.round(SequencedAssemblyMatrixBlockEntity.peakSpeed()));
        placeEnergy(helper);

        helper.succeedWhen(() -> {
            helper.assertTrue(matrix.getSpeed() != 0, "the creative motor never drove the matrix");
            helper.assertTrue(matrix.hasNetwork(), "the matrix never joined a kinetic network");
            helper.assertTrue(
                    matrix.getOrCreateNetwork().calculateStress() > 0,
                    "the kinetic network saw no stress from the matrix; its impact is speed derived and Create "
                            + "caches the value from attach time, when the speed is still zero");
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void aMatrixBelowTheMinimumSpeedNeverStarts(GameTestHelper helper) {
        SequencedAssemblyMatrixBlockEntity matrix = placeMatrix(helper);
        placeMotor(helper, Math.max(1, Math.round(SequencedAssemblyMatrixBlockEntity.minimumSpeed()) - 1));
        placeEnergy(helper);
        pushPlatePattern(helper);

        helper.runAfterDelay(120, () -> {
            helper.assertTrue(matrix.getSpeed() != 0, "the creative motor never drove the matrix");
            helper.assertTrue(matrix.stressDraw() == 0, "an under-geared matrix taxed the kinetic network");
            helper.assertTrue(matrix.craftProgress() == 0.0F, "an under-geared matrix made craft progress");
            helper.assertTrue(
                    outputCount(matrix, NepCreateContent.HARDENED_OBSIDIAN_PLATE.get()) == 0,
                    "an under-geared matrix produced a result");
            helper.assertTrue(
                    matrix.getInputBuffer().getStackInSlot(0).getCount() == 1,
                    "an under-geared matrix consumed its base item");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void aMatrixWithoutNetworkPowerNeverStarts(GameTestHelper helper) {
        SequencedAssemblyMatrixBlockEntity matrix = placeMatrix(helper);
        placeMotor(helper, Math.round(SequencedAssemblyMatrixBlockEntity.peakSpeed()));
        pushPlatePattern(helper);

        helper.runAfterDelay(120, () -> {
            helper.assertTrue(matrix.getSpeed() != 0, "the creative motor never drove the matrix");
            helper.assertTrue(
                    (matrix.statusFlags() & SequencedAssemblyMatrixBlockEntity.FLAG_POWERED) == 0,
                    "the matrix reported network power with no energy cell present");
            helper.assertTrue(matrix.craftProgress() == 0.0F, "an unpowered matrix made craft progress");
            helper.assertTrue(
                    outputCount(matrix, NepCreateContent.HARDENED_OBSIDIAN_PLATE.get()) == 0,
                    "an unpowered matrix produced a result");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void aHopperMayTopUpOnlyWhatAnOwedJobIsShortOf(GameTestHelper helper) {
        SequencedAssemblyMatrixBlockEntity matrix = placeMatrix(helper);
        pushPlatePattern(helper);
        matrix.getInputBuffer().extractItem(0, 64, false);

        helper.runAfterDelay(30, () -> {
            helper.assertTrue(
                    !matrix.missingInputs().isEmpty(),
                    "the matrix never reported the base item it is short of " + describe(matrix));

            IItemHandler view =
                    helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(MATRIX), null);
            helper.assertTrue(view != null, "the matrix exposed no item handler");

            ItemStack unrelated = new ItemStack(Items.DIAMOND, 1);
            helper.assertTrue(
                    view.insertItem(0, unrelated, false).getCount() == 1,
                    "a hopper fed the matrix an item no owed job asked for");
            helper.assertTrue(!view.isItemValid(0, unrelated), "the matrix advertised an undemanded item as valid");

            ItemStack offered = new ItemStack(AllItems.POWDERED_OBSIDIAN.get(), 4);
            ItemStack refused = view.insertItem(0, offered, false);
            helper.assertTrue(
                    refused.getCount() == 3,
                    "the matrix took " + (4 - refused.getCount()) + " base items when it was short of exactly one; "
                            + "a hopper must top up a job, not stockpile past it");
            helper.assertTrue(
                    matrix.getInputBuffer().getStackInSlot(0).getCount() == 1,
                    "the accepted top-up did not land in the input buffer " + describe(matrix));
            helper.assertTrue(
                    view.insertItem(0, new ItemStack(AllItems.POWDERED_OBSIDIAN.get(), 1), false)
                                    .getCount()
                            == 1,
                    "the matrix kept taking base items after the shortfall was met");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void aMatrixOwedNothingCraftsNothingFromLooseIngredients(GameTestHelper helper) {
        SequencedAssemblyMatrixBlockEntity matrix = placeMatrix(helper);
        placeMotor(helper, Math.round(SequencedAssemblyMatrixBlockEntity.peakSpeed()));
        placeEnergy(helper);

        matrix.getInputBuffer().insertItem(0, new ItemStack(AllItems.POWDERED_OBSIDIAN.get(), 1), false);
        IFluidHandler tanks = matrix.fluidHandler();
        tanks.fill(new FluidStack(Fluids.LAVA, FILL_PER_STEP), IFluidHandler.FluidAction.EXECUTE);
        tanks.fill(new FluidStack(Fluids.WATER, FILL_PER_STEP), IFluidHandler.FluidAction.EXECUTE);

        helper.runAfterDelay(120, () -> {
            helper.assertTrue(matrix.getSpeed() != 0, "the creative motor never drove the matrix");
            helper.assertTrue(
                    matrix.craftProgress() == 0.0F,
                    "a matrix owing nothing started a craft from loose ingredients " + describe(matrix));
            helper.assertTrue(
                    matrix.getInputBuffer().getStackInSlot(0).getCount() == 1,
                    "a matrix owing nothing consumed loose ingredients " + describe(matrix));
            helper.assertTrue(
                    matrix.getOutputBuffer().getStackInSlot(0).isEmpty(),
                    "a matrix owing nothing produced a result " + describe(matrix));
            helper.succeed();
        });
    }
}
