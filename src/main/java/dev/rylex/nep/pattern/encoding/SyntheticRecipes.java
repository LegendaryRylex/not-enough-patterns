package dev.rylex.nep.pattern.encoding;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public final class SyntheticRecipes {
    private SyntheticRecipes() {}

    @FunctionalInterface
    public interface Lookup {
        boolean knows(ResourceLocation recipe, Level level);
    }

    private static final List<Lookup> LOOKUPS = new CopyOnWriteArrayList<>();

    public static void register(Lookup lookup) {
        LOOKUPS.add(lookup);
    }

    public static boolean known(ResourceLocation recipe, Level level) {
        for (Lookup lookup : LOOKUPS) {
            if (lookup.knows(recipe, level)) {
                return true;
            }
        }
        return false;
    }
}
