package dev.rylex.nep.compat.create;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

enum LinkerMode implements StringRepresentable {
    INPUT("input"),
    OUTPUT("output"),
    MACHINE("machine");

    static final Codec<LinkerMode> CODEC = StringRepresentable.fromEnum(LinkerMode::values);
    static final StreamCodec<ByteBuf, LinkerMode> STREAM_CODEC =
            ByteBufCodecs.idMapper(id -> values()[id], LinkerMode::ordinal);

    private final String serializedName;

    LinkerMode(String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }

    Component displayName() {
        return Component.translatable("chat.nep.linker.mode." + serializedName);
    }

    LinkerMode next() {
        return values()[(ordinal() + 1) % values().length];
    }
}
