package dev.rylex.nep.pattern.encoding;

import appeng.api.stacks.GenericStack;
import appeng.crafting.pattern.AEProcessingPattern;
import dev.rylex.nep.pattern.EncodedMechanicalPattern;
import dev.rylex.nep.pattern.EncodedRecipePattern;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public final class ProcessingPatternConversion {
    private ProcessingPatternConversion() {}

    public record Conversion(
            List<GenericStack> inputs,
            List<GenericStack> outputs,
            @Nullable ResourceLocation recipe) {

        public Conversion {
            inputs = List.copyOf(inputs);
            outputs = List.copyOf(outputs);
        }
    }

    @Nullable
    public static Conversion of(EncodedRecipePattern encoded) {
        if (!encoded.retained().isEmpty() || !encoded.worn().isEmpty()) {
            return null;
        }
        return build(encoded.inputs(), encoded.result(), encoded.containsMissingContent(), encoded.recipe());
    }

    @Nullable
    public static Conversion of(EncodedMechanicalPattern encoded) {
        List<GenericStack> cells = new ArrayList<>(encoded.cells().size());
        for (GenericStack cell : encoded.cells()) {
            if (cell != null) {
                cells.add(cell);
            }
        }
        return build(cells, encoded.result(), encoded.containsMissingContent(), null);
    }

    @Nullable
    private static Conversion build(
            List<GenericStack> inputs, GenericStack result, boolean missingContent, @Nullable ResourceLocation recipe) {
        if (missingContent || result.amount() <= 0) {
            return null;
        }
        List<GenericStack> condensed = PatternContents.condense(inputs);
        if (condensed.isEmpty() || condensed.size() > AEProcessingPattern.MAX_INPUT_SLOTS) {
            return null;
        }
        for (GenericStack input : condensed) {
            if (input.amount() <= 0) {
                return null;
            }
        }
        return new Conversion(condensed, List.of(result), recipe);
    }
}
