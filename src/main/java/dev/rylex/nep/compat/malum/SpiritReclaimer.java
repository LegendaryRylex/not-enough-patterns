package dev.rylex.nep.compat.malum;

import dev.rylex.nep.Nep;
import dev.rylex.nep.NepConfig;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

public final class SpiritReclaimer {
    private SpiritReclaimer() {}

    static void expect(BlockEntity host, Direction direction) {
        host.setData(NepMalumContent.SPIRIT_RECLAIM.get(), new SpiritReclaim(direction));
        host.setChanged();
    }

    public static boolean pending(BlockEntity host) {
        return host.hasData(NepMalumContent.SPIRIT_RECLAIM.get());
    }

    public static void forget(BlockEntity host) {
        if (host.getExistingData(NepMalumContent.SPIRIT_RECLAIM.get()).isEmpty()) {
            return;
        }
        host.removeData(NepMalumContent.SPIRIT_RECLAIM.get());
        host.setChanged();
    }

    /**
     * A machine crafting for a player has no reclaim pending, so its result is returned untouched and drops the way
     * Malum intends.
     */
    public static ItemStack reclaim(BlockEntity host, ItemStack output) {
        ItemStack remaining = reclaimKeeping(host, output);
        forget(host);
        return remaining;
    }

    /**
     * A Spirit Crucible emits its fortune bonuses through the same call as its main output, so forgetting on the first
     * item would drop every extra one.
     */
    public static ItemStack reclaimKeeping(BlockEntity host, ItemStack output) {
        Level level = host.getLevel();
        if (level == null || level.isClientSide() || output.isEmpty()) {
            return output;
        }
        SpiritReclaim pending =
                host.getExistingData(NepMalumContent.SPIRIT_RECLAIM.get()).orElse(null);
        if (pending == null) {
            return output;
        }

        ItemStack remaining = insert(level, host, pending.direction(), output);
        for (Direction side : Direction.values()) {
            if (remaining.isEmpty()) {
                break;
            }
            if (side != pending.direction()) {
                remaining = insert(level, host, side, remaining);
            }
        }
        if (!remaining.isEmpty() && NepConfig.debugLogging()) {
            Nep.LOGGER.info(
                    "{} could not return {} to the pattern provider and dropped it", host.getBlockPos(), remaining);
        }
        return remaining;
    }

    private static ItemStack insert(Level level, BlockEntity host, Direction side, ItemStack stack) {
        IItemHandler target = level.getCapability(
                Capabilities.ItemHandler.BLOCK, host.getBlockPos().relative(side), side.getOpposite());
        return target == null ? stack : ItemHandlerHelper.insertItem(target, stack, false);
    }
}
