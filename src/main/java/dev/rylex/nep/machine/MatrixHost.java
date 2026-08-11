package dev.rylex.nep.machine;

import appeng.api.config.Actionable;
import appeng.api.stacks.AEKey;
import net.minecraft.core.BlockPos;

public interface MatrixHost {

    BlockPos getBlockPos();

    void setChanged();

    long bufferedAmount(AEKey key);

    long acceptCrafted(AEKey key, long amount, Actionable mode);

    void onGridStateChanged();
}
