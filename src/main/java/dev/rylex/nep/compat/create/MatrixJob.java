package dev.rylex.nep.compat.create;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

record MatrixJob(
        MatrixJob.Kind kind,
        AEItemKey output,
        int outputCount,
        List<GenericStack> consumed,
        List<GenericStack> retained) {

    enum Kind {
        FILLING,
        DEPLOYING
    }

    private static final String KIND_KEY = "Kind";
    private static final String RESULT_KEY = "Result";
    private static final String CONSUMED_KEY = "Consumed";
    private static final String RETAINED_KEY = "Retained";

    ItemStack outputStack() {
        return output.toStack(outputCount);
    }

    MatrixDemand demand() {
        List<GenericStack> needed = new ArrayList<>(consumed.size() + retained.size());
        needed.addAll(consumed);
        needed.addAll(retained);
        return MatrixDemand.exact(needed);
    }

    List<ItemStack> retainedStacks() {
        List<ItemStack> stacks = new ArrayList<>(retained.size());
        for (GenericStack stack : retained) {
            if (stack.what() instanceof AEItemKey key) {
                stacks.add(key.toStack((int) Math.min(stack.amount(), Integer.MAX_VALUE)));
            }
        }
        return stacks;
    }

    CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putString(KIND_KEY, kind.name());
        tag.put(RESULT_KEY, GenericStack.writeTag(registries, new GenericStack(output, outputCount)));
        tag.put(CONSUMED_KEY, saveAll(registries, consumed));
        if (!retained.isEmpty()) {
            tag.put(RETAINED_KEY, saveAll(registries, retained));
        }
        return tag;
    }

    @Nullable
    static MatrixJob load(HolderLookup.Provider registries, CompoundTag tag) {
        Kind kind = kindNamed(tag.getString(KIND_KEY));
        GenericStack result = GenericStack.readTag(registries, tag.getCompound(RESULT_KEY));
        if (kind == null || result == null || result.amount() <= 0 || !(result.what() instanceof AEItemKey key)) {
            return null;
        }
        List<GenericStack> consumed = loadAll(registries, tag, CONSUMED_KEY);
        if (consumed.isEmpty()) {
            return null;
        }
        return new MatrixJob(
                kind,
                key,
                (int) Math.min(result.amount(), Integer.MAX_VALUE),
                consumed,
                loadAll(registries, tag, RETAINED_KEY));
    }

    private static ListTag saveAll(HolderLookup.Provider registries, List<GenericStack> stacks) {
        ListTag list = new ListTag();
        for (GenericStack stack : stacks) {
            list.add(GenericStack.writeTag(registries, stack));
        }
        return list;
    }

    private static List<GenericStack> loadAll(HolderLookup.Provider registries, CompoundTag tag, String key) {
        if (!tag.contains(key, Tag.TAG_LIST)) {
            return List.of();
        }
        ListTag list = tag.getList(key, Tag.TAG_COMPOUND);
        List<GenericStack> stacks = new ArrayList<>(list.size());
        for (int index = 0; index < list.size(); index++) {
            GenericStack stack = GenericStack.readTag(registries, list.getCompound(index));
            if (stack != null && stack.amount() > 0) {
                stacks.add(stack);
            }
        }
        return List.copyOf(stacks);
    }

    @Nullable
    private static Kind kindNamed(String name) {
        for (Kind kind : Kind.values()) {
            if (kind.name().equals(name)) {
                return kind;
            }
        }
        return null;
    }
}
