package dev.rylex.nep.util;

import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

public final class ItemCounts {
    private ItemCounts() {}

    public static ListTag save(Map<Item, Long> map) {
        ListTag list = new ListTag();
        for (Map.Entry<Item, Long> entry : map.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putString(
                    "Id", BuiltInRegistries.ITEM.getKey(entry.getKey()).toString());
            entryTag.putLong("Count", entry.getValue());
            list.add(entryTag);
        }
        return list;
    }

    public static void load(Map<Item, Long> map, CompoundTag tag, String key) {
        map.clear();
        if (!tag.contains(key, Tag.TAG_LIST)) {
            return;
        }
        ListTag list = tag.getList(key, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entryTag = list.getCompound(i);
            long count = entryTag.getLong("Count");
            Item item = item(entryTag.getString("Id"));
            if (count > 0 && item != null) {
                map.merge(item, count, Long::sum);
            }
        }
    }

    @Nullable
    public static Item item(String id) {
        ResourceLocation location = ResourceLocation.tryParse(id);
        return location == null
                ? null
                : BuiltInRegistries.ITEM.getOptional(location).orElse(null);
    }
}
