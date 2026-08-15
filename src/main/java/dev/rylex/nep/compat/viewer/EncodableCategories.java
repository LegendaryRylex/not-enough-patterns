package dev.rylex.nep.compat.viewer;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;

public final class EncodableCategories {
    private EncodableCategories() {}

    private static final Set<ResourceLocation> IDS = ConcurrentHashMap.newKeySet();

    public static void add(ResourceLocation id) {
        IDS.add(id);
    }

    public static boolean contains(ResourceLocation id) {
        return IDS.contains(id);
    }
}
