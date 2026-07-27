package dev.rylex.nep.guide;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;

public final class NepModules {
    private NepModules() {}

    private static final Map<String, BooleanSupplier> MODULES = new ConcurrentHashMap<>();

    public static void register(String id, BooleanSupplier enabled) {
        MODULES.put(id, enabled);
    }

    public static boolean disabled(String id) {
        BooleanSupplier enabled = MODULES.get(id);
        return enabled != null && !enabled.getAsBoolean();
    }
}
