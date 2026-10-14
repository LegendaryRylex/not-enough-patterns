package dev.rylex.nep.compat.ars;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;
import dev.rylex.nep.Nep;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.machine.PushOutcome;
import dev.rylex.nep.machine.RejectLog;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

final class PatternPushMemo {
    private static final int MAX_MEMOIZED_REJECTS = 512;

    private final String machineName;
    private final Set<AEItemKey> unsatisfiable = new HashSet<>();
    private final RejectLog rejectLog = new RejectLog();

    PatternPushMemo(String machineName) {
        this.machineName = machineName;
    }

    void clear() {
        unsatisfiable.clear();
        rejectLog.clear();
    }

    boolean isUnsatisfiable(AEItemKey pattern) {
        return unsatisfiable.contains(pattern);
    }

    void record(AEItemKey pattern, PushOutcome outcome) {
        if (outcome.unsatisfiable() && unsatisfiable.size() < MAX_MEMOIZED_REJECTS) {
            unsatisfiable.add(pattern);
        }
    }

    void log(Level level, BlockPos pos, IPatternDetails patternDetails, PushOutcome outcome) {
        if (!NepConfig.debugLogging()) {
            return;
        }
        if (!outcome.accepted() && !outcome.unsatisfiable() && !rejectLog.shouldLog(level, pos, outcome.detail())) {
            return;
        }
        Nep.LOGGER.info(
                "{} {} {} push: {} (output={})",
                machineName,
                pos,
                outcome.accepted() ? "accepted" : "rejected",
                outcome.detail(),
                patternDetails.getOutputs().isEmpty()
                        ? "?"
                        : patternDetails.getOutputs().get(0).toString());
    }

    @Nullable
    static Map<AEItemKey, Long> collectItems(KeyCounter[] inputs) {
        Map<AEItemKey, Long> provided = new HashMap<>();
        for (KeyCounter counter : inputs) {
            for (var entry : counter) {
                if (!(entry.getKey() instanceof AEItemKey itemKey) || entry.getLongValue() <= 0) {
                    return null;
                }
                provided.merge(itemKey, entry.getLongValue(), Long::sum);
            }
        }
        return provided;
    }
}
