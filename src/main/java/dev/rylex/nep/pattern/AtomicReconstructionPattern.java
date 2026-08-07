package dev.rylex.nep.pattern;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import dev.rylex.nep.NepComponents;
import dev.rylex.nep.NepItems;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class AtomicReconstructionPattern extends RecipePattern {

    public AtomicReconstructionPattern(AEItemKey definition, Level level) {
        super(definition, NepComponents.ENCODED_ATOMIC_RECONSTRUCTION_PATTERN.get(), level);
    }

    public static ItemStack encode(ResourceLocation recipe, List<GenericStack> inputs, GenericStack result) {
        return encode(
                NepItems.ATOMIC_RECONSTRUCTION_PATTERN.get(),
                NepComponents.ENCODED_ATOMIC_RECONSTRUCTION_PATTERN.get(),
                recipe,
                inputs,
                result);
    }
}
