package dev.rylex.nep.compat.ars;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.Level;

public enum RitualDaylight {
    ANY("any"),
    DAY("day"),
    NIGHT("night");

    private static final long DAY_LENGTH = 24_000L;
    private static final long DUSK = 12_000L;

    public static final StreamCodec<ByteBuf, RitualDaylight> STREAM_CODEC =
            ByteBufCodecs.BYTE.map(RitualDaylight::byOrdinal, daylight -> (byte) daylight.ordinal());

    private final String key;

    RitualDaylight(String keySuffix) {
        this.key = "gui.nep.ritual_conductor.daylight." + keySuffix;
    }

    public String key() {
        return key;
    }

    public RitualDaylight next() {
        return CyclingSetting.next(values(), this);
    }

    public RitualDaylight previous() {
        return CyclingSetting.previous(values(), this);
    }

    public static RitualDaylight byOrdinal(int ordinal) {
        return CyclingSetting.byOrdinal(values(), ordinal, ANY);
    }

    public static RitualDaylight byName(String name) {
        return CyclingSetting.byName(values(), name, ANY);
    }

    public boolean satisfiedBy(Level level) {
        if (this == ANY) {
            return true;
        }
        boolean daytime = Math.floorMod(level.getDayTime(), DAY_LENGTH) < DUSK;
        return this == DAY ? daytime : !daytime;
    }
}
