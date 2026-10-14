package dev.rylex.nep.pattern;

import appeng.api.crafting.PatternDetailsTooltip;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.AmountFormat;
import appeng.api.stacks.GenericStack;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class RetainedInputs {
    private RetainedInputs() {}

    public static final int HIGHLIGHT = 0xFFFFD24A;

    public static void describe(PatternDetailsTooltip tooltip, List<GenericStack> retained) {
        describe(tooltip, retained, "nep.pattern.returned", "nep.pattern.returned.and");
    }

    public static void describeWorn(PatternDetailsTooltip tooltip, List<GenericStack> worn) {
        describe(tooltip, worn, "nep.pattern.worn", "nep.pattern.worn.and");
    }

    private static void describe(
            PatternDetailsTooltip tooltip, List<GenericStack> stacks, String firstKey, String andKey) {
        boolean first = true;
        for (GenericStack stack : stacks) {
            tooltip.addProperty(Component.empty()
                    .append(Component.translatable(first ? firstKey : andKey))
                    .append(": ")
                    .append(entry(stack).withStyle(ChatFormatting.YELLOW)));
            first = false;
        }
    }

    public static MutableComponent entry(GenericStack stack) {
        AEKeyType type = stack.what().getType();
        return Component.literal(type.formatAmount(stack.amount(), AmountFormat.FULL))
                .append(" x ")
                .append(stack.what().getDisplayName());
    }

    public static Component slotNote() {
        return Component.translatable("nep.pattern.retained.slot").withStyle(ChatFormatting.YELLOW);
    }
}
