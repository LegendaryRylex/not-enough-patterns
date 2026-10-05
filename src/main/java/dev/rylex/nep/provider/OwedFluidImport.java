package dev.rylex.nep.provider;

import appeng.api.behaviors.StackImportStrategy;
import appeng.api.behaviors.StackTransferContext;
import appeng.api.config.Actionable;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEKeyType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * AE2's own fluid import counts a bucket per operation, which would drain a whole basin for a craft that owes a
 * hundred millibuckets, so the owed amount is spent directly instead.
 */
public final class OwedFluidImport implements StackImportStrategy {

    private final BlockCapabilityCache<IFluidHandler, Direction> cache;

    public OwedFluidImport(ServerLevel level, BlockPos fromPos, Direction fromSide) {
        this.cache = BlockCapabilityCache.create(Capabilities.FluidHandler.BLOCK, level, fromPos, fromSide);
    }

    @Override
    public boolean transfer(StackTransferContext context) {
        if (!context.isKeyTypeEnabled(AEKeyType.fluids())) {
            return false;
        }
        IFluidHandler handler = cache.getCapability();
        if (handler == null) {
            return false;
        }
        var storage = context.getInternalStorage().getInventory();
        boolean moved = false;
        for (int tank = 0; tank < handler.getTanks() && context.hasOperationsLeft(); tank++) {
            FluidStack held = handler.getFluidInTank(tank);
            AEFluidKey key = AEFluidKey.of(held);
            if (key == null || !context.isInFilter(key)) {
                continue;
            }
            long room = storage.insert(key, held.getAmount(), Actionable.SIMULATE, context.getActionSource());
            int wanted = (int) Math.min(context.getOperationsRemaining(), Math.min(held.getAmount(), room));
            if (wanted <= 0) {
                continue;
            }
            FluidStack drained = handler.drain(key.toStack(wanted), IFluidHandler.FluidAction.EXECUTE);
            if (drained.isEmpty()) {
                continue;
            }
            long inserted = storage.insert(key, drained.getAmount(), Actionable.MODULATE, context.getActionSource());
            long leftover = drained.getAmount() - inserted;
            if (leftover > 0) {
                handler.fill(key.toStack((int) leftover), IFluidHandler.FluidAction.EXECUTE);
            }
            if (inserted > 0) {
                context.reduceOperationsRemaining(inserted);
                moved = true;
            }
        }
        return moved;
    }
}
