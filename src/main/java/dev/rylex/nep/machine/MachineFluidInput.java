package dev.rylex.nep.machine;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidActionResult;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

public final class MachineFluidInput {

    private MachineFluidInput() {}

    public static boolean canFillFrom(ItemStack stack) {
        return !stack.isEmpty()
                && FluidUtil.getFluidContained(stack)
                        .map(fluid -> !fluid.isEmpty())
                        .orElse(false);
    }

    public static void interactInWorld(Player player, InteractionHand hand, IFluidHandler tank) {
        FluidUtil.interactWithFluidHandler(player, hand, tank);
    }

    public static boolean fillFromCarried(AbstractContainerMenu menu, Player player, IFluidHandler tank) {
        ItemStack carried = menu.getCarried();
        if (!canFillFrom(carried)) {
            return false;
        }
        FluidActionResult result =
                FluidUtil.tryEmptyContainer(carried.copyWithCount(1), tank, Integer.MAX_VALUE, player, true);
        if (!result.isSuccess()) {
            return false;
        }
        ItemStack emptied = result.getResult();
        if (carried.getCount() <= 1) {
            menu.setCarried(emptied);
        } else {
            carried.shrink(1);
            menu.setCarried(carried);
            if (!emptied.isEmpty() && !player.getInventory().add(emptied)) {
                player.drop(emptied, false);
            }
        }
        return true;
    }
}
