package dev.rylex.nep.compat.ars;

import com.hollingsworth.arsnouveau.api.source.ISourceTile;
import com.hollingsworth.arsnouveau.api.source.ISpecialSourceProvider;
import com.hollingsworth.arsnouveau.api.source.SourceManager;
import com.hollingsworth.arsnouveau.common.block.tile.CreativeSourceJarTile;
import com.hollingsworth.arsnouveau.common.block.tile.SourceJarTile;
import com.hollingsworth.arsnouveau.common.entity.EntityFollowProjectile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

final class SourceJarDraw {
    private SourceJarDraw() {}

    static int draw(ServerLevel level, BlockPos pos, int range, int wanted) {
        int drawn = 0;
        int minX = SectionPos.blockToSectionCoord(pos.getX() - range);
        int maxX = SectionPos.blockToSectionCoord(pos.getX() + range);
        int minZ = SectionPos.blockToSectionCoord(pos.getZ() - range);
        int maxZ = SectionPos.blockToSectionCoord(pos.getZ() + range);
        for (int chunkX = minX; chunkX <= maxX && drawn < wanted; chunkX++) {
            for (int chunkZ = minZ; chunkZ <= maxZ && drawn < wanted; chunkZ++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk == null) {
                    continue;
                }
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (drawn >= wanted) {
                        break;
                    }
                    if (blockEntity instanceof SourceJarTile jar
                            && jar.getBlockPos().distManhattan(pos) <= range) {
                        drawn += take(level, jar, jar.getBlockPos(), pos, wanted - drawn);
                    }
                }
            }
        }
        for (ISpecialSourceProvider provider : SourceManager.INSTANCE.canTakeSourceNearby(pos, level, range)) {
            if (drawn >= wanted) {
                break;
            }
            drawn += take(level, provider.getSource(), provider.getCurrentPos(), pos, wanted - drawn);
        }
        return drawn;
    }

    private static int take(ServerLevel level, ISourceTile tile, BlockPos from, BlockPos to, int wanted) {
        if (tile instanceof CreativeSourceJarTile) {
            EntityFollowProjectile.spawn(level, from, to);
            return wanted;
        }
        int before = tile.getSource();
        int available = Math.min(wanted, before);
        if (available <= 0) {
            return 0;
        }
        int taken = before - tile.removeSource(available);
        if (taken > 0) {
            EntityFollowProjectile.spawn(level, from, to);
        }
        return Math.max(0, taken);
    }
}
