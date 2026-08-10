package dev.rylex.nep.hub;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.junit.jupiter.api.Test;

class HubReturnTest {

    private static final AEItemKey DIAMOND = AEItemKey.of(Items.DIAMOND);
    private static final AEFluidKey WATER = AEFluidKey.of(Fluids.WATER);

    private static final class Network implements MEStorage {

        private final KeyCounter held = new KeyCounter();
        private final long room;

        private Network(long room) {
            this.room = room;
        }

        private long total() {
            long total = 0;
            for (var entry : held) {
                total += entry.getLongValue();
            }
            return total;
        }

        @Override
        public long insert(AEKey what, long amount, Actionable mode, IActionSource source) {
            long accepted = Math.min(amount, room - total());
            if (accepted <= 0) {
                return 0;
            }
            if (mode == Actionable.MODULATE) {
                held.add(what, accepted);
            }
            return accepted;
        }

        @Override
        public long extract(AEKey what, long amount, Actionable mode, IActionSource source) {
            return 0;
        }

        @Override
        public void getAvailableStacks(KeyCounter out) {
            out.addAll(held);
        }

        @Override
        public Component getDescription() {
            return Component.empty();
        }
    }

    private static HubTarget output(ItemStackHandler handler) {
        return new HubTarget(BlockPos.ZERO, HubRole.OUTPUT, handler, null);
    }

    private static HubReturn.Outcome push(List<HubTarget> targets, Network network) {
        return HubReturn.push(targets, network, IActionSource.empty());
    }

    @Test
    void resultsSittingInAnOutputAreHandedToTheNetwork() {
        ItemStackHandler results = new ItemStackHandler(2);
        results.setStackInSlot(0, new ItemStack(Items.DIAMOND, 5));
        results.setStackInSlot(1, new ItemStack(Items.EMERALD, 3));
        Network network = new Network(64);

        HubReturn.Outcome outcome = push(List.of(output(results)), network);

        assertEquals(8, outcome.moved());
        assertFalse(outcome.refused(), "a network with room to spare is not a reason to show an error");
        assertTrue(results.getStackInSlot(0).isEmpty());
        assertTrue(results.getStackInSlot(1).isEmpty());
        assertEquals(5, network.held.get(DIAMOND));
    }

    @Test
    void ingredientsStagedInAnInputAreLeftForTheMachineToEat() {
        ItemStackHandler staged = new ItemStackHandler(1);
        staged.setStackInSlot(0, new ItemStack(Items.DIAMOND, 5));
        Network network = new Network(64);

        HubReturn.Outcome outcome = push(List.of(new HubTarget(BlockPos.ZERO, HubRole.INPUT, staged, null)), network);

        assertEquals(0, outcome.moved(), "taking back the ingredients it just pushed would deadlock every craft");
        assertEquals(5, staged.getStackInSlot(0).getCount());
    }

    @Test
    void aFullNetworkLeavesTheResultsInTheMachineAndSaysSo() {
        ItemStackHandler results = new ItemStackHandler(1);
        results.setStackInSlot(0, new ItemStack(Items.DIAMOND, 5));
        Network network = new Network(0);

        HubReturn.Outcome outcome = push(List.of(output(results)), network);

        assertEquals(0, outcome.moved());
        assertTrue(outcome.refused(), "the screen has nothing to report the jam with unless the push says it happened");
        assertEquals(5, results.getStackInSlot(0).getCount(), "results must never be voided into a full network");
    }

    @Test
    void aNetworkWithRoomForSomeOfItTakesThatMuchAndStillReportsTheJam() {
        ItemStackHandler results = new ItemStackHandler(1);
        results.setStackInSlot(0, new ItemStack(Items.DIAMOND, 5));
        Network network = new Network(2);

        HubReturn.Outcome outcome = push(List.of(output(results)), network);

        assertEquals(2, outcome.moved());
        assertTrue(outcome.refused());
        assertEquals(3, results.getStackInSlot(0).getCount());
    }

    @Test
    void fluidResultsComeBackTheSameWayItemsDo() {
        FluidTank tank = new FluidTank(4_000);
        tank.fill(new FluidStack(Fluids.WATER, 1_500), FluidTank.FluidAction.EXECUTE);
        Network network = new Network(4_000);

        HubReturn.Outcome outcome = push(List.of(new HubTarget(BlockPos.ZERO, HubRole.OUTPUT, null, tank)), network);

        assertEquals(1_500, outcome.moved());
        assertEquals(1_500, network.held.get(WATER));
        assertTrue(tank.getFluid().isEmpty());
    }

    @Test
    void aTankTheNetworkHasNoRoomForKeepsItsFluid() {
        FluidTank tank = new FluidTank(4_000);
        tank.fill(new FluidStack(Fluids.WATER, 1_500), FluidTank.FluidAction.EXECUTE);
        Network network = new Network(0);

        HubReturn.Outcome outcome = push(List.of(new HubTarget(BlockPos.ZERO, HubRole.OUTPUT, null, tank)), network);

        assertEquals(0, outcome.moved());
        assertTrue(outcome.refused());
        assertEquals(1_500, tank.getFluid().getAmount());
    }
}
