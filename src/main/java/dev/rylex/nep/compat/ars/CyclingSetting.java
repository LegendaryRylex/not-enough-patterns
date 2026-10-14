package dev.rylex.nep.compat.ars;

final class CyclingSetting {
    private CyclingSetting() {}

    static <E extends Enum<E>> E next(E[] values, E current) {
        return values[(current.ordinal() + 1) % values.length];
    }

    static <E extends Enum<E>> E previous(E[] values, E current) {
        return values[(current.ordinal() + values.length - 1) % values.length];
    }

    static <E extends Enum<E>> E byOrdinal(E[] values, int ordinal, E fallback) {
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : fallback;
    }

    static <E extends Enum<E>> E byName(E[] values, String name, E fallback) {
        for (E value : values) {
            if (value.name().equals(name)) {
                return value;
            }
        }
        return fallback;
    }
}
