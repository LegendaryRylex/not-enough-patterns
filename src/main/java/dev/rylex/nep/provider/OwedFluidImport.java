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
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * AE2's own fluid import counts a bucket per operation, which would drain a whole basin for a craft that owes a
 * hundred millibuckets, so the owed amount is spent directly instead.
 */
public final class OwedFluidImport implements StackImportStrategy {

    private final BlockCapabilityCache<ResourceHandler<FluidResource>, Direction> cache;

    public OwedFluidImport(ServerLevel level, BlockPos fromPos, Direction fromSide) {
        this.cache = BlockCapabilityCache.create(Capabilities.Fluid.BLOCK, level, fromPos, fromSide);
    }

    @Override
    public boolean transfer(StackTransferContext context) {
        if (!context.isKeyTypeEnabled(AEKeyType.fluids())) {
            return false;
        }
        ResourceHandler<FluidResource> handler = cache.getCapability();
        if (handler == null) {
            return false;
        }
        var storage = context.getInternalStorage().getInventory();
        boolean moved = false;
        for (int tank = 0; tank < handler.size() && context.hasOperationsLeft(); tank++) {
            FluidResource held = handler.getResource(tank);
            if (held.isEmpty()) {
                continue;
            }
            AEFluidKey key = AEFluidKey.of(held);
            if (!context.isInFilter(key)) {
                continue;
            }
            long amount = handler.getAmountAsLong(tank);
            long room = storage.insert(key, amount, Actionable.SIMULATE, context.getActionSource());
            int wanted = (int) Math.min(context.getOperationsRemaining(), Math.min(amount, room));
            if (wanted <= 0) {
                continue;
            }
            int drained;
            try (Transaction tx = Transaction.openRoot()) {
                drained = handler.extract(tank, held, wanted, tx);
            }
            if (drained <= 0) {
                continue;
            }
            long inserted = storage.insert(key, drained, Actionable.MODULATE, context.getActionSource());
            if (inserted <= 0) {
                continue;
            }
            try (Transaction tx = Transaction.openRoot()) {
                handler.extract(tank, held, (int) inserted, tx);
                tx.commit();
            }
            context.reduceOperationsRemaining(inserted);
            moved = true;
        }
        return moved;
    }
}
