package dev.rylex.nep.pattern;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import java.util.List;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public abstract class RecipePattern implements NepPattern {

    private final AEItemKey definition;
    private final ResourceLocation recipe;
    private final PatternInput[] inputs;
    private final List<GenericStack> outputs;

    protected RecipePattern(AEItemKey definition, DataComponentType<EncodedRecipePattern> component, Level level) {
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
        if (level != null && level.getRecipeManager().byKey(encoded.recipe()).isEmpty()) {
            throw new IllegalArgumentException("Pattern references unknown recipe " + encoded.recipe());
        }

        this.recipe = encoded.recipe();
        this.inputs = new PatternInput[encoded.inputs().size()];
        for (int i = 0; i < inputs.length; i++) {
            GenericStack input = encoded.inputs().get(i);
            if (input.amount() <= 0) {
                throw new IllegalArgumentException("Pattern has an empty ingredient");
            }
            inputs[i] = new PatternInput(input.what(), input.amount());
        }
        this.outputs = List.of(encoded.result());
    }

    protected static ItemStack encode(
            Item item,
            DataComponentType<EncodedRecipePattern> component,
            ResourceLocation recipe,
            List<GenericStack> inputs,
            GenericStack result) {
        ItemStack stack = new ItemStack(item);
        stack.set(component, new EncodedRecipePattern(recipe, List.copyOf(inputs), result));
        return stack;
    }

    public ResourceLocation recipe() {
        return recipe;
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
    public int hashCode() {
        return definition.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && obj.getClass() == getClass() && ((RecipePattern) obj).definition.equals(definition);
    }
}
