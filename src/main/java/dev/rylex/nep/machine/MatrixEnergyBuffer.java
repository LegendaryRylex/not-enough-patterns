package dev.rylex.nep.machine;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public final class MatrixEnergyBuffer extends SnapshotJournal<Long> implements EnergyHandler {

    public static final int SYNC_STEPS = 1000;

    private static final String STORED_KEY = "Stored";

    private long capacity;
    private long maxReceive;
    private long stored;

    public MatrixEnergyBuffer(long capacity, long maxReceive) {
        this.capacity = Math.max(0L, capacity);
        this.maxReceive = Math.max(0L, maxReceive);
    }

    public long stored() {
        return stored;
    }

    public long capacity() {
        return capacity;
    }

    public long maxReceive() {
        return maxReceive;
    }

    public int syncLevel() {
        if (capacity <= 0L || stored <= 0L) {
            return 0;
        }
        if (stored >= capacity) {
            return SYNC_STEPS;
        }
        return (int) Math.min(SYNC_STEPS - 1L, stored / Math.max(1L, capacity / SYNC_STEPS));
    }

    public long receive(long toReceive, boolean simulate) {
        long accepted = Math.min(Math.min(maxReceive, toReceive), capacity - stored);
        if (accepted <= 0L) {
            return 0L;
        }
        if (!simulate) {
            stored += accepted;
        }
        return accepted;
    }

    public long spend(long amount) {
        long taken = Math.min(amount, stored);
        if (taken <= 0L) {
            return 0L;
        }
        stored -= taken;
        return taken;
    }

    public boolean resize(long capacity, long maxReceive) {
        long wanted = Math.max(0L, capacity);
        long rate = Math.max(0L, maxReceive);
        if (this.capacity == wanted && this.maxReceive == rate) {
            return false;
        }
        this.capacity = wanted;
        this.maxReceive = rate;
        stored = Math.min(stored, wanted);
        return true;
    }

    @Override
    public long getAmountAsLong() {
        return stored;
    }

    @Override
    public long getCapacityAsLong() {
        return capacity;
    }

    @Override
    public int insert(int amount, TransactionContext transaction) {
        long accepted = receive(Math.max(0, amount), true);
        if (accepted <= 0L) {
            return 0;
        }
        updateSnapshots(transaction);
        stored += accepted;
        return (int) accepted;
    }

    @Override
    public int extract(int amount, TransactionContext transaction) {
        return 0;
    }

    @Override
    protected Long createSnapshot() {
        return stored;
    }

    @Override
    protected void revertToSnapshot(Long snapshot) {
        stored = snapshot;
    }

    public void save(ValueOutput output) {
        output.putLong(STORED_KEY, stored);
    }

    public void load(ValueInput input) {
        stored = Math.max(0L, Math.min(capacity, input.getLongOr(STORED_KEY, 0L)));
    }
}
