package dev.rylex.nep.compat.oritech;

import dev.rylex.nep.machine.MatrixEnergyBuffer;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import rearth.oritech.api.transfer.energy.DynamicEnergyStorage;

final class MatrixAddonStorage extends DynamicEnergyStorage {

    private final MatrixEnergyBuffer buffer;

    MatrixAddonStorage(MatrixEnergyBuffer buffer) {
        super(0L, 0L, 0L, 0L);
        this.buffer = buffer;
    }

    @Override
    public int insert(int amount, TransactionContext transaction) {
        return buffer.insert(amount, transaction);
    }

    @Override
    public long internalInsert(long amount, TransactionContext transaction) {
        return buffer.insert((int) Math.min(Integer.MAX_VALUE, Math.max(0L, amount)), transaction);
    }

    @Override
    public int extract(int amount, TransactionContext transaction) {
        return 0;
    }

    @Override
    public long internalExtract(long amount, TransactionContext transaction) {
        return 0L;
    }

    @Override
    public void set(long amount) {}

    @Override
    public void set(long amount, TransactionContext transaction) {}

    @Override
    public long getAmountAsLong() {
        return buffer.stored();
    }

    @Override
    public long getCapacityAsLong() {
        return buffer.capacity();
    }

    @Override
    public long getMaxInsert() {
        return buffer.maxReceive();
    }

    @Override
    public long getMaxExtract() {
        return 0L;
    }
}
