package dev.rylex.nep.net;

import dev.rylex.nep.Nep;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

public record PatternRecipePayload(Optional<Identifier> recipe) implements CustomPacketPayload {

    public static final Type<PatternRecipePayload> TYPE = new Type<>(Nep.id("pattern_recipe"));

    public static final StreamCodec<ByteBuf, PatternRecipePayload> STREAM_CODEC = ByteBufCodecs.optional(
                    Identifier.STREAM_CODEC)
            .map(PatternRecipePayload::new, PatternRecipePayload::recipe);

    public static PatternRecipePayload of(@Nullable Identifier recipe) {
        return new PatternRecipePayload(Optional.ofNullable(recipe));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
