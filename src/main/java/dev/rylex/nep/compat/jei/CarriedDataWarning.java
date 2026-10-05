package dev.rylex.nep.compat.jei;

import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * A {@link IRecipeTransferError.Type#COSMETIC} error, so the transfer button stays usable and only warns on hover.
 */
public final class CarriedDataWarning implements IRecipeTransferError {

    private final ItemStack carried;

    public CarriedDataWarning(ItemStack carried) {
        this.carried = carried;
    }

    @Override
    public Type getType() {
        return Type.COSMETIC;
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip) {
        tooltip.add(Component.translatable("jei.nep.manual.carries_data", carried.getHoverName())
                .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("jei.nep.manual.carries_data.hint").withStyle(ChatFormatting.GRAY));
    }
}
