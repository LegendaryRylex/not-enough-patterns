package dev.rylex.nep.util;

import net.minecraft.world.entity.item.ItemEntity;

public final class OfferedItems {
    private OfferedItems() {}

    public static final String TAG = "nep:offered";

    public static void mark(ItemEntity item) {
        item.addTag(TAG);
    }

    public static boolean isOffered(ItemEntity item) {
        return item.entityTags().contains(TAG);
    }
}
