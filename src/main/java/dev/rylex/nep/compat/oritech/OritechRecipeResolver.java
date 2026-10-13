package dev.rylex.nep.compat.oritech;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import dev.rylex.nep.pattern.PatternStacks;
import dev.rylex.nep.pattern.encoding.EncodedIngredients;
import dev.rylex.nep.pattern.encoding.IngredientMatching;
import dev.rylex.nep.util.RecipeCache;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.init.recipes.OritechRecipe;
import rearth.oritech.init.recipes.RecipeContent;

final class OritechRecipeResolver {
    private OritechRecipeResolver() {}

    private static final RecipeCache<List<RecipeHolder<OritechRecipe>>> COLLISION_CACHE = RecipeCache.of(
            level -> List.copyOf(level.getRecipeManager().getAllRecipesFor(RecipeContent.PARTICLE_COLLISION)));

    /**
     * {@code minimumSpeed} is the summed velocity of both particles at impact, which Oritech stores in the recipe's
     * time field.
     */
    record CollisionPlan(
            RecipeHolder<OritechRecipe> holder,
            long batch,
            List<GenericStack> particles,
            Map<AEItemKey, Long> expectedItems,
            GenericStack result) {

        int minimumSpeed() {
            return holder.value().getTime();
        }
    }

    static void clearCache() {
        COLLISION_CACHE.clear();
    }

    static boolean isCollision(OritechRecipe recipe) {
        return recipe.getOriType() == RecipeContent.PARTICLE_COLLISION;
    }

    static List<RecipeHolder<OritechRecipe>> collisionCandidates(Level level) {
        return COLLISION_CACHE.get(level);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    static RecipeHolder<OritechRecipe> collisionById(Level level, ResourceLocation recipe) {
        RecipeHolder<?> holder = level.getRecipeManager().byKey(recipe).orElse(null);
        return holder != null && holder.value() instanceof OritechRecipe oritech && isCollision(oritech)
                ? (RecipeHolder<OritechRecipe>) holder
                : null;
    }

    @Nullable
    static CollisionPlan planCollision(RecipeHolder<OritechRecipe> holder, IPatternDetails pattern) {
        EncodedIngredients expected = OritechRecipeIngredients.collision(holder.value());
        List<GenericStack> inputs = PatternStacks.condensedInputs(pattern);
        GenericStack result = PatternStacks.singleResult(pattern);
        if (expected == null || inputs == null || result == null) {
            return null;
        }

        GenericStack unitResult = expected.outputs().get(0);
        if (!unitResult.what().equals(result.what()) || result.amount() % unitResult.amount() != 0) {
            return null;
        }
        long batch = result.amount() / unitResult.amount();
        if (batch <= 0) {
            return null;
        }

        List<GenericStack> unitInputs = new ArrayList<>(inputs.size());
        Map<AEItemKey, Long> expectedItems = new LinkedHashMap<>();
        for (GenericStack input : inputs) {
            if (!(input.what() instanceof AEItemKey key) || input.amount() % batch != 0) {
                return null;
            }
            unitInputs.add(new GenericStack(key, input.amount() / batch));
            expectedItems.merge(key, input.amount() / batch, Long::sum);
        }

        GenericStack[] chosen = IngredientMatching.assign(expected.inputs(), unitInputs);
        if (chosen == null) {
            return null;
        }
        return new CollisionPlan(holder, batch, List.of(chosen), Map.copyOf(expectedItems), unitResult);
    }
}
