package dev.rylex.nep.hub;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import appeng.api.config.Actionable;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;
import dev.rylex.nep.MinecraftBootstrap;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MinecraftBootstrap.class)
class HubRoutingTest {

    private static final AEItemKey OBSIDIAN = AEItemKey.of(Items.OBSIDIAN);
    private static final AEFluidKey WATER = AEFluidKey.of(Fluids.WATER);

    private static void put(ItemStacksResourceHandler handler, int slot, ItemStack stack) {
        handler.set(slot, ItemResource.of(stack), stack.getCount());
    }

    private static HubTarget input(int slots) {
        return new HubTarget(BlockPos.ZERO, HubRole.INPUT, new ItemStacksResourceHandler(slots), null);
    }

    private static HubTarget target(HubRole role, ResourceHandler<ItemResource> handler) {
        return new HubTarget(BlockPos.ZERO, role, handler, null);
    }

    private static FluidStacksResourceHandler tank(int capacity) {
        return new FluidStacksResourceHandler(1, capacity);
    }

    @Test
    void ingredientsOnlyEnterInventoriesMarkedAsInputs() {
        HubTarget in = input(1);
        HubTarget out = new HubTarget(BlockPos.ZERO, HubRole.OUTPUT, new ItemStacksResourceHandler(1), null);

        assertEquals(4, HubRouting.insert(List.of(out, in), OBSIDIAN, 4, Actionable.MODULATE));
        assertTrue(out.items().getResource(0).isEmpty(), "an output hatch must never be handed ingredients");
        assertEquals(4, in.items().getAmountAsInt(0));
    }

    @Test
    void aSimulatedInsertNeverCountsTheSameSlotTwice() {
        HubTarget in = input(1);

        assertEquals(
                64,
                HubRouting.insert(List.of(in), OBSIDIAN, 128, Actionable.SIMULATE),
                "one slot holds one stack; reporting 128 would have the provider push a pattern the machine cannot"
                        + " take");
        assertTrue(in.items().getResource(0).isEmpty(), "a simulation must leave the inventory alone");
    }

    @Test
    void oneIngredientSpreadsAcrossEveryLinkedInput() {
        HubTarget first = input(1);
        HubTarget second = input(2);

        assertEquals(
                192,
                HubRouting.insert(List.of(first, second), OBSIDIAN, 200, Actionable.MODULATE),
                "three slots hold three stacks between them; the eight left over stay with the provider");
        assertEquals(64, first.items().getAmountAsInt(0));
        assertEquals(64, second.items().getAmountAsInt(0));
        assertEquals(64, second.items().getAmountAsInt(1));
    }

    @Test
    void whatWillNotFitIsReportedAsRefused() {
        HubTarget in = input(1);

        assertEquals(64, HubRouting.insert(List.of(in), OBSIDIAN, 100, Actionable.MODULATE));
    }

    @Test
    void itemsAndFluidsFindTheirOwnHandlerOnSeparateBlocks() {
        HubTarget items = input(1);
        HubTarget tank = new HubTarget(new BlockPos(1, 0, 0), HubRole.INPUT, null, tank(4_000));

        assertEquals(2, HubRouting.insert(List.of(items, tank), OBSIDIAN, 2, Actionable.MODULATE));
        assertEquals(1_000, HubRouting.insert(List.of(items, tank), WATER, 1_000, Actionable.MODULATE));

        assertEquals(2, items.items().getAmountAsInt(0));
        assertEquals(1_000, tank.fluids().getAmountAsInt(0));
    }

    @Test
    void extractionReachesInventoriesThatProvide() {
        ItemStacksResourceHandler outputHatch = new ItemStacksResourceHandler(1);
        put(outputHatch, 0, new ItemStack(Items.OBSIDIAN, 5));
        HubTarget out = target(HubRole.OUTPUT, outputHatch);

        assertEquals(3, HubRouting.extract(List.of(out), OBSIDIAN, 3, Actionable.MODULATE));
        assertEquals(2, outputHatch.getAmountAsInt(0));
    }

    @Test
    void extractionNeverTakesIngredientsBackOutOfAnInput() {
        ItemStacksResourceHandler staged = new ItemStacksResourceHandler(1);
        put(staged, 0, new ItemStack(Items.OBSIDIAN, 5));

        assertEquals(
                0,
                HubRouting.extract(List.of(target(HubRole.INPUT, staged)), OBSIDIAN, 5, Actionable.MODULATE),
                "pulling an ingredient back out of a machine mid-craft is what the role gate exists to stop");
        assertEquals(5, staged.getAmountAsInt(0));
    }

    @Test
    void aCombinedInventoryMarkedBothTakesIngredientsAndGivesResultsBack() {
        ItemStacksResourceHandler combined = new ItemStacksResourceHandler(2);
        put(combined, 1, new ItemStack(Items.DIAMOND, 3));
        HubTarget both = target(HubRole.BOTH, combined);

        assertEquals(4, HubRouting.insert(List.of(both), OBSIDIAN, 4, Actionable.MODULATE));
        assertEquals(3, HubRouting.extract(List.of(both), AEItemKey.of(Items.DIAMOND), 3, Actionable.MODULATE));
    }

