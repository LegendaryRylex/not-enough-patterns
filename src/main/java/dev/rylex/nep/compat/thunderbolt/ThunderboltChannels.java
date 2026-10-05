package dev.rylex.nep.compat.thunderbolt;

import appeng.api.networking.IGridNode;
import appeng.me.GridNode;
import com.moakiee.thunderbolt.ae2.channel.BorrowedCapacityCalculator;
import dev.rylex.nep.machine.ChannelDemand;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.Set;
import java.util.function.Supplier;
import org.jetbrains.annotations.Nullable;

public final class ThunderboltChannels {

    private static final int MAX_ONE_AT_A_TIME_ROUNDS = 4;

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

    /**
     * Each {@code solve} call is a full Dinic max-flow over the whole grid inside Thunderbolt, so the
     * round count is capped and the remaining part-fed machines are withdrawn together at the cap.
     */
    @Nullable
    public static BorrowedCapacityCalculator.Result solveUntilNobodyIsPartFed(
            Supplier<BorrowedCapacityCalculator.Result> solve, Set<IGridNode> network) {
        WITHDRAWN.clear();
        boolean refining = hasMultiChannelDemand(network);
        BorrowedCapacityCalculator.Result result;
        for (int round = 0; ; round++) {
            claimed = 0;
            result = solve.get();
            if (result == null) {
                WITHDRAWN.clear();
                return null;
            }
            if (!refining) {
                break;
            }
            if (round < MAX_ONE_AT_A_TIME_ROUNDS) {
                IGridNode leastFed = leastFedPartFed(result, network);
                if (leastFed == null) {
                    break;
                }
                WITHDRAWN.add(leastFed);
            } else {
                if (!withdrawAllPartFed(result, network)) {
                    break;
                }
                refining = false;
            }
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

    private static boolean hasMultiChannelDemand(Set<IGridNode> network) {
        for (IGridNode node : network) {
            if (ChannelDemand.of(node) >= 2) {
                return true;
            }
        }
        return false;
    }

    private static boolean isPartFed(BorrowedCapacityCalculator.Result result, IGridNode node) {
        return !WITHDRAWN.contains(node)
                && ChannelDemand.of(node) >= 2
                && !result.channelNodes().contains(node);
    }

    private static boolean withdrawAllPartFed(BorrowedCapacityCalculator.Result result, Set<IGridNode> network) {
        boolean withdrew = false;
        for (IGridNode node : network) {
            if (isPartFed(result, node)) {
                withdrew |= WITHDRAWN.add(node);
            }
        }
        return withdrew;
    }

    @Nullable
    private static IGridNode leastFedPartFed(BorrowedCapacityCalculator.Result result, Set<IGridNode> network) {
        IGridNode leastFed = null;
        int leastFlow = Integer.MAX_VALUE;
        for (IGridNode node : network) {
            if (!isPartFed(result, node)) {
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
