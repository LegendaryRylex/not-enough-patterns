package dev.rylex.nep.compat.occultism;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public class SpiritInfusedBookItem extends Item {

    public SpiritInfusedBookItem(Properties properties) {
        super(properties);
    }

    public static ItemStack of(BoundSpirit spirit) {
        ItemStack stack = new ItemStack(NepOccultismContent.SPIRIT_INFUSED_BOOK.get());
        stack.set(NepOccultismContent.BOUND_SPIRIT.get(), spirit);
        return stack;
    }

    @Override
    public void appendHoverText(
            ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        BoundSpirit spirit = stack.get(NepOccultismContent.BOUND_SPIRIT.get());
        if (spirit == null) {
            tooltip.add(Component.translatable("tooltip.nep.spirit_infused_book.empty")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }
        tooltip.add(Component.translatable(
                        "tooltip.nep.spirit_infused_book.spirit",
                        spirit.tier().displayName(),
                        spirit.work().displayName())
                .withStyle(ChatFormatting.GRAY));
    }
}
