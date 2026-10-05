package dev.rylex.nep.decoder;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

public class ModuleSlots extends ItemStackHandler {

    public ModuleSlots() {
        super(PatternDecoderBlockEntity.SLOTS);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return stack.getItem() instanceof EncodingModuleItem module
                && module.module().ordinal() == slot;
    }

    @Override
    public int getSlotLimit(int slot) {
        return 1;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider registries, CompoundTag tag) {
        ItemStackHandler saved = new ItemStackHandler();
        saved.deserializeNBT(registries, tag);
        stacks = NonNullList.withSize(PatternDecoderBlockEntity.SLOTS, ItemStack.EMPTY);
        for (int slot = 0; slot < saved.getSlots(); slot++) {
            ItemStack stack = saved.getStackInSlot(slot);
            if (stack.getItem() instanceof EncodingModuleItem module
                    && stacks.get(module.module().ordinal()).isEmpty()) {
                stacks.set(module.module().ordinal(), stack.copyWithCount(1));
            }
        }
        onLoad();
    }

    public int held() {
        int held = 0;
        for (ItemStack stack : stacks) {
            if (stack.getItem() instanceof EncodingModuleItem module) {
                held |= module.module().bit();
            }
        }
        return held;
    }
}
