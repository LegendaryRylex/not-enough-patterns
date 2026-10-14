package dev.rylex.nep.compat.ars;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.Level;

public enum RitualWeather {
    ANY("any"),
    CLEAR("clear"),
    RAIN("rain"),
    STORM("storm");

    public static final StreamCodec<ByteBuf, RitualWeather> STREAM_CODEC =
            ByteBufCodecs.BYTE.map(RitualWeather::byOrdinal, weather -> (byte) weather.ordinal());

    private final String key;

    RitualWeather(String keySuffix) {
        this.key = "gui.nep.ritual_conductor.weather." + keySuffix;
    }

    public String key() {
        return key;
    }

    public RitualWeather next() {
        return CyclingSetting.next(values(), this);
    }

    public RitualWeather previous() {
        return CyclingSetting.previous(values(), this);
    }

    public static RitualWeather byOrdinal(int ordinal) {
        return CyclingSetting.byOrdinal(values(), ordinal, ANY);
    }

    public static RitualWeather byName(String name) {
        return CyclingSetting.byName(values(), name, ANY);
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
