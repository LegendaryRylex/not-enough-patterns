package dev.rylex.nep.hub;

import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

public record HubTarget(
        BlockPos pos,
        HubRole role,
        @Nullable IItemHandler items,
        @Nullable IFluidHandler fluids) {

    public boolean isEmpty() {
        return items == null && fluids == null;
    }

    public boolean accepts() {
        return role == HubRole.INPUT;
    }
}
