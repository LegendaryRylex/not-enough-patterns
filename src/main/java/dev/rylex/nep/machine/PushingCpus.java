package dev.rylex.nep.machine;

import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

public final class PushingCpus {

    private static final String POSITIONS_KEY = "PushCpus";
    private static final String PARTIAL_KEY = "PushCpusPartial";
    private static final Codec<List<BlockPos>> POSITIONS_CODEC = BlockPos.CODEC.listOf();

    private final Set<BlockPos> positions = new LinkedHashSet<>();
    private boolean partial;

    public void record() {
        CraftingCPUCluster cpu = PushingCpuContext.current();
        if (cpu == null) {
            partial = true;
        } else {
            positions.add(cpu.getBoundsMin());
        }
    }

    public void clear() {
        positions.clear();
        partial = false;
    }

    public int cancelJobsFor(@Nullable IGrid grid, Set<AEItemKey> outputs) {
        if (grid == null || outputs.isEmpty()) {
            return 0;
        }
        List<CraftingCPUCluster> clusters = new ArrayList<>();
        Set<BlockPos> present = new HashSet<>();
        for (ICraftingCPU cpu : grid.getCraftingService().getCpus()) {
            if (cpu instanceof CraftingCPUCluster cluster) {
                clusters.add(cluster);
                present.add(cluster.getBoundsMin());
            }
        }
        int cancelled = 0;
        for (CraftingCPUCluster cluster : clusters) {
            if (!partial && !positions.contains(cluster.getBoundsMin())) {
                continue;
            }
            if (cluster.isBusy() && waitsForAny(cluster, outputs)) {
                cluster.cancelJob();
                cancelled++;
            }
        }
        return cancelled;
    }

    private static boolean waitsForAny(CraftingCPUCluster cluster, Set<AEItemKey> outputs) {
        Set<AEKey> waiting = new HashSet<>();
        cluster.craftingLogic.getAllWaitingFor(waiting);
        for (AEKey key : waiting) {
            if (key instanceof AEItemKey item && outputs.contains(item)) {
                return true;
            }
        }
        return false;
    }

    public void save(ValueOutput output) {
        if (positions.isEmpty() && !partial) {
            return;
        }
        output.store(POSITIONS_KEY, POSITIONS_CODEC, List.copyOf(positions));
        output.putBoolean(PARTIAL_KEY, partial);
    }

    public void load(ValueInput input) {
        positions.clear();
        input.read(POSITIONS_KEY, POSITIONS_CODEC).ifPresent(positions::addAll);
        partial = input.getBooleanOr(PARTIAL_KEY, false);
    }
}
