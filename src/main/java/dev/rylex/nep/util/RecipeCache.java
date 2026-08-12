package dev.rylex.nep.util;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class RecipeCache<T> {

    private static final List<RecipeCache<?>> ALL = new CopyOnWriteArrayList<>();

    private record Entry<T>(RecipeManager source, T value) {}

    private final Function<Level, T> loader;

    @Nullable
    private volatile Entry<T> client;

    @Nullable
    private volatile Entry<T> server;

    private RecipeCache(Function<Level, T> loader) {
        this.loader = loader;
        ALL.add(this);
    }

    public static <T> RecipeCache<T> of(Function<Level, T> loader) {
        return new RecipeCache<>(loader);
    }

    public T get(Level level) {
        RecipeManager source = level.getRecipeManager();
        Entry<T> entry = level.isClientSide() ? client : server;
        return entry != null && entry.source() == source ? entry.value() : load(level, source);
    }

    public void clear() {
        client = null;
        server = null;
    }

    public static void clearClient() {
        for (RecipeCache<?> cache : ALL) {
            cache.client = null;
        }
    }

    private synchronized T load(Level level, RecipeManager source) {
        boolean clientSide = level.isClientSide();
        Entry<T> entry = clientSide ? client : server;
        if (entry == null || entry.source() != source) {
            entry = new Entry<>(source, loader.apply(level));
            if (clientSide) {
                client = entry;
            } else {
                server = entry;
            }
        }
        return entry.value();
    }
}
