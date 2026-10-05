package dev.rylex.nep.compat.draconic;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import com.brandon3055.draconicevolution.api.crafting.IFusionRecipe;
import dev.rylex.nep.pattern.InputSubstitution;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

final class FusionInputSubstitution {
    private FusionInputSubstitution() {}

    @Nullable
    static InputSubstitution of(ResourceLocation recipe, Level level) {
        RecipeHolder<IFusionRecipe> holder = FusionRecipeResolver.resolveById(level, recipe);
        if (holder == null) {
            return null;
        }
        List<Ingredient> ingredients = ingredientsOf(holder.value());
        return (template, candidate, ignored) -> accepts(ingredients, template, candidate);
    }

    static List<Ingredient> ingredientsOf(IFusionRecipe recipe) {
        List<Ingredient> ingredients = new ArrayList<>();
        ingredients.add(recipe.getCatalyst());
        for (IFusionRecipe.IFusionIngredient ingredient : recipe.fusionIngredients()) {
            ingredients.add(ingredient.get());
        }
        return List.copyOf(ingredients);
    }

    private static boolean accepts(List<Ingredient> ingredients, AEKey template, AEKey candidate) {
        if (!(template instanceof AEItemKey wanted) || !(candidate instanceof AEItemKey offered)) {
            return false;
        }
        if (wanted.getItem() != offered.getItem()) {
            return false;
        }
        ItemStack wantedStack = wanted.toStack();
        ItemStack offeredStack = offered.toStack();
        for (Ingredient ingredient : ingredients) {
            if (ingredient.test(wantedStack) && ingredient.test(offeredStack)) {
                return true;
            }
        }
        return false;
    }
}
