package dev.rylex.nep.compat.malum;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import dev.rylex.nep.compat.malum.FocusedSpiritMatrixBlockEntity.Process;
import dev.rylex.nep.machine.StackColumns;
import dev.rylex.nep.util.EnumNames;
import dev.rylex.nep.util.ItemCounts;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;

record MatrixTemplate(List<AEKey> keys, List<Long> counts, ResourceLocation recipe, Process process) {
    private static final String STACKS_KEY = "Stacks";
    private static final String RECIPE_KEY = "Recipe";
    private static final String PROCESS_KEY = "Process";

    static MatrixTemplate capture(Map<AEItemKey, Long> items, ResourceLocation recipe, Process process) {
        List<AEKey> keys = new ArrayList<>(items.size());
        List<Long> counts = new ArrayList<>(items.size());
        for (Map.Entry<AEItemKey, Long> entry : items.entrySet()) {
            keys.add(entry.getKey());
            counts.add(entry.getValue());
        }
        return new MatrixTemplate(List.copyOf(keys), List.copyOf(counts), recipe, process);
    }

    static ListTag saveAll(Map<AEItemKey, MatrixTemplate> templates, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Map.Entry<AEItemKey, MatrixTemplate> entry : templates.entrySet()) {
            CompoundTag tag = new CompoundTag();
            ItemCounts.putKey(tag, entry.getKey(), registries);
            tag.put(
                    STACKS_KEY,
                    new StackColumns(entry.getValue().keys(), entry.getValue().counts()).write(registries));
            tag.putString(RECIPE_KEY, entry.getValue().recipe().toString());
            tag.putString(PROCESS_KEY, entry.getValue().process().name());
            list.add(tag);
        }
        return list;
    }

    static void loadAll(Map<AEItemKey, MatrixTemplate> templates, ListTag list, HolderLookup.Provider registries) {
        templates.clear();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            AEItemKey item = ItemCounts.key(tag, ItemCounts.LEGACY_ID_TAG, registries);
            ResourceLocation recipe = ResourceLocation.tryParse(tag.getString(RECIPE_KEY));
            if (item == null || recipe == null) {
                continue;
            }
            StackColumns columns = StackColumns.read(tag, STACKS_KEY, registries);
            if (!columns.keys().isEmpty()) {
                Process process = EnumNames.byName(Process.values(), tag.getString(PROCESS_KEY), Process.INFUSION);
                templates.put(item, new MatrixTemplate(columns.keys(), columns.counts(), recipe, process));
            }
        }
    }
}
