package dev.rylex.nep.provider;

import appeng.api.behaviors.StackImportStrategy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

/** Import routes a compat module contributes, for a machine that finishes a craft without an inventory to pull from. */
public final class MachineImports {
    private MachineImports() {}

    public static final String DRACONIC_FUSION_CORE = "draconic_fusion_core";

    @FunctionalInterface
    public interface Factory {
        @Nullable
        StackImportStrategy create(ServerLevel level, BlockPos pos);
    }

    private static final Map<String, Factory> FACTORIES = new ConcurrentHashMap<>();

    public static void register(String kind, Factory factory) {
        FACTORIES.put(kind, factory);
    }

    static StackImportStrategy strategyFor(String kind, ServerLevel level, BlockPos pos) {
        Factory factory = FACTORIES.get(kind);
        StackImportStrategy strategy = factory == null ? null : factory.create(level, pos);
        return strategy == null ? context -> false : strategy;
    }
}
