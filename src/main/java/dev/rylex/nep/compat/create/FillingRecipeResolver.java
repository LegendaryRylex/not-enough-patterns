package dev.rylex.nep.compat.create;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.fluids.transfer.FillingRecipe;
import com.simibubi.create.content.fluids.transfer.GenericItemFilling;
import dev.rylex.nep.pattern.AndesiteCraftingPattern;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import org.jetbrains.annotations.Nullable;

final class FillingRecipeResolver {
    private FillingRecipeResolver() {}

    record Plan(AEItemKey depotItem, AEFluidKey fluid, long fluidAmount, Map<AEItemKey, Long> expectedItems) {}

    private static final Map<AEItemKey, Optional<Plan>> CACHE = new HashMap<>();

    static void clearCache() {
        CACHE.clear();
    }

    @Nullable
    static Plan resolve(IPatternDetails pattern, Level level) {
        return CACHE.computeIfAbsent(pattern.getDefinition(), def -> Optional.ofNullable(compute(pattern, level)))
                .orElse(null);
    }

    @Nullable
    private static Plan compute(IPatternDetails pattern, Level level) {
        List<GenericStack> outputs = pattern.getOutputs();
        if (outputs.size() != 1 || !(outputs.get(0).what() instanceof AEItemKey outputKey)) {
            return null;
        }
        long outputCount = outputs.get(0).amount();

        AEItemKey depotItem = null;
        long depotCount = 0;
        AEFluidKey fluid = null;
        long fluidAmount = 0;
        for (IPatternDetails.IInput input : pattern.getInputs()) {
            GenericStack primary = input.getPossibleInputs()[0];
            long amount = primary.amount() * input.getMultiplier();
            if (primary.what() instanceof AEItemKey itemKey) {
                if (depotItem != null) {
                    return null;
                }
                depotItem = itemKey;
                depotCount = amount;
            } else if (primary.what() instanceof AEFluidKey fluidKey) {
                if (fluid != null) {
                    return null;
                }
                fluid = fluidKey;
                fluidAmount = amount;
            } else {
                return null;
            }
        }
        if (depotItem == null || depotCount != 1 || fluid == null || fluidAmount <= 0) {
            return null;
        }

        ItemStack expected = outputKey.toStack();
        ItemStack depotStack = depotItem.toStack();
        if (pattern instanceof AndesiteCraftingPattern andesite) {
            RecipeHolder<?> holder =
                    level.getRecipeManager().byKey(andesite.recipe()).orElse(null);
            if (holder == null
                    || !(holder.value() instanceof FillingRecipe recipe)
                    || !matches(holder, recipe, depotStack, fluid, fluidAmount, expected, outputCount)) {
                return null;
            }
            return new Plan(depotItem, fluid, fluidAmount, Map.of(depotItem, 1L));
        }

        for (RecipeHolder<FillingRecipe> holder : level.getRecipeManager().getAllRecipesFor(recipeType())) {
            if (matches(holder, holder.value(), depotStack, fluid, fluidAmount, expected, outputCount)) {
                return new Plan(depotItem, fluid, fluidAmount, Map.of(depotItem, 1L));
            }
        }

        if (matchesGenericFill(level, depotStack, fluid, fluidAmount, expected, outputCount)) {
            return new Plan(depotItem, fluid, fluidAmount, Map.of(depotItem, 1L));
        }
        return null;
    }

    private static boolean matches(
            RecipeHolder<?> holder,
            FillingRecipe recipe,
            ItemStack depotStack,
            AEFluidKey fluid,
            long fluidAmount,
            ItemStack expected,
            long outputCount) {
        if (!AllRecipeTypes.CAN_BE_AUTOMATED.test(holder)) {
            return false;
        }
        if (!CreateRecipeIngredients.matchesSingleResult(recipe.getRollableResults(), expected, outputCount)) {
            return false;
        }
        if (recipe.getIngredients().isEmpty() || !recipe.getIngredients().get(0).test(depotStack)) {
            return false;
        }
        SizedFluidIngredient required = recipe.getRequiredFluid();
        return required.amount() == fluidAmount && required.ingredient().test(fluid.toStack(1));
    }

    private static boolean matchesGenericFill(
            Level level, ItemStack item, AEFluidKey fluid, long amount, ItemStack expected, long expectedCount) {
        if (amount <= 0 || amount > Integer.MAX_VALUE) {
            return false;
        }
        if (!GenericItemFilling.canItemBeFilled(level, item)) {
            return false;
        }
        int required = GenericItemFilling.getRequiredAmountForItem(level, item, fluid.toStack(Integer.MAX_VALUE));
        if (required <= 0 || required != amount) {
            return false;
        }
        ItemStack result = GenericItemFilling.fillItem(level, required, item.copy(), fluid.toStack(required));
        return !result.isEmpty()
                && result.getCount() == expectedCount
                && ItemStack.isSameItemSameComponents(result, expected);
    }

    static List<RecipeHolder<FillingRecipe>> candidates(Level level) {
        return level.getRecipeManager().getAllRecipesFor(recipeType());
    }

    private static RecipeType<FillingRecipe> recipeType() {
        return AllRecipeTypes.FILLING.<SingleRecipeInput, FillingRecipe>getType();
    }
}
