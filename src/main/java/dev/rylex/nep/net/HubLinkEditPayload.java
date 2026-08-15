package dev.rylex.nep.net;

import appeng.api.stacks.AEKey;
import dev.rylex.nep.Nep;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.Nullable;

public record HubLinkEditPayload(
        int index, Action action, int value, @Nullable AEKey key) implements CustomPacketPayload {

    public static final Type<HubLinkEditPayload> TYPE = new Type<>(Nep.id("hub_link_edit"));

    public enum Action {
        SET_FACE,
        SET_PRIORITY,
        SET_INSERT_FILTER,
        SET_RETURN_FILTER,
        TOGGLE_INSERT_MODE,
        TOGGLE_RETURN_MODE;

        static Action byOrdinal(int ordinal) {
            Action[] values = values();
            return ordinal >= 0 && ordinal < values.length ? values[ordinal] : SET_PRIORITY;
        }
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, HubLinkEditPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeVarInt(payload.index());
                buffer.writeByte(payload.action().ordinal());
                buffer.writeVarInt(payload.value());
                AEKey.OPTIONAL_STREAM_CODEC.encode(buffer, payload.key());
            },
            buffer -> new HubLinkEditPayload(
                    buffer.readVarInt(),
                    Action.byOrdinal(buffer.readByte()),
                    buffer.readVarInt(),
                    AEKey.OPTIONAL_STREAM_CODEC.decode(buffer)));

    @Override
    public Type<HubLinkEditPayload> type() {
        return TYPE;
    }
}
