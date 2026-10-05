package dev.rylex.nep.util;

import appeng.api.stacks.AEItemKey;
import java.util.Map;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

public final class ItemCounts {
    private ItemCounts() {}

    private static final String KEY_TAG = "Key";
    private static final String LEGACY_ID_TAG = "Id";
    private static final String COUNT_TAG = "Count";

    public static ListTag save(Map<AEItemKey, Long> map, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Map.Entry<AEItemKey, Long> entry : map.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.put(KEY_TAG, entry.getKey().toTag(registries));
            entryTag.putLong(COUNT_TAG, entry.getValue());
            list.add(entryTag);
        }
        return list;
    }

    public static void load(Map<AEItemKey, Long> map, CompoundTag tag, String key, HolderLookup.Provider registries) {
        map.clear();
        if (!tag.contains(key, Tag.TAG_LIST)) {
            return;
        }
        ListTag list = tag.getList(key, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entryTag = list.getCompound(i);
            long count = entryTag.getLong(COUNT_TAG);
            AEItemKey itemKey = key(entryTag, registries);
            if (count > 0 && itemKey != null) {
                map.merge(itemKey, count, Long::sum);
            }
        }
    }

    /**
     * Reads the item key an entry was written with, falling back to the bare item id written before matrix
     * bookkeeping carried data components.
     */
    @Nullable
    public static AEItemKey key(CompoundTag entryTag, HolderLookup.Provider registries) {
        return key(entryTag, LEGACY_ID_TAG, registries);
    }

    @Nullable
    public static AEItemKey key(CompoundTag entryTag, String legacyIdTag, HolderLookup.Provider registries) {
        if (entryTag.contains(KEY_TAG, Tag.TAG_COMPOUND)) {
            return AEItemKey.fromTag(registries, entryTag.getCompound(KEY_TAG));
        }
        Item item = item(entryTag.getString(legacyIdTag));
        return item == null ? null : AEItemKey.of(item);
    }

    public static void putKey(CompoundTag entryTag, AEItemKey key, HolderLookup.Provider registries) {
        entryTag.put(KEY_TAG, key.toTag(registries));
    }

    @Nullable
    public static Item item(String id) {
        ResourceLocation location = ResourceLocation.tryParse(id);
        return location == null
                ? null
                : BuiltInRegistries.ITEM.getOptional(location).orElse(null);
    }
}
