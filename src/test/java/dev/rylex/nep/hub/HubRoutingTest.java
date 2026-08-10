package dev.rylex.nep.hub;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import appeng.api.config.Actionable;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

class HubRoutingTest {

    private static final AEItemKey OBSIDIAN = AEItemKey.of(Items.OBSIDIAN);
    private static final AEFluidKey WATER = AEFluidKey.of(Fluids.WATER);

    private static HubTarget input(int slots) {
        return new HubTarget(BlockPos.ZERO, HubRole.INPUT, new ItemStackHandler(slots), null);
    }

    private static HubTarget target(HubRole role, IItemHandler handler) {
        return new HubTarget(BlockPos.ZERO, role, handler, null);
    }

    @Test
    void ingredientsOnlyEnterInventoriesMarkedAsInputs() {
        HubTarget in = input(1);
        HubTarget out = new HubTarget(BlockPos.ZERO, HubRole.OUTPUT, new ItemStackHandler(1), null);

        assertEquals(4, HubRouting.insert(List.of(out, in), OBSIDIAN, 4, Actionable.MODULATE));
        assertTrue(out.items().getStackInSlot(0).isEmpty(), "an output hatch must never be handed ingredients");
        assertEquals(4, in.items().getStackInSlot(0).getCount());
    }

    @Test
    void aSimulatedInsertNeverCountsTheSameSlotTwice() {
        HubTarget in = input(1);

        assertEquals(
                64,
                HubRouting.insert(List.of(in), OBSIDIAN, 128, Actionable.SIMULATE),
                "one slot holds one stack; reporting 128 would have the provider push a pattern the machine cannot"
                        + " take");
        assertTrue(in.items().getStackInSlot(0).isEmpty(), "a simulation must leave the inventory alone");
    }

    @Test
    void oneIngredientSpreadsAcrossEveryLinkedInput() {
        HubTarget first = input(1);
        HubTarget second = input(2);

        assertEquals(
                192,
                HubRouting.insert(List.of(first, second), OBSIDIAN, 200, Actionable.MODULATE),
                "three slots hold three stacks between them; the eight left over stay with the provider");
        assertEquals(64, first.items().getStackInSlot(0).getCount());
        assertEquals(64, second.items().getStackInSlot(0).getCount());
        assertEquals(64, second.items().getStackInSlot(1).getCount());
    }

    @Test
    void whatWillNotFitIsReportedAsRefused() {
        HubTarget in = input(1);

        assertEquals(64, HubRouting.insert(List.of(in), OBSIDIAN, 100, Actionable.MODULATE));
    }

    @Test
    void itemsAndFluidsFindTheirOwnHandlerOnSeparateBlocks() {
        HubTarget items = input(1);
        HubTarget tank = new HubTarget(new BlockPos(1, 0, 0), HubRole.INPUT, null, new FluidTank(4_000));

        assertEquals(2, HubRouting.insert(List.of(items, tank), OBSIDIAN, 2, Actionable.MODULATE));
        assertEquals(1_000, HubRouting.insert(List.of(items, tank), WATER, 1_000, Actionable.MODULATE));

        assertEquals(2, items.items().getStackInSlot(0).getCount());
        assertEquals(1_000, tank.fluids().getFluidInTank(0).getAmount());
    }

    @Test
    void extractionReachesEveryLinkedInventoryWhateverRoleItCarries() {
        ItemStackHandler outputHatch = new ItemStackHandler(1);
        outputHatch.setStackInSlot(0, new ItemStack(Items.OBSIDIAN, 5));
        HubTarget out = target(HubRole.OUTPUT, outputHatch);

        assertEquals(3, HubRouting.extract(List.of(out), OBSIDIAN, 3, Actionable.MODULATE));
        assertEquals(2, outputHatch.getStackInSlot(0).getCount());
    }

    @Test
    void aSimulatedExtractionTakesNothing() {
        ItemStackHandler outputHatch = new ItemStackHandler(1);
        outputHatch.setStackInSlot(0, new ItemStack(Items.OBSIDIAN, 5));

        assertEquals(
                5, HubRouting.extract(List.of(target(HubRole.OUTPUT, outputHatch)), OBSIDIAN, 9, Actionable.SIMULATE));
        assertEquals(5, outputHatch.getStackInSlot(0).getCount());
    }

    @Test
    void everythingLinkedIsReportedSoBlockingModeAndImportCardsCanSeeIt() {
        ItemStackHandler staged = new ItemStackHandler(1);
        staged.setStackInSlot(0, new ItemStack(Items.OBSIDIAN, 7));
        FluidTank tank = new FluidTank(4_000);
        tank.fill(new FluidStack(Fluids.WATER, 500), FluidTank.FluidAction.EXECUTE);

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
        ItemStackHandler resultOnly = new ItemStackHandler(1) {
            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                return false;
            }

            @Override
            public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
                return stack;
            }
        };
        resultOnly.setStackInSlot(0, new ItemStack(Items.DIAMOND, 1));

        assertEquals(HubRole.OUTPUT, HubScan.roleFor(resultOnly));
        assertEquals(HubRole.INPUT, HubScan.roleFor(new ItemStackHandler(1)));
    }

    @Test
    void anInventoryWithNoSlotsAtAllIsProposedAsAnInputRatherThanCrashingTheScan() {
        assertEquals(HubRole.INPUT, HubScan.roleFor(null));
        assertEquals(HubRole.INPUT, HubScan.roleFor(new ItemStackHandler(0)));
    }
}
