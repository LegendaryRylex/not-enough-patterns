package dev.rylex.nep.hub;

import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jetbrains.annotations.Nullable;

public record HubTarget(
        BlockPos pos,
        HubRole role,
        @Nullable ResourceHandler<ItemResource> items,
        @Nullable ResourceHandler<FluidResource> fluids) {

    public boolean isEmpty() {
        return items == null && fluids == null;
    }

    public boolean accepts() {
        return role == HubRole.INPUT;
    }
}
