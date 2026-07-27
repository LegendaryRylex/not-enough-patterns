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
import dev.rylex.nep.pattern.SequencedAssemblyPattern;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;

@GameTestHolder(Nep.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SequencedAssemblyMatrixReturnGameTest {

    private static final String TEMPLATE = "empty_9x6x9";
    private static final String BATCH = "nep_matrix_return";

    private static final BlockPos MATRIX = new BlockPos(4, 1, 4);
    private static final BlockPos MOTOR = new BlockPos(4, 1, 3);
    private static final BlockPos ENERGY = new BlockPos(5, 1, 4);
    private static final BlockPos TARGET = new BlockPos(3, 1, 4);

    private static final Direction EJECTION = Direction.WEST;
    private static final int FILL_PER_STEP = 500;

    private SequencedAssemblyMatrixReturnGameTest() {}

    private static SequencedAssemblyMatrixBlockEntity placePoweredMatrix(GameTestHelper helper) {
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

        helper.setBlock(ENERGY, AEBlocks.CREATIVE_ENERGY_CELL.block().defaultBlockState());
        return (SequencedAssemblyMatrixBlockEntity) be;
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

    private static void pushPlatePattern(GameTestHelper helper) {
        ItemStack encoded = SequencedAssemblyPattern.encode(
                plateRecipe(helper),
                List.of(
                        new GenericStack(AEItemKey.of(AllItems.POWDERED_OBSIDIAN.get()), 1),
                        new GenericStack(AEFluidKey.of(Fluids.LAVA), FILL_PER_STEP),
                        new GenericStack(AEFluidKey.of(Fluids.WATER), FILL_PER_STEP)),
                new GenericStack(AEItemKey.of(NepCreateContent.HARDENED_OBSIDIAN_PLATE.get()), 1));
        IPatternDetails details = PatternDetailsHelper.decodePattern(encoded, helper.getLevel());
        helper.assertTrue(details != null, "the hardened obsidian plate pattern did not decode");

        ICraftingMachine machine =
                helper.getLevel().getCapability(AECapabilities.CRAFTING_MACHINE, helper.absolutePos(MATRIX), null);
        helper.assertTrue(machine != null, "the matrix exposed no crafting machine capability");

        KeyCounter[] inputs = new KeyCounter[details.getInputs().length];
        for (int slot = 0; slot < inputs.length; slot++) {
            inputs[slot] = new KeyCounter();
            IPatternDetails.IInput input = details.getInputs()[slot];
            inputs[slot].add(input.getPossibleInputs()[0].what(), input.getMultiplier());
        }
        helper.assertTrue(
                machine.pushPattern(details, inputs, EJECTION),
                "the matrix refused the hardened obsidian plate pattern");
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

    private static int outputCount(SequencedAssemblyMatrixBlockEntity matrix, Item item) {
        return countIn(matrix.getOutputBuffer(), item);
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 400)
    public static void theMatrixPushesItsResultBackAlongTheEjectionSide(GameTestHelper helper) {
        SequencedAssemblyMatrixBlockEntity matrix = placePoweredMatrix(helper);
        helper.setBlock(TARGET, Blocks.CHEST.defaultBlockState());
        pushPlatePattern(helper);

        helper.succeedWhen(() -> {
            IItemHandler chest = helper.getLevel()
                    .getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(TARGET), EJECTION.getOpposite());
            helper.assertTrue(chest != null, "the chest exposed no item handler");
            helper.assertTrue(
                    countIn(chest, NepCreateContent.HARDENED_OBSIDIAN_PLATE.get()) == 1,
                    "the matrix never pushed its result out of the ejection side");
            helper.assertTrue(
                    outputCount(matrix, NepCreateContent.HARDENED_OBSIDIAN_PLATE.get()) == 0,
                    "the matrix kept a copy of the result it pushed out");
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 600)
    public static void everyResultOfABatchedRequestFindsItsWayBack(GameTestHelper helper) {
        SequencedAssemblyMatrixBlockEntity matrix = placePoweredMatrix(helper);
        helper.setBlock(TARGET, Blocks.CHEST.defaultBlockState());
        pushPlatePattern(helper);
        pushPlatePattern(helper);
        pushPlatePattern(helper);

        helper.succeedWhen(() -> {
            IItemHandler chest = helper.getLevel()
                    .getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(TARGET), EJECTION.getOpposite());
            helper.assertTrue(chest != null, "the chest exposed no item handler");
            int returned = countIn(chest, NepCreateContent.HARDENED_OBSIDIAN_PLATE.get());
            int stranded = outputCount(matrix, NepCreateContent.HARDENED_OBSIDIAN_PLATE.get());
            helper.assertTrue(
                    returned == 3,
                    "only " + returned + " of 3 batched results came back, " + stranded + " stranded in the output "
                            + "buffer; a request for several items leaves all but the first behind");
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 400)
    public static void theMatrixPushesItsResultBackIntoAPatternProvider(GameTestHelper helper) {
        SequencedAssemblyMatrixBlockEntity matrix = placePoweredMatrix(helper);
        helper.setBlock(TARGET, AEBlocks.PATTERN_PROVIDER.block().defaultBlockState());

        IItemHandler provider = helper.getLevel()
                .getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(TARGET), EJECTION.getOpposite());
        helper.assertTrue(
                provider != null,
                "the pattern provider exposed no item handler on the face touching the matrix; nothing can be "
                        + "returned to it");

        pushPlatePattern(helper);

        helper.succeedWhen(() -> {
            IItemHandler returnInv = helper.getLevel()
                    .getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(TARGET), EJECTION.getOpposite());
            helper.assertTrue(returnInv != null, "the pattern provider stopped exposing its item handler");
            helper.assertTrue(
                    countIn(returnInv, NepCreateContent.HARDENED_OBSIDIAN_PLATE.get()) == 1,
                    "the matrix never returned its result to the pattern provider that pushed the pattern");
            helper.assertTrue(
                    outputCount(matrix, NepCreateContent.HARDENED_OBSIDIAN_PLATE.get()) == 0,
                    "the matrix kept a copy of the result it returned");
        });
    }
}
