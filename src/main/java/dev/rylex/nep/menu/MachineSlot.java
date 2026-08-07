package dev.rylex.nep.menu;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.SlotItemHandler;

public class MachineSlot extends SlotItemHandler {

    private boolean setting;

    public MachineSlot(IItemHandler handler, int index, int x, int y) {
        super(handler, index, x, y);
    }

    @Override
    public void set(ItemStack stack) {
        setting = true;
        try {
            super.set(stack);
        } finally {
            setting = false;
        }
    }

    @Override
    public void setChanged() {
        if (setting || !(getItemHandler() instanceof IItemHandlerModifiable handler)) {
            return;
        }
        handler.setStackInSlot(index, handler.getStackInSlot(index));
    }
}
