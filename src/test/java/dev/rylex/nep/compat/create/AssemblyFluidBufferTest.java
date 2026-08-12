package dev.rylex.nep.compat.create;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import org.junit.jupiter.api.Test;

class AssemblyFluidBufferTest {

    private static final int CAPACITY = 64_000;

    private final AtomicInteger capacity = new AtomicInteger(CAPACITY);
    private final AtomicInteger changes = new AtomicInteger();
    private final AssemblyFluidBuffer buffer = new AssemblyFluidBuffer(capacity::get, changes::incrementAndGet);

    private static FluidStack stack(Fluid fluid, int amount) {
        return new FluidStack(fluid, amount);
    }

    private static FluidStack tagged(Fluid fluid, int amount) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("nepTest", true);
        FluidStack stack = new FluidStack(fluid, amount);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    private int occupiedTanks() {
        int used = 0;
        for (int tank = 0; tank < AssemblyFluidBuffer.TANKS; tank++) {
            if (!buffer.getFluidInTank(tank).isEmpty()) {
                used++;
            }
        }
        return used;
    }

    @Test
    void startsEmptyWithFourTanks() {
        assertEquals(4, buffer.getTanks());
        assertTrue(buffer.isEmpty());
        assertEquals(0, occupiedTanks());
    }

    @Test
    void oneFluidClaimsExactlyOneTankNoMatterHowOftenItIsFilled() {
        buffer.fill(stack(Fluids.WATER, 500), FluidAction.EXECUTE);
        buffer.fill(stack(Fluids.WATER, 500), FluidAction.EXECUTE);
        buffer.fill(stack(Fluids.WATER, 1_000), FluidAction.EXECUTE);

        assertEquals(1, occupiedTanks());
        assertEquals(2_000, buffer.getFluidInTank(0).getAmount());
    }

    @Test
    void distinctFluidsNeverShareATank() {
        buffer.fill(stack(Fluids.WATER, 500), FluidAction.EXECUTE);
        buffer.fill(stack(Fluids.LAVA, 500), FluidAction.EXECUTE);

        assertEquals(2, occupiedTanks());
        assertSame(Fluids.WATER, buffer.getFluidInTank(0).getFluid());
        assertSame(Fluids.LAVA, buffer.getFluidInTank(1).getFluid());
    }

    @Test
    void aFifthDistinctFluidIsRejectedRatherThanSplittingAnExistingTank() {
        assertEquals(10, buffer.fill(stack(Fluids.WATER, 10), FluidAction.EXECUTE));
        assertEquals(10, buffer.fill(stack(Fluids.LAVA, 10), FluidAction.EXECUTE));
        assertEquals(10, buffer.fill(stack(Fluids.FLOWING_WATER, 10), FluidAction.EXECUTE));
        assertEquals(10, buffer.fill(stack(Fluids.FLOWING_LAVA, 10), FluidAction.EXECUTE));
        assertEquals(0, buffer.fill(tagged(Fluids.WATER, 10), FluidAction.EXECUTE));
        assertEquals(4, occupiedTanks());
        assertEquals(10, buffer.getFluidInTank(0).getAmount(), "the rejected fluid must not join another tank");
    }

    @Test
    void saveAndLoadRoundTripsEveryTank() {
        buffer.fill(stack(Fluids.WATER, 1_000), FluidAction.EXECUTE);
        buffer.fill(tagged(Fluids.WATER, 250), FluidAction.EXECUTE);

        AssemblyFluidBuffer restored = new AssemblyFluidBuffer(capacity::get, () -> {});
        restored.load(registries(), buffer.save(registries()));

        for (int tank = 0; tank < AssemblyFluidBuffer.TANKS; tank++) {
            FluidStack expected = buffer.getFluidInTank(tank);
            FluidStack actual = restored.getFluidInTank(tank);
            assertEquals(expected.getAmount(), actual.getAmount(), "tank " + tank + " amount");
            assertTrue(
                    FluidStack.isSameFluidSameComponents(expected, actual),
                    "tank " + tank + " fluid identity survived the round trip");
        }
    }

    @Test
    void loadIgnoresOutOfRangeTankIndices() {
        buffer.fill(stack(Fluids.WATER, 500), FluidAction.EXECUTE);
        ListTag saved = buffer.save(registries());
        CompoundTag rogue = new CompoundTag();
        rogue.putInt("Tank", 9);
        rogue.put("Fluid", stack(Fluids.LAVA, 100).save(registries()));
        saved.add(rogue);

        AssemblyFluidBuffer restored = new AssemblyFluidBuffer(capacity::get, () -> {});
        restored.load(registries(), saved);

        assertEquals(500, restored.getFluidInTank(0).getAmount());
    }

