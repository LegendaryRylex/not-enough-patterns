package dev.rylex.nep.compat.draconic;

import com.brandon3055.brandonscore.api.power.IOPStorage;
import dev.rylex.nep.machine.MatrixEnergyBuffer;

final class FusionMatrixOpStorage implements IOPStorage {

    private final MatrixEnergyBuffer buffer;

    FusionMatrixOpStorage(MatrixEnergyBuffer buffer) {
        this.buffer = buffer;
    }

    @Override
    public long getOPStored() {
        return buffer.stored();
    }

    @Override
    public long getMaxOPStored() {
        return buffer.capacity();
    }

    @Override
    public long receiveOP(long amount, boolean simulate) {
        return buffer.receive(amount, simulate);
    }

    @Override
    public long extractOP(long amount, boolean simulate) {
        return 0L;
    }

    @Override
    public long maxReceive() {
        return buffer.maxReceive();
    }

    @Override
    public long maxExtract() {
        return 0L;
    }

    @Override
    public long modifyEnergyStored(long delta) {
        return buffer.modify(delta);
    }

    @Override
    public boolean canReceive() {
        return true;
    }

    @Override
    public boolean canExtract() {
        return false;
    }
}
