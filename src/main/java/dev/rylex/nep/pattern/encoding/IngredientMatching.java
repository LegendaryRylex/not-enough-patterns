package dev.rylex.nep.pattern.encoding;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.Nullable;

public final class IngredientMatching {
    private IngredientMatching() {}

    private static final int ASSIGN_BUDGET = 1 << 16;

    public static boolean satisfies(EncodedIngredients expected, List<GenericStack> actualInputs) {
        return assign(expected.inputs(), actualInputs) != null;
    }

    public static GenericStack @Nullable [] matchAssign(
            @Nullable EncodedIngredients expected, @Nullable List<GenericStack> inputs, @Nullable GenericStack result) {
        if (expected == null
                || inputs == null
                || result == null
                || expected.outputs().size() != 1
                || !expected.outputs().get(0).equals(result)) {
            return null;
        }
        return assign(expected.inputs(), inputs);
    }

    public static GenericStack @Nullable [] assign(List<List<GenericStack>> slots, List<GenericStack> actualInputs) {
        Map<AEKey, Long> remaining = new LinkedHashMap<>();
        for (GenericStack stack : actualInputs) {
            remaining.merge(stack.what(), stack.amount(), Long::sum);
        }

        Integer[] order = new Integer[slots.size()];
        for (int i = 0; i < order.length; i++) {
            order[i] = i;
        }
        java.util.Arrays.sort(order, java.util.Comparator.comparingInt(i -> matchingOptions(remaining, slots.get(i))));

        GenericStack[] chosen = new GenericStack[slots.size()];
        return search(remaining, slots, order, 0, chosen, new int[] {ASSIGN_BUDGET}) ? chosen : null;
    }

    private static boolean search(
            Map<AEKey, Long> remaining,
            List<List<GenericStack>> slots,
            Integer[] order,
            int index,
            GenericStack[] chosen,
            int[] budget) {
        if (index == order.length) {
            return remaining.isEmpty();
        }
        if (--budget[0] < 0) {
            return false;
        }
        int slot = order[index];
        for (GenericStack option : slots.get(slot)) {
            Long held = remaining.get(option.what());
            if (held == null || held < option.amount()) {
                continue;
            }
            long left = held - option.amount();
            if (left == 0) {
                remaining.remove(option.what());
            } else {
                remaining.put(option.what(), left);
            }
            chosen[slot] = option;
            if (search(remaining, slots, order, index + 1, chosen, budget)) {
                return true;
            }
            chosen[slot] = null;
            remaining.put(option.what(), held);
        }
        return false;
    }

    private static int matchingOptions(Map<AEKey, Long> remaining, List<GenericStack> options) {
        int matches = 0;
        for (GenericStack option : options) {
            if (remaining.containsKey(option.what())) {
                matches++;
            }
        }
        return matches;
    }

    public static List<GenericStack> options(List<AEItemKey> keys, long amount) {
        List<GenericStack> options = new ArrayList<>(keys.size());
        for (AEItemKey key : keys) {
            options.add(new GenericStack(key, amount));
        }
        return List.copyOf(options);
    }

    public static List<AEItemKey> itemOptions(Ingredient ingredient) {
        List<AEItemKey> keys = new ArrayList<>();
        for (ItemStack stack : ingredient.getItems()) {
            AEItemKey key = AEItemKey.of(stack);
            if (key != null) {
                keys.add(key);
            }
        }
        return List.copyOf(keys);
    }

    @Nullable
    public static GenericStack resultOf(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        AEItemKey key = AEItemKey.of(stack);
        return key == null ? null : new GenericStack(key, Math.max(1, stack.getCount()));
    }

    @Nullable
    public static Map<AEItemKey, Long> flattenItemInputs(IPatternDetails pattern) {
        Map<AEItemKey, Long> available = new LinkedHashMap<>();
        for (IPatternDetails.IInput input : pattern.getInputs()) {
            GenericStack primary = input.getPossibleInputs()[0];
            if (!(primary.what() instanceof AEItemKey itemKey)) {
                return null;
            }
            available.merge(itemKey, primary.amount() * input.getMultiplier(), Long::sum);
        }
        return available.isEmpty() ? null : available;
    }
}
