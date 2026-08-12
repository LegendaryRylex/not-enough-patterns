package dev.rylex.nep.machine;

import java.util.function.ToIntFunction;
import javax.annotation.Nullable;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public final class MachineItemView implements ResourceHandler<ItemResource> {

    private final ResourceHandler<ItemResource> input;

    @Nullable
    private final ResourceHandler<ItemResource> output;

    @Nullable
    private final ToIntFunction<ItemResource> demand;

    private MachineItemView(
            ResourceHandler<ItemResource> input,
            @Nullable ResourceHandler<ItemResource> output,
            @Nullable ToIntFunction<ItemResource> demand) {
        this.input = input;
        this.output = output;
        this.demand = demand;
    }

    public static MachineItemView feedable(
            ResourceHandler<ItemResource> input, @Nullable ResourceHandler<ItemResource> output) {
        return new MachineItemView(input, output, null);
    }

    public static MachineItemView demandLimited(
            ResourceHandler<ItemResource> input,
            @Nullable ResourceHandler<ItemResource> output,
            ToIntFunction<ItemResource> demand) {
        return new MachineItemView(input, output, demand);
    }

    private int allowance(ItemResource resource) {
        return demand == null ? Integer.MAX_VALUE : Math.max(0, demand.applyAsInt(resource));
    }

    private int inputSlots() {
        return input.size();
    }

    private boolean isOutput(int index) {
        return index >= inputSlots();
    }

    @Override
    public int size() {
        return inputSlots() + (output == null ? 0 : output.size());
    }

    @Override
    public ItemResource getResource(int index) {
        return isOutput(index) ? output.getResource(index - inputSlots()) : input.getResource(index);
    }

    @Override
    public long getAmountAsLong(int index) {
        return isOutput(index) ? output.getAmountAsLong(index - inputSlots()) : input.getAmountAsLong(index);
    }

    @Override
    public long getCapacityAsLong(int index, ItemResource resource) {
        return isOutput(index)
                ? output.getCapacityAsLong(index - inputSlots(), resource)
                : input.getCapacityAsLong(index, resource);
    }

    @Override
    public boolean isValid(int index, ItemResource resource) {
        return !isOutput(index) && allowance(resource) > 0 && input.isValid(index, resource);
    }

    @Override
    public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
        if (isOutput(index)) {
            return 0;
        }
        int allowed = Math.min(amount, allowance(resource));
        return allowed <= 0 ? 0 : input.insert(index, resource, allowed, transaction);
    }

    @Override
    public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
        return isOutput(index) ? output.extract(index - inputSlots(), resource, amount, transaction) : 0;
    }
}
