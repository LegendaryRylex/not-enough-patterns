package dev.rylex.nep.machine;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public record ManualCraftOutcome(ManualCraftResult status, int batches, ItemStack result) {

    public static ManualCraftOutcome failed(ManualCraftResult status) {
        return new ManualCraftOutcome(status, 0, ItemStack.EMPTY);
    }

    public static ManualCraftOutcome started(int batches, ItemStack result) {
        return new ManualCraftOutcome(ManualCraftResult.STARTED, batches, result);
    }

    @Nullable
    public Component message() {
        return switch (status) {
            case STARTED -> Component.translatable("nep.manual.queued", batches, result.getHoverName());
            case DISABLED -> Component.translatable("nep.manual.disabled");
            case UNKNOWN_RECIPE -> Component.translatable("nep.manual.unknown_recipe");
            case UNSUPPORTED -> Component.translatable("nep.manual.unsupported");
            case BUSY -> Component.translatable("nep.manual.busy");
            case MISSING_ITEMS -> Component.translatable("nep.manual.missing_items");
            case BUFFER_FULL -> Component.translatable("nep.manual.buffer_full");
            case TIER_TOO_HIGH -> Component.translatable("nep.manual.tier_too_high");
            case FIELD_TOO_LARGE -> Component.translatable("nep.manual.field_too_large");
            case NO_FIELD -> Component.translatable("nep.manual.no_field");
            case NO_LINE -> Component.translatable("nep.manual.no_line");
            case NO_IMPETUS -> Component.translatable("nep.manual.no_impetus");
        };
    }
}
