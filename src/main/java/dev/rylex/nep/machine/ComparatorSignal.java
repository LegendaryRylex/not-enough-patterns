package dev.rylex.nep.machine;

import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

public final class ComparatorSignal {

    private ComparatorSignal() {}

    public static int of(IItemHandler buffer) {
        int slots = buffer.getSlots();
        if (slots == 0) {
            return 0;
        }
        float fill = 0.0F;
        for (int slot = 0; slot < slots; slot++) {
            ItemStack stack = buffer.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                fill += Math.min(
                        1.0F, stack.getCount() / (float) Math.min(buffer.getSlotLimit(slot), stack.getMaxStackSize()));
            }
        }
        fill /= slots;
        return Mth.floor(fill * 14.0F) + (fill > 0.0F ? 1 : 0);
    }
}
