package dev.rylex.nep.hub;

import com.mojang.serialization.Codec;
import dev.rylex.nep.NepIcons;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

public enum HubRole implements StringRepresentable {
    INPUT("input", NepIcons.INPUT, true, false),
    OUTPUT("output", NepIcons.OUTPUT, false, true),
    BOTH("both", NepIcons.BOTH, true, true);

    public static final Codec<HubRole> CODEC = StringRepresentable.fromEnum(HubRole::values);
    public static final StreamCodec<ByteBuf, HubRole> STREAM_CODEC =
            ByteBufCodecs.idMapper(HubRole::byOrdinal, HubRole::ordinal);

    private final String serializedName;
    private final Component glyph;
    private final boolean accepts;
    private final boolean provides;

    HubRole(String serializedName, Component glyph, boolean accepts, boolean provides) {
        this.serializedName = serializedName;
        this.glyph = glyph;
        this.accepts = accepts;
        this.provides = provides;
    }

    public boolean accepts() {
        return accepts;
    }

    public boolean provides() {
        return provides;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }

    public Component glyph() {
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
