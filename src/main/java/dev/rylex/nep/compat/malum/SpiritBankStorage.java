package dev.rylex.nep.compat.malum;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

class SpiritBankStorage implements MEStorage {

    private final FocusedSpiritMatrixBlockEntity matrix;

    SpiritBankStorage(FocusedSpiritMatrixBlockEntity matrix) {
        this.matrix = matrix;
    }

    @Override
    public Component getDescription() {
        return matrix.getDisplayName();
    }

    @Override
    public long insert(AEKey what, long amount, Actionable mode, IActionSource source) {
        if (!(what instanceof AEItemKey key) || amount <= 0 || !matrix.banksSpirit(key)) {
            return 0;
        }
        return matrix.bankSpirits(key, amount, mode);
    }

    @Override
    public long extract(AEKey what, long amount, Actionable mode, IActionSource source) {
        if (!(what instanceof AEItemKey key) || amount <= 0 || matrix.isOwnActionSource(source)) {
            return 0;
        }
        long free = matrix.freeSpirits(key);
        long taken = Math.min(amount, free);
        if (taken <= 0) {
            return 0;
        }
        return mode == Actionable.SIMULATE ? taken : matrix.takeSpirits(key, taken);
    }

    @Override
    public void getAvailableStacks(KeyCounter out) {
        matrix.forEachFreeSpirit(out::add);
    }

    static ItemStack stackOf(AEItemKey key, int count) {
        return key.toStack(count);
    }
}
