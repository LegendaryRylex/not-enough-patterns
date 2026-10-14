package dev.rylex.nep.machine;

import appeng.me.cluster.implementations.CraftingCPUCluster;
import org.jetbrains.annotations.Nullable;

/** Server-thread only and not reentrant: {@link #exit()} clears the outer value of a nested {@link #enter}. */
public final class PushingCpuContext {

    private static CraftingCPUCluster current;

    private PushingCpuContext() {}

    public static void enter(CraftingCPUCluster cpu) {
        current = cpu;
    }

    public static void exit() {
        current = null;
    }

    @Nullable
    public static CraftingCPUCluster current() {
        return current;
    }
}
