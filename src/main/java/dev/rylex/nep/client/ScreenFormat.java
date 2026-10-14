package dev.rylex.nep.client;

import java.util.Locale;

public final class ScreenFormat {
    private ScreenFormat() {}

    public static String format(long value) {
        if (value >= 1_000_000_000L) {
            return String.format(Locale.ROOT, "%.1fG", value / 1_000_000_000.0);
        }
        if (value >= 1_000_000L) {
            return String.format(Locale.ROOT, "%.1fM", value / 1_000_000.0);
        }
        if (value >= 1_000L) {
            return String.format(Locale.ROOT, "%.1fK", value / 1_000.0);
        }
        return Long.toString(value);
    }
}
