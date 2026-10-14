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

    private static final ThreadLocal<Solve> ACTIVE = ThreadLocal.withInitial(Solve::new);

    private static final ThreadLocal<Integer> FED_DEMAND = new ThreadLocal<>();

    private static final class Solve {
        final Set<IGridNode> withdrawn = new ReferenceOpenHashSet<>();
        int claimed;
    }

    private ThunderboltChannels() {}

    public static int consumeFedDemand() {
        Integer fed = FED_DEMAND.get();
        FED_DEMAND.remove();
        return fed == null ? -1 : fed;
    }

    public static int sinkCapacity(IGridNode node) {
        Solve solve = ACTIVE.get();
        int demand = solve.withdrawn.contains(node) ? 0 : ChannelDemand.of(node);
        solve.claimed += demand;
        return demand;
    }

    public static int claimedCapacity() {
        return ACTIVE.get().claimed;
    }

    /**
     * Each {@code solve} call is a full Dinic max-flow over the whole grid inside Thunderbolt, so the
     * round count is capped and the remaining part-fed machines are withdrawn together at the cap.
     */
    @Nullable
    public static BorrowedCapacityCalculator.Result solveUntilNobodyIsPartFed(
            Supplier<BorrowedCapacityCalculator.Result> solve, Set<IGridNode> network) {
        Solve ctx = new Solve();
        ACTIVE.set(ctx);
        try {
            return solveRounds(ctx, solve, network);
        } finally {
            ACTIVE.remove();
        }
    }

    @Nullable
    private static BorrowedCapacityCalculator.Result solveRounds(
            Solve ctx, Supplier<BorrowedCapacityCalculator.Result> solve, Set<IGridNode> network) {
        boolean refining = hasMultiChannelDemand(network);
        BorrowedCapacityCalculator.Result result;
        for (int round = 0; ; round++) {
            ctx.claimed = 0;
            result = solve.get();
            if (result == null) {
                return null;
            }
            if (!refining) {
                break;
            }
            if (round < MAX_ONE_AT_A_TIME_ROUNDS) {
                IGridNode leastFed = leastFedPartFed(ctx, result, network);
                if (leastFed == null) {
                    break;
                }
                ctx.withdrawn.add(leastFed);
            } else {
                if (!withdrawAllPartFed(ctx, result, network)) {
                    break;
                }
                refining = false;
            }
        }
        if (ctx.withdrawn.isEmpty()) {
            FED_DEMAND.set(totalDemandOf(result.channelNodes()));
            return result;
        }
        Set<GridNode> fed = new ReferenceOpenHashSet<>(result.channelNodes());
        fed.removeAll(ctx.withdrawn);
        FED_DEMAND.set(totalDemandOf(fed));
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

    private static boolean isPartFed(Solve ctx, BorrowedCapacityCalculator.Result result, IGridNode node) {
        return !ctx.withdrawn.contains(node)
                && ChannelDemand.of(node) >= 2
                && !result.channelNodes().contains(node);
    }

    private static boolean withdrawAllPartFed(
            Solve ctx, BorrowedCapacityCalculator.Result result, Set<IGridNode> network) {
        boolean withdrew = false;
        for (IGridNode node : network) {
            if (isPartFed(ctx, result, node)) {
                withdrew |= ctx.withdrawn.add(node);
            }
        }
        return withdrew;
    }

    @Nullable
    private static IGridNode leastFedPartFed(
            Solve ctx, BorrowedCapacityCalculator.Result result, Set<IGridNode> network) {
        IGridNode leastFed = null;
        int leastFlow = Integer.MAX_VALUE;
        for (IGridNode node : network) {
            if (!isPartFed(ctx, result, node)) {
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
