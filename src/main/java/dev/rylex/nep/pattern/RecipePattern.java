package dev.rylex.nep.pattern;

import appeng.api.crafting.PatternDetailsTooltip;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import dev.rylex.nep.pattern.encoding.PatternContents;
import dev.rylex.nep.pattern.encoding.SyntheticRecipes;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public abstract class RecipePattern implements NepPattern {

    private final AEItemKey definition;
    private final ResourceLocation recipe;
    private final PatternInput[] inputs;
    private final List<GenericStack> consumed;
    private final List<GenericStack> kept;
    private final List<GenericStack> worn;
    private final List<GenericStack> retained;
    private final List<GenericStack> outputs;

    protected RecipePattern(AEItemKey definition, DataComponentType<EncodedRecipePattern> component, Level level) {
        this(definition, component, level, UnaryOperator.identity(), null);
    }

    protected RecipePattern(
            AEItemKey definition,
            DataComponentType<EncodedRecipePattern> component,
            Level level,
            UnaryOperator<GenericStack> adapt,
            @Nullable InputSubstitution substitution) {
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

        requirePositive(encoded.inputs(), "Pattern has an empty ingredient");
        requirePositive(encoded.retained(), "Pattern has an empty kept ingredient");
        requirePositive(encoded.worn(), "Pattern has an empty worn ingredient");

        this.recipe = encoded.recipe();
        this.consumed = PatternContents.condense(adapt(encoded.inputs(), adapt));
        this.kept = PatternContents.condense(adapt(encoded.retained(), adapt));
        this.worn = PatternContents.condense(adapt(encoded.worn(), adapt));
        List<GenericStack> returned = new ArrayList<>(kept.size() + worn.size());
        returned.addAll(kept);
        returned.addAll(worn);
        this.retained = List.copyOf(returned);
        this.inputs = new PatternInput[consumed.size() + retained.size()];
        int slot = 0;
        for (GenericStack input : consumed) {
            inputs[slot++] = new PatternInput(input.what(), input.amount(), false, false, substitution);
        }
        for (GenericStack stack : kept) {
            inputs[slot++] = new PatternInput(stack.what(), stack.amount(), true, false, substitution);
        }
        for (GenericStack tool : worn) {
            inputs[slot++] = new PatternInput(tool.what(), tool.amount(), true, true, substitution);
        }
        this.outputs = List.of(adapt.apply(encoded.result()));
    }

    private static void requirePositive(List<GenericStack> stacks, String message) {
        for (GenericStack stack : stacks) {
            if (stack.amount() <= 0) {
                throw new IllegalArgumentException(message);
            }
        }
    }

    @Nullable
    protected static InputSubstitution substitutionFor(
            DataComponentType<EncodedRecipePattern> component,
            AEItemKey definition,
            @Nullable Level level,
            BiFunction<ResourceLocation, Level, InputSubstitution> substitutions) {
        if (level == null) {
            return null;
        }
        EncodedRecipePattern encoded = definition.get(component);
        return encoded == null ? null : substitutions.apply(encoded.recipe(), level);
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
        return encode(item, component, recipe, inputs, retained, List.of(), result);
    }

    protected static ItemStack encode(
            Item item,
            DataComponentType<EncodedRecipePattern> component,
            ResourceLocation recipe,
            List<GenericStack> inputs,
            List<GenericStack> retained,
            List<GenericStack> worn,
            GenericStack result) {
        ItemStack stack = new ItemStack(item);
        stack.set(
                component,
                new EncodedRecipePattern(
                        recipe, List.copyOf(inputs), List.copyOf(retained), List.copyOf(worn), result));
        return stack;
    }

    public ResourceLocation recipe() {
        return recipe;
    }

    public List<GenericStack> retained() {
        return retained;
    }

    public List<GenericStack> worn() {
        return worn;
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
        RetainedInputs.describe(tooltip, kept);
        RetainedInputs.describeWorn(tooltip, worn);
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
