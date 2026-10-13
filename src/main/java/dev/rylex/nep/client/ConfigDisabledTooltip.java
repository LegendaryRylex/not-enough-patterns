package dev.rylex.nep.client;

import dev.rylex.nep.Nep;
import dev.rylex.nep.NepConfigGates;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(modid = Nep.MOD_ID, value = Dist.CLIENT)
public final class ConfigDisabledTooltip {
    private ConfigDisabledTooltip() {}

    @SubscribeEvent
    static void onItemTooltip(ItemTooltipEvent event) {
        if (!NepConfigGates.disabled(event.getItemStack().getItem())) {
            return;
        }
        List<Component> lines = event.getToolTip();
        lines.add(
                Math.min(1, lines.size()),
                Component.translatable("tooltip.nep.disabled_in_config").withStyle(ChatFormatting.RED));
    }
}
