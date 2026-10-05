package dev.rylex.nep.pattern;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class ToolWear {
    private ToolWear() {}

    /**
     * The key a tool comes back as after one use, or null when that use breaks it.
     */
    @Nullable
    public static AEKey worn(AEKey used) {
        if (!(used instanceof AEItemKey key)) {
            return null;
        }
        ItemStack stack = key.toStack();
        if (!stack.isDamageableItem()) {
            return null;
        }
        int next = stack.getDamageValue() + 1;
        if (next >= stack.getMaxDamage()) {
            return null;
        }
        stack.setDamageValue(next);
        return AEItemKey.of(stack);
    }

    public static boolean differsOnlyByDamage(AEKey template, AEKey candidate) {
        if (!(template instanceof AEItemKey wanted) || !(candidate instanceof AEItemKey offered)) {
            return false;
        }
        ItemStack wantedStack = wanted.toStack();
        ItemStack offeredStack = offered.toStack();
        if (!wantedStack.isDamageableItem() || !offeredStack.isDamageableItem()) {
            return false;
        }
        wantedStack.setDamageValue(0);
        offeredStack.setDamageValue(0);
        return ItemStack.isSameItemSameComponents(wantedStack, offeredStack);
    }
}
