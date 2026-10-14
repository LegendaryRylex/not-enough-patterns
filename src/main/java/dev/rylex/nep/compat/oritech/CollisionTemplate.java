package dev.rylex.nep.compat.oritech;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import dev.rylex.nep.machine.DemandTally;
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

record CollisionTemplate(StackColumns stacks, @Nullable ResourceLocation recipe) {
    private static final String STACKS_KEY = "Stacks";
    private static final String RECIPE_KEY = "Recipe";

    static CollisionTemplate capture(Map<AEItemKey, Long> items, ResourceLocation recipe) {
        List<AEKey> keys = new ArrayList<>(items.size());
        List<Long> counts = new ArrayList<>(items.size());
        for (Map.Entry<AEItemKey, Long> entry : items.entrySet()) {
            keys.add(entry.getKey());
            counts.add(entry.getValue());
        }
        return new CollisionTemplate(new StackColumns(List.copyOf(keys), List.copyOf(counts)), recipe);
    }

    void addDemand(DemandTally tally, long crafts) {
        List<AEKey> keys = stacks.keys();
        List<Long> counts = stacks.counts();
        for (int i = 0; i < keys.size(); i++) {
            tally.add(keys.get(i), counts.get(i) * crafts);
        }
    }

    long demandFor(AEKey key, long crafts) {
        int index = stacks.keys().indexOf(key);
        return index >= 0 ? stacks.counts().get(index) * crafts : 0;
    }

    static ListTag save(Map<AEItemKey, CollisionTemplate> templates, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Map.Entry<AEItemKey, CollisionTemplate> entry : templates.entrySet()) {
            CompoundTag tag = new CompoundTag();
            ItemCounts.putKey(tag, entry.getKey(), registries);
            tag.put(STACKS_KEY, entry.getValue().stacks().write(registries));
            if (entry.getValue().recipe() != null) {
                tag.putString(RECIPE_KEY, entry.getValue().recipe().toString());
            }
            list.add(tag);
        }
        return list;
    }

    static void load(
            Map<AEItemKey, CollisionTemplate> templates,
            ListTag list,
            HolderLookup.Provider registries,
            boolean recipeRequired) {
        templates.clear();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            AEItemKey item = ItemCounts.key(tag, ItemCounts.LEGACY_ID_TAG, registries);
            ResourceLocation recipe = ResourceLocation.tryParse(tag.getString(RECIPE_KEY));
            if (item == null || (recipeRequired && recipe == null)) {
                continue;
            }
            StackColumns columns = StackColumns.read(tag, STACKS_KEY, registries);
            if (!columns.keys().isEmpty()) {
                templates.put(item, new CollisionTemplate(columns, recipe));
            }
        }
    }
}
