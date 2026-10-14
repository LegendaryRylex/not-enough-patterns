package dev.rylex.nep;

import appeng.api.AECapabilities;
import appeng.api.crafting.IPatternDetails;
import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.stacks.KeyCounter;
import appeng.core.definitions.AEBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

public final class GameTestFixtures {
    private GameTestFixtures() {}

    public static KeyCounter[] inputsOf(IPatternDetails details) {
        KeyCounter[] inputs = new KeyCounter[details.getInputs().length];
        for (int slot = 0; slot < inputs.length; slot++) {
            inputs[slot] = new KeyCounter();
            IPatternDetails.IInput input = details.getInputs()[slot];
            inputs[slot].add(input.getPossibleInputs()[0].what(), input.getMultiplier());
        }
        return inputs;
    }

    public static int countIn(IItemHandler handler, Item item) {
        int total = 0;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    public static boolean holds(IItemHandler handler, ItemStack expected, int count) {
        int found = 0;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (ItemStack.isSameItem(stack, expected)) {
                found += stack.getCount();
            }
        }
        return found >= count;
    }

    public static boolean isEmpty(IItemHandler handler) {
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            if (!handler.getStackInSlot(slot).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public static void powerUp(GameTestHelper helper, BlockPos controller, BlockPos energyCell) {
        helper.setBlock(controller, AEBlocks.CONTROLLER.block());
        helper.setBlock(energyCell, AEBlocks.CREATIVE_ENERGY_CELL.block().defaultBlockState());
    }

    public static ICraftingMachine craftingMachine(GameTestHelper helper, BlockPos pos, String failure) {
        ICraftingMachine machine =
                helper.getLevel().getCapability(AECapabilities.CRAFTING_MACHINE, helper.absolutePos(pos), null);
        helper.assertTrue(machine != null, failure);
        return machine;
    }

    public static ItemStack firstStack(IItemHandler handler) {
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }
}
