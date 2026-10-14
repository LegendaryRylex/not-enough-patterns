package dev.rylex.nep.compat.malum;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.compat.malum.FocusedSpiritMatrixBlockEntity.RestockPlan;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import org.jetbrains.annotations.Nullable;

final class MatrixRestock {
    private MatrixRestock() {}

    /** Spirits are named by the bank's fixed layout rather than by what it holds, so a slot drained to zero still restocks. */
    @Nullable
    static RestockPlan plan(
            Map<AEItemKey, Long> owed,
            Map<AEItemKey, MatrixTemplate> templates,
            Predicate<AEItemKey> isManualJob,
            boolean spiritRestock) {
        boolean pullIngredients = NepConfig.malumFocusedSpiritMatrixAutoRequest() && !owed.isEmpty();
        if (!pullIngredients && !spiritRestock) {
            return null;
        }
        List<AEKey> keys = new ArrayList<>();
        List<Long> targets = new ArrayList<>();
        if (pullIngredients) {
            for (Map.Entry<AEItemKey, Long> entry : owed.entrySet()) {
                MatrixTemplate template = templates.get(entry.getKey());
                if (template == null || isManualJob.test(entry.getKey())) {
                    continue;
                }
                for (int i = 0; i < template.keys().size(); i++) {
                    long target = template.counts().get(i) * entry.getValue();
                    int existing = keys.indexOf(template.keys().get(i));
                    if (existing >= 0) {
                        targets.set(existing, targets.get(existing) + target);
                    } else {
                        keys.add(template.keys().get(i));
                        targets.add(target);
                    }
                }
            }
        }
        int ingredientKeys = keys.size();
        Map<AEKey, Long> needs = new HashMap<>();
        for (int i = 0; i < ingredientKeys; i++) {
            needs.put(keys.get(i), targets.get(i));
        }
        if (spiritRestock) {
            long stock = NepConfig.malumFocusedSpiritMatrixSpiritStock();
            for (int slot = 0; slot < SpiritBank.SLOTS; slot++) {
                if (slot == SpiritBank.JOKER_SLOT) {
                    continue;
                }
                AEItemKey key = AEItemKey.of(SpiritBank.spiritFor(slot));
                int existing = keys.indexOf(key);
                if (existing >= 0) {
                    targets.set(existing, Math.max(targets.get(existing), stock));
                } else {
                    keys.add(key);
                    targets.add(stock);
                }
            }
        }
        if (keys.isEmpty()) {
            return null;
        }
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < keys.size(); i++) {
            order.add(i);
        }
        order.sort(Comparator.comparingInt((Integer i) -> i < ingredientKeys ? 0 : 1)
                .thenComparing(i -> keys.get(i).toString()));
        List<AEKey> stableKeys = new ArrayList<>(keys.size());
        List<Long> stableTargets = new ArrayList<>(targets.size());
        for (int i : order) {
            stableKeys.add(keys.get(i));
            stableTargets.add(targets.get(i));
        }
        return new RestockPlan(stableKeys, stableTargets, Map.copyOf(needs));
    }
}
