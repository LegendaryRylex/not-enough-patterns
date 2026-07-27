package dev.rylex.nep.compat.create;

import appeng.api.crafting.IPatternDetails;
import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;
import dev.rylex.nep.NepConfig;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

public class SequencedAssemblyCraftingMachine implements ICraftingMachine {

    private final SequencedAssemblyControllerBlockEntity controller;

    public SequencedAssemblyCraftingMachine(SequencedAssemblyControllerBlockEntity controller) {
        this.controller = controller;
    }

    @Override
    public PatternContainerGroup getCraftingMachineInfo() {
        return new PatternContainerGroup(
                AEItemKey.of(new ItemStack(NepCreateContent.CONTROLLER_ITEM.get())),
                NepCreateContent.CONTROLLER.get().getName(),
                List.of());
    }

    @Override
    public boolean acceptsPlans() {
        return NepConfig.createSequencedAssembly() && controller.readyForPatterns();
    }

    @Override
    public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputs, Direction ejectionDirection) {
        return controller.pushSequencedPattern(patternDetails, inputs, ejectionDirection);
    }
}
