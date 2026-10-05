package dev.rylex.nep.machine;

import java.util.HashMap;
import java.util.Map;

public final class ThrottledLog {

    private static final int BURST = 5;
    private static final long INTERVAL_TICKS = 200;
    private static final int MAX_ENTRIES = 512;

    private final Map<String, Entry> seen = new HashMap<>();

    public boolean shouldLog(long now, String subject, String message) {
        Entry previous = seen.get(subject);
        if (previous == null || !previous.message.equals(message)) {
            if (seen.size() >= MAX_ENTRIES) {
                seen.clear();
            }
            seen.put(subject, new Entry(message, now));
            return true;
        }
        previous.repeats++;
        if (previous.repeats <= BURST || now - previous.loggedAt >= INTERVAL_TICKS) {
            previous.loggedAt = now;
            return true;
        }
        return false;
    }

    public void clear() {
        seen.clear();
    }

    private static final class Entry {
        private final String message;
        private int repeats;
        private long loggedAt;

        private Entry(String message, long loggedAt) {
            this.message = message;
            this.loggedAt = loggedAt;
        }
    }
}
