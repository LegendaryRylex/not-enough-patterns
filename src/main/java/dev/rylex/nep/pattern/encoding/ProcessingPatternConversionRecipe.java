package dev.rylex.nep.pattern.encoding;

import appeng.api.crafting.PatternDetailsHelper;
import com.mojang.serialization.MapCodec;
import dev.rylex.nep.NepComponents;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.guide.NepRecipes;
import dev.rylex.nep.pattern.EncodedRecipePattern;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class ProcessingPatternConversionRecipe extends CustomRecipe {

    public static final ProcessingPatternConversionRecipe INSTANCE = new ProcessingPatternConversionRecipe();
    public static final MapCodec<ProcessingPatternConversionRecipe> MAP_CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, ProcessingPatternConversionRecipe> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return !convert(input).isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        return convert(input);
    }

    @Override
    public RecipeSerializer<ProcessingPatternConversionRecipe> getSerializer() {
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
        }
        return null;
    }
}
