package dev.rylex.nep.compat.create;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import dev.rylex.nep.util.ItemCounts;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

final class SequencedAssemblyMatrixPersistence {
    private SequencedAssemblyMatrixPersistence() {}

    static ListTag saveStacks(List<GenericStack> stacks, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (GenericStack stack : stacks) {
            list.add(GenericStack.writeTag(registries, stack));
        }
        return list;
    }

    static void loadStacks(List<GenericStack> into, CompoundTag tag, String key, HolderLookup.Provider registries) {
        into.clear();
        if (tag.contains(key, Tag.TAG_LIST)) {
            ListTag list = tag.getList(key, Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                GenericStack stack = GenericStack.readTag(registries, list.getCompound(i));
                if (stack != null) {
                    into.add(stack);
                }
            }
        }
    }

    static ListTag saveItems(List<ItemStack> stacks, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (ItemStack stack : stacks) {
            list.add(stack.save(registries));
        }
        return list;
    }

    static void loadItems(List<ItemStack> into, CompoundTag tag, String key, HolderLookup.Provider registries) {
        into.clear();
        if (tag.contains(key, Tag.TAG_LIST)) {
            ListTag list = tag.getList(key, Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                ItemStack stack = ItemStack.parseOptional(registries, list.getCompound(i));
                if (!stack.isEmpty()) {
                    into.add(stack);
                }
            }
        }
    }

    static ListTag saveFluids(List<FluidStack> stacks, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (FluidStack stack : stacks) {
            list.add(stack.save(registries));
        }
        return list;
    }

    static void loadFluids(List<FluidStack> into, CompoundTag tag, String key, HolderLookup.Provider registries) {
        into.clear();
        if (tag.contains(key, Tag.TAG_LIST)) {
            ListTag list = tag.getList(key, Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                FluidStack stack = FluidStack.parseOptional(registries, list.getCompound(i));
                if (!stack.isEmpty()) {
                    into.add(stack);
                }
            }
        }
    }

    static ListTag saveTemplates(Map<AEItemKey, MatrixTemplate> templates, HolderLookup.Provider registries) {
        ListTag templateList = new ListTag();
        for (Map.Entry<AEItemKey, MatrixTemplate> entry : templates.entrySet()) {
            ListTag entries = new ListTag();
            for (GenericStack stack : entry.getValue().stacks()) {
                CompoundTag keyTag = new CompoundTag();
                keyTag.put("Key", stack.what().toTagGeneric(registries));
                keyTag.putLong("Per", stack.amount());
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
            Map<AEItemKey, MatrixTemplate> templates, CompoundTag tag, String key, HolderLookup.Provider registries) {
        templates.clear();
        if (!tag.contains(key, Tag.TAG_LIST)) {
            return;
        }
        ListTag templateList = tag.getList(key, Tag.TAG_COMPOUND);
        for (int i = 0; i < templateList.size(); i++) {
            CompoundTag templateTag = templateList.getCompound(i);
            ListTag entries = templateTag.getList("Entries", Tag.TAG_COMPOUND);
            List<GenericStack> stacks = new ArrayList<>(entries.size());
            for (int entry = 0; entry < entries.size(); entry++) {
                CompoundTag keyTag = entries.getCompound(entry);
                AEKey what = AEKey.fromTagGeneric(registries, keyTag.getCompound("Key"));
                if (what != null) {
                    stacks.add(new GenericStack(what, keyTag.getLong("Per")));
                }
            }
            AEItemKey outputItem = ItemCounts.key(templateTag, "Output", registries);
            if (!stacks.isEmpty() && outputItem != null) {
                templates.put(outputItem, new MatrixTemplate(stacks));
            }
        }
    }

    static ListTag saveJobs(Map<AEItemKey, MatrixJob> jobs, HolderLookup.Provider registries) {
        ListTag jobList = new ListTag();
        for (Map.Entry<AEItemKey, MatrixJob> entry : jobs.entrySet()) {
            CompoundTag jobTag = entry.getValue().save(registries);
            ItemCounts.putKey(jobTag, entry.getKey(), registries);
            jobList.add(jobTag);
        }
        return jobList;
    }

    static void loadJobs(
            Map<AEItemKey, MatrixJob> jobs, CompoundTag tag, String key, HolderLookup.Provider registries) {
        jobs.clear();
        if (!tag.contains(key, Tag.TAG_LIST)) {
            return;
        }
        ListTag jobList = tag.getList(key, Tag.TAG_COMPOUND);
        for (int i = 0; i < jobList.size(); i++) {
            CompoundTag jobTag = jobList.getCompound(i);
            AEItemKey outputItem = ItemCounts.key(jobTag, "Output", registries);
            MatrixJob job = MatrixJob.load(registries, jobTag);
            if (outputItem != null && job != null) {
                jobs.put(outputItem, job);
            }
        }
    }
}
