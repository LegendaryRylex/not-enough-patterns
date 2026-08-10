package dev.rylex.nep.hub;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

public enum HubRole implements StringRepresentable {
    INPUT("input", "→"),
    OUTPUT("output", "←");

    public static final Codec<HubRole> CODEC = StringRepresentable.fromEnum(HubRole::values);
    public static final StreamCodec<ByteBuf, HubRole> STREAM_CODEC =
            ByteBufCodecs.idMapper(HubRole::byOrdinal, HubRole::ordinal);

    private final String serializedName;
    private final String glyph;

    HubRole(String serializedName, String glyph) {
        this.serializedName = serializedName;
        this.glyph = glyph;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }

    public String glyph() {
        return glyph;
    }

    public Component displayName() {
        return Component.translatable("gui.nep.machine_hub.role." + serializedName);
    }

    public HubRole next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public static HubRole byOrdinal(int ordinal) {
        HubRole[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : INPUT;
    }

    public static HubRole byName(String name) {
        for (HubRole role : values()) {
            if (role.serializedName.equals(name)) {
                return role;
            }
        }
        return INPUT;
    }
}
