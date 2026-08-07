package dev.rylex.nep.compat.actuallyadditions;

import de.ellpeck.actuallyadditions.mod.tile.TileEntityDisplayStand;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

final class EmpowererLayout {
    private EmpowererLayout() {}

    static final int STANDS = 4;

    private static final int DISTANCE = 3;

    static BlockPos standPos(BlockPos empowerer, int index) {
        return empowerer.relative(Direction.from2DDataValue(index), DISTANCE);
    }

    static TileEntityDisplayStand @Nullable [] stands(Level level, BlockPos empowerer) {
        TileEntityDisplayStand[] stands = new TileEntityDisplayStand[STANDS];
        for (int index = 0; index < STANDS; index++) {
            if (!(level.getBlockEntity(standPos(empowerer, index)) instanceof TileEntityDisplayStand stand)) {
                return null;
            }
            stands[index] = stand;
        }
        return stands;
    }
}
