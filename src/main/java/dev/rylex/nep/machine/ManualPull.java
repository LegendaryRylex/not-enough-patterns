package dev.rylex.nep.machine;

import appeng.api.stacks.AEItemKey;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class ManualPull {
    private ManualPull() {}

    public static final int MAXIMUM_BATCHES = 64;

    public static final int NO_PREFERENCE = -1;

    public record Take(int slot, int count, int requirement) {}

    public static List<ItemStack> snapshot(List<ItemStack> inventory) {
        List<ItemStack> copy = new ArrayList<>(inventory.size());
        for (ItemStack stack : inventory) {
            copy.add(stack.copy());
        }
        return List.copyOf(copy);
    }

    @Nullable
    public static List<Take> plan(
            List<ItemStack> inventory, List<ManualRequirement> requirements, int batches, int preferredSlot) {
        return draw(inventory, requirements, batches, preferredSlot, null);
    }

    public static List<Integer> unmetRequirements(
            List<ItemStack> inventory, List<ManualRequirement> requirements, int batches, int preferredSlot) {
        List<Integer> unmet = new ArrayList<>();
        draw(inventory, requirements, batches, preferredSlot, unmet);
        return List.copyOf(unmet);
    }

    @Nullable
    private static List<Take> draw(
            List<ItemStack> inventory,
            List<ManualRequirement> requirements,
            int batches,
            int preferredSlot,
            @Nullable List<Integer> unmet) {
        if (batches <= 0 || requirements.isEmpty()) {
            return null;
        }
        int[] left = new int[inventory.size()];
        for (int slot = 0; slot < inventory.size(); slot++) {
            left[slot] = inventory.get(slot).getCount();
        }
        List<Take> takes = new ArrayList<>();
        for (int index = 0; index < requirements.size(); index++) {
            ManualRequirement requirement = requirements.get(index);
            int need = requirement.consume() ? requirement.count() * batches : requirement.count();
            for (int slot : slotOrder(left.length, requirement, preferredSlot)) {
                if (need <= 0) {
                    break;
                }
                if (left[slot] <= 0 || !requirement.ingredient().test(inventory.get(slot))) {
                    continue;
                }
                int take = Math.min(need, left[slot]);
                left[slot] -= take;
                need -= take;
                takes.add(new Take(slot, take, index));
            }
            if (need > 0) {
                if (unmet == null) {
                    return null;
                }
                unmet.add(index);
            }
        }
        return unmet == null || unmet.isEmpty() ? List.copyOf(takes) : null;
    }

    public static int affordableBatches(
            List<ItemStack> inventory, List<ManualRequirement> requirements, int wanted, int preferredSlot) {
        for (int batches = Math.min(wanted, MAXIMUM_BATCHES); batches > 0; batches--) {
            if (plan(inventory, requirements, batches, preferredSlot) != null) {
                return batches;
            }
        }
        return 0;
    }

    private static int[] slotOrder(int slots, ManualRequirement requirement, int preferredSlot) {
        if (!requirement.carriesData() || preferredSlot < 0 || preferredSlot >= slots) {
            int[] order = new int[slots];
            for (int slot = 0; slot < slots; slot++) {
                order[slot] = slot;
            }
            return order;
        }
        int[] order = new int[slots];
        order[0] = preferredSlot;
        int next = 1;
        for (int slot = 0; slot < slots; slot++) {
            if (slot != preferredSlot) {
                order[next++] = slot;
            }
        }
        return order;
    }

    public static Map<AEItemKey, Long> totals(List<ItemStack> inventory, List<Take> takes) {
        Map<AEItemKey, Long> totals = new LinkedHashMap<>();
        for (Take take : takes) {
            AEItemKey key = AEItemKey.of(inventory.get(take.slot()));
            if (key != null) {
                totals.merge(key, (long) take.count(), Long::sum);
            }
        }
        return totals;
    }

    public static Map<AEItemKey, Long> consumedTotals(
            List<ItemStack> inventory, List<Take> takes, List<ManualRequirement> requirements) {
        List<Take> consumed = new ArrayList<>(takes.size());
        for (Take take : takes) {
            if (requirements.get(take.requirement()).consume()) {
                consumed.add(take);
            }
        }
        return totals(inventory, consumed);
    }
}
