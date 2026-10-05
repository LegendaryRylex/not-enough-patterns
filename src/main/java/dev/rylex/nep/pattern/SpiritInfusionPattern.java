package dev.rylex.nep.pattern;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import dev.rylex.nep.NepComponents;
import dev.rylex.nep.NepItems;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class SpiritInfusionPattern extends RecipePattern {

    public SpiritInfusionPattern(AEItemKey definition, Level level) {
        super(definition, NepComponents.ENCODED_SPIRIT_INFUSION_PATTERN.get(), level);
    }

    public static ItemStack encode(ResourceLocation recipe, List<GenericStack> inputs, GenericStack result) {
        return encode(
                NepItems.SPIRIT_INFUSION_PATTERN.get(),
                NepComponents.ENCODED_SPIRIT_INFUSION_PATTERN.get(),
                recipe,
                inputs,
                result);
    }
}
