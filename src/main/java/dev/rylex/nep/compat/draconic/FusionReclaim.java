package dev.rylex.nep.compat.draconic;

import appeng.api.stacks.GenericStack;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.Direction;

record FusionReclaim(List<GenericStack> items, Direction direction) {

    static final FusionReclaim NONE = new FusionReclaim(List.of(), Direction.UP);

    static final Codec<FusionReclaim> CODEC = RecordCodecBuilder.create(builder -> builder.group(
                    GenericStack.FAULT_TOLERANT_LIST_CODEC.fieldOf("items").forGetter(FusionReclaim::items),
                    Direction.CODEC.fieldOf("direction").forGetter(FusionReclaim::direction))
            .apply(builder, FusionReclaim::new));

    FusionReclaim {
        items = List.copyOf(items);
    }

    boolean isEmpty() {
        return items.isEmpty();
    }
}
