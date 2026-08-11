package dev.rylex.nep.util;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * A value derived from a level's recipes, held per logical side.
 *
 * <p>A single-player client and its integrated server deserialize their own recipe instances, so one static field
 * shared by both sides hands whichever side asked second the other side's objects, and identity comparisons against
 * them silently fail. Each side therefore gets its own entry, tagged with the {@link RecipeMap} it was built from.
 * That tag is also the invalidation: a datapack reload builds the server a new map and pushes the client a new one,
 * so an entry from the previous generation can never be served.
 */
public final class RecipeCache<T> {

    private static final List<RecipeCache<?>> ALL = new CopyOnWriteArrayList<>();

    private record Entry<T>(RecipeMap source, T value) {}

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
        RecipeMap source = Recipes.of(level);
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

    private synchronized T load(Level level, RecipeMap source) {
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
