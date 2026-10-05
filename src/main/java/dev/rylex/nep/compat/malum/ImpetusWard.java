package dev.rylex.nep.compat.malum;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class ImpetusWard {
    private ImpetusWard() {}

    static void ward(BlockEntity host, int ticks) {
        Level level = host.getLevel();
        if (level == null) {
            return;
        }
        long until = level.getGameTime() + ticks;
        long existing = host.getExistingData(NepMalumContent.IMPETUS_WARD.get()).orElse(0L);
        host.setData(NepMalumContent.IMPETUS_WARD.get(), Math.max(existing, until));
        host.setChanged();
    }

    public static boolean shields(BlockEntity host) {
        Level level = host.getLevel();
        if (level == null) {
            return false;
        }
        return host.getExistingData(NepMalumContent.IMPETUS_WARD.get())
                .map(until -> level.getGameTime() < until)
                .orElse(false);
    }
}
