package dev.rylex.nep.compat.malum;

import appeng.api.config.Actionable;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

final class RiteNetwork {
    private RiteNetwork() {}

    static void forEachBlockEntity(ServerLevel level, BlockPos center, int range, Consumer<BlockEntity> action) {
        int minChunkX = SectionPos.blockToSectionCoord(center.getX() - range);
        int maxChunkX = SectionPos.blockToSectionCoord(center.getX() + range);
        int minChunkZ = SectionPos.blockToSectionCoord(center.getZ() - range);
        int maxChunkZ = SectionPos.blockToSectionCoord(center.getZ() + range);
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk == null) {
                    continue;
                }
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (within(center, blockEntity.getBlockPos(), range)) {
                        action.accept(blockEntity);
                    }
                }
            }
        }
    }

    static List<IGrid> gridsNear(ServerLevel level, BlockPos center, int range) {
        Map<IGrid, Double> nearest = new HashMap<>();
        forEachBlockEntity(level, center, range, blockEntity -> {
            IInWorldGridNodeHost host = GridHelper.getNodeHost(level, blockEntity.getBlockPos());
            if (host == null) {
                return;
            }
            for (Direction side : Direction.values()) {
                IGridNode node = host.getGridNode(side);
                if (node != null && node.getGrid() != null) {
                    nearest.merge(node.getGrid(), blockEntity.getBlockPos().distSqr(center), Math::min);
                    return;
                }
            }
        });
        List<IGrid> grids = new ArrayList<>(nearest.keySet());
        grids.sort(Comparator.comparingDouble(nearest::get));
        return grids;
    }

    static long insertNearest(List<IGrid> grids, ItemStack stack) {
        for (IGrid grid : grids) {
            long moved = insert(grid, stack);
            if (moved > 0) {
                return moved;
            }
        }
        return 0;
    }

    private static long insert(IGrid grid, ItemStack stack) {
        AEItemKey key = AEItemKey.of(stack);
        return key == null
                ? 0
                : grid.getStorageService()
                        .getInventory()
                        .insert(key, stack.getCount(), Actionable.MODULATE, IActionSource.empty());
    }

    private static boolean within(BlockPos center, BlockPos pos, int range) {
        return Math.abs(pos.getX() - center.getX()) <= range
                && Math.abs(pos.getY() - center.getY()) <= range
                && Math.abs(pos.getZ() - center.getZ()) <= range;
    }
}
