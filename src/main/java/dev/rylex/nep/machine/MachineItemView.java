package dev.rylex.nep.machine;

import java.util.function.ToIntFunction;
import javax.annotation.Nullable;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;

public final class MachineItemView implements IItemHandler {

    private final IItemHandler input;

    @Nullable
    private final IItemHandler output;

    @Nullable
    private final ToIntFunction<ItemStack> demand;

    private MachineItemView(
            IItemHandler input, @Nullable IItemHandler output, @Nullable ToIntFunction<ItemStack> demand) {
        this.input = input;
        this.output = output;
        this.demand = demand;
    }

    public static MachineItemView feedable(IItemHandler input, @Nullable IItemHandler output) {
        return new MachineItemView(input, output, null);
    }

    public static MachineItemView demandLimited(
            IItemHandler input, @Nullable IItemHandler output, ToIntFunction<ItemStack> demand) {
        return new MachineItemView(input, output, demand);
    }

    private int allowance(ItemStack stack) {
        return demand == null ? stack.getCount() : Math.min(stack.getCount(), demand.applyAsInt(stack));
    }

    private int inputSlots() {
        return input.getSlots();
    }

    private boolean isOutput(int slot) {
        return slot >= inputSlots();
    }

    @Override
    public int getSlots() {
        return inputSlots() + (output == null ? 0 : output.getSlots());
    }

    @Override
    public @NotNull ItemStack getStackInSlot(int slot) {
        return isOutput(slot) ? output.getStackInSlot(slot - inputSlots()) : input.getStackInSlot(slot);
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        if (isOutput(slot)) {
            return stack;
        }
        int allowed = allowance(stack);
        if (allowed <= 0) {
            return stack;
        }
        if (allowed >= stack.getCount()) {
            return input.insertItem(slot, stack, simulate);
        }
        ItemStack refused = input.insertItem(slot, stack.copyWithCount(allowed), simulate);
        return stack.copyWithCount(stack.getCount() - allowed + refused.getCount());
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        return isOutput(slot) ? output.extractItem(slot - inputSlots(), amount, simulate) : ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot) {
        return isOutput(slot) ? output.getSlotLimit(slot - inputSlots()) : input.getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return !isOutput(slot) && allowance(stack) > 0 && input.isItemValid(slot, stack);
    }
}
