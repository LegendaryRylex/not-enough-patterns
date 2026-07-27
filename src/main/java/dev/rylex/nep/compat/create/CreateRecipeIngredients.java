package dev.rylex.nep.compat.create;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import com.simibubi.create.content.fluids.transfer.FillingRecipe;
import com.simibubi.create.content.kinetics.deployer.ItemApplicationRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import dev.rylex.nep.pattern.encoding.EncodedIngredients;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import org.jetbrains.annotations.Nullable;

final class CreateRecipeIngredients {
    private CreateRecipeIngredients() {}

    @Nullable
    static EncodedIngredients mechanicalCrafting(RecipeHolder<CraftingRecipe> holder, Level level) {
        CraftingRecipe recipe = holder.value();
        GenericStack result = resultOf(recipe.getResultItem(level.registryAccess()));
        if (result == null) {
            return null;
        }

        Map<List<AEItemKey>, Long> counts = new LinkedHashMap<>();
        for (Ingredient ingredient : recipe.getIngredients()) {
            if (ingredient.isEmpty()) {
                continue;
            }
            List<AEItemKey> options = itemOptions(ingredient);
            if (options.isEmpty()) {
                return null;
            }
            counts.merge(options, 1L, Long::sum);
        }
        if (counts.isEmpty()) {
            return null;
        }

        List<List<GenericStack>> inputs = new ArrayList<>(counts.size());
        for (Map.Entry<List<AEItemKey>, Long> entry : counts.entrySet()) {
            List<GenericStack> options = new ArrayList<>(entry.getKey().size());
            for (AEItemKey key : entry.getKey()) {
                options.add(new GenericStack(key, entry.getValue()));
            }
            inputs.add(List.copyOf(options));
        }
        return new EncodedIngredients(List.copyOf(inputs), List.of(result));
    }

    @Nullable
    static EncodedIngredients sequencedAssembly(RecipeHolder<SequencedAssemblyRecipe> holder, Level level) {
        SequencedAssemblyRecipe recipe = holder.value();
        GenericStack result = resultOf(recipe.getResultItem(level.registryAccess()));
        if (result == null) {
            return null;
        }

        SequencedAssemblyResolver.Demand demand = SequencedAssemblyResolver.demandOf(recipe);
        List<List<GenericStack>> inputs = new ArrayList<>();

        for (SequencedAssemblyResolver.ItemDemand item : demand.items()) {
            List<GenericStack> options = new ArrayList<>();
            for (AEItemKey key : itemOptions(item.ingredient())) {
                options.add(new GenericStack(key, item.count()));
            }
            if (!options.isEmpty()) {
                inputs.add(List.copyOf(options));
            }
        }

        for (SequencedAssemblyResolver.FluidDemand fluid : demand.fluids()) {
            List<GenericStack> options = new ArrayList<>();
            for (FluidStack stack : fluid.ingredient().getStacks()) {
                AEFluidKey key = AEFluidKey.of(stack);
                if (key != null) {
                    options.add(new GenericStack(key, fluid.amount()));
                }
            }
            if (!options.isEmpty()) {
                inputs.add(List.copyOf(options));
            }
        }

        if (inputs.isEmpty()) {
            return null;
        }
        return new EncodedIngredients(List.copyOf(inputs), List.of(result));
    }

    @Nullable
    static EncodedIngredients itemApplication(RecipeHolder<? extends ItemApplicationRecipe> holder, Level level) {
        ItemApplicationRecipe recipe = holder.value();
        GenericStack result = singleRollableResult(recipe.getRollableResults());
        if (result == null) {
            return null;
        }

        List<AEItemKey> processed = itemOptions(recipe.getProcessedItem());
        if (processed.isEmpty()) {
            return null;
        }
        List<List<GenericStack>> inputs = new ArrayList<>();
        inputs.add(options(processed, 1));

        if (!recipe.shouldKeepHeldItem()) {
            List<AEItemKey> held = itemOptions(recipe.getRequiredHeldItem());
            if (held.isEmpty()) {
                return null;
            }
            inputs.add(options(held, 1));
        }
        return new EncodedIngredients(List.copyOf(inputs), List.of(result));
    }

