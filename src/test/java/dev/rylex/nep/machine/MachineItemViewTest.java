package dev.rylex.nep.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.rylex.nep.MinecraftBootstrap;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MinecraftBootstrap.class)
class MachineItemViewTest {

    private static final ItemResource DIAMOND = ItemResource.of(Items.DIAMOND);
    private static final ItemResource OBSIDIAN = ItemResource.of(Items.OBSIDIAN);

    private final ItemStacksResourceHandler input = new ItemStacksResourceHandler(2);
    private final ItemStacksResourceHandler output = new ItemStacksResourceHandler(1);

    private static int insert(MachineItemView view, int index, ItemResource resource, int amount) {
        try (Transaction tx = Transaction.openRoot()) {
            int moved = view.insert(index, resource, amount, tx);
            tx.commit();
            return moved;
        }
    }

    private static int extract(MachineItemView view, int index, ItemResource resource, int amount) {
        try (Transaction tx = Transaction.openRoot()) {
            int moved = view.extract(index, resource, amount, tx);
            tx.commit();
            return moved;
        }
    }

    @Test
    void inputSlotsComeFirstAndResultSlotsFollow() {
        MachineItemView view = MachineItemView.feedable(input, output);
        output.set(0, DIAMOND, 3);

        assertEquals(3, view.size());
        assertEquals(3, view.getAmountAsLong(2));
        assertEquals(0, view.getAmountAsLong(0));
    }

    @Test
    void resultSlotsRefuseInsertsAndInputSlotsRefuseExtraction() {
        MachineItemView view = MachineItemView.feedable(input, output);
        input.set(0, OBSIDIAN, 8);

        assertEquals(0, insert(view, 2, DIAMOND, 4));
        assertEquals(0, extract(view, 0, OBSIDIAN, 8));
        assertEquals(8, input.getAmountAsLong(0));
    }

    @Test
    void resultSlotsGiveItemsUp() {
        MachineItemView view = MachineItemView.feedable(input, output);
        output.set(0, DIAMOND, 5);

        assertEquals(2, extract(view, 2, DIAMOND, 2));
        assertEquals(3, output.getAmountAsLong(0));
    }

    @Test
    void aControllerWithoutResultSlotsExposesOnlyItsInput() {
        MachineItemView view = MachineItemView.feedable(input, null);
        assertEquals(2, view.size());
    }

    @Test
    void demandLimitsHowMuchAnInsertMayStage() {
        MachineItemView view = MachineItemView.demandLimited(input, output, resource -> 3);

        assertEquals(3, insert(view, 0, OBSIDIAN, 8), "only the demanded 3 may enter");
        assertEquals(3, input.getAmountAsLong(0));
    }

    @Test
    void partialInsertReturnsTheRefusedRemainderOnTopOfTheDemandCut() {
        input.set(0, OBSIDIAN, 63);
        MachineItemView view = MachineItemView.demandLimited(input, output, resource -> 4);

        assertEquals(1, insert(view, 0, OBSIDIAN, 8), "one item fits before the slot cap");
        assertEquals(64, input.getAmountAsLong(0));
    }

    @Test
    void zeroDemandBlocksInsertsAndValidity() {
        MachineItemView view = MachineItemView.demandLimited(input, output, resource -> 0);

        assertEquals(0, insert(view, 0, OBSIDIAN, 8));
        assertFalse(view.isValid(0, OBSIDIAN));
        assertEquals(0, input.getAmountAsLong(0));
    }

    @Test
    void abortedInsertsChangeNothing() {
        MachineItemView view = MachineItemView.demandLimited(input, output, resource -> 3);

        try (Transaction tx = Transaction.openRoot()) {
            assertEquals(3, view.insert(0, OBSIDIAN, 8, tx));
        }

        assertEquals(0, input.getAmountAsLong(0));
        assertTrue(input.getResource(0).isEmpty());
    }
}
