package dev.rylex.nep.client;

import dev.rylex.nep.Nep;
import dev.rylex.nep.pattern.encoding.ProcessingPatternConversionRecipe;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(modid = Nep.MOD_ID, value = Dist.CLIENT)
public final class PatternConversionTooltip {
    private PatternConversionTooltip() {}

    @SubscribeEvent
    static void onItemTooltip(ItemTooltipEvent event) {
        if (!ProcessingPatternConversionRecipe.converts(event.getItemStack())) {
            return;
        }
        List<Component> lines = event.getToolTip();
        lines.add(
                Math.min(1, lines.size()),
                Component.translatable("tooltip.nep.pattern_conversion")
                        .withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC));
    }
}
