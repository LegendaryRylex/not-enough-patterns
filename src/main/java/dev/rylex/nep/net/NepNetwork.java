package dev.rylex.nep.net;

import dev.rylex.nep.pattern.encoding.PatternRecipeHolder;
import dev.rylex.nep.pattern.encoding.RetainedSlotHolder;
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
                        (payload, context) -> context.enqueueWork(() -> handleRecipe(payload, context)))
                .playToClient(
                        RetainedSlotsPayload.TYPE,
                        RetainedSlotsPayload.STREAM_CODEC,
                        (payload, context) -> context.enqueueWork(() -> handleRetainedSlots(payload, context)));
    }

    private static void handleRecipe(PatternRecipePayload payload, IPayloadContext context) {
        Player player = context.player();
        if (player.containerMenu instanceof PatternRecipeHolder holder) {
            holder.nep$setRecipeId(payload.recipe().orElse(null));
        }
    }

    private static void handleRetainedSlots(RetainedSlotsPayload payload, IPayloadContext context) {
        Player player = context.player();
        if (player.containerMenu instanceof RetainedSlotHolder holder) {
            holder.nep$setRetainedSlots(payload.slots());
        }
    }
}