    @Test
    void anInsertFilterKeepsAnIngredientOutOfTheWrongMachine() {
        HubTarget filtered = new HubTarget(
                new HubLink(BlockPos.ZERO, HubRole.INPUT)
                        .withInsertFilter(HubFilter.EMPTY.withKey(0, AEItemKey.of(Items.DIAMOND))),
                new ItemStacksResourceHandler(1),
                null);
        HubTarget open = input(1);

        assertEquals(4, HubRouting.insert(List.of(filtered, open), OBSIDIAN, 4, Actionable.MODULATE));
        assertTrue(filtered.items().getResource(0).isEmpty(), "obsidian is not on the whitelist");
        assertEquals(4, open.items().getAmountAsInt(0));
    }

    @Test
    void aDenyFilterRefusesOnlyWhatItNames() {
        HubFilter deny = HubFilter.EMPTY.withKey(0, OBSIDIAN).toggled();
        HubTarget filtered = new HubTarget(
                new HubLink(BlockPos.ZERO, HubRole.INPUT).withInsertFilter(deny),
                new ItemStacksResourceHandler(2),
                null);

        assertEquals(0, HubRouting.insert(List.of(filtered), OBSIDIAN, 4, Actionable.MODULATE));
        assertEquals(4, HubRouting.insert(List.of(filtered), AEItemKey.of(Items.DIAMOND), 4, Actionable.MODULATE));
    }

    @Test
    void anEmptyFilterAllowsEverything() {
        assertTrue(HubFilter.EMPTY.permits(OBSIDIAN));
        assertTrue(HubFilter.EMPTY.toggled().permits(OBSIDIAN), "a deny list naming nothing still denies nothing");
    }

    @Test
    void higherPriorityIsFilledFirstWhateverOrderTheLinksAreIn() {
        HubTarget low = input(1);
        HubTarget high = new HubTarget(
                new HubLink(new BlockPos(1, 0, 0), HubRole.INPUT).withPriority(5),
                new ItemStacksResourceHandler(1),
                null);

        assertEquals(64, HubRouting.insert(List.of(low, high), OBSIDIAN, 64, Actionable.MODULATE));
        assertEquals(64, high.items().getAmountAsInt(0));
        assertTrue(low.items().getResource(0).isEmpty(), "the lower-priority link only sees the overflow");
    }

    @Test
    void aSimulatedExtractionTakesNothing() {
        ItemStacksResourceHandler outputHatch = new ItemStacksResourceHandler(1);
        put(outputHatch, 0, new ItemStack(Items.OBSIDIAN, 5));

        assertEquals(
                5, HubRouting.extract(List.of(target(HubRole.OUTPUT, outputHatch)), OBSIDIAN, 9, Actionable.SIMULATE));
        assertEquals(5, outputHatch.getAmountAsInt(0));
    }

    @Test
    void everythingLinkedIsReportedWhateverItsRoleSoBlockingModeCanSeeIt() {
        ItemStacksResourceHandler staged = new ItemStacksResourceHandler(1);
        put(staged, 0, new ItemStack(Items.OBSIDIAN, 7));
        FluidStacksResourceHandler tank = tank(4_000);
        tank.set(0, FluidResource.of(new FluidStack(Fluids.WATER, 500)), 500);

        KeyCounter counter = new KeyCounter();
        HubRouting.collect(
                List.of(
                        target(HubRole.INPUT, staged),
                        new HubTarget(new BlockPos(1, 0, 0), HubRole.OUTPUT, null, tank)),
                counter);

        assertEquals(7, counter.get(OBSIDIAN));
        assertEquals(500, counter.get(WATER));
    }

    @Test
    void anInventoryThatRefusesEverythingItAlreadyHoldsIsProposedAsAnOutput() {
        ItemStacksResourceHandler resultOnly = new ItemStacksResourceHandler(1) {
            @Override
            public boolean isValid(int index, ItemResource resource) {
                return false;
            }

            @Override
            public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
                return 0;
            }
        };
        put(resultOnly, 0, new ItemStack(Items.DIAMOND, 1));

        assertEquals(HubRole.OUTPUT, HubScan.roleFor(resultOnly));
        assertEquals(HubRole.INPUT, HubScan.roleFor(new ItemStacksResourceHandler(1)));
    }

    @Test
    void anInventoryWithNoSlotsAtAllIsProposedAsAnInputRatherThanCrashingTheScan() {
        assertEquals(HubRole.INPUT, HubScan.roleFor(null));
        assertEquals(HubRole.INPUT, HubScan.roleFor(new ItemStacksResourceHandler(0)));
    }
}
