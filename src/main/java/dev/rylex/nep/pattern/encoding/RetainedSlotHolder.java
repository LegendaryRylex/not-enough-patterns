package dev.rylex.nep.pattern.encoding;

import java.util.List;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;

public interface RetainedSlotHolder {

    boolean nep$isRetainedSlot(@Nullable Slot slot);

    void nep$setRetainedSlots(List<Integer> slots);
}
