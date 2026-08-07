package dev.rylex.nep.machine;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NumericTag;
import net.neoforged.neoforge.energy.IEnergyStorage;

public final class MatrixEnergyBuffer implements IEnergyStorage {

    private static final String STORED_KEY = "Stored";
    private static final String LEGACY_STORED_KEY = "Energy";

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

    public long modify(long delta) {
        if (delta >= 0L) {
            long added = Math.min(delta, capacity - stored);
            stored += Math.max(0L, added);
            return Math.max(0L, added);
        }
        return -spend(-delta);
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

    public boolean setCapacity(long capacity) {
        return resize(capacity, maxReceive);
    }

    @Override
    public int receiveEnergy(int toReceive, boolean simulate) {
        return (int) receive(toReceive, simulate);
    }

    @Override
    public int extractEnergy(int toExtract, boolean simulate) {
        return 0;
    }

    @Override
    public int getEnergyStored() {
        return (int) Math.min(Integer.MAX_VALUE, stored);
    }

    @Override
    public int getMaxEnergyStored() {
        return (int) Math.min(Integer.MAX_VALUE, capacity);
    }

    @Override
    public boolean canExtract() {
        return false;
    }

    @Override
    public boolean canReceive() {
        return true;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putLong(STORED_KEY, stored);
        return tag;
    }

    public void load(CompoundTag tag) {
        stored = Math.max(0L, Math.min(capacity, readStored(tag)));
    }

    private static long readStored(CompoundTag tag) {
        if (tag.get(STORED_KEY) instanceof NumericTag value) {
            return value.getAsLong();
        }
        if (tag.get(LEGACY_STORED_KEY) instanceof NumericTag value) {
            return value.getAsLong();
        }
        return 0L;
    }
}
