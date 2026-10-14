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
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

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

    public static int countIn(ResourceHandler<ItemResource> handler, Item item) {
        int total = 0;
        for (int slot = 0; slot < handler.size(); slot++) {
            if (handler.getResource(slot).is(item)) {
                total += handler.getAmountAsInt(slot);
            }
        }
        return total;
    }

    public static boolean holds(ResourceHandler<ItemResource> handler, ItemStack expected, int count) {
        return countIn(handler, expected.getItem()) >= count;
    }

    public static boolean isEmpty(ResourceHandler<ItemResource> handler) {
        for (int slot = 0; slot < handler.size(); slot++) {
            if (!handler.getResource(slot).isEmpty()) {
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

    public static ItemStack firstStack(ResourceHandler<ItemResource> handler) {
        for (int slot = 0; slot < handler.size(); slot++) {
            ItemResource resource = handler.getResource(slot);
            if (!resource.isEmpty()) {
                return resource.toStack(handler.getAmountAsInt(slot));
            }
        }
        return ItemStack.EMPTY;
    }
}
