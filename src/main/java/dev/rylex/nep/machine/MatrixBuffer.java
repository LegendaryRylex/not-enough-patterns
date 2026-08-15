package dev.rylex.nep.machine;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

public class MatrixBuffer extends ItemStacksResourceHandler {

    public static final int SLOT_LIMIT = 64;

    private final int fixedSize;
    private final Runnable onChanged;

    public MatrixBuffer(int size, Runnable onChanged) {
        super(size);
        this.fixedSize = size;
        this.onChanged = onChanged;
    }

    @Override
    protected int getCapacity(int index, ItemResource resource) {
        return SLOT_LIMIT;
    }

    @Override
    protected void onContentsChanged(int index, ItemStack previousContents) {
        onChanged.run();
    }

    public ItemStack stackAt(int index) {
        return getResource(index).toStack(getAmountAsInt(index));
    }

    public void clear(int index) {
        set(index, ItemResource.EMPTY, 0);
    }

    @Override
    public void deserialize(ValueInput input) {
        super.deserialize(input);
        if (size() == fixedSize) {
            return;
        }
        NonNullList<ItemStack> loaded = copyToList();
        NonNullList<ItemStack> resized = NonNullList.withSize(fixedSize, ItemStack.EMPTY);
        for (int index = 0; index < Math.min(fixedSize, loaded.size()); index++) {
            resized.set(index, loaded.get(index));
        }
        setStacks(resized);
    }
}
