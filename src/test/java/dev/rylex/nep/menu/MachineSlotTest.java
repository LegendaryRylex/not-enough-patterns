package dev.rylex.nep.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.junit.jupiter.api.Test;

class MachineSlotTest {

    private int changes;

    private final ItemStackHandler handler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            changes++;
        }
    };

    @Test
    void aStackGrownInPlaceStillNotifiesTheHandler() {
        handler.setStackInSlot(0, new ItemStack(Items.DIAMOND, 4));
        MachineSlot slot = new MachineSlot(handler, 0, 0, 0);
        changes = 0;

        slot.getItem().setCount(6);
        slot.setChanged();

        assertEquals(1, changes, "shift-clicking onto an occupied slot only calls setChanged()");
        assertEquals(6, handler.getStackInSlot(0).getCount());
    }

    @Test
    void replacingTheStackNotifiesExactlyOnce() {
        MachineSlot slot = new MachineSlot(handler, 0, 0, 0);
        changes = 0;

        slot.set(new ItemStack(Items.DIAMOND, 2));

        assertEquals(1, changes);
        assertEquals(2, handler.getStackInSlot(0).getCount());
    }
}
