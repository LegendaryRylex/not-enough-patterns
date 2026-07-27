package dev.rylex.nep.machine;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public final class RejectLog {

    private static final int MAX_ENTRIES = 512;
    private static final long LOG_INTERVAL_TICKS = 100;

    private final Map<Key, Entry> lastReject = new HashMap<>();

    public boolean shouldLog(Level level, BlockPos pos, String reason) {
        Key key = new Key(level.dimension(), pos);
        Entry previous = lastReject.get(key);
        long now = level.getGameTime();
        if (previous != null && previous.reason().equals(reason) && now - previous.tick() < LOG_INTERVAL_TICKS) {
            return false;
        }
        if (lastReject.size() >= MAX_ENTRIES) {
            lastReject.clear();
        }
        lastReject.put(key, new Entry(reason, now));
        return true;
    }

    public void clear() {
        lastReject.clear();
    }

    private record Key(ResourceKey<Level> dimension, BlockPos pos) {}

    private record Entry(String reason, long tick) {}
}
