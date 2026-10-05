package dev.rylex.nep.decoder;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

public class ModuleSlots extends ItemStacksResourceHandler {

    public ModuleSlots() {
        super(PatternDecoderBlockEntity.SLOTS);
    }

    @Override
    public boolean isValid(int index, ItemResource resource) {
        return resource.getItem() instanceof EncodingModuleItem module
                && module.module().ordinal() == index;
    }

    @Override
    protected int getCapacity(int index, ItemResource resource) {
        return 1;
    }

    @Override
    public void deserialize(ValueInput input) {
        ItemStacksResourceHandler saved = new ItemStacksResourceHandler(0);
        saved.deserialize(input);
        NonNullList<ItemStack> seated = NonNullList.withSize(PatternDecoderBlockEntity.SLOTS, ItemStack.EMPTY);
        for (ItemStack stack : saved.copyToList()) {
            if (stack.getItem() instanceof EncodingModuleItem module
                    && seated.get(module.module().ordinal()).isEmpty()) {
                seated.set(module.module().ordinal(), stack.copyWithCount(1));
            }
        }
        setStacks(seated);
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
