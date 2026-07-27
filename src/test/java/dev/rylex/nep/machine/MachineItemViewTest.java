package dev.rylex.nep.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.junit.jupiter.api.Test;

class MachineItemViewTest {

    private final ItemStackHandler input = new ItemStackHandler(2);
    private final ItemStackHandler output = new ItemStackHandler(1);

    @Test
    void inputSlotsComeFirstAndResultSlotsFollow() {
        MachineItemView view = MachineItemView.feedable(input, output);
        output.setStackInSlot(0, new ItemStack(Items.DIAMOND, 3));

        assertEquals(3, view.getSlots());
        assertEquals(3, view.getStackInSlot(2).getCount());
        assertTrue(view.getStackInSlot(0).isEmpty());
    }

    @Test
    void resultSlotsRefuseInsertsAndInputSlotsRefuseExtraction() {
        MachineItemView view = MachineItemView.feedable(input, output);
        input.setStackInSlot(0, new ItemStack(Items.OBSIDIAN, 8));

        assertEquals(
                4, view.insertItem(2, new ItemStack(Items.DIAMOND, 4), false).getCount());
        assertTrue(view.extractItem(0, 8, false).isEmpty());
        assertEquals(8, input.getStackInSlot(0).getCount());
    }

    @Test
    void resultSlotsGiveItemsUp() {
        MachineItemView view = MachineItemView.feedable(input, output);
        output.setStackInSlot(0, new ItemStack(Items.DIAMOND, 5));

        assertEquals(2, view.extractItem(2, 2, false).getCount());
        assertEquals(3, output.getStackInSlot(0).getCount());
    }

    @Test
    void aControllerWithoutResultSlotsExposesOnlyItsInput() {
        MachineItemView view = MachineItemView.feedable(input, null);
        assertEquals(2, view.getSlots());
    }

    @Test
    void demandLimitsHowMuchAnInsertMayStage() {
        MachineItemView view = MachineItemView.demandLimited(input, output, stack -> 3);

        ItemStack refused = view.insertItem(0, new ItemStack(Items.OBSIDIAN, 8), false);

        assertEquals(5, refused.getCount(), "only the demanded 3 may enter; 5 must come back");
        assertEquals(3, input.getStackInSlot(0).getCount());
    }

    @Test
    void partialInsertReturnsTheRefusedRemainderOnTopOfTheDemandCut() {
        input.setStackInSlot(0, new ItemStack(Items.OBSIDIAN, 63));
        MachineItemView view = MachineItemView.demandLimited(input, output, stack -> 4);

        ItemStack refused = view.insertItem(0, new ItemStack(Items.OBSIDIAN, 8), false);

        assertEquals(64, input.getStackInSlot(0).getCount(), "one item fits before the slot cap");
        assertEquals(7, refused.getCount(), "4 over demand plus 3 refused by the slot");
    }

    @Test
    void zeroDemandBlocksInsertsAndValidity() {
        MachineItemView view = MachineItemView.demandLimited(input, output, stack -> 0);

        assertEquals(
                8, view.insertItem(0, new ItemStack(Items.OBSIDIAN, 8), false).getCount());
        assertFalse(view.isItemValid(0, new ItemStack(Items.OBSIDIAN)));
        assertTrue(input.getStackInSlot(0).isEmpty());
    }

    @Test
    void simulatedInsertsChangeNothing() {
        MachineItemView view = MachineItemView.demandLimited(input, output, stack -> 3);

        ItemStack refused = view.insertItem(0, new ItemStack(Items.OBSIDIAN, 8), true);

        assertEquals(5, refused.getCount());
        assertTrue(input.getStackInSlot(0).isEmpty());
    }
}
