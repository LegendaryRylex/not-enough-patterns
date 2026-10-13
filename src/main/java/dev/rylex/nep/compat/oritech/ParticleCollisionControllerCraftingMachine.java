package dev.rylex.nep.compat.oritech;

import appeng.api.crafting.IPatternDetails;
import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

public class ParticleCollisionControllerCraftingMachine implements ICraftingMachine {

    private final ParticleCollisionControllerBlockEntity controller;

    public ParticleCollisionControllerCraftingMachine(ParticleCollisionControllerBlockEntity controller) {
        this.controller = controller;
    }

    @Override
    public PatternContainerGroup getCraftingMachineInfo() {
        return new PatternContainerGroup(
                AEItemKey.of(new ItemStack(NepOritechContent.CONTROLLER_ITEM.get())),
                NepOritechContent.CONTROLLER.get().getName(),
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
