package dev.rylex.nep.compat.occultism;

import appeng.api.crafting.IPatternDetails;
import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

public class RitualControllerCraftingMachine implements ICraftingMachine {

    private final RitualControllerBlockEntity controller;

    public RitualControllerCraftingMachine(RitualControllerBlockEntity controller) {
        this.controller = controller;
    }

    @Override
    public PatternContainerGroup getCraftingMachineInfo() {
        return new PatternContainerGroup(
                AEItemKey.of(new ItemStack(NepOccultismContent.CONTROLLER_ITEM.get())),
                NepOccultismContent.CONTROLLER.get().getName(),
                List.of());
    }

    @Override
    public boolean acceptsPlans() {
        return controller.readyForPatterns();
    }

    @Override
    public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputs, Direction ejectionDirection) {
        return controller.pushControllerPattern(patternDetails, inputs, ejectionDirection);
    }
}
