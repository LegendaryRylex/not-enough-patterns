package dev.rylex.nep;

import java.lang.reflect.Field;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class ConfigOverrides {

    private ConfigOverrides() {}

    public interface Restore {
        void undo();
    }

    @SuppressWarnings("unchecked")
    public static <T> Restore override(String field, T value) {
        try {
            Field declared = NepConfig.class.getDeclaredField(field);
            declared.setAccessible(true);
            ModConfigSpec.ConfigValue<T> config = (ModConfigSpec.ConfigValue<T>) declared.get(null);
            T previous = config.get();
            config.set(value);
            return () -> config.set(previous);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("no settable config field named " + field, e);
        }
    }
}
