package dev.rylex.nep.hub;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record HubLink(BlockPos pos, HubRole role) {

    public static final Codec<HubLink> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    BlockPos.CODEC.fieldOf("pos").forGetter(HubLink::pos),
                    HubRole.CODEC.optionalFieldOf("role", HubRole.INPUT).forGetter(HubLink::role))
            .apply(instance, HubLink::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, HubLink> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, HubLink::pos, HubRole.STREAM_CODEC, HubLink::role, HubLink::new);

    public HubLink withRole(HubRole role) {
        return new HubLink(pos, role);
    }
}
