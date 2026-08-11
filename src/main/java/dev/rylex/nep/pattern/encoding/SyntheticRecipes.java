package dev.rylex.nep.pattern.encoding;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;

public final class SyntheticRecipes {
    private SyntheticRecipes() {}

    @FunctionalInterface
    public interface Lookup {
        boolean knows(Identifier recipe, Level level);
    }

    private static final List<Lookup> LOOKUPS = new CopyOnWriteArrayList<>();

    public static void register(Lookup lookup) {
        LOOKUPS.add(lookup);
    }

    public static boolean known(Identifier recipe, Level level) {
        for (Lookup lookup : LOOKUPS) {
            if (lookup.knows(recipe, level)) {
                return true;
            }
        }
        return false;
    }
}
