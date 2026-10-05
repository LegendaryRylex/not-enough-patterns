package dev.rylex.nep.compat.ars;

import com.hollingsworth.arsnouveau.common.capability.SourceStorage;

final class MatrixSourceStore extends SourceStorage {

    static final int CAPACITY = 100_000;

    private final Runnable onChanged;

    MatrixSourceStore(Runnable onChanged) {
        super(CAPACITY, CAPACITY, 0);
        this.onChanged = onChanged;
    }

    int room() {
        return Math.max(0, capacity - source);
    }

    int add(int amount) {
        int added = Math.min(room(), Math.max(0, amount));
        if (added > 0) {
            source += added;
            onContentsChanged();
        }
        return added;
    }

    boolean spend(int amount) {
        if (amount > source) {
            return false;
        }
        if (amount > 0) {
            source -= amount;
            onContentsChanged();
        }
        return true;
    }

    @Override
    public void onContentsChanged() {
        onChanged.run();
    }
}
