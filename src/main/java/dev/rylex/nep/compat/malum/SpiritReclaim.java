package dev.rylex.nep.compat.malum;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Direction;

record SpiritReclaim(Direction direction) {

    static final SpiritReclaim NONE = new SpiritReclaim(Direction.UP);

    static final Codec<SpiritReclaim> CODEC = RecordCodecBuilder.create(
            builder -> builder.group(Direction.CODEC.fieldOf("direction").forGetter(SpiritReclaim::direction))
                    .apply(builder, SpiritReclaim::new));
}
