package dev.rylex.nep.pattern;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import dev.rylex.nep.NepComponents;
import dev.rylex.nep.NepItems;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class InfusionPattern extends RecipePattern {

    public InfusionPattern(AEItemKey definition, Level level) {
        super(definition, NepComponents.ENCODED_INFUSION_PATTERN.get(), level);
    }

    public static ItemStack encode(Identifier recipe, List<GenericStack> inputs, GenericStack result) {
        return encode(
                NepItems.INFUSION_PATTERN.get(), NepComponents.ENCODED_INFUSION_PATTERN.get(), recipe, inputs, result);
    }
}
