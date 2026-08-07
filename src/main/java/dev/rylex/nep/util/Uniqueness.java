package dev.rylex.nep.util;

import java.util.function.Function;
import org.jetbrains.annotations.Nullable;

public final class Uniqueness {
    private Uniqueness() {}

    @Nullable
    public static <T, R> R onlyMatch(Iterable<T> candidates, Function<? super T, ? extends R> mapper) {
        R only = null;
        for (T candidate : candidates) {
            R mapped = mapper.apply(candidate);
            if (mapped == null) {
                continue;
            }
            if (only != null) {
                return null;
            }
            only = mapped;
        }
        return only;
    }
}
