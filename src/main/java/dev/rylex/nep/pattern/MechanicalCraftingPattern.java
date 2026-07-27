package dev.rylex.nep.pattern;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import dev.rylex.nep.NepComponents;
import dev.rylex.nep.NepItems;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class MechanicalCraftingPattern implements NepPattern {

    private final AEItemKey definition;
    private final GridPlan plan;
    private final PatternInput[] inputs;
    private final List<GenericStack> outputs;

    public MechanicalCraftingPattern(AEItemKey definition, Level level) {
        this.definition = definition;
        EncodedMechanicalPattern encoded = definition.get(NepComponents.ENCODED_MECHANICAL_PATTERN.get());
        if (encoded == null) {
            throw new IllegalArgumentException(
                    "Given item does not encode a mechanical crafting pattern: " + definition);
        }
        if (encoded.containsMissingContent()) {
            throw new IllegalArgumentException("Pattern references missing content");
        }
        if (encoded.width() < 1 || encoded.height() < 1) {
            throw new IllegalArgumentException("Pattern has invalid dimensions");
        }
        if (encoded.cells().size() != encoded.width() * encoded.height()) {
            throw new IllegalArgumentException("Pattern cell count does not match its dimensions");
        }
        if (encoded.result().amount() <= 0) {
            throw new IllegalArgumentException("Pattern has no result");
        }

        List<AEItemKey> cells = new ArrayList<>(encoded.cells().size());
        Map<AEItemKey, Long> counts = new LinkedHashMap<>();
        for (GenericStack cell : encoded.cells()) {
            if (cell == null) {
                cells.add(null);
                continue;
            }
            if (!(cell.what() instanceof AEItemKey itemKey)) {
                throw new IllegalArgumentException("Pattern has a non-item ingredient");
            }
            if (cell.amount() != 1) {
                throw new IllegalArgumentException("Pattern cell amounts must be 1");
            }
            cells.add(itemKey);
            counts.merge(itemKey, 1L, Long::sum);
        }
        if (counts.isEmpty()) {
            throw new IllegalArgumentException("Pattern has no ingredients");
        }
        this.plan = new GridPlan(encoded.width(), encoded.height(), cells);
        this.inputs = counts.entrySet().stream()
                .map(entry -> new PatternInput(entry.getKey(), entry.getValue()))
                .toArray(PatternInput[]::new);
        this.outputs = List.of(encoded.result());
    }

    public static ItemStack encode(int width, int height, List<@Nullable GenericStack> cells, GenericStack result) {
        ItemStack stack = new ItemStack(NepItems.MECHANICAL_CRAFTING_PATTERN.get());
        stack.set(
                NepComponents.ENCODED_MECHANICAL_PATTERN.get(),
                new EncodedMechanicalPattern(width, height, cells, result));
        return stack;
    }

    public GridPlan plan() {
        return plan;
    }

    @Override
    public AEItemKey getDefinition() {
        return definition;
    }

    @Override
    public IInput[] getInputs() {
        return inputs;
    }

    @Override
    public List<GenericStack> getOutputs() {
        return outputs;
    }

    @Override
    public boolean supportsPushInputsToExternalInventory() {
        return false;
    }

    @Override
    public int hashCode() {
        return definition.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null
                && obj.getClass() == getClass()
                && ((MechanicalCraftingPattern) obj).definition.equals(definition);
    }
}
