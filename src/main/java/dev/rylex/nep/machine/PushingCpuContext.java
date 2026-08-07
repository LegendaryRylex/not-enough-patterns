package dev.rylex.nep.machine;

import appeng.me.cluster.implementations.CraftingCPUCluster;
import org.jetbrains.annotations.Nullable;

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
