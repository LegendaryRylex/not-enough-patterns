package dev.rylex.nep.compat.create;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import dev.rylex.nep.compat.create.SequencedAssemblyControllerBlockEntity.Template;
import dev.rylex.nep.util.ItemCounts;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

final class SequencedAssemblyControllerPersistence {

    private SequencedAssemblyControllerPersistence() {}

    static ListTag saveTemplates(Map<AEItemKey, Template> templates, HolderLookup.Provider registries) {
        ListTag templateList = new ListTag();
        for (Map.Entry<AEItemKey, Template> entry : templates.entrySet()) {
            Template template = entry.getValue();
            ListTag entries = new ListTag();
            for (int i = 0; i < template.keys().size(); i++) {
                CompoundTag keyTag = new CompoundTag();
                keyTag.put("Key", template.keys().get(i).toTagGeneric(registries));
                keyTag.putLong("Per", template.counts().get(i));
                entries.add(keyTag);
            }
            CompoundTag templateTag = new CompoundTag();
            ItemCounts.putKey(templateTag, entry.getKey(), registries);
            templateTag.put("Entries", entries);
            templateList.add(templateTag);
        }
        return templateList;
    }

    static void loadTemplates(
            Map<AEItemKey, Template> templates, ListTag templateList, HolderLookup.Provider registries) {
        for (int i = 0; i < templateList.size(); i++) {
            CompoundTag templateTag = templateList.getCompound(i);
            ListTag entries = templateTag.getList("Entries", Tag.TAG_COMPOUND);
            List<AEKey> keys = new ArrayList<>(entries.size());
            List<Long> counts = new ArrayList<>(entries.size());
            for (int entry = 0; entry < entries.size(); entry++) {
                CompoundTag keyTag = entries.getCompound(entry);
                AEKey key = AEKey.fromTagGeneric(registries, keyTag.getCompound("Key"));
                if (key != null) {
                    keys.add(key);
                    counts.add(keyTag.getLong("Per"));
                }
            }
            AEItemKey outputItem = ItemCounts.key(templateTag, "Output", registries);
            if (!keys.isEmpty() && outputItem != null) {
                templates.put(outputItem, new Template(List.copyOf(keys), List.copyOf(counts)));
            }
        }
    }

    static ListTag saveQueue(Collection<AEItemKey> queue, HolderLookup.Provider registries) {
        ListTag queueList = new ListTag();
        for (AEItemKey output : queue) {
            CompoundTag entryTag = new CompoundTag();
            ItemCounts.putKey(entryTag, output, registries);
            queueList.add(entryTag);
        }
        return queueList;
    }

    static void loadQueue(List<AEItemKey> queue, ListTag queueList, HolderLookup.Provider registries) {
        for (int i = 0; i < queueList.size(); i++) {
            AEItemKey output = ItemCounts.key(queueList.getCompound(i), ItemCounts.LEGACY_ID_TAG, registries);
            if (output != null && !queue.contains(output)) {
                queue.add(output);
            }
        }
    }

    static ListTag saveStacks(Collection<ItemStack> stacks, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (ItemStack held : stacks) {
            list.add(held.save(registries));
        }
        return list;
    }

    static void loadStacks(List<ItemStack> stacks, ListTag list, HolderLookup.Provider registries) {
        for (int i = 0; i < list.size(); i++) {
            ItemStack held = ItemStack.parseOptional(registries, list.getCompound(i));
            if (!held.isEmpty()) {
                stacks.add(held);
            }
        }
    }
}
