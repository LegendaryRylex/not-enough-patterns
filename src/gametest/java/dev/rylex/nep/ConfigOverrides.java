package dev.rylex.nep;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class ConfigOverrides {

    private static final Map<String, Held<?>> HELD = new HashMap<>();

    private ConfigOverrides() {}

    public interface Restore {
        void undo();
    }

    private static final class Held<T> {
        private final ModConfigSpec.ConfigValue<T> config;
        private final T original;
        private int overrides;

        private Held(ModConfigSpec.ConfigValue<T> config) {
            this.config = config;
            this.original = config.get();
        }
    }

    /** Overlapping overrides of one field restore the value from before the first of them once the last is undone. */
    @SuppressWarnings("unchecked")
    public static synchronized <T> Restore override(String field, T value) {
        Held<T> held = (Held<T>) HELD.computeIfAbsent(field, name -> new Held<>(lookup(name)));
        held.overrides++;
        held.config.set(value);
        boolean[] undone = {false};
        return () -> {
            synchronized (ConfigOverrides.class) {
                if (undone[0]) {
                    return;
                }
                undone[0] = true;
                if (--held.overrides == 0) {
                    HELD.remove(field);
                    held.config.set(held.original);
                }
            }
        };
    }

    @SuppressWarnings("unchecked")
    private static <T> ModConfigSpec.ConfigValue<T> lookup(String field) {
        try {
            Field declared = NepConfig.class.getDeclaredField(field);
            declared.setAccessible(true);
            return (ModConfigSpec.ConfigValue<T>) declared.get(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("no settable config field named " + field, e);
        }
    }
}
