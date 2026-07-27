package dev.rylex.nep.compat.create;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import com.simibubi.create.content.logistics.depot.DepotBlockEntity;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

final class DepotMachines {
    private DepotMachines() {}

    @Nullable
    static DepotBlockEntity depotBelow(BlockEntity machine, Direction direction) {
        Level level = machine.getLevel();
        if (level == null) {
            return null;
        }
        BlockPos pos = machine.getBlockPos().relative(direction, 2);
        return level.getBlockEntity(pos) instanceof DepotBlockEntity depot ? depot : null;
    }

    @Nullable
    static IItemHandler itemHandler(BlockEntity be) {
        Level level = be.getLevel();
        if (level == null) {
            return null;
        }
        return level.getCapability(Capabilities.ItemHandler.BLOCK, be.getBlockPos(), null);
    }

    static boolean depotReady(DepotBlockEntity depot) {
        if (!depot.getHeldItem().isEmpty()) {
            return false;
        }
        IItemHandler handler = itemHandler(depot);
        if (handler == null) {
            return false;
        }
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            if (!handler.getStackInSlot(slot).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    static boolean collectInputs(KeyCounter[] inputs, Map<AEItemKey, Long> items, Map<AEFluidKey, Long> fluids) {
        for (KeyCounter counter : inputs) {
            for (Object2LongMap.Entry<AEKey> entry : counter) {
                long count = entry.getLongValue();
                if (count == 0) {
                    continue;
                }
                if (count < 0) {
                    return false;
                }
                AEKey key = entry.getKey();
                if (key instanceof AEItemKey itemKey) {
                    items.merge(itemKey, count, Long::sum);
                } else if (key instanceof AEFluidKey fluidKey) {
                    fluids.merge(fluidKey, count, Long::sum);
                } else {
                    return false;
                }
            }
        }
        return true;
    }
}
