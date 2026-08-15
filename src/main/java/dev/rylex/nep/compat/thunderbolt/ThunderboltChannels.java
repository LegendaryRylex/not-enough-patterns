package dev.rylex.nep.compat.thunderbolt;

import appeng.api.networking.IGridNode;
import appeng.me.GridNode;
import com.moakiee.ae2lt.grid.BorrowedCapacityCalculator;
import dev.rylex.nep.machine.ChannelDemand;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.Set;
import java.util.function.Supplier;
import org.jetbrains.annotations.Nullable;

public final class ThunderboltChannels {

    private static final Set<IGridNode> WITHDRAWN = new ReferenceOpenHashSet<>();

    private static int claimed;

    private static int fedDemand = -1;

    private ThunderboltChannels() {}

    public static int consumeFedDemand() {
        int fed = fedDemand;
        fedDemand = -1;
        return fed;
    }

    public static int sinkCapacity(IGridNode node) {
        int demand = WITHDRAWN.contains(node) ? 0 : ChannelDemand.of(node);
        claimed += demand;
        return demand;
    }

    public static int claimedCapacity() {
        return claimed;
    }

    @Nullable
    public static BorrowedCapacityCalculator.Result solveUntilNobodyIsPartFed(
            Supplier<BorrowedCapacityCalculator.Result> solve, Set<IGridNode> network) {
        WITHDRAWN.clear();
        BorrowedCapacityCalculator.Result result;
        while (true) {
            claimed = 0;
            result = solve.get();
            if (result == null) {
                WITHDRAWN.clear();
                return null;
            }
            IGridNode leastFed = leastFedPartFed(result, network);
            if (leastFed == null) {
                break;
            }
            WITHDRAWN.add(leastFed);
        }
        if (WITHDRAWN.isEmpty()) {
            fedDemand = totalDemandOf(result.channelNodes());
            return result;
        }
        Set<GridNode> fed = new ReferenceOpenHashSet<>(result.channelNodes());
        fed.removeAll(WITHDRAWN);
        WITHDRAWN.clear();
        fedDemand = totalDemandOf(fed);
        return new BorrowedCapacityCalculator.Result(
                fed, result.networkNodes(), result.nodeFlow(), result.connectionFlow());
    }

    private static int totalDemandOf(Set<GridNode> fed) {
        int total = 0;
        for (GridNode node : fed) {
            total += ChannelDemand.of(node);
        }
        return total;
    }

    @Nullable
    private static IGridNode leastFedPartFed(BorrowedCapacityCalculator.Result result, Set<IGridNode> network) {
        IGridNode leastFed = null;
        int leastFlow = Integer.MAX_VALUE;
        for (IGridNode node : network) {
            if (WITHDRAWN.contains(node)
                    || ChannelDemand.of(node) < 2
                    || result.channelNodes().contains(node)) {
                continue;
            }
            int flow = result.nodeFlow().getInt(node);
            if (flow < leastFlow) {
                leastFlow = flow;
                leastFed = node;
            }
        }
        return leastFed;
    }
}
