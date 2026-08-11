package dev.rylex.nep.hub;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.Nullable;

public enum HubStatus {
    OK(null),
    NO_NETWORK("gui.nep.machine_hub.status.no_network"),
    OFFLINE("gui.nep.machine_hub.status.offline"),
    FULL("gui.nep.machine_hub.status.full");

    public static final StreamCodec<ByteBuf, HubStatus> STREAM_CODEC =
            ByteBufCodecs.idMapper(HubStatus::byOrdinal, HubStatus::ordinal);

    @Nullable
    private final String key;

    HubStatus(@Nullable String key) {
        this.key = key;
    }

    public boolean healthy() {
        return key == null;
    }

    @Nullable
    public Component message() {
        return key == null ? null : Component.translatable(key);
    }

    public static HubStatus byOrdinal(int ordinal) {
        HubStatus[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : OK;
    }
}
