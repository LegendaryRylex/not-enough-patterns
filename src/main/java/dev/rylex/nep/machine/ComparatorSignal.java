package dev.rylex.nep.machine;

import net.minecraft.util.Mth;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

public final class ComparatorSignal {

    private ComparatorSignal() {}

    public static int of(ResourceHandler<ItemResource> buffer) {
        int slots = buffer.size();
        if (slots == 0) {
            return 0;
        }
        float fill = 0.0F;
        for (int slot = 0; slot < slots; slot++) {
            ItemResource resource = buffer.getResource(slot);
            long amount = buffer.getAmountAsLong(slot);
            if (amount <= 0) {
                continue;
            }
            long capacity = Math.min(buffer.getCapacityAsLong(slot, resource), resource.getMaxStackSize());
            if (capacity > 0) {
                fill += Math.min(1.0F, amount / (float) capacity);
            }
        }
        fill /= slots;
        return Mth.floor(fill * 14.0F) + (fill > 0.0F ? 1 : 0);
    }
}
