package dev.rylex.nep.compat.occultism;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;

public record BoundSpirit(Tier tier, Work work) {

    public static final Codec<BoundSpirit> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Tier.CODEC.fieldOf("tier").forGetter(BoundSpirit::tier),
                    Work.CODEC.fieldOf("work").forGetter(BoundSpirit::work))
            .apply(instance, BoundSpirit::new));

    public static final StreamCodec<ByteBuf, BoundSpirit> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.idMapper(
                    ByIdMap.continuous(Tier::ordinal, Tier.values(), ByIdMap.OutOfBoundsStrategy.ZERO), Tier::ordinal),
            BoundSpirit::tier,
            ByteBufCodecs.idMapper(
                    ByIdMap.continuous(Work::ordinal, Work.values(), ByIdMap.OutOfBoundsStrategy.ZERO), Work::ordinal),
            BoundSpirit::work,
            BoundSpirit::new);

    public enum Tier implements StringRepresentable {
        FOLIOT,
        DJINNI,
        AFRIT,
        MARID;

        public static final Codec<Tier> CODEC = StringRepresentable.fromEnum(Tier::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        public int level() {
            return ordinal() + 1;
        }

        public Component displayName() {
            return Component.translatable("gui.nep.spirit_tier." + getSerializedName());
        }
    }

    public enum Work implements StringRepresentable {
        CRUSHER,
        CRYSTALLIZER;

        public static final Codec<Work> CODEC = StringRepresentable.fromEnum(Work::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        public Component displayName() {
            return Component.translatable("gui.nep.spirit_work." + getSerializedName());
        }
    }
}
