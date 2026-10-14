package dev.rylex.nep.compat.mysticalagriculture;

import appeng.api.stacks.AEItemKey;
import java.util.function.Consumer;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

final class TankBank {
    private final EssenceTank[] tanks;

    TankBank(int size) {
        tanks = new EssenceTank[size];
        for (int tank = 0; tank < size; tank++) {
            tanks[tank] = new EssenceTank();
        }
    }

    EssenceTank get(int index) {
        return tanks[index];
    }

    EssenceTank[] snapshot() {
        EssenceTank[] copy = new EssenceTank[tanks.length];
        for (int tank = 0; tank < tanks.length; tank++) {
            copy[tank] = tanks[tank].copy();
        }
        return copy;
    }

    long insert(AEItemKey key, long amount, boolean simulate) {
        return insertInto(simulate ? snapshot() : tanks, key, amount);
    }

    static long insertInto(EssenceTank[] target, AEItemKey key, long amount) {
        EssenceTank home = soleTankFor(target, key);
        return home == null ? 0 : home.insert(key, amount, false);
    }

    @Nullable
    private static EssenceTank soleTankFor(EssenceTank[] target, AEItemKey key) {
        for (EssenceTank tank : target) {
            if (tank.holds(key)) {
                return tank;
            }
        }
        for (EssenceTank tank : target) {
            if (tank.isEmpty()) {
                return tank;
            }
        }
        return null;
    }

    long amount(AEItemKey key) {
        long total = 0;
        for (EssenceTank tank : tanks) {
            if (tank.holds(key)) {
                total += tank.amount();
            }
        }
        return total;
    }

    long room(AEItemKey key) {
        EssenceTank home = soleTankFor(tanks, key);
        return home == null ? 0 : home.room(key);
    }

    void take(AEItemKey key, long amount) {
        long remaining = amount;
        for (EssenceTank tank : tanks) {
            if (remaining <= 0 || !tank.holds(key)) {
                continue;
            }
            remaining -= tank.extract(remaining, false);
        }
    }

    void drainAll(Consumer<ItemStack> sink) {
        for (EssenceTank tank : tanks) {
            while (!tank.isEmpty()) {
                AEItemKey key = tank.essence();
                int chunk = (int) Math.min(tank.amount(), key.toStack(1).getMaxStackSize());
                tank.extract(chunk, false);
                sink.accept(key.toStack(chunk));
            }
        }
    }

    void clear() {
        for (EssenceTank tank : tanks) {
            tank.clear();
        }
    }

    int signature(int hash) {
        int result = hash;
        for (EssenceTank tank : tanks) {
            result = 31 * result + (tank.isEmpty() ? 0 : tank.essence().hashCode());
            result = 31 * result + Long.hashCode(tank.amount());
        }
        return result;
    }

    ListTag save(HolderLookup.Provider registries) {
        ListTag tankList = new ListTag();
        for (EssenceTank tank : tanks) {
            tankList.add(tank.save(registries));
        }
        return tankList;
    }

    void load(CompoundTag tag, String key, HolderLookup.Provider registries) {
        ListTag tankList = tag.getList(key, Tag.TAG_COMPOUND);
        for (int index = 0; index < tanks.length; index++) {
            tanks[index].load(index < tankList.size() ? tankList.getCompound(index) : new CompoundTag(), registries);
        }
    }
}
