package dev.rylex.nep.net;

import dev.rylex.nep.pattern.encoding.PatternRecipeHolder;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class NepNetwork {
    private NepNetwork() {}

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1")
                .playToServer(
                        PatternRecipePayload.TYPE,
                        PatternRecipePayload.STREAM_CODEC,
                        (payload, context) -> context.enqueueWork(() -> handleRecipe(payload, context)));
    }

    private static void handleRecipe(PatternRecipePayload payload, IPayloadContext context) {
        Player player = context.player();
        if (player.containerMenu instanceof PatternRecipeHolder holder) {
            holder.nep$setRecipeId(payload.recipe().orElse(null));
        }
    }
}
