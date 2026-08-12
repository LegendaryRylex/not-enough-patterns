package dev.rylex.nep.hub;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.storage.MEStorage;
import dev.rylex.nep.Nep;
import java.util.List;
import java.util.function.Function;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.resource.Resource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

public final class HubReturn {

    private HubReturn() {}

    public record Outcome(long moved, boolean refused) {}

    public static Outcome push(List<HubTarget> targets, MEStorage network, IActionSource source) {
        long moved = 0;
        boolean refused = false;
        for (HubTarget target : targets) {
            if (target.accepts()) {
                continue;
            }
            ResourceHandler<ItemResource> items = target.items();
            if (items != null) {
                for (int slot = 0; slot < items.size(); slot++) {
                    Outcome slotOutcome = pushIndex(items, slot, network, source, AEItemKey::of);
                    moved += slotOutcome.moved();
                    refused |= slotOutcome.refused();
                }
            }
            ResourceHandler<FluidResource> fluids = target.fluids();
            if (fluids != null) {
                for (int tank = 0; tank < fluids.size(); tank++) {
                    Outcome tankOutcome = pushIndex(fluids, tank, network, source, AEFluidKey::of);
                    moved += tankOutcome.moved();
                    refused |= tankOutcome.refused();
                }
            }
        }
        return new Outcome(moved, refused);
    }

    private static <T extends Resource> Outcome pushIndex(
            ResourceHandler<T> handler, int index, MEStorage network, IActionSource source, Function<T, AEKey> keyOf) {
        T resource = handler.getResource(index);
        int held = handler.getAmountAsInt(index);
        if (resource.isEmpty() || held <= 0) {
            return new Outcome(0, false);
        }
        int offered;
        try (Transaction simulation = Transaction.openRoot()) {
            offered = handler.extract(index, resource, held, simulation);
        }
        if (offered <= 0) {
            return new Outcome(0, false);
        }
        AEKey key = keyOf.apply(resource);
        long room = network.insert(key, offered, Actionable.SIMULATE, source);
        if (room <= 0) {
            return new Outcome(0, true);
        }
        boolean partial = room < offered;
        int taken;
        try (Transaction tx = Transaction.openRoot()) {
            taken = handler.extract(index, resource, (int) Math.min(room, offered), tx);
            tx.commit();
        }
        if (taken <= 0) {
            return new Outcome(0, partial);
        }
        long accepted = network.insert(key, taken, Actionable.MODULATE, source);
        long leftover = taken - accepted;
        if (leftover > 0) {
            int returned = ResourceHandlerUtil.insertStacking(handler, resource, (int) leftover, null);
            if (returned < leftover) {
                Nep.LOGGER.warn(
                        "Machine Hub took {} of {} from an output inventory that would no longer take it back and the"
                                + " network refused it",
                        leftover - returned,
                        key.getDisplayName().getString());
            }
        }
        return new Outcome(accepted, partial);
    }
}
