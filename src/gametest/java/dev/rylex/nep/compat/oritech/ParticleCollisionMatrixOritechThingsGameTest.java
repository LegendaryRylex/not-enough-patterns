package dev.rylex.nep.compat.oritech;

import com.lumengrid.oritechthings.block.ModBlocks;
import dev.rylex.nep.NepGameTests;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import rearth.oritech.block.blocks.addons.MachineAddonBlock;

public final class ParticleCollisionMatrixOritechThingsGameTest {

    private static final BlockPos MATRIX = new BlockPos(2, 1, 2);
    private static final BlockPos WEST = MATRIX.west();
    private static final BlockPos EAST = MATRIX.east();

    private ParticleCollisionMatrixOritechThingsGameTest() {}

    public static void register(NepGameTests.Batch main) {
        main.add(
                "a_tiered_speed_addon_applies", ParticleCollisionMatrixOritechThingsGameTest::aTieredSpeedAddonApplies);
        main.add(
                "a_tiered_efficiency_addon_applies",
                ParticleCollisionMatrixOritechThingsGameTest::aTieredEfficiencyAddonApplies);
        main.add(
                "tiered_processing_and_capacitor_addons_apply_to_the_matrix",
                ParticleCollisionMatrixOritechThingsGameTest::tieredProcessingAndCapacitorAddonsApplyToTheMatrix);
        main.add(
                "the_cross_dimensional_addon_is_ignored",
                ParticleCollisionMatrixOritechThingsGameTest::theCrossDimensionalAddonIsIgnored);
    }

    private static ParticleCollisionMatrixBlockEntity place(GameTestHelper helper) {
        helper.setBlock(MATRIX, NepOritechContent.MATRIX.get().defaultBlockState());
        ParticleCollisionMatrixBlockEntity matrix =
                helper.getBlockEntity(MATRIX, ParticleCollisionMatrixBlockEntity.class);
        helper.assertTrue(matrix != null, "the Particle Collision Matrix has no block entity");
        return matrix;
    }

    private static boolean lit(GameTestHelper helper, BlockPos pos) {
        BlockState state = helper.getBlockState(pos);
        return state.getBlock() instanceof MachineAddonBlock && state.getValue(MachineAddonBlock.ADDON_USED);
    }

    private static MachineAddonBlock.AddonSettings settings(Block block) {
        return ((MachineAddonBlock) block).getAddonSettings();
    }

    public static void aTieredSpeedAddonApplies(GameTestHelper helper) {
        ParticleCollisionMatrixBlockEntity matrix = place(helper);
        Block speed = ModBlocks.ADDON_BLOCK_SPEED_TIER_3.get();
        helper.setBlock(WEST, speed.defaultBlockState());

        matrix.initAddons();

        helper.assertTrue(lit(helper, WEST), "a tiered speed addon was not claimed by the Matrix");
        helper.assertTrue(
                matrix.speedMultiplier() == ParticleCollisionMatrixAddonGameTest.expectedSpeed(speed),
                "the tiered speed multiplier is not applied, got " + matrix.speedMultiplier());
        helper.assertTrue(
                matrix.craftTicks() < matrix.baseCraftTicks(), "a tiered speed addon did not shorten the craft");
        helper.succeed();
    }

    public static void aTieredEfficiencyAddonApplies(GameTestHelper helper) {
        ParticleCollisionMatrixBlockEntity matrix = place(helper);
        Block efficiency = ModBlocks.ADDON_BLOCK_EFFICIENCY_TIER_3.get();
        helper.setBlock(WEST, efficiency.defaultBlockState());

        matrix.initAddons();

        helper.assertTrue(lit(helper, WEST), "a tiered efficiency addon was not claimed by the Matrix");
        helper.assertTrue(
                matrix.efficiencyMultiplier() == ParticleCollisionMatrixAddonGameTest.expectedEfficiency(efficiency),
                "the tiered efficiency multiplier is not applied, got " + matrix.efficiencyMultiplier());
        helper.assertTrue(matrix.efficiencyMultiplier() < 1.0F, "a tiered efficiency addon did not cut the cost");
        helper.succeed();
    }

    public static void tieredProcessingAndCapacitorAddonsApplyToTheMatrix(GameTestHelper helper) {
        ParticleCollisionMatrixBlockEntity matrix = place(helper);
        Block processing = ModBlocks.ADDON_BLOCK_PROCESSING_TIER_3.get();
        Block capacitor = ModBlocks.ADDON_BLOCK_CAPACITOR_TIER_3.get();
        long base = matrix.energyCapacity();
        helper.setBlock(WEST, processing.defaultBlockState());
        helper.setBlock(EAST, capacitor.defaultBlockState());

        matrix.initAddons();

        helper.assertTrue(
                matrix.extraChambers() == settings(processing).chamberCount(),
                "the tiered processing addon's chambers are not applied, got " + matrix.extraChambers());
        helper.assertTrue(
                matrix.energyCapacity() == base + settings(capacitor).addedCapacity(),
                "the tiered capacitor's capacity is not applied, got " + matrix.energyCapacity());
        helper.succeed();
    }

    public static void theCrossDimensionalAddonIsIgnored(GameTestHelper helper) {
        ParticleCollisionMatrixBlockEntity matrix = place(helper);
        helper.setBlock(WEST, ModBlocks.ADDON_BLOCK_CROSS_DIMENSIONAL.get().defaultBlockState());

        matrix.initAddons();

        helper.assertTrue(!lit(helper, WEST), "the drone-only addon was lit as used");
        helper.assertTrue(matrix.addonCount() == 0, "the drone-only addon was counted");
        helper.succeed();
    }
}
