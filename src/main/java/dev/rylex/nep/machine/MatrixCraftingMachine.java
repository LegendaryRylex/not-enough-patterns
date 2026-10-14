package dev.rylex.nep.machine;

import appeng.api.crafting.IPatternDetails;
import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public class MatrixCraftingMachine implements ICraftingMachine {

    @FunctionalInterface
    public interface Pusher {
        boolean push(IPatternDetails patternDetails, KeyCounter[] inputs, Direction ejectionDirection);
    }

    private final BooleanSupplier ready;
    private final Pusher pusher;
    private final Supplier<? extends Item> icon;
    private final Supplier<? extends Block> block;

    public MatrixCraftingMachine(
            BooleanSupplier ready, Pusher pusher, Supplier<? extends Item> icon, Supplier<? extends Block> block) {
        this.ready = ready;
        this.pusher = pusher;
        this.icon = icon;
        this.block = block;
    }

    @Override
    public PatternContainerGroup getCraftingMachineInfo() {
        return new PatternContainerGroup(
                AEItemKey.of(new ItemStack(icon.get())), block.get().getName(), List.of());
    }

    @Override
    public boolean acceptsPlans() {
        return ready.getAsBoolean();
    }

    @Override
    public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputs, Direction ejectionDirection) {
        return pusher.push(patternDetails, inputs, ejectionDirection);
    }
}
