package dev.rylex.nep.pattern.encoding;

import appeng.api.stacks.GenericStack;
import java.util.List;
import java.util.Set;

public record EncodedIngredients(
        List<List<GenericStack>> inputs, List<GenericStack> outputs, Set<Integer> retainedSlots) {

    public EncodedIngredients {
        inputs = inputs.stream().map(List::copyOf).toList();
        outputs = List.copyOf(outputs);
        retainedSlots = Set.copyOf(retainedSlots);
    }

    public EncodedIngredients(List<List<GenericStack>> inputs, List<GenericStack> outputs) {
        this(inputs, outputs, Set.of());
    }

    public boolean isRetained(int slot) {
        return retainedSlots.contains(slot);
    }
}
