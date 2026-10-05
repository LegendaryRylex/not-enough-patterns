package dev.rylex.nep.compat.ars;

import dev.rylex.nep.Nep;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SelectRitualPayload(Optional<ResourceLocation> ritual) implements CustomPacketPayload {

    public static final Type<SelectRitualPayload> TYPE = new Type<>(Nep.id("select_ritual"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SelectRitualPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC),
            SelectRitualPayload::ritual,
            SelectRitualPayload::new);

    @Override
    public Type<SelectRitualPayload> type() {
        return TYPE;
    }
}
