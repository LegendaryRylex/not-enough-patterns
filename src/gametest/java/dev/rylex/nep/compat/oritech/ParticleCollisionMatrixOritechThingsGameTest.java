package dev.rylex.nep.compat.oritech;

import com.lumengrid.oritechthings.block.ModBlocks;
import dev.rylex.nep.Nep;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import rearth.oritech.block.blocks.addons.MachineAddonBlock;

@GameTestHolder(Nep.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ParticleCollisionMatrixOritechThingsGameTest {

    private static final String TEMPLATE = "empty_5x5x5";
    private static final String BATCH = "nep_particle_collision_matrix_oritech_things_addons";
    private static final BlockPos MATRIX = new BlockPos(2, 1, 2);
    private static final BlockPos WEST = MATRIX.west();
    private static final BlockPos EAST = MATRIX.east();

    private ParticleCollisionMatrixOritechThingsGameTest() {}

    private static ParticleCollisionMatrixBlockEntity place(GameTestHelper helper) {
        helper.setBlock(MATRIX, NepOritechContent.MATRIX.get().defaultBlockState());
        ParticleCollisionMatrixBlockEntity matrix =
                helper.getBlockEntity(MATRIX) instanceof ParticleCollisionMatrixBlockEntity be ? be : null;
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

    @GameTest(template = TEMPLATE, batch = BATCH)
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

    @GameTest(template = TEMPLATE, batch = BATCH)
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

    @GameTest(template = TEMPLATE, batch = BATCH)
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

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void theCrossDimensionalAddonIsIgnored(GameTestHelper helper) {
        ParticleCollisionMatrixBlockEntity matrix = place(helper);
        helper.setBlock(WEST, ModBlocks.ADDON_BLOCK_CROSS_DIMENSIONAL.get().defaultBlockState());

        matrix.initAddons();

        helper.assertTrue(!lit(helper, WEST), "the drone-only addon was lit as used");
        helper.assertTrue(matrix.addonCount() == 0, "the drone-only addon was counted");
        helper.succeed();
    }
}
