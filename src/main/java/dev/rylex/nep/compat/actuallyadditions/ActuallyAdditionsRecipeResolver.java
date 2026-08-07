package dev.rylex.nep.compat.actuallyadditions;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import de.ellpeck.actuallyadditions.mod.crafting.ActuallyRecipes;
import de.ellpeck.actuallyadditions.mod.crafting.EmpowererRecipe;
import de.ellpeck.actuallyadditions.mod.crafting.LaserRecipe;
import dev.rylex.nep.pattern.PatternStacks;
import dev.rylex.nep.pattern.encoding.EncodedIngredients;
import dev.rylex.nep.pattern.encoding.IngredientMatching;
import dev.rylex.nep.util.RecipeCache;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

final class ActuallyAdditionsRecipeResolver {
    private ActuallyAdditionsRecipeResolver() {}

    private static final RecipeCache<List<RecipeHolder<EmpowererRecipe>>> EMPOWERING_CACHE = RecipeCache.of(
            level -> List.copyOf(level.getRecipeManager().getAllRecipesFor(ActuallyRecipes.Types.EMPOWERING.get())));

    private static final RecipeCache<List<RecipeHolder<LaserRecipe>>> LASER_CACHE = RecipeCache.of(
            level -> List.copyOf(level.getRecipeManager().getAllRecipesFor(ActuallyRecipes.Types.LASER.get())));

    record EmpoweringPlan(
            RecipeHolder<EmpowererRecipe> holder,
            long batch,
            GenericStack base,
            List<GenericStack> modifiers,
            Map<AEItemKey, Long> expectedItems,
            GenericStack result) {

        int energyPerStand() {
            return holder.value().getEnergyPerStand();
        }

        int time() {
            return holder.value().getTime();
        }

        long energyCost() {
            return (long) energyPerStand() * 4L;
        }
    }

    record LaserPlan(
            RecipeHolder<LaserRecipe> holder,
            long batch,
            GenericStack input,
            Map<AEItemKey, Long> expectedItems,
            GenericStack result) {

        long energyCost() {
            return (long) holder.value().getEnergy();
        }
    }

    static void clearCache() {
        EMPOWERING_CACHE.clear();
        LASER_CACHE.clear();
    }

    static List<RecipeHolder<EmpowererRecipe>> empoweringCandidates(Level level) {
        return EMPOWERING_CACHE.get(level);
    }

    static List<RecipeHolder<LaserRecipe>> laserCandidates(Level level) {
        return LASER_CACHE.get(level);
    }

    @Nullable
    static RecipeHolder<EmpowererRecipe> empoweringById(Level level, ResourceLocation recipe) {
        RecipeHolder<?> holder = level.getRecipeManager().byKey(recipe).orElse(null);
        return holder != null && holder.value() instanceof EmpowererRecipe ? cast(holder) : null;
    }

    @Nullable
    static RecipeHolder<LaserRecipe> laserById(Level level, ResourceLocation recipe) {
        RecipeHolder<?> holder = level.getRecipeManager().byKey(recipe).orElse(null);
        return holder != null && holder.value() instanceof LaserRecipe ? cast(holder) : null;
    }

    @Nullable
    static EmpoweringPlan planEmpowering(RecipeHolder<EmpowererRecipe> holder, IPatternDetails pattern) {
        EncodedIngredients expected = ActuallyAdditionsRecipeIngredients.empowering(holder.value());
        Batch batch = batchOf(expected, pattern);
        if (batch == null) {
            return null;
        }
        GenericStack[] chosen = IngredientMatching.assign(expected.inputs(), batch.unitInputs());
        if (chosen == null) {
            return null;
        }
        return new EmpoweringPlan(
                holder,
                batch.count(),
                chosen[0],
                List.of(chosen[1], chosen[2], chosen[3], chosen[4]),
                batch.expectedItems(),
                batch.result());
    }

    @Nullable
    static LaserPlan planLaser(RecipeHolder<LaserRecipe> holder, IPatternDetails pattern, Level level) {
        EncodedIngredients expected = ActuallyAdditionsRecipeIngredients.laser(holder.value(), level);
        Batch batch = batchOf(expected, pattern);
        if (batch == null) {
            return null;
        }
        GenericStack[] chosen = IngredientMatching.assign(expected.inputs(), batch.unitInputs());
        if (chosen == null) {
            return null;
        }
        return new LaserPlan(holder, batch.count(), chosen[0], batch.expectedItems(), batch.result());
    }

    private record Batch(
            long count, List<GenericStack> unitInputs, Map<AEItemKey, Long> expectedItems, GenericStack result) {}

    @Nullable
    private static Batch batchOf(@Nullable EncodedIngredients expected, IPatternDetails pattern) {
        List<GenericStack> inputs = PatternStacks.condensedInputs(pattern);
        GenericStack result = PatternStacks.singleResult(pattern);
        if (expected == null || expected.outputs().size() != 1 || inputs == null || result == null) {
            return null;
        }

        GenericStack unitResult = expected.outputs().get(0);
        if (!unitResult.what().equals(result.what()) || result.amount() % unitResult.amount() != 0) {
            return null;
        }
        long count = result.amount() / unitResult.amount();
        if (count <= 0) {
            return null;
        }

        List<GenericStack> unitInputs = new ArrayList<>(inputs.size());
        Map<AEItemKey, Long> expectedItems = new LinkedHashMap<>();
        for (GenericStack input : inputs) {
            if (!(input.what() instanceof AEItemKey key) || input.amount() % count != 0) {
                return null;
            }
            unitInputs.add(new GenericStack(key, input.amount() / count));
            expectedItems.merge(key, input.amount() / count, Long::sum);
        }
        return new Batch(count, List.copyOf(unitInputs), Map.copyOf(expectedItems), unitResult);
    }

    @SuppressWarnings("unchecked")
    private static <R extends Recipe<?>> RecipeHolder<R> cast(RecipeHolder<?> holder) {
        return (RecipeHolder<R>) holder;
    }
}