    private static HolderLookup.Provider registries() {
        return RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    }

    @Test
    void canFillAllAccountsForEarlierEntriesAgainstTheSharedCapacity() {
        buffer.fill(stack(Fluids.WATER, CAPACITY - 4_000), FluidAction.EXECUTE);

        assertTrue(buffer.canFillAll(List.of(stack(Fluids.WATER, 3_000))));
        assertFalse(
                buffer.canFillAll(List.of(stack(Fluids.WATER, 3_000), stack(Fluids.WATER, 3_000))),
                "each entry fits alone but not together; admitting the pair would void the overflow");
    }

    @Test
    void canFillAllRejectsTwoNewFluidsCompetingForTheLastFreeTank() {
        buffer.fill(stack(Fluids.WATER, 10), FluidAction.EXECUTE);
        buffer.fill(stack(Fluids.LAVA, 10), FluidAction.EXECUTE);
        buffer.fill(stack(Fluids.FLOWING_WATER, 10), FluidAction.EXECUTE);

        assertTrue(buffer.canFillAll(List.of(stack(Fluids.FLOWING_LAVA, 10))));
        assertFalse(
                buffer.canFillAll(List.of(stack(Fluids.FLOWING_LAVA, 10), tagged(Fluids.WATER, 10))),
                "both simulate fine against the one free tank, but only one can claim it");
    }

    @Test
    void canFillAllIgnoresEmptyEntriesAndAcceptsAnEmptyList() {
        assertTrue(buffer.canFillAll(List.of()));
        assertTrue(buffer.canFillAll(List.of(FluidStack.EMPTY)));
        assertTrue(buffer.isEmpty());
    }

    @Test
    void fillIsCappedAtTheConfiguredCapacityAndReportsWhatItTook() {
        assertEquals(CAPACITY, buffer.fill(stack(Fluids.WATER, CAPACITY + 5_000), FluidAction.EXECUTE));
        assertEquals(CAPACITY, buffer.getFluidInTank(0).getAmount());
        assertEquals(0, buffer.fill(stack(Fluids.WATER, 1), FluidAction.EXECUTE));
    }

    @Test
    void simulatedFillChangesNothing() {
        assertEquals(500, buffer.fill(stack(Fluids.WATER, 500), FluidAction.SIMULATE));
        assertTrue(buffer.isEmpty());
        assertEquals(0, changes.get());
    }

    @Test
    void simulatedDrainChangesNothing() {
        buffer.fill(stack(Fluids.WATER, 500), FluidAction.EXECUTE);
        int afterFill = changes.get();

        assertEquals(
                200,
                buffer.drain(stack(Fluids.WATER, 200), FluidAction.SIMULATE).getAmount());
        assertEquals(500, buffer.getFluidInTank(0).getAmount());
        assertEquals(afterFill, changes.get());
    }

    @Test
    void drainingByFluidOnlyTakesThatFluid() {
        buffer.fill(stack(Fluids.WATER, 1_000), FluidAction.EXECUTE);
        buffer.fill(stack(Fluids.LAVA, 1_000), FluidAction.EXECUTE);

        FluidStack drained = buffer.drain(stack(Fluids.LAVA, 400), FluidAction.EXECUTE);

        assertSame(Fluids.LAVA, drained.getFluid());
        assertEquals(400, drained.getAmount());
        assertEquals(1_000, buffer.getFluidInTank(0).getAmount());
        assertEquals(600, buffer.getFluidInTank(1).getAmount());
    }

    @Test
    void drainingAFluidThatIsNotHeldReturnsEmpty() {
        buffer.fill(stack(Fluids.WATER, 1_000), FluidAction.EXECUTE);
        assertTrue(buffer.drain(stack(Fluids.LAVA, 100), FluidAction.EXECUTE).isEmpty());
        assertEquals(1_000, buffer.getFluidInTank(0).getAmount());
    }

    @Test
    void drainingEmptiesTheTankSoAnotherFluidCanClaimIt() {
        buffer.fill(stack(Fluids.WATER, 500), FluidAction.EXECUTE);
        buffer.drain(stack(Fluids.WATER, 500), FluidAction.EXECUTE);

        assertTrue(buffer.getFluidInTank(0).isEmpty());
        assertTrue(buffer.isEmpty());

        buffer.fill(stack(Fluids.LAVA, 250), FluidAction.EXECUTE);
        assertSame(Fluids.LAVA, buffer.getFluidInTank(0).getFluid());
    }

    @Test
    void drainingMoreThanIsHeldTakesOnlyWhatIsThere() {
        buffer.fill(stack(Fluids.WATER, 300), FluidAction.EXECUTE);
        assertEquals(
                300,
                buffer.drain(stack(Fluids.WATER, 5_000), FluidAction.EXECUTE).getAmount());
        assertTrue(buffer.isEmpty());
    }

