package dev.rylex.nep.pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import dev.rylex.nep.MinecraftBootstrap;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MinecraftBootstrap.class)
class EncodedRecipePatternTest {

    private static EncodedRecipePattern pattern() {
        return new EncodedRecipePattern(
                Identifier.parse("nep:test_recipe"),
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
        encoded.getCompoundOrEmpty("result").putString("id", "removedmod:gone");

        EncodedRecipePattern decoded =
                EncodedRecipePattern.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();

        assertTrue(decoded.containsMissingContent(), "an unknown result item must surface as missing content");
    }

    @Test
    void missingContentKeepsTheOriginalIdThroughRepeatedSaveCycles() {
        CompoundTag encoded = (CompoundTag) EncodedRecipePattern.CODEC
                .encodeStart(NbtOps.INSTANCE, pattern())
                .getOrThrow();
        encoded.getCompoundOrEmpty("result").putString("id", "removedmod:gone");

        EncodedRecipePattern decoded =
                EncodedRecipePattern.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();
        CompoundTag reEncoded = (CompoundTag)
                EncodedRecipePattern.CODEC.encodeStart(NbtOps.INSTANCE, decoded).getOrThrow();

        assertEquals(
                "removedmod:gone",
                reEncoded.getCompoundOrEmpty("result").getStringOr("id", ""),
                "re-saving a missing-content pattern must keep the original id for when the mod returns");

        EncodedRecipePattern decodedAgain =
                EncodedRecipePattern.CODEC.parse(NbtOps.INSTANCE, reEncoded).getOrThrow();
        assertTrue(decodedAgain.containsMissingContent(), "a second save cycle must not degrade the pattern");
    }
}
