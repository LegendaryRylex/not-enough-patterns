package dev.rylex.nep.pattern.encoding;

import appeng.api.crafting.PatternDetailsHelper;
import dev.rylex.nep.NepComponents;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.guide.NepRecipes;
import dev.rylex.nep.pattern.EncodedMechanicalPattern;
import dev.rylex.nep.pattern.EncodedRecipePattern;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class ProcessingPatternConversionRecipe extends CustomRecipe {

    public ProcessingPatternConversionRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return !convert(input).isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return convert(input);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 1;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return NepRecipes.PROCESSING_PATTERN_CONVERSION.get();
    }

    private static ItemStack convert(CraftingInput input) {
        ItemStack pattern = ItemStack.EMPTY;
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (!pattern.isEmpty()) {
                return ItemStack.EMPTY;
            }
            pattern = stack;
        }
        return convert(pattern);
    }

    public static boolean converts(ItemStack pattern) {
        return conversion(pattern) != null;
    }

    public static ItemStack convert(ItemStack pattern) {
        ProcessingPatternConversion.Conversion conversion = conversion(pattern);
        if (conversion == null) {
            return ItemStack.EMPTY;
        }
        ItemStack converted = PatternDetailsHelper.encodeProcessingPattern(conversion.inputs(), conversion.outputs());
        if (conversion.recipe() != null) {
            converted.set(NepComponents.SOURCE_RECIPE.get(), conversion.recipe());
        }
        return converted;
    }

    @Nullable
    private static ProcessingPatternConversion.Conversion conversion(ItemStack pattern) {
        if (pattern.isEmpty()
                || !PatternDetailsHelper.isEncodedPattern(pattern)
                || !NepConfig.processingPatternConversion()) {
            return null;
        }
        return conversionOf(pattern);
    }

    @Nullable
    private static ProcessingPatternConversion.Conversion conversionOf(ItemStack pattern) {
        for (TypedDataComponent<?> component : pattern.getComponents()) {
            if (component.value() instanceof EncodedRecipePattern encoded) {
                return ProcessingPatternConversion.of(encoded);
            }
            if (component.value() instanceof EncodedMechanicalPattern encoded) {
                return ProcessingPatternConversion.of(encoded);
            }
        }
        return null;
    }
}
