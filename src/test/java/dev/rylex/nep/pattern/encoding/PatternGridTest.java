package dev.rylex.nep.pattern.encoding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import dev.rylex.nep.MinecraftBootstrap;
import java.util.Arrays;
import java.util.List;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MinecraftBootstrap.class)
class PatternGridTest {

    private static GenericStack stack(Item item, long amount) {
        return new GenericStack(AEItemKey.of(item), amount);
    }

    private static PatternGrid grid() {
        return new PatternGrid(
                Arrays.asList(stack(Items.OBSIDIAN, 1), null, null, null, stack(Items.REDSTONE, 8)),
                List.of(stack(Items.DIAMOND, 1)));
    }

    @Test
    void roundTripsThroughNbt() {
        Tag encoded = PatternGrid.CODEC.encodeStart(NbtOps.INSTANCE, grid()).getOrThrow();
        PatternGrid decoded = PatternGrid.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();

        assertEquals(grid(), decoded);
        assertNull(decoded.inputs().get(1), "an empty slot between two ingredients must survive the round trip");
    }

    @Test
    void trailingEmptySlotsAreNotStored() {
        PatternGrid padded = new PatternGrid(
                Arrays.asList(stack(Items.OBSIDIAN, 1), null, null), Arrays.asList(stack(Items.DIAMOND, 1), null));

        assertEquals(1, padded.inputs().size(), "a grid of 36 mostly empty slots must not be written out in full");
        assertEquals(1, padded.outputs().size());
        assertEquals(stack(Items.OBSIDIAN, 1), padded.inputs().get(0));
    }

    @Test
    void anEmptyGridIsRecognisedSoItIsNeverAttachedToAPattern() {
        assertTrue(new PatternGrid(Arrays.asList(null, null), List.of()).isEmpty());
        assertTrue(!grid().isEmpty());
    }
}
