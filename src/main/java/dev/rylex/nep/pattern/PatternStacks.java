package dev.rylex.nep.pattern;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.GenericStack;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;

public final class PatternStacks {
    private PatternStacks() {}

    @Nullable
    public static GenericStack singleResult(IPatternDetails pattern) {
        List<GenericStack> outputs = pattern.getOutputs();
        return outputs.size() == 1 && outputs.get(0).amount() > 0 ? outputs.get(0) : null;
    }

    @Nullable
    public static List<GenericStack> condensedInputs(IPatternDetails pattern) {
        List<GenericStack> inputs = new ArrayList<>();
        for (IPatternDetails.IInput input : pattern.getInputs()) {
            GenericStack[] possible = input.getPossibleInputs();
            if (possible.length == 0) {
                return null;
            }
            long amount = possible[0].amount() * input.getMultiplier();
            if (amount <= 0) {
                return null;
            }
            inputs.add(new GenericStack(possible[0].what(), amount));
        }
        return inputs.isEmpty() ? null : inputs;
    }
}
