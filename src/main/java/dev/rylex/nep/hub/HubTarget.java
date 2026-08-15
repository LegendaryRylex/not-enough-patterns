package dev.rylex.nep.hub;

import appeng.api.stacks.AEKey;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jetbrains.annotations.Nullable;

public record HubTarget(
        HubLink link,
        @Nullable ResourceHandler<ItemResource> items,
        @Nullable ResourceHandler<FluidResource> fluids) {

    public HubTarget(
            BlockPos pos,
            HubRole role,
            @Nullable ResourceHandler<ItemResource> items,
            @Nullable ResourceHandler<FluidResource> fluids) {
        this(new HubLink(pos, role), items, fluids);
    }

    public BlockPos pos() {
        return link.pos();
    }

    public HubRole role() {
        return link.role();
    }

    public int priority() {
        return link.priority();
    }

    public boolean isEmpty() {
        return items == null && fluids == null;
    }

    public boolean accepts() {
        return link.role().accepts();
    }

    public boolean provides() {
        return link.role().provides();
    }

    public boolean acceptsKey(AEKey what) {
        return accepts() && link.insertFilter().permits(what);
    }

    public boolean returnsKey(AEKey what) {
        return provides() && link.returnFilter().permits(what);
    }
}
