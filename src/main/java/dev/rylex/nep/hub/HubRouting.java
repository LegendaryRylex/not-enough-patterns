package dev.rylex.nep.hub;

import appeng.api.config.Actionable;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

public final class HubRouting {

    private HubRouting() {}

    public static long insert(List<HubTarget> targets, AEKey what, long amount, Actionable mode) {
        if (amount <= 0) {
            return 0;
        }
        boolean simulate = mode == Actionable.SIMULATE;
        long remaining = amount;
        for (HubTarget target : byPriority(targets)) {
            if (!target.acceptsKey(what)) {
                continue;
            }
            if (what instanceof AEItemKey key && target.items() != null) {
                remaining -= insertItems(target.items(), key, remaining, simulate);
            } else if (what instanceof AEFluidKey key && target.fluids() != null) {
                remaining -= insertFluid(target.fluids(), key, remaining, simulate);
            }
            if (remaining <= 0) {
                break;
            }
        }
        return amount - remaining;
    }

    public static long extract(List<HubTarget> targets, AEKey what, long amount, Actionable mode) {
        if (amount <= 0) {
            return 0;
        }
        boolean simulate = mode == Actionable.SIMULATE;
        long remaining = amount;
        for (HubTarget target : byPriority(targets)) {
            if (!target.returnsKey(what)) {
                continue;
            }
            if (what instanceof AEItemKey key && target.items() != null) {
                remaining -= extractItems(target.items(), key, remaining, simulate);
            } else if (what instanceof AEFluidKey key && target.fluids() != null) {
                remaining -= extractFluid(target.fluids(), key, remaining, simulate);
            }
            if (remaining <= 0) {
                break;
            }
        }
        return amount - remaining;
    }

    public static void collect(List<HubTarget> targets, KeyCounter out) {
        for (HubTarget target : targets) {
            IItemHandler items = target.items();
            if (items != null) {
                for (int slot = 0; slot < items.getSlots(); slot++) {
                    ItemStack stack = items.getStackInSlot(slot);
                    AEItemKey key = stack.isEmpty() ? null : AEItemKey.of(stack);
                    if (key != null) {
                        out.add(key, stack.getCount());
                    }
                }
            }
            IFluidHandler fluids = target.fluids();
            if (fluids != null) {
                for (int tank = 0; tank < fluids.getTanks(); tank++) {
                    FluidStack stack = fluids.getFluidInTank(tank);
                    AEFluidKey key = stack.isEmpty() ? null : AEFluidKey.of(stack);
                    if (key != null) {
                        out.add(key, stack.getAmount());
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

    private static long insertItems(IItemHandler handler, AEItemKey key, long amount, boolean simulate) {
        int maxStack = key.getReadOnlyStack().getMaxStackSize();
        long remaining = amount;
        for (int slot = 0; slot < handler.getSlots() && remaining > 0; slot++) {
            int slotCap = Math.max(maxStack, handler.getSlotLimit(slot));
            int want = (int) Math.min(remaining, slotCap);
            if (want <= 0) {
                continue;
            }
            ItemStack refused = handler.insertItem(slot, key.toStack(want), simulate);
            remaining -= want - refused.getCount();
        }
        return amount - remaining;
    }

    private static long extractItems(IItemHandler handler, AEItemKey key, long amount, boolean simulate) {
        long remaining = amount;
        for (int slot = 0; slot < handler.getSlots() && remaining > 0; slot++) {
            ItemStack held = handler.getStackInSlot(slot);
            if (held.isEmpty() || !key.matches(held)) {
                continue;
            }
            int want = (int) Math.min(remaining, held.getCount());
            ItemStack taken = handler.extractItem(slot, want, simulate);
            remaining -= taken.getCount();
        }
        return amount - remaining;
    }

    private static long insertFluid(IFluidHandler handler, AEFluidKey key, long amount, boolean simulate) {
        int want = (int) Math.min(amount, Integer.MAX_VALUE);
        return handler.fill(
                key.toStack(want), simulate ? IFluidHandler.FluidAction.SIMULATE : IFluidHandler.FluidAction.EXECUTE);
    }

    private static long extractFluid(IFluidHandler handler, AEFluidKey key, long amount, boolean simulate) {
        int want = (int) Math.min(amount, Integer.MAX_VALUE);
        FluidStack drained = handler.drain(
                key.toStack(want), simulate ? IFluidHandler.FluidAction.SIMULATE : IFluidHandler.FluidAction.EXECUTE);
        return drained.getAmount();
    }
}
