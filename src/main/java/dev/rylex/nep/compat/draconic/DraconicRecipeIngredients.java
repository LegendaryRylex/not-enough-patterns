package dev.rylex.nep.compat.draconic;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import com.brandon3055.draconicevolution.api.crafting.IFusionDataTransfer;
import com.brandon3055.draconicevolution.api.crafting.IFusionRecipe;
import com.brandon3055.draconicevolution.api.crafting.StackIngredient;
import dev.rylex.nep.pattern.encoding.EncodedIngredients;
import dev.rylex.nep.pattern.encoding.IngredientMatching;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

final class DraconicRecipeIngredients {
    private DraconicRecipeIngredients() {}

    @Nullable
    static EncodedIngredients fusion(RecipeHolder<IFusionRecipe> holder, Level level) {
        return fusion(holder, level, true);
    }

    @Nullable
    static EncodedIngredients fusion(RecipeHolder<IFusionRecipe> holder, Level level, boolean includeRetained) {
        IFusionRecipe recipe = holder.value();
        if (!FusionResults.producesAStableResult(recipe, holder.id(), level)) {
            return null;
        }
        GenericStack result = IngredientMatching.resultOf(FusionResults.expectedResult(recipe, level));
        if (result == null) {
            return null;
        }

        List<List<GenericStack>> inputs = new ArrayList<>();
        Set<Integer> retainedSlots = new LinkedHashSet<>();

        List<AEItemKey> catalyst = itemOptions(recipe.getCatalyst(), level);
        if (catalyst.isEmpty()) {
            return null;
        }
        inputs.add(IngredientMatching.options(catalyst, requiredCount(recipe.getCatalyst())));

        for (IFusionRecipe.IFusionIngredient ingredient : recipe.fusionIngredients()) {
            if (!ingredient.consume() && !includeRetained) {
                continue;
            }
            if (requiredCount(ingredient.get()) != 1) {
                return null;
            }
            List<AEItemKey> options = itemOptions(ingredient.get(), level);
            if (options.isEmpty()) {
                return null;
            }
            if (!ingredient.consume()) {
                retainedSlots.add(inputs.size());
            }
            inputs.add(IngredientMatching.options(options, 1));
        }

        return new EncodedIngredients(List.copyOf(inputs), List.of(result), retainedSlots);
    }

    private static List<AEItemKey> itemOptions(Ingredient ingredient, Level level) {
        List<AEItemKey> options = new ArrayList<>();
        for (AEItemKey key : IngredientMatching.itemOptions(ingredient)) {
            AEItemKey canonical = FusionResults.canonical(key, level);
            if (!options.contains(canonical)) {
                options.add(canonical);
            }
            if (!options.contains(key)) {
                options.add(key);
            }
        }
        return List.copyOf(options);
    }

    static boolean carriesIngredientData(ItemStack result) {
        return result.getItem() instanceof IFusionDataTransfer;
    }

    static int requiredCount(Ingredient ingredient) {
        return ingredient.getCustomIngredient() instanceof StackIngredient stack ? stack.getCount() : 1;
    }

    record Demand(Ingredient ingredient, int count, boolean consume) {}

    @Nullable
    static List<Demand> demandOf(IFusionRecipe recipe) {
        List<Demand> demands = new ArrayList<>();
        demands.add(new Demand(recipe.getCatalyst(), requiredCount(recipe.getCatalyst()), true));
        for (IFusionRecipe.IFusionIngredient ingredient : recipe.fusionIngredients()) {
            if (requiredCount(ingredient.get()) != 1) {
                return null;
            }
            demands.add(new Demand(ingredient.get(), 1, ingredient.consume()));
        }
        return List.copyOf(demands);
    }
}
