package dev.rylex.nep.compat.create;

import appeng.api.AECapabilities;
import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.content.kinetics.deployer.DeployerBlockEntity;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.content.logistics.depot.DepotBlockEntity;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import dev.rylex.nep.GameTestFixtures;
import dev.rylex.nep.pattern.SequencedAssemblyPattern;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;

final class CreateFixtures {
    static final BlockPos DEPOT = new BlockPos(2, 1, 2);
    static final BlockPos DEPLOYER = DEPOT.above(2);
    static final Direction PROVIDER_SIDE = Direction.NORTH;
    static final BlockPos PROVIDER = DEPOT.relative(PROVIDER_SIDE);

    private CreateFixtures() {}

    static ICraftingMachine placeDepot(GameTestHelper helper) {
        helper.setBlock(DEPOT, AllBlocks.DEPOT.getDefaultState());
        ICraftingMachine machine =
                helper.getLevel().getCapability(AECapabilities.CRAFTING_MACHINE, helper.absolutePos(DEPOT), null);
        helper.assertTrue(machine != null, "the depot exposed no crafting machine capability");
        return machine;
    }

    static DeployerBlockEntity placeDeployer(GameTestHelper helper) {
        helper.setBlock(
                DEPLOYER,
                AllBlocks.DEPLOYER.getDefaultState().setValue(DirectionalKineticBlock.FACING, Direction.DOWN));
        BlockEntity be = helper.getBlockEntity(DEPLOYER);
        helper.assertTrue(be instanceof DeployerBlockEntity, "the deployer did not create its block entity");
        return (DeployerBlockEntity) be;
    }

    static IItemHandler placeProvider(GameTestHelper helper) {
        helper.setBlock(PROVIDER, Blocks.CHEST);
        IItemHandler handler = helper.getLevel()
                .getCapability(
                        Capabilities.ItemHandler.BLOCK, helper.absolutePos(PROVIDER), PROVIDER_SIDE.getOpposite());
        helper.assertTrue(handler != null, "the stand-in pattern provider exposed no item handler");
        return handler;
    }

    static IItemHandler deployerHandler(GameTestHelper helper, DeployerBlockEntity deployer) {
        IItemHandler handler = DepotMachines.itemHandler(deployer);
        helper.assertTrue(handler != null, "the deployer exposed no item handler");
        return handler;
    }

    static ItemStack heldByDeployer(GameTestHelper helper, DeployerBlockEntity deployer) {
        IItemHandler handler = deployerHandler(helper, deployer);
        return handler.getStackInSlot(handler.getSlots() - 1);
    }

    static ItemStack stagedOnDepot(GameTestHelper helper) {
        BlockEntity be = helper.getBlockEntity(DEPOT);
        helper.assertTrue(be instanceof DepotBlockEntity, "the depot did not create its block entity");
        return ((DepotBlockEntity) be).getHeldItem();
    }

    static SequencedAssemblyMatrixBlockEntity placeMatrix(GameTestHelper helper, BlockPos matrixPos) {
        helper.setBlock(
                matrixPos,
                NepCreateContent.MATRIX
                        .get()
                        .defaultBlockState()
                        .setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.Z));
        BlockEntity be = helper.getBlockEntity(matrixPos);
        helper.assertTrue(
                be instanceof SequencedAssemblyMatrixBlockEntity, "the matrix did not create its block entity");
        return (SequencedAssemblyMatrixBlockEntity) be;
    }

    static ResourceLocation plateRecipe(GameTestHelper helper) {
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

    static SequencedAssemblyMatrixBlockEntity poweredMatrix(
            GameTestHelper helper, BlockPos matrixPos, BlockPos motorPos, BlockPos controller, BlockPos energyCell) {
        helper.setBlock(
                matrixPos,
                NepCreateContent.MATRIX
                        .get()
                        .defaultBlockState()
                        .setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.Z));
        BlockEntity be = helper.getBlockEntity(matrixPos);
        helper.assertTrue(
                be instanceof SequencedAssemblyMatrixBlockEntity, "the matrix did not create its block entity");

        helper.setBlock(
                motorPos,
                AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(DirectionalKineticBlock.FACING, Direction.SOUTH));
        BlockEntity motor = helper.getBlockEntity(motorPos);
        helper.assertTrue(
                motor instanceof CreativeMotorBlockEntity, "the creative motor did not create its block entity");
        ((CreativeMotorBlockEntity) motor)
                .generatedSpeed.setValue(Math.round(SequencedAssemblyMatrixBlockEntity.peakSpeed()));

        GameTestFixtures.powerUp(helper, controller, energyCell);
        return (SequencedAssemblyMatrixBlockEntity) be;
    }

    static int comparator(GameTestHelper helper, BlockPos pos) {
        BlockPos absolute = helper.absolutePos(pos);
        return helper.getLevel().getBlockState(absolute).getAnalogOutputSignal(helper.getLevel(), absolute);
    }

    static IItemHandler itemHandler(GameTestHelper helper, BlockPos pos, String failure) {
        IItemHandler handler =
                helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(pos), null);
        helper.assertTrue(handler != null, failure);
        return handler;
    }

    static final int PLATE_FILL_PER_STEP = 500;

    static List<GenericStack> plateInputs() {
        return List.of(
                new GenericStack(AEItemKey.of(AllItems.POWDERED_OBSIDIAN.get()), 1),
                new GenericStack(AEFluidKey.of(Fluids.LAVA), PLATE_FILL_PER_STEP),
                new GenericStack(AEFluidKey.of(Fluids.WATER), PLATE_FILL_PER_STEP));
    }

    static GenericStack plateOutput() {
        return new GenericStack(AEItemKey.of(NepCreateContent.HARDENED_OBSIDIAN_PLATE.get()), 1);
    }

    static ItemStack processingPlatePattern() {
        return PatternDetailsHelper.encodeProcessingPattern(plateInputs(), List.of(plateOutput()));
    }

    static IPatternDetails platePattern(GameTestHelper helper) {
        ItemStack encoded = SequencedAssemblyPattern.encode(plateRecipe(helper), plateInputs(), plateOutput());
        IPatternDetails details = PatternDetailsHelper.decodePattern(encoded, helper.getLevel());
        helper.assertTrue(details != null, "the hardened obsidian plate pattern did not decode");
        return details;
    }

    static void pushPlatePattern(GameTestHelper helper, BlockPos matrixPos, Direction ejection) {
        ICraftingMachine machine = GameTestFixtures.craftingMachine(
                helper, matrixPos, "the matrix exposed no crafting machine capability");
        IPatternDetails details = platePattern(helper);
        helper.assertTrue(
                machine.pushPattern(details, GameTestFixtures.inputsOf(details), ejection),
                "the matrix refused the hardened obsidian plate pattern");
    }
}
