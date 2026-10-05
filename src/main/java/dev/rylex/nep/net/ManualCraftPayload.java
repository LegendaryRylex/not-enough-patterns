package dev.rylex.nep.net;

import dev.rylex.nep.Nep;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ManualCraftPayload(BlockPos pos, ResourceLocation recipe, int batches) implements CustomPacketPayload {

    public static final Type<ManualCraftPayload> TYPE = new Type<>(Nep.id("manual_craft"));

    public static final StreamCodec<ByteBuf, ManualCraftPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            ManualCraftPayload::pos,
            ResourceLocation.STREAM_CODEC,
            ManualCraftPayload::recipe,
            ByteBufCodecs.VAR_INT,
            ManualCraftPayload::batches,
            ManualCraftPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
