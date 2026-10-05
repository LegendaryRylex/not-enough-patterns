package dev.rylex.nep.compat.ars;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.Level;

public enum RitualDaylight {
    ANY("◐", "any"),
    DAY("☼", "day"),
    NIGHT("☾", "night");

    private static final long DAY_LENGTH = 24_000L;
    private static final long DUSK = 12_000L;

    public static final StreamCodec<io.netty.buffer.ByteBuf, RitualDaylight> STREAM_CODEC =
            ByteBufCodecs.BYTE.map(RitualDaylight::byOrdinal, daylight -> (byte) daylight.ordinal());

    private final String glyph;
    private final String key;

    RitualDaylight(String glyph, String keySuffix) {
        this.glyph = glyph;
        this.key = "gui.nep.ritual_conductor.daylight." + keySuffix;
    }

    public String glyph() {
        return glyph;
    }

    public String key() {
        return key;
    }

    public RitualDaylight next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public RitualDaylight previous() {
        return values()[(ordinal() + values().length - 1) % values().length];
    }

    public static RitualDaylight byOrdinal(int ordinal) {
        RitualDaylight[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : ANY;
    }

    public static RitualDaylight byName(String name) {
        for (RitualDaylight daylight : values()) {
            if (daylight.name().equals(name)) {
                return daylight;
            }
        }
        return ANY;
    }

    public boolean satisfiedBy(Level level) {
        if (this == ANY) {
            return true;
        }
        boolean daytime = Math.floorMod(level.getDayTime(), DAY_LENGTH) < DUSK;
        return this == DAY ? daytime : !daytime;
    }
}
