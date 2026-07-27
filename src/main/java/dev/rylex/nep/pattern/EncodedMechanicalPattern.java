package dev.rylex.nep.pattern;

import appeng.api.stacks.GenericStack;
import appeng.core.definitions.AEItems;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.Nullable;

public record EncodedMechanicalPattern(int width, int height, List<@Nullable GenericStack> cells, GenericStack result) {

    public EncodedMechanicalPattern {
        cells = Collections.unmodifiableList(new ArrayList<>(cells));
    }

    public static final Codec<EncodedMechanicalPattern> CODEC = RecordCodecBuilder.create(builder -> builder.group(
                    Codec.INT.fieldOf("width").forGetter(EncodedMechanicalPattern::width),
                    Codec.INT.fieldOf("height").forGetter(EncodedMechanicalPattern::height),
                    GenericStack.FAULT_TOLERANT_NULLABLE_LIST_CODEC
                            .fieldOf("cells")
                            .forGetter(EncodedMechanicalPattern::cells),
                    GenericStackCodecs.FAULT_TOLERANT.fieldOf("result").forGetter(EncodedMechanicalPattern::result))
            .apply(builder, EncodedMechanicalPattern::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, EncodedMechanicalPattern> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    EncodedMechanicalPattern::width,
                    ByteBufCodecs.VAR_INT,
                    EncodedMechanicalPattern::height,
                    GenericStack.STREAM_CODEC.apply(ByteBufCodecs.list()),
                    EncodedMechanicalPattern::cells,
                    GenericStack.STREAM_CODEC,
                    EncodedMechanicalPattern::result,
                    EncodedMechanicalPattern::new);

    public boolean containsMissingContent() {
        if (AEItems.MISSING_CONTENT.is(result.what())) {
            return true;
        }
        for (GenericStack cell : cells) {
            if (cell != null && AEItems.MISSING_CONTENT.is(cell.what())) {
                return true;
            }
        }
        return false;
    }
}
