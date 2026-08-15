package dev.rylex.nep.hub;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.Nullable;

public record HubLink(
        BlockPos pos,
        HubRole role,
        @Nullable Direction face,
        int priority,
        HubFilter insertFilter,
        HubFilter returnFilter) {

    public static final int MINIMUM_PRIORITY = -99;
    public static final int MAXIMUM_PRIORITY = 99;

    public static final Codec<HubLink> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    BlockPos.CODEC.fieldOf("pos").forGetter(HubLink::pos),
                    HubRole.CODEC.optionalFieldOf("role", HubRole.INPUT).forGetter(HubLink::role),
                    Direction.CODEC.optionalFieldOf("face").forGetter(link -> Optional.ofNullable(link.face())),
                    Codec.intRange(MINIMUM_PRIORITY, MAXIMUM_PRIORITY)
                            .optionalFieldOf("priority", 0)
                            .forGetter(HubLink::priority),
                    HubFilter.CODEC
                            .optionalFieldOf("insert_filter", HubFilter.EMPTY)
                            .forGetter(HubLink::insertFilter),
                    HubFilter.CODEC
                            .optionalFieldOf("return_filter", HubFilter.EMPTY)
                            .forGetter(HubLink::returnFilter))
            .apply(
                    instance,
                    (pos, role, face, priority, insertFilter, returnFilter) ->
                            new HubLink(pos, role, face.orElse(null), priority, insertFilter, returnFilter)));

    public static final StreamCodec<RegistryFriendlyByteBuf, HubLink> STREAM_CODEC = StreamCodec.of(
            (buffer, link) -> {
                BlockPos.STREAM_CODEC.encode(buffer, link.pos());
                HubRole.STREAM_CODEC.encode(buffer, link.role());
                buffer.writeByte(link.face() == null ? -1 : link.face().ordinal());
                buffer.writeVarInt(link.priority());
                HubFilter.STREAM_CODEC.encode(buffer, link.insertFilter());
                HubFilter.STREAM_CODEC.encode(buffer, link.returnFilter());
            },
            buffer -> new HubLink(
                    BlockPos.STREAM_CODEC.decode(buffer),
                    HubRole.STREAM_CODEC.decode(buffer),
                    faceByOrdinal(buffer.readByte()),
                    buffer.readVarInt(),
                    HubFilter.STREAM_CODEC.decode(buffer),
                    HubFilter.STREAM_CODEC.decode(buffer)));

    public HubLink(BlockPos pos, HubRole role) {
        this(pos, role, null, 0, HubFilter.EMPTY, HubFilter.EMPTY);
    }

    public HubLink withRole(HubRole role) {
        return new HubLink(pos, role, face, priority, insertFilter, returnFilter);
    }

    public HubLink withFace(@Nullable Direction face) {
        return new HubLink(pos, role, face, priority, insertFilter, returnFilter);
    }

    public HubLink withPriority(int priority) {
        return new HubLink(
                pos,
                role,
                face,
                Math.max(MINIMUM_PRIORITY, Math.min(MAXIMUM_PRIORITY, priority)),
                insertFilter,
                returnFilter);
    }

    public HubLink withInsertFilter(HubFilter filter) {
        return new HubLink(pos, role, face, priority, filter, returnFilter);
    }

    public HubLink withReturnFilter(HubFilter filter) {
        return new HubLink(pos, role, face, priority, insertFilter, filter);
    }

    @Nullable
    public static Direction faceByOrdinal(int ordinal) {
        Direction[] values = Direction.values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : null;
    }
}
