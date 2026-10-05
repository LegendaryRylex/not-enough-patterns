package dev.rylex.nep.pattern.encoding;

import appeng.api.stacks.GenericStack;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public record EncodedIngredients(
        List<List<GenericStack>> inputs,
        List<GenericStack> outputs,
        Set<Integer> retainedSlots,
        Set<Integer> wornSlots) {

    public EncodedIngredients {
        inputs = inputs.stream().map(List::copyOf).toList();
        outputs = List.copyOf(outputs);
        retainedSlots = Set.copyOf(retainedSlots);
        wornSlots = Set.copyOf(wornSlots);
    }

    public EncodedIngredients(List<List<GenericStack>> inputs, List<GenericStack> outputs) {
        this(inputs, outputs, Set.of(), Set.of());
    }

    public EncodedIngredients(List<List<GenericStack>> inputs, List<GenericStack> outputs, Set<Integer> retainedSlots) {
        this(inputs, outputs, retainedSlots, Set.of());
    }

    public boolean isRetained(int slot) {
        return retainedSlots.contains(slot);
    }

    public boolean isWorn(int slot) {
        return wornSlots.contains(slot);
    }

    /**
     * Every slot the network gets back, whether unchanged or one use worse.
     */
    public Set<Integer> returnedSlots() {
        Set<Integer> returned = new LinkedHashSet<>(retainedSlots);
        returned.addAll(wornSlots);
        return Set.copyOf(returned);
    }
}
