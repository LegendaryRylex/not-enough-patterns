package dev.rylex.nep.compat.occultism;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class RitualClaims {
    private RitualClaims() {}

    private record Bowl(ResourceKey<Level> dimension, BlockPos pos) {}

    private static final Map<Bowl, WeakReference<RitualControllerBlockEntity>> CLAIMS = new HashMap<>();

    static void claim(Level level, BlockPos bowl, RitualControllerBlockEntity controller) {
        CLAIMS.values().removeIf(held -> held.get() == null);
        CLAIMS.put(new Bowl(level.dimension(), bowl.immutable()), new WeakReference<>(controller));
    }

    static void release(Level level, BlockPos bowl, RitualControllerBlockEntity controller) {
        Bowl key = new Bowl(level.dimension(), bowl.immutable());
        WeakReference<RitualControllerBlockEntity> held = CLAIMS.get(key);
        if (held != null && (held.get() == null || held.get() == controller)) {
            CLAIMS.remove(key);
        }
    }

    public static boolean deliver(Level level, BlockPos bowl, ItemStack result) {
        if (level.isClientSide() || CLAIMS.isEmpty() || result.isEmpty()) {
            return false;
        }
        WeakReference<RitualControllerBlockEntity> held = CLAIMS.get(new Bowl(level.dimension(), bowl.immutable()));
        RitualControllerBlockEntity controller = held == null ? null : held.get();
        return controller != null && !controller.isRemoved() && controller.receiveResult(bowl, result.copy());
    }
}
