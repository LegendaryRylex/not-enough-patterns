package dev.rylex.nep.decoder;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public class EncodingModuleItem extends Item {

    private final DecoderModule module;

    public EncodingModuleItem(Properties properties, DecoderModule module) {
        super(properties);
        this.module = module;
    }

    public DecoderModule module() {
        return module;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.nep.encoding_module.tooltip", module.modName())
                .withStyle(ChatFormatting.GRAY));
    }
}
