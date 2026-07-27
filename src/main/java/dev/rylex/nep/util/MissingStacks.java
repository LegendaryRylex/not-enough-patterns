package dev.rylex.nep.util;

import appeng.api.stacks.AmountFormat;
import appeng.api.stacks.GenericStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class MissingStacks {

    private MissingStacks() {}

    public static MutableComponent describe(GenericStack stack) {
        MutableComponent name = stack.what().getDisplayName().copy();
        if (stack.amount() <= 1) {
            return name;
        }
        return Component.translatable(
                "nep.missing.entry", name, stack.what().formatAmount(stack.amount(), AmountFormat.FULL));
    }
}
