package dev.rylex.nep.pattern;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import dev.rylex.nep.NepComponents;
import dev.rylex.nep.NepItems;
import java.util.List;
import java.util.function.BiFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class FusionCraftingPattern extends RecipePattern {

    private static volatile BiFunction<GenericStack, Level, GenericStack> canonicalizer = (stack, level) -> stack;

    private static volatile BiFunction<ResourceLocation, Level, InputSubstitution> substitutions =
            (recipe, level) -> null;

    public static void canonicalizeWith(BiFunction<GenericStack, Level, GenericStack> canonicalizer) {
        FusionCraftingPattern.canonicalizer = canonicalizer;
    }

    public static void substituteInputsWith(BiFunction<ResourceLocation, Level, InputSubstitution> substitutions) {
        FusionCraftingPattern.substitutions = substitutions;
    }

    public FusionCraftingPattern(AEItemKey definition, Level level) {
        super(
                definition,
                NepComponents.ENCODED_FUSION_CRAFTING_PATTERN.get(),
                level,
                stack -> level == null ? stack : canonicalizer.apply(stack, level),
                substitutionFor(definition, level));
    }

    @Nullable
    private static InputSubstitution substitutionFor(AEItemKey definition, @Nullable Level level) {
        if (level == null) {
            return null;
        }
        EncodedRecipePattern encoded = definition.get(NepComponents.ENCODED_FUSION_CRAFTING_PATTERN.get());
        return encoded == null ? null : substitutions.apply(encoded.recipe(), level);
    }

    public static ItemStack encode(ResourceLocation recipe, List<GenericStack> inputs, GenericStack result) {
        return encode(recipe, inputs, List.of(), result);
    }

    public static ItemStack encode(
            ResourceLocation recipe, List<GenericStack> inputs, List<GenericStack> retained, GenericStack result) {
        return encode(
                NepItems.FUSION_CRAFTING_PATTERN.get(),
                NepComponents.ENCODED_FUSION_CRAFTING_PATTERN.get(),
                recipe,
                inputs,
                retained,
                result);
    }
}
