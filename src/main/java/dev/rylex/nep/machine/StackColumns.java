package dev.rylex.nep.machine;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public record StackColumns(List<AEKey> keys, List<Long> counts) {
    public static StackColumns read(ValueInput input, String key) {
        List<AEKey> keys = new ArrayList<>();
        List<Long> counts = new ArrayList<>();
        for (GenericStack stack : input.listOrEmpty(key, GenericStack.CODEC)) {
            keys.add(stack.what());
            counts.add(stack.amount());
        }
        return new StackColumns(List.copyOf(keys), List.copyOf(counts));
    }

    public void write(ValueOutput output, String key) {
        ValueOutput.TypedOutputList<GenericStack> stacks = output.list(key, GenericStack.CODEC);
        for (int i = 0; i < keys.size(); i++) {
            stacks.add(new GenericStack(keys.get(i), counts.get(i)));
        }
    }
}
