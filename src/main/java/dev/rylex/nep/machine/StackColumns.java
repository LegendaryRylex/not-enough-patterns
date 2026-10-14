package dev.rylex.nep.machine;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

public record StackColumns(List<AEKey> keys, List<Long> counts) {
    public static StackColumns read(CompoundTag tag, String key, HolderLookup.Provider registries) {
        ListTag stacks = tag.getList(key, Tag.TAG_COMPOUND);
        List<AEKey> keys = new ArrayList<>();
        List<Long> counts = new ArrayList<>();
        for (int s = 0; s < stacks.size(); s++) {
            GenericStack stack = GenericStack.readTag(registries, stacks.getCompound(s));
            if (stack != null) {
                keys.add(stack.what());
                counts.add(stack.amount());
            }
        }
        return new StackColumns(List.copyOf(keys), List.copyOf(counts));
    }

    public ListTag write(HolderLookup.Provider registries) {
        ListTag stacks = new ListTag();
        for (int i = 0; i < keys.size(); i++) {
            stacks.add(GenericStack.writeTag(registries, new GenericStack(keys.get(i), counts.get(i))));
        }
        return stacks;
    }
}
