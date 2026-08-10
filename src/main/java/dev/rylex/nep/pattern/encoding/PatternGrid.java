package dev.rylex.nep.pattern.encoding;

import appeng.api.stacks.GenericStack;
import appeng.util.ConfigInventory;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.Nullable;

public record PatternGrid(List<@Nullable GenericStack> inputs, List<@Nullable GenericStack> outputs) {

    public PatternGrid {
        inputs = trim(inputs);
        outputs = trim(outputs);
    }

    public static final Codec<PatternGrid> CODEC = RecordCodecBuilder.create(builder -> builder.group(
                    GenericStack.FAULT_TOLERANT_NULLABLE_LIST_CODEC
                            .optionalFieldOf("inputs", List.of())
                            .forGetter(PatternGrid::inputs),
                    GenericStack.FAULT_TOLERANT_NULLABLE_LIST_CODEC
                            .optionalFieldOf("outputs", List.of())
                            .forGetter(PatternGrid::outputs))
            .apply(builder, PatternGrid::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, PatternGrid> STREAM_CODEC = StreamCodec.composite(
            GenericStack.STREAM_CODEC.apply(ByteBufCodecs.list()),
            PatternGrid::inputs,
            GenericStack.STREAM_CODEC.apply(ByteBufCodecs.list()),
            PatternGrid::outputs,
            PatternGrid::new);

    public static PatternGrid capture(ConfigInventory inputs, ConfigInventory outputs) {
        return new PatternGrid(stacksOf(inputs), stacksOf(outputs));
    }

    public boolean isEmpty() {
        return inputs.isEmpty() && outputs.isEmpty();
    }

    public void restore(ConfigInventory inputs, ConfigInventory outputs) {
        fill(inputs, this.inputs);
        fill(outputs, this.outputs);
    }

    public static void fill(ConfigInventory inv, List<@Nullable GenericStack> stacks) {
        inv.beginBatch();
        try {
            for (int slot = 0; slot < inv.size(); slot++) {
                inv.setStack(slot, slot < stacks.size() ? stacks.get(slot) : null);
            }
        } finally {
            inv.endBatch();
        }
    }

    private static List<@Nullable GenericStack> stacksOf(ConfigInventory inv) {
        List<GenericStack> stacks = new ArrayList<>(inv.size());
        for (int slot = 0; slot < inv.size(); slot++) {
            stacks.add(inv.getStack(slot));
        }
        return stacks;
    }

    private static List<@Nullable GenericStack> trim(List<@Nullable GenericStack> stacks) {
        int size = stacks.size();
        while (size > 0 && stacks.get(size - 1) == null) {
            size--;
        }
        return Collections.unmodifiableList(new ArrayList<>(stacks.subList(0, size)));
    }
}
