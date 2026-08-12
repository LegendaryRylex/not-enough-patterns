package dev.rylex.nep.net;

import dev.rylex.nep.Nep;
import dev.rylex.nep.pattern.encoding.PatternOrigin;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record PatternRecipePayload(PatternOrigin origin) implements CustomPacketPayload {

    public static final Type<PatternRecipePayload> TYPE = new Type<>(Nep.id("pattern_recipe"));

    public static final StreamCodec<ByteBuf, PatternRecipePayload> STREAM_CODEC =
            PatternOrigin.STREAM_CODEC.map(PatternRecipePayload::new, PatternRecipePayload::origin);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
