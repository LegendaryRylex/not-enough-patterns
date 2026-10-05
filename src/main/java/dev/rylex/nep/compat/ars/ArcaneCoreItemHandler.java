package dev.rylex.nep.compat.ars;

import com.hollingsworth.arsnouveau.common.block.tile.ArcaneCoreTile;
import com.hollingsworth.arsnouveau.common.block.tile.EnchantingApparatusTile;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

/** Resolved on every call, since the apparatus above the core can be placed or broken while this handler is cached. */
record ArcaneCoreItemHandler(ArcaneCoreTile core, @Nullable Direction side) implements IItemHandler {

    @Nullable
    private IItemHandler apparatus() {
        EnchantingApparatusTile apparatus = EnchantingApparatusCraftingMachine.apparatusFor(core);
        if (apparatus == null || apparatus.getLevel() == null) {
            return null;
        }
        return apparatus.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, apparatus.getBlockPos(), side);
    }

    @Override
    public int getSlots() {
        IItemHandler apparatus = apparatus();
        return apparatus == null ? 0 : apparatus.getSlots();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        IItemHandler apparatus = apparatus();
        return apparatus == null || slot >= apparatus.getSlots() ? ItemStack.EMPTY : apparatus.getStackInSlot(slot);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        IItemHandler apparatus = apparatus();
        return apparatus == null || slot >= apparatus.getSlots() ? stack : apparatus.insertItem(slot, stack, simulate);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        IItemHandler apparatus = apparatus();
        return apparatus == null || slot >= apparatus.getSlots()
                ? ItemStack.EMPTY
                : apparatus.extractItem(slot, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        IItemHandler apparatus = apparatus();
        return apparatus == null || slot >= apparatus.getSlots() ? 0 : apparatus.getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        IItemHandler apparatus = apparatus();
        return apparatus != null && slot < apparatus.getSlots() && apparatus.isItemValid(slot, stack);
    }
}
