package dev.rylex.nep.compat.create;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import io.netty.buffer.Unpooled;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import org.junit.jupiter.api.Test;

class SequencedAssemblyStateTest {

    private static SequencedAssemblyState sample() {
        return new SequencedAssemblyState(
                1,
                true,
                false,
                List.of(new SequencedAssemblyState.Station(
                        StationKind.DEPLOYER,
                        new BlockPos(1, 2, 3),
                        SequencedAssemblyState.Issue.UNPOWERED,
                        StationKind.UNKNOWN)),
                List.of(new SequencedAssemblyState.Making(new ItemStack(Items.DIAMOND), 4)),
                List.of(new GenericStack(AEItemKey.of(Items.OBSIDIAN), 2)),
                List.of(new FluidStack(Fluids.WATER, 500)),
                2);
    }

    private static SequencedAssemblyState roundTrip(SequencedAssemblyState state) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(
                Unpooled.buffer(), RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
        SequencedAssemblyState.STREAM_CODEC.encode(buf, state);
        return SequencedAssemblyState.STREAM_CODEC.decode(buf);
    }

    @Test
    void encodeAndDecodeAreSymmetric() {
        SequencedAssemblyState state = sample();
        SequencedAssemblyState decoded = roundTrip(state);

        assertEquals(state.status(), decoded.status());
        assertEquals(state.halted(), decoded.halted());
        assertEquals(state.outputBlocked(), decoded.outputBlocked());
        assertEquals(state.stations(), decoded.stations());
        assertEquals(state.missing(), decoded.missing());
        assertEquals(state.redstoneMode(), decoded.redstoneMode());
        assertTrue(state.matches(decoded), "a decoded state must compare equal to what was sent");
    }

    @Test
    void matchesSpotsEveryFieldChange() {
        SequencedAssemblyState state = sample();
        assertTrue(state.matches(sample()));

        assertFalse(state.matches(SequencedAssemblyState.empty()));
        assertFalse(state.matches(new SequencedAssemblyState(
                state.status(),
                state.halted(),
                state.outputBlocked(),
                state.stations(),
                List.of(new SequencedAssemblyState.Making(new ItemStack(Items.DIAMOND), 5)),
                state.missing(),
                state.fluids(),
                state.redstoneMode())));
        assertFalse(state.matches(new SequencedAssemblyState(
                state.status(),
                state.halted(),
                state.outputBlocked(),
                state.stations(),
                state.making(),
                state.missing(),
                List.of(new FluidStack(Fluids.WATER, 501)),
                state.redstoneMode())));
    }
}