    @Nullable
    static EncodedIngredients spoutFilling(RecipeHolder<FillingRecipe> holder, Level level) {
        FillingRecipe recipe = holder.value();
        GenericStack result = singleRollableResult(recipe.getRollableResults());
        if (result == null) {
            return null;
        }
        if (recipe.getIngredients().isEmpty()) {
            return null;
        }

        List<AEItemKey> processed = itemOptions(recipe.getIngredients().get(0));
        if (processed.isEmpty()) {
            return null;
        }

        SizedFluidIngredient required = recipe.getRequiredFluid();
        if (required == null || required.amount() <= 0) {
            return null;
        }
        List<GenericStack> fluids = new ArrayList<>();
        for (FluidStack stack : required.ingredient().getStacks()) {
            AEFluidKey key = AEFluidKey.of(stack);
            if (key != null) {
                fluids.add(new GenericStack(key, required.amount()));
            }
        }
        if (fluids.isEmpty()) {
            return null;
        }

        return new EncodedIngredients(List.of(options(processed, 1), List.copyOf(fluids)), List.of(result));
    }

    static boolean satisfies(EncodedIngredients expected, List<GenericStack> actualInputs) {
        Map<AEKey, Long> remaining = new LinkedHashMap<>();
        for (GenericStack stack : actualInputs) {
            remaining.merge(stack.what(), stack.amount(), Long::sum);
        }

        List<List<GenericStack>> slots = new ArrayList<>(expected.inputs());
        slots.sort(Comparator.comparingInt(slot -> matchingOptions(remaining, slot)));
        return assign(remaining, slots, 0, new int[] {ASSIGN_BUDGET});
    }

    private static final int ASSIGN_BUDGET = 1 << 16;

    private static boolean assign(Map<AEKey, Long> remaining, List<List<GenericStack>> slots, int index, int[] budget) {
        if (index == slots.size()) {
            return remaining.isEmpty();
        }
        if (--budget[0] < 0) {
            return false;
        }
        for (GenericStack option : slots.get(index)) {
            Long held = remaining.get(option.what());
            if (held == null || held < option.amount()) {
                continue;
            }
            long left = held - option.amount();
            if (left == 0) {
                remaining.remove(option.what());
            } else {
                remaining.put(option.what(), left);
            }
            if (assign(remaining, slots, index + 1, budget)) {
                return true;
            }
            remaining.put(option.what(), held);
        }
        return false;
    }

    private static int matchingOptions(Map<AEKey, Long> remaining, List<GenericStack> options) {
        int matches = 0;
        for (GenericStack option : options) {
            if (remaining.containsKey(option.what())) {
                matches++;
            }
        }
        return matches;
    }

    private static List<GenericStack> options(List<AEItemKey> keys, long amount) {
        List<GenericStack> options = new ArrayList<>(keys.size());
        for (AEItemKey key : keys) {
            options.add(new GenericStack(key, amount));
        }
        return List.copyOf(options);
    }

    private static List<AEItemKey> itemOptions(Ingredient ingredient) {
        List<AEItemKey> keys = new ArrayList<>();
        for (ItemStack stack : ingredient.getItems()) {
            AEItemKey key = AEItemKey.of(stack);
            if (key != null) {
                keys.add(key);
            }
        }
        return List.copyOf(keys);
    }

    static boolean matchesSingleResult(List<ProcessingOutput> results, ItemStack expected, long expectedCount) {
        if (results.size() != 1) {
            return false;
        }
        ProcessingOutput output = results.get(0);
        if (output.getChance() < 1f) {
            return false;
        }
        ItemStack stack = output.getStack();
        return stack.getCount() == expectedCount && ItemStack.isSameItemSameComponents(stack, expected);
    }

    @Nullable
    static Map<AEItemKey, Long> flattenItemInputs(appeng.api.crafting.IPatternDetails pattern) {
        Map<AEItemKey, Long> available = new LinkedHashMap<>();
        for (appeng.api.crafting.IPatternDetails.IInput input : pattern.getInputs()) {
            GenericStack primary = input.getPossibleInputs()[0];
            if (!(primary.what() instanceof AEItemKey itemKey)) {
                return null;
            }
            available.merge(itemKey, primary.amount() * input.getMultiplier(), Long::sum);
        }
        return available.isEmpty() ? null : available;
    }

    @Nullable
    private static GenericStack singleRollableResult(List<ProcessingOutput> results) {
        if (results.size() != 1 || results.get(0).getChance() < 1f) {
            return null;
        }
        return resultOf(results.get(0).getStack());
    }

    @Nullable
    private static GenericStack resultOf(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        AEItemKey key = AEItemKey.of(stack);
        return key == null ? null : new GenericStack(key, Math.max(1, stack.getCount()));
    }
}
