package dev.rylex.nep.pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

class EncodedRecipePatternTest {

    private static EncodedRecipePattern pattern() {
        return new EncodedRecipePattern(
                ResourceLocation.parse("nep:test_recipe"),
                List.of(new GenericStack(AEItemKey.of(Items.OBSIDIAN), 2)),
                new GenericStack(AEItemKey.of(Items.DIAMOND), 1));
    }

    @Test
    void roundTripsThroughNbt() {
        Tag encoded = EncodedRecipePattern.CODEC
                .encodeStart(NbtOps.INSTANCE, pattern())
                .getOrThrow();
        EncodedRecipePattern decoded =
                EncodedRecipePattern.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();
        assertEquals(pattern(), decoded);
        assertTrue(!decoded.containsMissingContent());
    }

    @Test
    void aResultFromARemovedModDecodesAsMissingContentInsteadOfFailing() {
        CompoundTag encoded = (CompoundTag) EncodedRecipePattern.CODEC
                .encodeStart(NbtOps.INSTANCE, pattern())
                .getOrThrow();
        encoded.getCompound("result").putString("id", "removedmod:gone");

        EncodedRecipePattern decoded =
                EncodedRecipePattern.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();

        assertTrue(decoded.containsMissingContent(), "an unknown result item must surface as missing content");
    }

    @Test
    void missingContentKeepsTheOriginalIdThroughRepeatedSaveCycles() {
        CompoundTag encoded = (CompoundTag) EncodedRecipePattern.CODEC
                .encodeStart(NbtOps.INSTANCE, pattern())
                .getOrThrow();
        encoded.getCompound("result").putString("id", "removedmod:gone");

        EncodedRecipePattern decoded =
                EncodedRecipePattern.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();
        CompoundTag reEncoded = (CompoundTag)
                EncodedRecipePattern.CODEC.encodeStart(NbtOps.INSTANCE, decoded).getOrThrow();

        assertEquals(
                "removedmod:gone",
                reEncoded.getCompound("result").getString("id"),
                "re-saving a missing-content pattern must keep the original id for when the mod returns");

        EncodedRecipePattern decodedAgain =
                EncodedRecipePattern.CODEC.parse(NbtOps.INSTANCE, reEncoded).getOrThrow();
        assertTrue(decodedAgain.containsMissingContent(), "a second save cycle must not degrade the pattern");
    }
}
