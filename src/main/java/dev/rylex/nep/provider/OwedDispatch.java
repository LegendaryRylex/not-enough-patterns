package dev.rylex.nep.provider;

import appeng.api.stacks.AEItemKey;
import java.util.LinkedHashMap;
import java.util.Map;

public final class OwedDispatch {

    public static final long NO_DEADLINE = -1;

    private final Map<AEItemKey, Long> parked;
    private final Map<AEItemKey, Long> owed;
    private long deadline;

    public OwedDispatch(Map<AEItemKey, Long> parked, Map<AEItemKey, Long> owed) {
        this(parked, owed, NO_DEADLINE);
    }

    public OwedDispatch(Map<AEItemKey, Long> parked, Map<AEItemKey, Long> owed, long deadline) {
        this.parked = new LinkedHashMap<>(parked);
        this.owed = new LinkedHashMap<>(owed);
        this.deadline = deadline;
    }

    public Map<AEItemKey, Long> parked() {
        return parked;
    }

    public Map<AEItemKey, Long> owed() {
        return owed;
    }

    public long owedAmount(AEItemKey key) {
        return owed.getOrDefault(key, 0L);
    }

    public long reserved(AEItemKey key) {
        return Math.max(0, parked.getOrDefault(key, 0L) - owedAmount(key));
    }

    public long take(AEItemKey key, long amount) {
        long remaining = owedAmount(key);
        long taken = Math.min(remaining, amount);
        if (taken <= 0) {
            return 0;
        }
        if (remaining - taken <= 0) {
            owed.remove(key);
        } else {
            owed.put(key, remaining - taken);
        }
        deadline = NO_DEADLINE;
        return taken;
    }

    public boolean isSettled() {
        return owed.isEmpty();
    }

    public long deadline() {
        return deadline;
    }

    public boolean hasDeadline() {
        return deadline != NO_DEADLINE;
    }

    public void startGrace(long deadline) {
        this.deadline = deadline;
    }

    public void clearGrace() {
        deadline = NO_DEADLINE;
    }

    public boolean expired(long now) {
        return deadline != NO_DEADLINE && now >= deadline;
    }
}
