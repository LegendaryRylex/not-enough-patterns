package dev.rylex.nep.compat.create;

import java.util.function.IntSupplier;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

final class AssemblyFluidBuffer implements IFluidHandler {

    static final int TANKS = 4;

    private final FluidStack[] contents = new FluidStack[TANKS];
    private final IntSupplier capacity;
    private final Runnable onChanged;

    AssemblyFluidBuffer(IntSupplier capacity, Runnable onChanged) {
        this.capacity = capacity;
        this.onChanged = onChanged;
        for (int tank = 0; tank < TANKS; tank++) {
            contents[tank] = FluidStack.EMPTY;
        }
    }

    @Override
    public int getTanks() {
        return TANKS;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return contents[tank];
    }

    @Override
    public int getTankCapacity(int tank) {
        return capacity.getAsInt();
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return true;
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty()) {
            return 0;
        }
        int target = tankFor(resource);
        if (target < 0) {
            return 0;
        }
        FluidStack held = contents[target];
        int room = Math.min(resource.getAmount(), getTankCapacity(target) - held.getAmount());
        if (room <= 0) {
            return 0;
        }
        if (action.execute()) {
            if (held.isEmpty()) {
                contents[target] = resource.copyWithAmount(room);
            } else {
                held.grow(room);
            }
            onChanged.run();
        }
        return room;
    }

    private int tankFor(FluidStack resource) {
        return tankIn(contents, resource);
    }

    private static int tankIn(FluidStack[] store, FluidStack resource) {
        for (int tank = 0; tank < TANKS; tank++) {
            if (FluidStack.isSameFluidSameComponents(store[tank], resource)) {
                return tank;
            }
        }
        for (int tank = 0; tank < TANKS; tank++) {
            if (store[tank].isEmpty()) {
                return tank;
            }
        }
        return -1;
    }

    boolean canFillAll(Iterable<FluidStack> resources) {
        FluidStack[] scratch = new FluidStack[TANKS];
        for (int tank = 0; tank < TANKS; tank++) {
            scratch[tank] = contents[tank].copy();
        }
        int cap = capacity.getAsInt();
        for (FluidStack resource : resources) {
            if (resource.isEmpty()) {
                continue;
            }
            int target = tankIn(scratch, resource);
            if (target < 0 || cap - scratch[target].getAmount() < resource.getAmount()) {
                return false;
            }
            if (scratch[target].isEmpty()) {
                scratch[target] = resource.copy();
            } else {
                scratch[target].grow(resource.getAmount());
            }
        }
        return true;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.isEmpty()) {
            return FluidStack.EMPTY;
        }
        for (int tank = 0; tank < TANKS; tank++) {
            FluidStack held = contents[tank];
            if (held.isEmpty() || !FluidStack.isSameFluidSameComponents(held, resource)) {
                continue;
            }
            return drainTank(tank, Math.min(resource.getAmount(), held.getAmount()), action);
        }
        return FluidStack.EMPTY;
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        if (maxDrain <= 0) {
            return FluidStack.EMPTY;
        }
        for (int tank = 0; tank < TANKS; tank++) {
            if (!contents[tank].isEmpty()) {
                return drainTank(tank, Math.min(maxDrain, contents[tank].getAmount()), action);
            }
        }
        return FluidStack.EMPTY;
    }

    private FluidStack drainTank(int tank, int amount, FluidAction action) {
        FluidStack held = contents[tank];
        FluidStack drained = held.copyWithAmount(amount);
        if (action.execute()) {
            held.shrink(amount);
            if (held.isEmpty()) {
                contents[tank] = FluidStack.EMPTY;
            }
            onChanged.run();
        }
        return drained;
    }

    @FunctionalInterface
    interface OverflowSink {
        int accept(FluidStack overflow);
    }

    boolean trimToCapacity(OverflowSink sink) {
        boolean changed = false;
        for (int tank = 0; tank < TANKS; tank++) {
            FluidStack held = contents[tank];
            if (held.isEmpty()) {
                continue;
            }
            int excess = held.getAmount() - getTankCapacity(tank);
            if (excess <= 0) {
                continue;
            }
            int accepted = Math.min(excess, sink.accept(held.copyWithAmount(excess)));
            if (accepted <= 0) {
                continue;
            }
            held.shrink(accepted);
            if (held.isEmpty()) {
                contents[tank] = FluidStack.EMPTY;
            }
            changed = true;
        }
        if (changed) {
            onChanged.run();
        }
        return changed;
    }

    boolean overCapacity() {
        for (int tank = 0; tank < TANKS; tank++) {
            FluidStack held = contents[tank];
            if (!held.isEmpty() && held.getAmount() > getTankCapacity(tank)) {
                return true;
            }
        }
        return false;
    }

    void takeFrom(int tank, int amount) {
        FluidStack held = contents[tank];
        held.shrink(amount);
        if (held.isEmpty()) {
            contents[tank] = FluidStack.EMPTY;
        }
        onChanged.run();
    }

    boolean isEmpty() {
        for (FluidStack held : contents) {
            if (!held.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    void clear() {
        for (int tank = 0; tank < TANKS; tank++) {
            contents[tank] = FluidStack.EMPTY;
        }
        onChanged.run();
    }

    ListTag save(HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (int tank = 0; tank < TANKS; tank++) {
            CompoundTag entry = new CompoundTag();
            entry.putInt("Tank", tank);
            if (!contents[tank].isEmpty()) {
                entry.put("Fluid", contents[tank].save(registries));
            }
            list.add(entry);
        }
        return list;
    }

    void load(HolderLookup.Provider registries, ListTag list) {
        clear();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            int tank = entry.getInt("Tank");
            if (tank < 0 || tank >= TANKS || !entry.contains("Fluid", Tag.TAG_COMPOUND)) {
                continue;
            }
            contents[tank] = FluidStack.parseOptional(registries, entry.getCompound("Fluid"));
        }
    }
}
