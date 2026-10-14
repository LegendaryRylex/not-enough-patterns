package dev.rylex.nep.compat.compactcrafting;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import dev.rylex.nep.machine.StackColumns;
import dev.rylex.nep.util.ItemCounts;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

record MiniaturizationTemplate(
        List<AEKey> keys, List<Long> counts, @Nullable ResourceLocation recipe) {
    static void capture(
            Map<AEItemKey, MiniaturizationTemplate> templates,
            AEItemKey output,
            Map<AEItemKey, Long> items,
            ResourceLocation recipe) {
        List<AEKey> keys = new ArrayList<>(items.size());
        List<Long> counts = new ArrayList<>(items.size());
        for (Map.Entry<AEItemKey, Long> entry : items.entrySet()) {
            keys.add(entry.getKey());
            counts.add(entry.getValue());
        }
        templates.put(output, new MiniaturizationTemplate(List.copyOf(keys), List.copyOf(counts), recipe));
    }

    static ListTag save(Map<AEItemKey, MiniaturizationTemplate> templates, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Map.Entry<AEItemKey, MiniaturizationTemplate> entry : templates.entrySet()) {
            CompoundTag tag = new CompoundTag();
            ItemCounts.putKey(tag, entry.getKey(), registries);
            tag.put(
                    "Stacks",
                    new StackColumns(entry.getValue().keys(), entry.getValue().counts()).write(registries));
            if (entry.getValue().recipe() != null) {
                tag.putString("Recipe", entry.getValue().recipe().toString());
            }
            list.add(tag);
        }
        return list;
    }

    static void load(
            Map<AEItemKey, MiniaturizationTemplate> templates, ListTag list, HolderLookup.Provider registries) {
        templates.clear();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            AEItemKey item = ItemCounts.key(tag, ItemCounts.LEGACY_ID_TAG, registries);
            if (item == null) {
                continue;
            }
            StackColumns columns = StackColumns.read(tag, "Stacks", registries);
            if (!columns.keys().isEmpty()) {
                templates.put(
                        item,
                        new MiniaturizationTemplate(
                                columns.keys(), columns.counts(), ResourceLocation.tryParse(tag.getString("Recipe"))));
            }
        }
    }
}
