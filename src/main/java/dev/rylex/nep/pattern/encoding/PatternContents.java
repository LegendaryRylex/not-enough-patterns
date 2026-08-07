package dev.rylex.nep.pattern.encoding;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

public final class PatternContents {
    private PatternContents() {}

    private record Bucket(AEKey what, boolean retained) {}

    public static List<GenericStack> condenseInputs(IPatternDetails details) {
        Map<Bucket, Long> amounts = new LinkedHashMap<>();
        for (IPatternDetails.IInput input : details.getInputs()) {
            GenericStack[] possible = input.getPossibleInputs();
            if (possible.length == 0) {
                continue;
            }
            AEKey what = possible[0].what();
            amounts.merge(
                    new Bucket(what, input.getRemainingKey(what) != null),
                    possible[0].amount() * input.getMultiplier(),
                    Long::sum);
        }
        return stacksOf(amounts);
    }

    public static List<GenericStack> condenseSlots(List<@Nullable GenericStack> chosen, Set<Integer> retainedSlots) {
        Map<Bucket, Long> amounts = new LinkedHashMap<>();
        for (int slot = 0; slot < chosen.size(); slot++) {
            GenericStack stack = chosen.get(slot);
            if (stack == null) {
                continue;
            }
            amounts.merge(new Bucket(stack.what(), retainedSlots.contains(slot)), stack.amount(), Long::sum);
        }
        return stacksOf(amounts);
    }

    private static List<GenericStack> stacksOf(Map<Bucket, Long> amounts) {
        List<GenericStack> stacks = new ArrayList<>(amounts.size());
        for (Map.Entry<Bucket, Long> entry : amounts.entrySet()) {
            stacks.add(new GenericStack(entry.getKey().what(), entry.getValue()));
        }
        return List.copyOf(stacks);
    }

    public static List<GenericStack> condense(List<GenericStack> stacks) {
        Map<AEKey, Long> amounts = new LinkedHashMap<>();
        for (GenericStack stack : stacks) {
            amounts.merge(stack.what(), stack.amount(), Long::sum);
        }
        List<GenericStack> condensed = new ArrayList<>(amounts.size());
        for (Map.Entry<AEKey, Long> entry : amounts.entrySet()) {
            condensed.add(new GenericStack(entry.getKey(), entry.getValue()));
        }
        return List.copyOf(condensed);
    }
}
