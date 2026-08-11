package dev.rylex.nep.compat.mysticalagriculture;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import dev.rylex.nep.NepConfig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

final class EssenceTank {

    private static final String ESSENCE_KEY = "Essence";

    @Nullable
    private AEItemKey essence;

    private long amount;

    @Nullable
    AEItemKey essence() {
        return essence;
    }

    long amount() {
        return amount;
    }

    static long capacity() {
        return NepConfig.mysticalInfusedAwakeningMatrixTankCapacity();
    }

    boolean isEmpty() {
        return essence == null || amount <= 0;
    }

    boolean holds(AEItemKey key) {
        return essence != null && essence.equals(key) && amount > 0;
    }

    boolean accepts(AEItemKey key) {
        return isEmpty() || essence.equals(key);
    }

    long room(AEItemKey key) {
        if (!accepts(key)) {
            return 0;
        }
        return Math.max(0, capacity() - (isEmpty() ? 0 : amount));
    }

    long insert(AEItemKey key, long wanted, boolean simulate) {
        long room = Math.min(wanted, room(key));
        if (room <= 0) {
            return 0;
        }
        if (!simulate) {
            essence = key;
            amount = (isEmpty() ? 0 : amount) + room;
        }
        return room;
    }

    long extract(long wanted, boolean simulate) {
        if (isEmpty()) {
            return 0;
        }
        long taken = Math.min(wanted, amount);
        if (!simulate) {
            amount -= taken;
            if (amount <= 0) {
                amount = 0;
                essence = null;
            }
        }
        return taken;
    }

    @Nullable
    GenericStack contents() {
        return isEmpty() ? null : new GenericStack(essence, amount);
    }

    ItemStack displayStack() {
        return isEmpty() ? ItemStack.EMPTY : essence.toStack(1);
    }

    void clear() {
        essence = null;
        amount = 0;
    }

    EssenceTank copy() {
        EssenceTank copy = new EssenceTank();
        copy.essence = essence;
        copy.amount = amount;
        return copy;
    }

    void save(ValueOutput output) {
        if (!isEmpty()) {
            output.store(ESSENCE_KEY, GenericStack.CODEC, new GenericStack(essence, amount));
        }
    }

    void load(ValueInput input) {
        clear();
        input.read(ESSENCE_KEY, GenericStack.CODEC).ifPresent(stored -> {
            if (stored.what() instanceof AEItemKey key && stored.amount() > 0) {
                essence = key;
                amount = stored.amount();
            }
        });
    }
}
