package dev.rylex.nep.compat.ars;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.Level;

public enum RitualWeather {
    ANY("☀", "any"),
    CLEAR("○", "clear"),
    RAIN("☂", "rain"),
    STORM("⚡", "storm");

    public static final StreamCodec<io.netty.buffer.ByteBuf, RitualWeather> STREAM_CODEC =
            ByteBufCodecs.BYTE.map(RitualWeather::byOrdinal, weather -> (byte) weather.ordinal());

    private final String glyph;
    private final String key;

    RitualWeather(String glyph, String keySuffix) {
        this.glyph = glyph;
        this.key = "gui.nep.ritual_conductor.weather." + keySuffix;
    }

    public String glyph() {
        return glyph;
    }

    public String key() {
        return key;
    }

    public RitualWeather next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public RitualWeather previous() {
        return values()[(ordinal() + values().length - 1) % values().length];
    }

    public static RitualWeather byOrdinal(int ordinal) {
        RitualWeather[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : ANY;
    }

    public static RitualWeather byName(String name) {
        for (RitualWeather weather : values()) {
            if (weather.name().equals(name)) {
                return weather;
            }
        }
        return ANY;
    }

    /** Reads the weather flags rather than the fading rain and thunder levels, which lag several seconds behind them. */
    public boolean satisfiedBy(Level level) {
        boolean weathered =
                level.dimensionType().hasSkyLight() && !level.dimensionType().hasCeiling();
        boolean raining = weathered && level.getLevelData().isRaining();
        boolean thundering = raining && level.getLevelData().isThundering();
        return switch (this) {
            case ANY -> true;
            case CLEAR -> !raining;
            case RAIN -> raining && !thundering;
            case STORM -> thundering;
        };
    }
}
