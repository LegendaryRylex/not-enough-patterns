package dev.rylex.nep.util;

import appeng.api.stacks.AEItemKey;
import java.util.Map;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

public final class ItemRecipeIds {
    private ItemRecipeIds() {}

    public static ListTag save(Map<AEItemKey, ResourceLocation> map, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Map.Entry<AEItemKey, ResourceLocation> entry : map.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            ItemCounts.putKey(entryTag, entry.getKey(), registries);
            entryTag.putString("Recipe", entry.getValue().toString());
            list.add(entryTag);
        }
        return list;
    }

    public static void load(
            Map<AEItemKey, ResourceLocation> map, CompoundTag tag, String key, HolderLookup.Provider registries) {
        map.clear();
        if (!tag.contains(key, Tag.TAG_LIST)) {
            return;
        }
        ListTag list = tag.getList(key, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entryTag = list.getCompound(i);
            AEItemKey itemKey = ItemCounts.key(entryTag, registries);
            ResourceLocation recipe = ResourceLocation.tryParse(entryTag.getString("Recipe"));
            if (itemKey != null && recipe != null) {
                map.put(itemKey, recipe);
            }
        }
    }
}
