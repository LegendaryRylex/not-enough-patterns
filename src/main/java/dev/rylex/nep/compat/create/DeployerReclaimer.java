package dev.rylex.nep.compat.create;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import com.simibubi.create.content.kinetics.deployer.DeployerBlockEntity;
import dev.rylex.nep.Nep;
import dev.rylex.nep.NepConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

public final class DeployerReclaimer {
    private DeployerReclaimer() {}

    static void expect(DeployerBlockEntity deployer, AEItemKey tool, BlockPos provider, Direction face) {
        deployer.setData(
                NepCreateContent.DEPLOYER_RECLAIM.get(),
                new DeployerReclaim(new GenericStack(tool, 1), provider, face));
        deployer.setChanged();
    }

    static void forget(DeployerBlockEntity deployer) {
        deployer.removeData(NepCreateContent.DEPLOYER_RECLAIM.get());
        deployer.setChanged();
    }

    public static void onCrafted(DeployerBlockEntity deployer) {
        Level level = deployer.getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }
        DeployerReclaim pending = deployer.getExistingData(NepCreateContent.DEPLOYER_RECLAIM.get())
                .orElse(null);
        if (pending == null || pending.isEmpty() || !(pending.tool().what() instanceof AEItemKey key)) {
            return;
        }
        forget(deployer);

        ItemStack recovered = take(deployer, key, (int) pending.tool().amount());
        if (recovered.isEmpty()) {
            return;
        }

        IItemHandler target = providerHandler(level, pending.provider(), pending.face());
        ItemStack leftover = target == null ? recovered : ItemHandlerHelper.insertItem(target, recovered, false);
        if (leftover.isEmpty()) {
            return;
        }

        BlockPos pos = deployer.getBlockPos();
        ItemStack dropped = leftover.copy();
        Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, leftover);
        if (NepConfig.debugLogging()) {
            Nep.LOGGER.info("Deployer {} could not return {} to the pattern provider and dropped it", pos, dropped);
        }
    }

    private static ItemStack take(DeployerBlockEntity deployer, AEItemKey tool, int amount) {
        IItemHandler handler = DepotMachines.itemHandler(deployer);
        if (handler == null || handler.getSlots() == 0) {
            return ItemStack.EMPTY;
        }
        int heldSlot = handler.getSlots() - 1;
        if (!tool.matches(handler.getStackInSlot(heldSlot))) {
            return ItemStack.EMPTY;
        }
        ItemStack taken = handler.extractItem(heldSlot, amount, false);
        if (taken.isEmpty() && NepConfig.debugLogging()) {
            Nep.LOGGER.info(
                    "Deployer {} refused to give back {}; a filter is holding the tool in place",
                    deployer.getBlockPos(),
                    tool);
        }
        return taken;
    }

    @Nullable
    static IItemHandler providerHandler(Level level, BlockPos provider, Direction face) {
        return level.getCapability(Capabilities.ItemHandler.BLOCK, provider, face);
    }
}
