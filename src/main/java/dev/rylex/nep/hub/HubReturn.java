package dev.rylex.nep.hub;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.MEStorage;
import dev.rylex.nep.Nep;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

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
            IItemHandler items = target.items();
            if (items != null) {
                for (int slot = 0; slot < items.getSlots(); slot++) {
                    Outcome slotOutcome = pushSlot(items, slot, network, source);
                    moved += slotOutcome.moved();
                    refused |= slotOutcome.refused();
                }
            }
            IFluidHandler fluids = target.fluids();
            if (fluids != null) {
                for (int tank = 0; tank < fluids.getTanks(); tank++) {
                    Outcome tankOutcome = pushTank(fluids, tank, network, source);
                    moved += tankOutcome.moved();
                    refused |= tankOutcome.refused();
                }
            }
        }
        return new Outcome(moved, refused);
    }

    private static Outcome pushSlot(IItemHandler handler, int slot, MEStorage network, IActionSource source) {
        ItemStack held = handler.getStackInSlot(slot);
        if (held.isEmpty()) {
            return new Outcome(0, false);
        }
        ItemStack offered = handler.extractItem(slot, held.getCount(), true);
        if (offered.isEmpty()) {
            return new Outcome(0, false);
        }
        AEItemKey key = AEItemKey.of(offered);
        long room = network.insert(key, offered.getCount(), Actionable.SIMULATE, source);
        if (room <= 0) {
            return new Outcome(0, true);
        }
        boolean partial = room < offered.getCount();
        ItemStack taken = handler.extractItem(slot, (int) Math.min(room, offered.getCount()), false);
        if (taken.isEmpty()) {
            return new Outcome(0, partial);
        }
        long accepted = network.insert(AEItemKey.of(taken), taken.getCount(), Actionable.MODULATE, source);
        long leftover = taken.getCount() - accepted;
        if (leftover > 0) {
            ItemStack returned = ItemHandlerHelper.insertItem(handler, taken.copyWithCount((int) leftover), false);
            if (!returned.isEmpty()) {
                Nep.LOGGER.warn(
                        "Machine Hub took {} from an output inventory that would no longer take it back and the"
                                + " network refused it",
                        returned);
            }
        }
        return new Outcome(accepted, partial);
    }

    private static Outcome pushTank(IFluidHandler handler, int tank, MEStorage network, IActionSource source) {
        FluidStack held = handler.getFluidInTank(tank);
        if (held.isEmpty()) {
            return new Outcome(0, false);
        }
        FluidStack offered = handler.drain(held, IFluidHandler.FluidAction.SIMULATE);
        if (offered.isEmpty()) {
            return new Outcome(0, false);
        }
        AEFluidKey key = AEFluidKey.of(offered);
        long room = network.insert(key, offered.getAmount(), Actionable.SIMULATE, source);
        if (room <= 0) {
            return new Outcome(0, true);
        }
        boolean partial = room < offered.getAmount();
        FluidStack taken = handler.drain(
                offered.copyWithAmount((int) Math.min(room, offered.getAmount())), IFluidHandler.FluidAction.EXECUTE);
        if (taken.isEmpty()) {
            return new Outcome(0, partial);
        }
        long accepted = network.insert(AEFluidKey.of(taken), taken.getAmount(), Actionable.MODULATE, source);
        long leftover = taken.getAmount() - accepted;
        if (leftover > 0) {
            int returned = handler.fill(taken.copyWithAmount((int) leftover), IFluidHandler.FluidAction.EXECUTE);
            if (returned < leftover) {
                Nep.LOGGER.warn(
                        "Machine Hub drained {} mB of {} that neither the network nor the tank would take back",
                        leftover - returned,
                        taken.getHoverName().getString());
            }
        }
        return new Outcome(accepted, partial);
    }
}
