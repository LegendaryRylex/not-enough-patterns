package dev.rylex.nep.compat.occultism;

import appeng.api.crafting.IPatternDetails;
import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;
import dev.rylex.nep.NepConfig;
import java.util.List;
import net.minecraft.core.Direction;

public class SpiritProviderCraftingMachine implements ICraftingMachine {

    private final SpiritProviderBlockEntity provider;

    public SpiritProviderCraftingMachine(SpiritProviderBlockEntity provider) {
        this.provider = provider;
    }

    @Override
    public PatternContainerGroup getCraftingMachineInfo() {
        var block = NepOccultismContent.SPIRIT_PROVIDER.get();
        return new PatternContainerGroup(AEItemKey.of(block), block.getName(), List.of());
    }

    @Override
    public boolean acceptsPlans() {
        return NepConfig.occultismSpiritProvider();
    }

    @Override
    public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputs, Direction ejectionDirection) {
        return provider.push(patternDetails, inputs, ejectionDirection);
    }
}
