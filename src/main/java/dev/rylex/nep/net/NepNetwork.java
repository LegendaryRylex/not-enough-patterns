package dev.rylex.nep.net;

import dev.rylex.nep.client.MachineHubStateClient;
import dev.rylex.nep.hub.MachineHubMenu;
import dev.rylex.nep.hub.MachineHubState;
import dev.rylex.nep.machine.ManualCraftHost;
import dev.rylex.nep.machine.ManualCraftOutcome;
import dev.rylex.nep.machine.ManualPull;
import dev.rylex.nep.menu.ManualCraftMenu;
import dev.rylex.nep.pattern.encoding.PatternRecipeHolder;
import dev.rylex.nep.pattern.encoding.RetainedSlotHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
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
                .playToServer(
                        ManualCraftPayload.TYPE,
                        ManualCraftPayload.STREAM_CODEC,
                        (payload, context) -> context.enqueueWork(() -> handleManualCraft(payload, context)))
                .playToServer(
                        HubLinkEditPayload.TYPE,
                        HubLinkEditPayload.STREAM_CODEC,
                        (payload, context) -> context.enqueueWork(() -> handleHubLinkEdit(payload, context)))
                .playToClient(
                        RetainedSlotsPayload.TYPE,
                        RetainedSlotsPayload.STREAM_CODEC,
                        (payload, context) -> context.enqueueWork(() -> handleRetainedSlots(payload, context)))
                .playToClient(
                        MachineHubState.TYPE,
                        MachineHubState.STREAM_CODEC,
                        (payload, context) -> context.enqueueWork(() -> MachineHubStateClient.handle(payload)));
    }

    private static void handleRecipe(PatternRecipePayload payload, IPayloadContext context) {
        Player player = context.player();
        if (player.containerMenu instanceof PatternRecipeHolder holder) {
            holder.nep$setOrigin(payload.origin());
        }
    }

    private static void handleManualCraft(ManualCraftPayload payload, IPayloadContext context) {
        Player player = context.player();
        if (!(player.containerMenu instanceof ManualCraftMenu menu)
                || !menu.machinePos().equals(payload.pos())
                || !player.containerMenu.stillValid(player)) {
            return;
        }
        if (!(player.level().getBlockEntity(payload.pos()) instanceof ManualCraftHost host)) {
            return;
        }
        ManualCraftOutcome outcome = host.startManualCraft(
                player, payload.recipe(), Mth.clamp(payload.batches(), 1, ManualPull.MAXIMUM_BATCHES));
        Component message = outcome.message();
        if (message != null) {
            player.displayClientMessage(message, true);
        }
    }

    private static void handleHubLinkEdit(HubLinkEditPayload payload, IPayloadContext context) {
        Player player = context.player();
        if (player.containerMenu instanceof MachineHubMenu menu) {
            menu.applyEdit(player, payload);
        }
    }

    private static void handleRetainedSlots(RetainedSlotsPayload payload, IPayloadContext context) {
        Player player = context.player();
        if (player.containerMenu instanceof RetainedSlotHolder holder) {
            holder.nep$setRetainedSlots(payload.slots());
        }
    }
}
