package dev.rylex.nep.pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import java.util.Arrays;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

class EncodedMechanicalPatternTest {

    private static EncodedMechanicalPattern pattern() {
        GenericStack iron = new GenericStack(AEItemKey.of(Items.IRON_INGOT), 1);
        return new EncodedMechanicalPattern(
                2, 2, Arrays.asList(iron, null, null, iron), new GenericStack(AEItemKey.of(Items.DIAMOND), 1));
    }

    @Test
    void roundTripsThroughNbtWithNullCellsPreserved() {
        CompoundTag encoded = (CompoundTag) EncodedMechanicalPattern.CODEC
                .encodeStart(NbtOps.INSTANCE, pattern())
                .getOrThrow();
        EncodedMechanicalPattern decoded =
                EncodedMechanicalPattern.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();

        assertEquals(pattern(), decoded);
        assertNull(decoded.cells().get(1), "empty grid cells must stay empty through a save cycle");
        assertNull(decoded.cells().get(2));
    }

    @Test
    void aResultFromARemovedModDecodesAsMissingContent() {
        CompoundTag encoded = (CompoundTag) EncodedMechanicalPattern.CODEC
                .encodeStart(NbtOps.INSTANCE, pattern())
                .getOrThrow();
        encoded.getCompound("result").putString("id", "removedmod:gone");

        EncodedMechanicalPattern decoded =
                EncodedMechanicalPattern.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();

        assertTrue(decoded.containsMissingContent());
    }

    @Test
    void decodingGuardsRejectMalformedPatterns() {
        GenericStack iron = new GenericStack(AEItemKey.of(Items.IRON_INGOT), 1);
        GenericStack doubled = new GenericStack(AEItemKey.of(Items.IRON_INGOT), 2);
        GenericStack result = new GenericStack(AEItemKey.of(Items.DIAMOND), 1);
        GenericStack noResult = new GenericStack(AEItemKey.of(Items.DIAMOND), 0);

        assertThrows(
                IllegalArgumentException.class,
                () -> decode(new EncodedMechanicalPattern(2, 2, Arrays.asList(iron, null, null), result)));
        assertThrows(
                IllegalArgumentException.class,
                () -> decode(new EncodedMechanicalPattern(1, 1, List.of(iron), noResult)));
        assertThrows(
                IllegalArgumentException.class,
                () -> decode(new EncodedMechanicalPattern(1, 1, List.of(doubled), result)));
        assertThrows(
                IllegalArgumentException.class,
                () -> decode(new EncodedMechanicalPattern(1, 1, Arrays.asList((GenericStack) null), result)));
    }

    private static void decode(EncodedMechanicalPattern encoded) {
        net.minecraft.world.item.ItemStack stack =
                new net.minecraft.world.item.ItemStack(dev.rylex.nep.NepItems.MECHANICAL_CRAFTING_PATTERN.get());
        stack.set(dev.rylex.nep.NepComponents.ENCODED_MECHANICAL_PATTERN.get(), encoded);
        new MechanicalCraftingPattern(AEItemKey.of(stack), null);
    }
}
