package dev.rylex.nep.hub;

import dev.rylex.nep.ConfigOverrides;
import java.util.function.Supplier;

public final class HubConfigFields {
    public static final String CHANNELS = "MACHINE_HUB_CHANNELS";
    public static final String CHANNELS_PER_LINK = "MACHINE_HUB_CHANNELS_PER_LINK";
    public static final String SCAN_WHITELIST = "MACHINE_HUB_SCAN_WHITELIST";
    public static final String LINK_BLACKLIST = "MACHINE_HUB_LINK_BLACKLIST";

    private HubConfigFields() {}

    /** Undoes the override when the setup throws, because a delayed undo never runs once the test has failed. */
    public static <T> T guarded(ConfigOverrides.Restore restore, Supplier<T> setup) {
        try {
            return setup.get();
        } catch (RuntimeException e) {
            restore.undo();
            throw e;
        }
    }

    public static void guardedRun(ConfigOverrides.Restore restore, Runnable setup) {
        guarded(restore, () -> {
            setup.run();
            return null;
        });
    }
}
