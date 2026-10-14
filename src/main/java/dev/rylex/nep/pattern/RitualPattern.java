package dev.rylex.nep.pattern;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import dev.rylex.nep.NepComponents;
import dev.rylex.nep.NepItems;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class RitualPattern extends RecipePattern {

    private static volatile BiFunction<Identifier, Level, InputSubstitution> substitutions = (recipe, level) -> null;

    public static void substituteInputsWith(BiFunction<Identifier, Level, InputSubstitution> substitutions) {
        RitualPattern.substitutions = substitutions;
    }

    public RitualPattern(AEItemKey definition, Level level) {
        super(
                definition,
                NepComponents.ENCODED_RITUAL_PATTERN.get(),
                level,
                UnaryOperator.identity(),
                substitutionFor(definition, level));
    }

    @Nullable
    private static InputSubstitution substitutionFor(AEItemKey definition, @Nullable Level level) {
        if (level == null) {
            return null;
        }
        EncodedRecipePattern encoded = definition.get(NepComponents.ENCODED_RITUAL_PATTERN.get());
        return encoded == null ? null : substitutions.apply(encoded.recipe(), level);
    }

    public static ItemStack encode(Identifier recipe, List<GenericStack> inputs, GenericStack result) {
        return encode(
                NepItems.RITUAL_PATTERN.get(), NepComponents.ENCODED_RITUAL_PATTERN.get(), recipe, inputs, result);
    }
}
