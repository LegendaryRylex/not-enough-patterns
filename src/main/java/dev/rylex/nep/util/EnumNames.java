package dev.rylex.nep.util;

import org.jetbrains.annotations.Nullable;

public final class EnumNames {
    private EnumNames() {}

    @Nullable
    public static <E extends Enum<E>> E byName(E[] values, String name, @Nullable E fallback) {
        for (E value : values) {
            if (value.name().equals(name)) {
                return value;
            }
        }
        return fallback;
    }
}
