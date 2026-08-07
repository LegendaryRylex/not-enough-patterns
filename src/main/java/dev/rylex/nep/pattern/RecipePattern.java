package dev.rylex.nep.pattern;

import appeng.api.crafting.PatternDetailsTooltip;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import dev.rylex.nep.pattern.encoding.PatternContents;
import dev.rylex.nep.pattern.encoding.SyntheticRecipes;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public abstract class RecipePattern implements NepPattern {

    private final AEItemKey definition;
    private final ResourceLocation recipe;
    private final PatternInput[] inputs;
    private final List<GenericStack> consumed;
    private final List<GenericStack> retained;
    private final List<GenericStack> outputs;

    protected RecipePattern(AEItemKey definition, DataComponentType<EncodedRecipePattern> component, Level level) {
        this(definition, component, level, UnaryOperator.identity());
    }

    protected RecipePattern(
            AEItemKey definition,
            DataComponentType<EncodedRecipePattern> component,
            Level level,
            UnaryOperator<GenericStack> adapt) {
        this.definition = definition;
        EncodedRecipePattern encoded = definition.get(component);
        if (encoded == null) {
            throw new IllegalArgumentException("Given item does not encode a recipe pattern: " + definition);
        }
        if (encoded.containsMissingContent()) {
            throw new IllegalArgumentException("Pattern references missing content");
        }
        if (encoded.inputs().isEmpty()) {
            throw new IllegalArgumentException("Pattern has no ingredients");
        }
        if (encoded.result().amount() <= 0) {
            throw new IllegalArgumentException("Pattern has no result");
        }
        if (level != null
                && level.getRecipeManager().byKey(encoded.recipe()).isEmpty()
                && !SyntheticRecipes.known(encoded.recipe(), level)) {
            throw new IllegalArgumentException("Pattern references unknown recipe " + encoded.recipe());
        }

        for (GenericStack input : encoded.inputs()) {
            if (input.amount() <= 0) {
                throw new IllegalArgumentException("Pattern has an empty ingredient");
            }
        }
        for (GenericStack kept : encoded.retained()) {
            if (kept.amount() <= 0) {
                throw new IllegalArgumentException("Pattern has an empty kept ingredient");
            }
        }

        this.recipe = encoded.recipe();
        this.consumed = PatternContents.condense(adapt(encoded.inputs(), adapt));
        this.retained = PatternContents.condense(adapt(encoded.retained(), adapt));
        this.inputs = new PatternInput[consumed.size() + retained.size()];
        int slot = 0;
        for (GenericStack input : consumed) {
            inputs[slot++] = new PatternInput(input.what(), input.amount(), false);
        }
        for (GenericStack kept : retained) {
            inputs[slot++] = new PatternInput(kept.what(), kept.amount(), true);
        }
        this.outputs = List.of(adapt.apply(encoded.result()));
    }

    private static List<GenericStack> adapt(List<GenericStack> stacks, UnaryOperator<GenericStack> adapt) {
        List<GenericStack> adapted = new ArrayList<>(stacks.size());
        for (GenericStack stack : stacks) {
            adapted.add(adapt.apply(stack));
        }
        return adapted;
    }

    protected static ItemStack encode(
            Item item,
            DataComponentType<EncodedRecipePattern> component,
            ResourceLocation recipe,
            List<GenericStack> inputs,
            GenericStack result) {
        return encode(item, component, recipe, inputs, List.of(), result);
    }

    protected static ItemStack encode(
            Item item,
            DataComponentType<EncodedRecipePattern> component,
            ResourceLocation recipe,
            List<GenericStack> inputs,
            List<GenericStack> retained,
            GenericStack result) {
        ItemStack stack = new ItemStack(item);
        stack.set(component, new EncodedRecipePattern(recipe, List.copyOf(inputs), List.copyOf(retained), result));
        return stack;
    }

    public ResourceLocation recipe() {
        return recipe;
    }

    public List<GenericStack> retained() {
        return retained;
    }

    @Override
    public ResourceLocation nepRecipeId() {
        return recipe;
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
    public PatternDetailsTooltip getTooltip(Level level, TooltipFlag flags) {
        PatternDetailsTooltip tooltip = new PatternDetailsTooltip(PatternDetailsTooltip.OUTPUT_TEXT_PRODUCES);
        for (GenericStack output : outputs) {
            tooltip.addOutput(output);
        }
        for (GenericStack input : consumed) {
            tooltip.addInput(input);
        }
        RetainedInputs.describe(tooltip, retained);
        return tooltip;
    }

    @Override
    public int hashCode() {
        return definition.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && obj.getClass() == getClass() && ((RecipePattern) obj).definition.equals(definition);
    }
}
