package dev.rylex.nep.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import appeng.api.stacks.AEItemKey;
import java.util.List;
import java.util.Map;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import org.junit.jupiter.api.Test;

class ManualPullTest {

    private static final ManualRequirement DIAMONDS = new ManualRequirement(Ingredient.of(Items.DIAMOND), 4, true);
    private static final ManualRequirement TOOL = new ManualRequirement(Ingredient.of(Items.DIAMOND_PICKAXE), 1, false);
    private static final ManualRequirement CARRIED =
            new ManualRequirement(Ingredient.of(Items.DIAMOND_PICKAXE), 1, true, true);

    private static List<ItemStack> inventory(ItemStack... stacks) {
        return List.of(stacks);
    }

    @Test
    void aBatchIsPlannedOnlyWhenEveryIngredientIsCovered() {
        List<ItemStack> held = inventory(new ItemStack(Items.DIAMOND, 4));

        assertNotNull(ManualPull.plan(held, List.of(DIAMONDS), 1, ManualPull.NO_PREFERENCE));
        assertNull(
                ManualPull.plan(held, List.of(DIAMONDS), 2, ManualPull.NO_PREFERENCE),
                "8 diamonds are needed for two batches, 4 are held");
    }

    @Test
    void aRequirementIsDrawnFromAsManySlotsAsItTakes() {
        List<ItemStack> held = inventory(new ItemStack(Items.DIAMOND, 1), new ItemStack(Items.DIAMOND, 3));

        List<ManualPull.Take> takes = ManualPull.plan(held, List.of(DIAMONDS), 1, ManualPull.NO_PREFERENCE);

        assertNotNull(takes);
        assertEquals(2, takes.size());
        assertEquals(4, takes.stream().mapToInt(ManualPull.Take::count).sum());
    }

    @Test
    void anIngredientThatIsNotConsumedIsAskedForOnceHoweverManyBatchesRun() {
        List<ItemStack> held = inventory(new ItemStack(Items.DIAMOND, 12), new ItemStack(Items.DIAMOND_PICKAXE, 1));

        List<ManualPull.Take> takes = ManualPull.plan(held, List.of(DIAMONDS, TOOL), 3, ManualPull.NO_PREFERENCE);

        assertNotNull(takes);
        assertEquals(
                1,
                takes.stream()
                        .filter(take -> take.requirement() == 1)
                        .mapToInt(ManualPull.Take::count)
                        .sum());
    }

    @Test
    void theBatchCountFallsBackToWhatTheInventoryCanCover() {
        List<ItemStack> held = inventory(new ItemStack(Items.DIAMOND, 9));

        assertEquals(2, ManualPull.affordableBatches(held, List.of(DIAMONDS), 8, ManualPull.NO_PREFERENCE));
        assertEquals(0, ManualPull.affordableBatches(inventory(), List.of(DIAMONDS), 8, ManualPull.NO_PREFERENCE));
    }

    @Test
    void onlyConsumedIngredientsReachTheCraftTemplate() {
        List<ItemStack> held = inventory(new ItemStack(Items.DIAMOND, 4), new ItemStack(Items.DIAMOND_PICKAXE, 1));
        List<ManualRequirement> requirements = List.of(DIAMONDS, TOOL);

        List<ManualPull.Take> takes = ManualPull.plan(held, requirements, 1, ManualPull.NO_PREFERENCE);
        assertNotNull(takes);
        Map<AEItemKey, Long> consumed = ManualPull.consumedTotals(held, takes, requirements);

        assertEquals(Map.of(AEItemKey.of(Items.DIAMOND), 4L), consumed);
        assertEquals(2, ManualPull.totals(held, takes).size(), "the tool still has to be moved into the machine");
    }

    @Test
    void theIngredientsTheInventoryCannotCoverAreNamedByIndex() {
        List<ItemStack> held = inventory(new ItemStack(Items.DIAMOND, 4));

        assertEquals(
                List.of(1), ManualPull.unmetRequirements(held, List.of(DIAMONDS, TOOL), 1, ManualPull.NO_PREFERENCE));
        assertEquals(List.of(), ManualPull.unmetRequirements(held, List.of(DIAMONDS), 1, ManualPull.NO_PREFERENCE));
        assertEquals(
                List.of(0, 1),
                ManualPull.unmetRequirements(inventory(), List.of(DIAMONDS, TOOL), 1, ManualPull.NO_PREFERENCE));
    }

    @Test
    void anIngredientShortForOneBatchIsNamedEvenWhenAnotherIsCovered() {
        List<ItemStack> held = inventory(new ItemStack(Items.DIAMOND, 3), new ItemStack(Items.DIAMOND_PICKAXE, 1));

        assertEquals(
                List.of(0), ManualPull.unmetRequirements(held, List.of(DIAMONDS, TOOL), 1, ManualPull.NO_PREFERENCE));
    }

    @Test
    void aSlotSpentOnAnEarlierIngredientIsNotSpentTwice() {
        List<ItemStack> held = inventory(new ItemStack(Items.DIAMOND, 4));
        List<ManualRequirement> twice = List.of(DIAMONDS, DIAMONDS);

        assertNull(ManualPull.plan(held, twice, 1, ManualPull.NO_PREFERENCE));
    }

    @Test
    void aDataCarryingIngredientIsTakenFromTheHeldSlotFirst() {
        List<ItemStack> held =
                inventory(new ItemStack(Items.DIAMOND_PICKAXE, 1), new ItemStack(Items.DIAMOND_PICKAXE, 1));

        List<ManualPull.Take> takes = ManualPull.plan(held, List.of(CARRIED), 1, 1);

        assertNotNull(takes);
        assertEquals(1, takes.size());
        assertEquals(1, takes.get(0).slot(), "the held pickaxe was not the one spent");
    }

    @Test
    void anOrdinaryIngredientIgnoresTheHeldSlot() {
        List<ItemStack> held =
                inventory(new ItemStack(Items.DIAMOND_PICKAXE, 1), new ItemStack(Items.DIAMOND_PICKAXE, 1));

        List<ManualPull.Take> takes = ManualPull.plan(held, List.of(TOOL), 1, 1);

        assertNotNull(takes);
        assertEquals(0, takes.get(0).slot(), "a requirement that carries nothing should take the first match");
    }

    @Test
    void aHeldSlotThatDoesNotMatchFallsBackToInventoryOrder() {
        List<ItemStack> held = inventory(new ItemStack(Items.DIAMOND_PICKAXE, 1), new ItemStack(Items.DIAMOND, 4));

        List<ManualPull.Take> takes = ManualPull.plan(held, List.of(CARRIED), 1, 1);

        assertNotNull(takes);
        assertEquals(0, takes.get(0).slot());
    }
}
