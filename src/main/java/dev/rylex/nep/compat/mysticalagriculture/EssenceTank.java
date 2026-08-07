package dev.rylex.nep.compat.mysticalagriculture;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import dev.rylex.nep.NepConfig;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

final class EssenceTank {

    private static final String ESSENCE_KEY = "Essence";
    private static final String AMOUNT_KEY = "Amount";

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

    CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        if (!isEmpty()) {
            tag.put(ESSENCE_KEY, GenericStack.writeTag(registries, new GenericStack(essence, amount)));
            tag.putLong(AMOUNT_KEY, amount);
        }
        return tag;
    }

    void load(CompoundTag tag, HolderLookup.Provider registries) {
        clear();
        if (!tag.contains(ESSENCE_KEY)) {
            return;
        }
        GenericStack stored = GenericStack.readTag(registries, tag.getCompound(ESSENCE_KEY));
        if (stored != null && stored.what() instanceof AEItemKey key) {
            essence = key;
            amount = Math.max(0, tag.getLong(AMOUNT_KEY));
            if (amount <= 0) {
                clear();
            }
        }
    }
}
