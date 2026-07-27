package dev.rylex.nep.pattern.encoding;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PatternContents {
    private PatternContents() {}

    public static List<GenericStack> condenseInputs(IPatternDetails details) {
        List<GenericStack> stacks = new ArrayList<>();
        for (Map.Entry<AEKey, Long> entry : inputAmounts(details).entrySet()) {
            stacks.add(new GenericStack(entry.getKey(), entry.getValue()));
        }
        return stacks;
    }

    private static Map<AEKey, Long> inputAmounts(IPatternDetails details) {
        Map<AEKey, Long> amounts = new LinkedHashMap<>();
        for (IPatternDetails.IInput input : details.getInputs()) {
            GenericStack[] possible = input.getPossibleInputs();
            if (possible.length == 0) {
                continue;
            }
            amounts.merge(possible[0].what(), possible[0].amount() * input.getMultiplier(), Long::sum);
        }
        return amounts;
    }
}
