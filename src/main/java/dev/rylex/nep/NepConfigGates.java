package dev.rylex.nep;

import dev.rylex.nep.decoder.EncodingModuleItem;
import dev.rylex.nep.decoder.PatternDecoding;
import dev.rylex.nep.guide.NepModules;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

public final class NepConfigGates {
    private NepConfigGates() {}

    private record Gate(Supplier<? extends Item> item, BooleanSupplier enabled) {}

    private static final List<Gate> GATES = new CopyOnWriteArrayList<>();
    private static final List<Runnable> LISTENERS = new CopyOnWriteArrayList<>();
    private static volatile Map<Item, BooleanSupplier> resolved;

    public static void register(Supplier<? extends Item> item, BooleanSupplier enabled) {
        GATES.add(new Gate(item, enabled));
        resolved = null;
    }

    public static void onChange(Runnable listener) {
        LISTENERS.add(listener);
    }

    static void changed() {
        LISTENERS.forEach(Runnable::run);
    }

    public static boolean disabled(Item item) {
        if (!NepConfig.loaded()) {
            return false;
        }
        if (item instanceof EncodingModuleItem module) {
            return !PatternDecoding.required(module.module())
                    || NepModules.disabled(module.module().modId());
        }
        BooleanSupplier enabled = resolved().get(item);
        return enabled != null && !enabled.getAsBoolean();
    }

    public static List<Item> disabledItems() {
        if (!NepConfig.loaded()) {
            return List.of();
        }
        return BuiltInRegistries.ITEM.stream()
                .filter(item -> item instanceof EncodingModuleItem || resolved().containsKey(item))
                .filter(NepConfigGates::disabled)
                .toList();
    }

    private static Map<Item, BooleanSupplier> resolved() {
        Map<Item, BooleanSupplier> map = resolved;
        if (map == null) {
            map = GATES.stream()
                    .collect(Collectors.toUnmodifiableMap(
                            gate -> gate.item().get(),
                            Gate::enabled,
                            (first, second) -> () -> first.getAsBoolean() && second.getAsBoolean()));
            resolved = map;
        }
        return map;
    }
}
