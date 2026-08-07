package dev.rylex.nep.net;

import dev.rylex.nep.Nep;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record RetainedSlotsPayload(List<Integer> slots) implements CustomPacketPayload {

    public static final Type<RetainedSlotsPayload> TYPE = new Type<>(Nep.id("retained_slots"));

    public static final StreamCodec<ByteBuf, RetainedSlotsPayload> STREAM_CODEC = ByteBufCodecs.VAR_INT
            .apply(ByteBufCodecs.list())
            .map(RetainedSlotsPayload::new, RetainedSlotsPayload::slots);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
