package dev.rylex.nep.compat.compactcrafting;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import dev.compactmods.crafting.api.field.MiniaturizationFieldSize;
import dev.compactmods.crafting.core.CCMiniaturizationRecipes;
import dev.compactmods.crafting.recipes.MiniaturizationRecipe;
import dev.rylex.nep.pattern.PatternStacks;
import dev.rylex.nep.util.RecipeCache;
import dev.rylex.nep.util.Uniqueness;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

final class MiniaturizationRecipeResolver {
    private MiniaturizationRecipeResolver() {}

    private static final RecipeCache<List<RecipeHolder<MiniaturizationRecipe>>> CANDIDATE_CACHE =
            RecipeCache.of(level -> List.copyOf(level.getRecipeManager().getAllRecipesFor(recipeType())));

    static void clearCache() {
        CANDIDATE_CACHE.clear();
    }

    static List<RecipeHolder<MiniaturizationRecipe>> candidates(Level level) {
        return CANDIDATE_CACHE.get(level);
    }

    @SuppressWarnings("unchecked")
    private static RecipeType<MiniaturizationRecipe> recipeType() {
        return (RecipeType<MiniaturizationRecipe>) CCMiniaturizationRecipes.MINIATURIZATION_RECIPE.get();
    }

    @Nullable
    static RecipeHolder<MiniaturizationRecipe> resolveById(Level level, ResourceLocation recipe) {
        RecipeHolder<?> holder = level.getRecipeManager().byKey(recipe).orElse(null);
        return holder != null && holder.value() instanceof MiniaturizationRecipe ? cast(holder) : null;
    }

    @Nullable
    static RecipeHolder<MiniaturizationRecipe> resolveByOutputItem(Level level, Item result) {
        return Uniqueness.onlyMatch(candidates(level), candidate -> {
            GenericStack output = MiniaturizationRecipeIngredients.singleOutput(candidate.value());
            return output != null && output.what() instanceof AEItemKey key && key.getItem() == result
                    ? candidate
                    : null;
        });
    }

    static MiniaturizationFieldSize fieldSizeOf(MiniaturizationRecipe recipe) {
        for (MiniaturizationFieldSize size : MiniaturizationFieldSize.VALID_SIZES) {
            if (recipe.fitsInFieldSize(size)) {
                return size;
            }
        }
        return MiniaturizationFieldSize.maximum();
    }

    @SuppressWarnings("unchecked")
    private static RecipeHolder<MiniaturizationRecipe> cast(RecipeHolder<?> holder) {
        return (RecipeHolder<MiniaturizationRecipe>) holder;
    }

    @Nullable
    static GenericStack singleResult(IPatternDetails pattern) {
        return PatternStacks.singleResult(pattern);
    }

    @Nullable
    static List<GenericStack> condensedInputs(IPatternDetails pattern) {
        return PatternStacks.condensedInputs(pattern);
    }
}
