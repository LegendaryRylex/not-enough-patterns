package dev.rylex.nep.hub;

import appeng.api.config.Actionable;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

public final class HubRouting {

    private HubRouting() {}

    public static long insert(List<HubTarget> targets, AEKey what, long amount, Actionable mode) {
        if (amount <= 0) {
            return 0;
        }
        try (Transaction tx = Transaction.openRoot()) {
            long remaining = amount;
            for (HubTarget target : byPriority(targets)) {
                if (!target.acceptsKey(what)) {
                    continue;
                }
                if (what instanceof AEItemKey key && target.items() != null) {
                    remaining -= target.items().insert(key.toResource(), clampToInt(remaining), tx);
                } else if (what instanceof AEFluidKey key && target.fluids() != null) {
                    remaining -= target.fluids().insert(key.toResource(), clampToInt(remaining), tx);
                }
                if (remaining <= 0) {
                    break;
                }
            }
            if (mode == Actionable.MODULATE) {
                tx.commit();
            }
            return amount - remaining;
        }
    }

    public static long extract(List<HubTarget> targets, AEKey what, long amount, Actionable mode) {
        if (amount <= 0) {
            return 0;
        }
        try (Transaction tx = Transaction.openRoot()) {
            long remaining = amount;
            for (HubTarget target : byPriority(targets)) {
                if (!target.returnsKey(what)) {
                    continue;
                }
                if (what instanceof AEItemKey key && target.items() != null) {
                    remaining -= target.items().extract(key.toResource(), clampToInt(remaining), tx);
                } else if (what instanceof AEFluidKey key && target.fluids() != null) {
                    remaining -= target.fluids().extract(key.toResource(), clampToInt(remaining), tx);
                }
                if (remaining <= 0) {
                    break;
                }
            }
            if (mode == Actionable.MODULATE) {
                tx.commit();
            }
            return amount - remaining;
        }
    }

    public static void collect(List<HubTarget> targets, KeyCounter out) {
        for (HubTarget target : targets) {
            ResourceHandler<ItemResource> items = target.items();
            if (items != null) {
                for (int slot = 0; slot < items.size(); slot++) {
                    ItemResource resource = items.getResource(slot);
                    long amount = items.getAmountAsLong(slot);
                    if (!resource.isEmpty() && amount > 0) {
                        out.add(AEItemKey.of(resource), amount);
                    }
                }
            }
            ResourceHandler<FluidResource> fluids = target.fluids();
            if (fluids != null) {
                for (int tank = 0; tank < fluids.size(); tank++) {
                    FluidResource resource = fluids.getResource(tank);
                    long amount = fluids.getAmountAsLong(tank);
                    if (!resource.isEmpty() && amount > 0) {
                        out.add(AEFluidKey.of(resource), amount);
                    }
                }
            }
        }
    }

    private static List<HubTarget> byPriority(List<HubTarget> targets) {
        if (targets.size() < 2) {
            return targets;
        }
        List<HubTarget> sorted = new ArrayList<>(targets);
        sorted.sort(Comparator.comparingInt(HubTarget::priority).reversed());
        return sorted;
    }

    static int clampToInt(long amount) {
        return (int) Math.min(Math.max(amount, 0L), Integer.MAX_VALUE);
    }
}
