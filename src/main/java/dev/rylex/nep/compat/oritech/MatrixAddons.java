package dev.rylex.nep.compat.oritech;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import rearth.oritech.block.blocks.addons.CombiAddonBlock;
import rearth.oritech.block.blocks.addons.MachineAddonBlock;
import rearth.oritech.block.blocks.processing.MachineCoreBlock;
import rearth.oritech.block.entity.addons.CombiAddonEntity;
import rearth.oritech.util.MachineAddonController;

final class MatrixAddons {

    /** Every face but the bottom one, which stays free for an ME cable. */
    static final List<Vec3i> SLOTS = List.of(
            Direction.UP.getNormal(),
            Direction.NORTH.getNormal(),
            Direction.SOUTH.getNormal(),
            Direction.EAST.getNormal(),
            Direction.WEST.getNormal());

    private static final String HIDDEN_CORE = "machine_core_hidden";

    private MatrixAddons() {}

    /** Any Oritech machine core except the decorative Complex Plating, which shares the block class. */
    static boolean isCore(ItemStack stack) {
        return stack.getItem() instanceof BlockItem item
                && item.getBlock() instanceof MachineCoreBlock
                && !BuiltInRegistries.BLOCK.getKey(item.getBlock()).getPath().equals(HIDDEN_CORE);
    }

    static float coreQuality(ItemStack stack, float fallback) {
        return isCore(stack) && ((BlockItem) stack.getItem()).getBlock() instanceof MachineCoreBlock core
                ? core.getCoreQuality()
                : fallback;
    }

    static boolean honours(MachineAddonController.AddonBlock addon) {
        return addon.addonEntity() instanceof CombiAddonEntity || honours(addon.addonBlock());
    }

    static boolean honours(MachineAddonBlock block) {
        if (block instanceof CombiAddonBlock) {
            return true;
        }
        MachineAddonBlock.AddonSettings settings = block.getAddonSettings();
        return settings.extender()
                || settings.acceptEnergy()
                || settings.speedMultiplier() != 1.0F
                || settings.efficiencyMultiplier() != 1.0F
                || settings.addedCapacity() != 0L
                || settings.addedInsert() != 0L
                || settings.chamberCount() > 0;
    }

    static boolean outOfDate(Level level, BlockPos matrix, List<BlockPos> connected) {
        for (BlockPos pos : connected) {
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof MachineAddonBlock) || !state.getValue(MachineAddonBlock.ADDON_USED)) {
                return true;
            }
        }
        for (Vec3i offset : SLOTS) {
            BlockPos pos = matrix.offset(offset);
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof MachineAddonBlock block
                    && !state.getValue(MachineAddonBlock.ADDON_USED)
                    && honours(block)) {
                return true;
            }
        }
        return false;
    }
}
