package dev.rylex.nep.decoder;

import dev.rylex.nep.Nep;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.tooltip.TooltipLocation;
import net.neoforged.neoforge.event.RegisterTooltipAppendersEvent;

@EventBusSubscriber(modid = Nep.MOD_ID)
public class EncodingModuleItem extends Item {

    private final DecoderModule module;

    public EncodingModuleItem(Properties properties, DecoderModule module) {
        super(properties);
        this.module = module;
    }

    public DecoderModule module() {
        return module;
    }

    @SubscribeEvent
    static void registerTooltipAppenders(RegisterTooltipAppendersEvent event) {
        event.registerAppender(TooltipLocation.POST_CUSTOM, (stack, _, _, _, _, tooltip) -> {
            if (stack.getItem() instanceof EncodingModuleItem item) {
                tooltip.accept(Component.translatable(
                                "item.nep.encoding_module.tooltip",
                                item.module().modName())
                        .withStyle(ChatFormatting.GRAY));
            }
        });
    }
}
