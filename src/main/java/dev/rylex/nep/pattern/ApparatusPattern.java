package dev.rylex.nep.pattern;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import dev.rylex.nep.NepComponents;
import dev.rylex.nep.NepItems;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ApparatusPattern extends RecipePattern {

    private static volatile BiFunction<ResourceLocation, Level, InputSubstitution> substitutions =
            (recipe, level) -> null;

    public static void substituteInputsWith(BiFunction<ResourceLocation, Level, InputSubstitution> substitutions) {
        ApparatusPattern.substitutions = substitutions;
    }

    public ApparatusPattern(AEItemKey definition, Level level) {
        super(
                definition,
                NepComponents.ENCODED_APPARATUS_PATTERN.get(),
                level,
                UnaryOperator.identity(),
                substitutionFor(NepComponents.ENCODED_APPARATUS_PATTERN.get(), definition, level, substitutions));
    }

    public static ItemStack encode(ResourceLocation recipe, List<GenericStack> inputs, GenericStack result) {
        return encode(
                NepItems.APPARATUS_PATTERN.get(),
                NepComponents.ENCODED_APPARATUS_PATTERN.get(),
                recipe,
                inputs,
                result);
    }
}
