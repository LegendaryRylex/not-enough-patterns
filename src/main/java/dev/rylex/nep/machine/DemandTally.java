package dev.rylex.nep.machine;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.world.level.Level;

public final class DemandTally {
    private final List<AEKey> keys = new ArrayList<>();
    private final List<Long> targets = new ArrayList<>();

    public void add(AEKey key, long target) {
        int existing = keys.indexOf(key);
        if (existing >= 0) {
            targets.set(existing, targets.get(existing) + target);
        } else {
            keys.add(key);
            targets.add(target);
        }
    }

    public boolean isEmpty() {
        return keys.isEmpty();
    }

    public List<GenericStack> restock(Level level, MatrixGridNode power) {
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < keys.size(); i++) {
            order.add(i);
        }
        order.sort(Comparator.comparing(i -> keys.get(i).toString()));
        List<AEKey> stableKeys = new ArrayList<>(keys.size());
        List<Long> stableTargets = new ArrayList<>(targets.size());
        for (int i : order) {
            stableKeys.add(keys.get(i));
            stableTargets.add(targets.get(i));
        }
        return power.restock(level, stableKeys, stableTargets);
    }
}
