package dev.rylex.nep.compat.draconic;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

record FusionTemplate(
        List<GenericStack> inputs,
        List<GenericStack> retained,
        @Nullable ResourceLocation recipe) {
    private static final String STACKS_KEY = "Stacks";
    private static final String RETAINED_KEY = "Retained";
    private static final String RECIPE_KEY = "Recipe";

    int indexOf(AEKey key) {
        for (int i = 0; i < inputs.size(); i++) {
            if (inputs.get(i).what().equals(key)) {
                return i;
            }
        }
        return -1;
    }

    long demandFor(int index, long queued) {
        GenericStack input = inputs.get(index);
        long perCraft = input.amount();
        return isFullyRetained(input.what(), perCraft) ? perCraft : perCraft * queued;
    }

    long retainedAmount(AEKey key) {
        long kept = 0;
        for (GenericStack stack : retained) {
            if (stack.what().equals(key)) {
                kept += stack.amount();
            }
        }
        return kept;
    }

    boolean isFullyRetained(AEKey key, long perCraft) {
        long kept = retainedAmount(key);
        return kept > 0 && kept == perCraft;
    }

    static ListTag saveStacks(List<GenericStack> stacks, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (GenericStack stack : stacks) {
            list.add(GenericStack.writeTag(registries, stack));
        }
        return list;
    }

    static List<GenericStack> loadStacks(ListTag list, HolderLookup.Provider registries) {
        List<GenericStack> stacks = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            GenericStack stack = GenericStack.readTag(registries, list.getCompound(i));
            if (stack != null) {
                stacks.add(stack);
            }
        }
        return stacks;
    }

    void save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put(STACKS_KEY, saveStacks(inputs, registries));
        tag.put(RETAINED_KEY, saveStacks(retained, registries));
        if (recipe != null) {
            tag.putString(RECIPE_KEY, recipe.toString());
        }
    }

    @Nullable
    static FusionTemplate load(CompoundTag tag, HolderLookup.Provider registries) {
        List<GenericStack> inputs = loadStacks(tag.getList(STACKS_KEY, Tag.TAG_COMPOUND), registries);
        if (inputs.isEmpty()) {
            return null;
        }
        List<GenericStack> retained = loadStacks(tag.getList(RETAINED_KEY, Tag.TAG_COMPOUND), registries);
        return new FusionTemplate(
                List.copyOf(inputs), List.copyOf(retained), ResourceLocation.tryParse(tag.getString(RECIPE_KEY)));
    }
}
