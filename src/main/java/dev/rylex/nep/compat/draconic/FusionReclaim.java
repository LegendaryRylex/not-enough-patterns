package dev.rylex.nep.compat.draconic;

import appeng.api.stacks.GenericStack;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.Direction;

record FusionReclaim(
        List<GenericStack> retained, List<GenericStack> loaded, List<GenericStack> catalyst, Direction direction) {

    static final FusionReclaim NONE = new FusionReclaim(List.of(), List.of(), List.of(), Direction.UP);

    static final Codec<FusionReclaim> CODEC = RecordCodecBuilder.create(builder -> builder.group(
                    GenericStack.FAULT_TOLERANT_LIST_CODEC.fieldOf("items").forGetter(FusionReclaim::retained),
                    GenericStack.FAULT_TOLERANT_LIST_CODEC
                            .optionalFieldOf("loaded", List.of())
                            .forGetter(FusionReclaim::loaded),
                    GenericStack.FAULT_TOLERANT_LIST_CODEC
                            .optionalFieldOf("catalyst", List.of())
                            .forGetter(FusionReclaim::catalyst),
                    Direction.CODEC.fieldOf("direction").forGetter(FusionReclaim::direction))
            .apply(builder, FusionReclaim::new));

    FusionReclaim {
        retained = List.copyOf(retained);
        loaded = List.copyOf(loaded);
        catalyst = List.copyOf(catalyst);
    }

    boolean isEmpty() {
        return retained.isEmpty();
    }
}