    @Test
    void amountOnlyDrainTakesFromTheFirstOccupiedTank() {
        buffer.fill(stack(Fluids.WATER, 100), FluidAction.EXECUTE);
        buffer.fill(stack(Fluids.LAVA, 100), FluidAction.EXECUTE);
        buffer.drain(stack(Fluids.WATER, 100), FluidAction.EXECUTE);

        FluidStack drained = buffer.drain(50, FluidAction.EXECUTE);

        assertSame(Fluids.LAVA, drained.getFluid());
        assertEquals(50, drained.getAmount());
    }

    @Test
    void nonPositiveAndEmptyRequestsAreNoOps() {
        buffer.fill(stack(Fluids.WATER, 100), FluidAction.EXECUTE);

        assertEquals(0, buffer.fill(FluidStack.EMPTY, FluidAction.EXECUTE));
        assertTrue(buffer.drain(FluidStack.EMPTY, FluidAction.EXECUTE).isEmpty());
        assertTrue(buffer.drain(0, FluidAction.EXECUTE).isEmpty());
        assertTrue(buffer.drain(-5, FluidAction.EXECUTE).isEmpty());
        assertEquals(100, buffer.getFluidInTank(0).getAmount());
    }

    @Test
    void loweringTheConfiguredCapacityLeavesTheTankOverfilledUntilItIsTrimmed() {
        buffer.fill(stack(Fluids.WATER, 10_000), FluidAction.EXECUTE);
        assertFalse(buffer.overCapacity());

        capacity.set(4_000);
        assertTrue(buffer.overCapacity());
    }

    @Test
    void trimReturnsTheOverflowToTheSinkAndKeepsWhatFits() {
        buffer.fill(stack(Fluids.WATER, 10_000), FluidAction.EXECUTE);
        capacity.set(4_000);

        AtomicInteger dumped = new AtomicInteger();
        assertTrue(buffer.trimToCapacity(overflow -> {
            dumped.addAndGet(overflow.getAmount());
            return overflow.getAmount();
        }));

        assertEquals(6_000, dumped.get());
        assertEquals(4_000, buffer.getFluidInTank(0).getAmount());
        assertFalse(buffer.overCapacity());
    }

    @Test
    void trimKeepsTheFluidWhenTheSinkRefusesIt() {
        buffer.fill(stack(Fluids.WATER, 10_000), FluidAction.EXECUTE);
        capacity.set(4_000);

        assertFalse(buffer.trimToCapacity(overflow -> 0));

        assertEquals(10_000, buffer.getFluidInTank(0).getAmount());
        assertTrue(buffer.overCapacity(), "an unaccepted overflow must stay staged, not vanish");
    }

    @Test
    void trimAcceptsAPartialDumpAndLeavesTheRemainder() {
        buffer.fill(stack(Fluids.WATER, 10_000), FluidAction.EXECUTE);
        capacity.set(4_000);

        assertTrue(buffer.trimToCapacity(overflow -> 1_000));

        assertEquals(9_000, buffer.getFluidInTank(0).getAmount());
        assertTrue(buffer.overCapacity());
    }

    @Test
    void trimIgnoresTanksThatAreWithinCapacity() {
        buffer.fill(stack(Fluids.WATER, 1_000), FluidAction.EXECUTE);
        assertFalse(buffer.trimToCapacity(overflow -> overflow.getAmount()));
        assertEquals(1_000, buffer.getFluidInTank(0).getAmount());
    }

    @Test
    void takeFromEmptiesTheTankWhenItIsDrainedDry() {
        buffer.fill(stack(Fluids.WATER, 500), FluidAction.EXECUTE);
        buffer.takeFrom(0, 500);
        assertTrue(buffer.getFluidInTank(0).isEmpty());
        assertTrue(buffer.isEmpty());
    }

    @Test
    void clearEmptiesEveryTank() {
        buffer.fill(stack(Fluids.WATER, 500), FluidAction.EXECUTE);
        buffer.fill(stack(Fluids.LAVA, 500), FluidAction.EXECUTE);

        buffer.clear();

        assertTrue(buffer.isEmpty());
        assertEquals(0, occupiedTanks());
    }

    @Test
    void everyMutationNotifiesTheOwnerSoTheBlockEntitySaves() {
        buffer.fill(stack(Fluids.WATER, 500), FluidAction.EXECUTE);
        assertEquals(1, changes.get());

        buffer.drain(stack(Fluids.WATER, 100), FluidAction.EXECUTE);
        assertEquals(2, changes.get());

        buffer.takeFrom(0, 100);
        assertEquals(3, changes.get());

        buffer.clear();
        assertEquals(4, changes.get());
    }
}
