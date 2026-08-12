package dev.rylex.nep.compat.create;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

record MatrixDemand(List<MatrixDemand.ItemNeed> items, List<MatrixDemand.FluidNeed> fluids, long energy) {

    record ItemNeed(Predicate<ItemStack> matches, int count) {}

    record FluidNeed(Predicate<FluidStack> matches, int amount) {}

    static MatrixDemand of(SequencedAssemblyResolver.Demand demand) {
        List<ItemNeed> items = new ArrayList<>(demand.items().size());
        for (SequencedAssemblyResolver.ItemDemand entry : demand.items()) {
            items.add(new ItemNeed(entry.ingredient()::test, entry.count()));
        }
        List<FluidNeed> fluids = new ArrayList<>(demand.fluids().size());
        for (SequencedAssemblyResolver.FluidDemand entry : demand.fluids()) {
            fluids.add(new FluidNeed(entry.ingredient()::test, entry.amount()));
        }
        return new MatrixDemand(List.copyOf(items), List.copyOf(fluids), demand.energy());
    }

    static MatrixDemand exact(List<GenericStack> stacks) {
        List<ItemNeed> items = new ArrayList<>();
        List<FluidNeed> fluids = new ArrayList<>();
        for (GenericStack stack : stacks) {
            int amount = (int) Math.min(stack.amount(), Integer.MAX_VALUE);
            if (amount <= 0) {
                continue;
            }
            if (stack.what() instanceof AEItemKey key) {
                items.add(new ItemNeed(key::matches, amount));
            } else if (stack.what() instanceof AEFluidKey key) {
                fluids.add(new FluidNeed(key::matches, amount));
            }
        }
        return new MatrixDemand(List.copyOf(items), List.copyOf(fluids), 0);
    }
}
