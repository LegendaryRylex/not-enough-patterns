package dev.rylex.nep.machine;

import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

public class OverstackedItemHandler extends ItemStackHandler {

    public static final int SLOT_LIMIT = 64;

    private static final String ITEMS_KEY = "Items";
    private static final String SLOT_KEY = "Slot";
    private static final String SIZE_KEY = "Size";
    private static final String AMOUNT_KEY = "Amount";

    public OverstackedItemHandler(int size) {
        super(size);
    }

    /** Writes a stack of any size, which vanilla's item codec refuses past {@link Item#ABSOLUTE_MAX_STACK_SIZE}. */
    public static CompoundTag saveStack(ItemStack stack, HolderLookup.Provider registries) {
        if (stack.getCount() <= Item.ABSOLUTE_MAX_STACK_SIZE) {
            return (CompoundTag) stack.save(registries, new CompoundTag());
        }
        CompoundTag tag = (CompoundTag) stack.copyWithCount(1).save(registries, new CompoundTag());
        tag.putInt(AMOUNT_KEY, stack.getCount());
        return tag;
    }

    public static Optional<ItemStack> parseStack(HolderLookup.Provider registries, CompoundTag tag) {
        return ItemStack.parse(registries, tag)
                .map(stack ->
                        tag.contains(AMOUNT_KEY, Tag.TAG_INT) ? stack.copyWithCount(tag.getInt(AMOUNT_KEY)) : stack);
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider registries) {
        ListTag items = new ListTag();
        for (int slot = 0; slot < stacks.size(); slot++) {
            ItemStack stack = stacks.get(slot);
            if (!stack.isEmpty()) {
                CompoundTag item = saveStack(stack, registries);
                item.putInt(SLOT_KEY, slot);
                items.add(item);
            }
        }
        CompoundTag tag = new CompoundTag();
        tag.put(ITEMS_KEY, items);
        tag.putInt(SIZE_KEY, stacks.size());
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider registries, CompoundTag tag) {
        setSize(tag.contains(SIZE_KEY, Tag.TAG_INT) ? tag.getInt(SIZE_KEY) : stacks.size());
        ListTag items = tag.getList(ITEMS_KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < items.size(); i++) {
            CompoundTag item = items.getCompound(i);
            int slot = item.getInt(SLOT_KEY);
            if (slot >= 0 && slot < stacks.size()) {
                parseStack(registries, item).ifPresent(stack -> stacks.set(slot, stack));
            }
        }
        onLoad();
    }

    @Override
    public int getSlotLimit(int slot) {
        return SLOT_LIMIT;
    }

    @Override
    protected int getStackLimit(int slot, ItemStack stack) {
        return getSlotLimit(slot);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount == 0) {
            return ItemStack.EMPTY;
        }
        validateSlotIndex(slot);
        ItemStack existing = stacks.get(slot);
        if (existing.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int toExtract = Math.min(amount, getSlotLimit(slot));
        if (existing.getCount() <= toExtract) {
            if (simulate) {
                return existing.copy();
            }
            stacks.set(slot, ItemStack.EMPTY);
            onContentsChanged(slot);
            return existing;
        }
        if (!simulate) {
            stacks.set(slot, existing.copyWithCount(existing.getCount() - toExtract));
            onContentsChanged(slot);
        }
        return existing.copyWithCount(toExtract);
    }
}
