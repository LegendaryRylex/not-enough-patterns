package dev.rylex.nep.menu;

import dev.rylex.nep.util.SubLevels;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

public abstract class MachineMenu extends AbstractContainerMenu {

    private static final double REACH_SQR = 64.0;

    private int inventoryStart = -1;
    private int hotbarStart = -1;
    private int playerEnd = -1;

    protected MachineMenu(@Nullable MenuType<?> type, int windowId) {
        super(type, windowId);
    }

    protected void addPlayerInventory(Inventory playerInv, int x, int y) {
        inventoryStart = slots.size();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, 9 + row * 9 + col, x + col * 18, y + row * 18));
            }
        }
        hotbarStart = slots.size();
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, x + col * 18, y + 58));
        }
        playerEnd = slots.size();
    }

    protected boolean movePlayerStackIntoMachine(ItemStack stack) {
        return false;
    }

    protected static boolean withinReach(Player player, @Nullable BlockEntity machine) {
        return machine != null
                && !machine.isRemoved()
                && SubLevels.distanceSqr(player, machine.getBlockPos()) <= REACH_SQR;
    }

    protected final boolean moveIntoPlayerInventory(ItemStack stack) {
        boolean moved = moveItemStackTo(stack, inventoryStart, hotbarStart, false);
        if (!stack.isEmpty()) {
            moved |= moveItemStackTo(stack, hotbarStart, playerEnd, false);
        }
        return moved;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (inventoryStart < 0 || index < 0 || index >= slots.size()) {
            return ItemStack.EMPTY;
        }
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) {
            return ItemStack.EMPTY;
        }
        ItemStack original = slot.getItem().copy();
        ItemStack remainder = original.copy();
        boolean moved =
                index < inventoryStart ? moveIntoPlayerInventory(remainder) : movePlayerStackIntoMachine(remainder);
        if (!moved || remainder.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.setByPlayer(remainder);
        slot.onTake(player, original.copyWithCount(original.getCount() - remainder.getCount()));
        return original;
    }
}
