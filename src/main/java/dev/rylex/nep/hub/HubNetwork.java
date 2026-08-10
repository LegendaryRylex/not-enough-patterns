package dev.rylex.nep.hub;

import appeng.api.AECapabilities;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IInWorldGridNodeHost;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Finds the network a hub returns its results to. A hub is not itself a grid device, so it borrows
 * the grid of whatever it is standing against, which in practice is the Pattern Provider that feeds
 * it. An inactive node is remembered only so the screen can tell the difference between a network
 * that is switched off and no network at all.
 */
public final class HubNetwork {

    private HubNetwork() {}

    @Nullable
    public static IGridNode adjacentNode(Level level, BlockPos pos) {
        IGridNode inactive = null;
        for (Direction side : Direction.values()) {
            Direction facing = side.getOpposite();
            IInWorldGridNodeHost host = level.getCapability(AECapabilities.IN_WORLD_GRID_NODE_HOST, pos.relative(side));
            if (host == null) {
                continue;
            }
            IGridNode node = host.getGridNode(facing);
            if (node == null) {
                continue;
            }
            if (node.isActive() && node.getGrid() != null) {
                return node;
            }
            inactive = node;
        }
        return inactive;
    }
}
