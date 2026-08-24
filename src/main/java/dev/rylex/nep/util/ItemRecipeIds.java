package dev.rylex.nep.util;

import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

public final class ItemRecipeIds {
    private ItemRecipeIds() {}

    public static ListTag save(Map<Item, ResourceLocation> map) {
        ListTag list = new ListTag();
        for (Map.Entry<Item, ResourceLocation> entry : map.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putString(
                    "Id", BuiltInRegistries.ITEM.getKey(entry.getKey()).toString());
            entryTag.putString("Recipe", entry.getValue().toString());
            list.add(entryTag);
        }
        return list;
    }

    public static void load(Map<Item, ResourceLocation> map, CompoundTag tag, String key) {
        map.clear();
        if (!tag.contains(key, Tag.TAG_LIST)) {
            return;
        }
        ListTag list = tag.getList(key, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entryTag = list.getCompound(i);
            Item item = ItemCounts.item(entryTag.getString("Id"));
            ResourceLocation recipe = ResourceLocation.tryParse(entryTag.getString("Recipe"));
            if (item != null && recipe != null) {
                map.put(item, recipe);
            }
        }
    }
}
