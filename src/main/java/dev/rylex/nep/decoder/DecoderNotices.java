package dev.rylex.nep.decoder;

import dev.rylex.nep.Nep;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = Nep.MOD_ID)
public final class DecoderNotices {
    private DecoderNotices() {}

    private static final Map<UUID, Set<DecoderModule>> SHOWN = new ConcurrentHashMap<>();

    public static void plainFallback(ServerPlayer player, DecoderModule module) {
        Set<DecoderModule> shown = SHOWN.computeIfAbsent(player.getUUID(), id -> EnumSet.noneOf(DecoderModule.class));
        if (!shown.add(module)) {
            return;
        }
        player.sendSystemMessage(Component.translatable(
                        "nep.decoder.plain_fallback",
                        module.modName(),
                        Component.translatable("item.nep." + module.itemPath()))
                .withStyle(ChatFormatting.YELLOW));
    }

    @SubscribeEvent
    static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        SHOWN.remove(event.getEntity().getUUID());
    }
}
