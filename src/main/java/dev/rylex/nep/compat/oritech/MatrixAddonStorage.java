package dev.rylex.nep.compat.oritech;

import dev.rylex.nep.machine.MatrixEnergyBuffer;
import rearth.oritech.api.energy.containers.DynamicEnergyStorage;

final class MatrixAddonStorage extends DynamicEnergyStorage {

    private final MatrixEnergyBuffer buffer;

    MatrixAddonStorage(MatrixEnergyBuffer buffer) {
        super(0L, 0L, 0L, () -> {});
        this.buffer = buffer;
    }

    @Override
    public long insert(long amount, boolean simulate) {
        return buffer.receive(amount, simulate);
    }

    @Override
    public long insertIgnoringLimit(long amount, boolean simulate) {
        long accepted = Math.max(0L, Math.min(amount, buffer.capacity() - buffer.stored()));
        return simulate ? accepted : buffer.modify(accepted);
    }

    @Override
    public long extract(long amount, boolean simulate) {
        return 0L;
    }

    @Override
    public boolean supportsExtraction() {
        return false;
    }

    @Override
    public void setAmount(long amount) {
        buffer.modify(amount - buffer.stored());
    }

    @Override
    public long getAmount() {
        return buffer.stored();
    }

    @Override
    public long getCapacity() {
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
