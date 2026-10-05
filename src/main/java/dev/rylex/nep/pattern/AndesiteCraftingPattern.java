package dev.rylex.nep.pattern;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import dev.rylex.nep.NepComponents;
import dev.rylex.nep.NepItems;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class AndesiteCraftingPattern extends RecipePattern {

    public AndesiteCraftingPattern(AEItemKey definition, Level level) {
        super(definition, NepComponents.ENCODED_ANDESITE_CRAFTING_PATTERN.get(), level);
    }

    public static ItemStack encode(ResourceLocation recipe, List<GenericStack> inputs, GenericStack result) {
        return encode(recipe, inputs, List.of(), result);
    }

    public static ItemStack encode(
            ResourceLocation recipe, List<GenericStack> inputs, List<GenericStack> retained, GenericStack result) {
        return encode(recipe, inputs, retained, List.of(), result);
    }

    public static ItemStack encode(
            ResourceLocation recipe,
            List<GenericStack> inputs,
            List<GenericStack> retained,
            List<GenericStack> worn,
            GenericStack result) {
        return encode(
                NepItems.ANDESITE_CRAFTING_PATTERN.get(),
                NepComponents.ENCODED_ANDESITE_CRAFTING_PATTERN.get(),
                recipe,
                inputs,
                retained,
                worn,
                result);
    }
}
