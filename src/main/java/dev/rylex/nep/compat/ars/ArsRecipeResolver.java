package dev.rylex.nep.compat.ars;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import com.hollingsworth.arsnouveau.common.crafting.recipes.ApparatusRecipeInput;
import com.hollingsworth.arsnouveau.common.crafting.recipes.EnchantingApparatusRecipe;
import com.hollingsworth.arsnouveau.common.crafting.recipes.ImbuementRecipe;
import com.hollingsworth.arsnouveau.setup.registry.RecipeRegistry;
import dev.rylex.nep.pattern.ApparatusPattern;
import dev.rylex.nep.pattern.ImbuementPattern;
import dev.rylex.nep.pattern.PatternStacks;
import dev.rylex.nep.pattern.encoding.EncodedIngredients;
import dev.rylex.nep.pattern.encoding.IngredientMatching;
import dev.rylex.nep.util.RecipeCache;
import dev.rylex.nep.util.Uniqueness;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

final class ArsRecipeResolver {
    private ArsRecipeResolver() {}

    private static final RecipeCache<List<RecipeHolder<EnchantingApparatusRecipe>>> APPARATUS_CACHE =
            RecipeCache.of(ArsRecipeResolver::allApparatusRecipes);

    private static final RecipeCache<List<RecipeHolder<ImbuementRecipe>>> IMBUEMENT_CACHE =
            RecipeCache.of(level -> List.copyOf(level.getRecipeManager().getAllRecipesFor(imbuementType())));

    record ApparatusPlan(
            RecipeHolder<EnchantingApparatusRecipe> holder,
            GenericStack reagent,
            List<GenericStack> pedestals,
            Map<AEItemKey, Long> expectedItems,
            GenericStack result,
            int sourceCost) {}

    /** {@code catalystsInPattern} marks a pattern that also carries the pedestal items, which the chamber never uses up. */
    record ImbuementPlan(
            RecipeHolder<ImbuementRecipe> holder,
            GenericStack reagent,
            boolean catalystsInPattern,
            Map<AEItemKey, Long> expectedItems,
            GenericStack result,
            int sourceCost) {}

    static void clearCache() {
        APPARATUS_CACHE.clear();
        IMBUEMENT_CACHE.clear();
    }

    static RecipeType<EnchantingApparatusRecipe> apparatusType() {
        return RecipeRegistry.APPARATUS_TYPE.get();
    }

    static boolean runsOnApparatus(RecipeType<?> type) {
        return type == apparatusType() || opensReagent(type);
    }

    static boolean opensReagent(RecipeType<?> type) {
        return type == RecipeRegistry.ENCHANTMENT_TYPE.get() || type == RecipeRegistry.ARMOR_UPGRADE_TYPE.get();
    }

    static boolean upgradesArmor(RecipeType<?> type) {
        return type == RecipeRegistry.ARMOR_UPGRADE_TYPE.get();
    }

    private static List<RecipeHolder<EnchantingApparatusRecipe>> allApparatusRecipes(Level level) {
        List<RecipeHolder<EnchantingApparatusRecipe>> recipes = new ArrayList<>();
        for (RecipeType<? extends EnchantingApparatusRecipe> type : List.of(
                apparatusType(), RecipeRegistry.ENCHANTMENT_TYPE.get(), RecipeRegistry.ARMOR_UPGRADE_TYPE.get())) {
            for (RecipeHolder<?> holder : level.getRecipeManager().getAllRecipesFor(type)) {
                recipes.add(cast(holder));
            }
        }
        return List.copyOf(recipes);
    }

    static List<RecipeHolder<EnchantingApparatusRecipe>> apparatusCandidates(Level level) {
        return APPARATUS_CACHE.get(level);
    }

    @Nullable
    static RecipeHolder<EnchantingApparatusRecipe> apparatusById(Level level, ResourceLocation recipe) {
        RecipeHolder<?> holder = level.getRecipeManager().byKey(recipe).orElse(null);
        if (holder == null || !(holder.value() instanceof EnchantingApparatusRecipe)) {
            return null;
        }
        return runsOnApparatus(holder.value().getType()) ? cast(holder) : null;
    }

    @Nullable
    static ApparatusPlan resolve(IPatternDetails pattern, Level level) {
        List<GenericStack> inputs = PatternStacks.condensedInputs(pattern);
        GenericStack result = PatternStacks.singleResult(pattern);
        if (inputs == null || result == null) {
            return null;
        }
        if (pattern instanceof ApparatusPattern apparatus) {
            RecipeHolder<EnchantingApparatusRecipe> holder = apparatusById(level, apparatus.recipe());
            return holder == null ? null : plan(holder, level, inputs, result);
        }
        return Uniqueness.onlyMatch(apparatusCandidates(level), candidate -> plan(candidate, level, inputs, result));
    }

    static int apparatusMatchCount(IPatternDetails pattern, Level level) {
        List<GenericStack> inputs = PatternStacks.condensedInputs(pattern);
        GenericStack result = PatternStacks.singleResult(pattern);
        if (inputs == null || result == null) {
            return 0;
        }
        int matches = 0;
        for (RecipeHolder<EnchantingApparatusRecipe> candidate : apparatusCandidates(level)) {
            if (plan(candidate, level, inputs, result) != null) {
                matches++;
            }
        }
        return matches;
    }

    @Nullable
    static ApparatusPlan plan(
            RecipeHolder<EnchantingApparatusRecipe> holder,
            Level level,
            List<GenericStack> inputs,
            GenericStack result) {
        if (opensReagent(holder.value().getType())) {
            Map<AEItemKey, Long> items = expectedItems(inputs);
            return items == null ? null : openPlan(holder, level, items, result, null);
        }
        EncodedIngredients expected = ArsRecipeIngredients.apparatus(holder.value(), level);
        GenericStack[] chosen = IngredientMatching.matchAssign(expected, inputs, result);
        if (chosen == null || chosen.length < 2) {
            return null;
        }

        GenericStack reagent = chosen[0];
        List<GenericStack> pedestals = List.copyOf(List.of(chosen).subList(1, chosen.length));
        if (!producesExactly(holder.value(), level, reagent, pedestals, result)) {
            return null;
        }

        Map<AEItemKey, Long> expectedItems = expectedItems(inputs);
        if (expectedItems == null) {
            return null;
        }
        return new ApparatusPlan(
                holder,
                reagent,
                pedestals,
                expectedItems,
                result,
                holder.value().sourceCost());
    }

    @Nullable
    static ApparatusPlan planFromProvided(ApparatusPlan plan, Map<AEItemKey, Long> provided, Level level) {
        if (!upgradesArmor(plan.holder().value().getType())
                || !(plan.reagent().what() instanceof AEItemKey declaredReagent)) {
            return null;
        }
        return openPlan(plan.holder(), level, provided, null, declaredReagent.getItem());
    }

    @Nullable
    static ApparatusPlan planFromItems(
            RecipeHolder<EnchantingApparatusRecipe> holder, Level level, Map<AEItemKey, Long> items) {
        return openPlan(holder, level, items, null, null);
    }

    @Nullable
    private static ApparatusPlan openPlan(
            RecipeHolder<EnchantingApparatusRecipe> holder,
            Level level,
            Map<AEItemKey, Long> items,
            @Nullable GenericStack result,
            @Nullable Item reagentItem) {
        List<List<GenericStack>> pedestalSlots = ArsRecipeIngredients.pedestals(holder.value());
        if (pedestalSlots == null) {
            return null;
        }
        for (AEItemKey candidate : items.keySet()) {
            if (reagentItem != null && candidate.getItem() != reagentItem) {
                continue;
            }
            List<GenericStack> rest = new ArrayList<>(items.size());
            items.forEach((key, amount) -> {
                long left = key.equals(candidate) ? amount - 1 : amount;
                if (left > 0) {
                    rest.add(new GenericStack(key, left));
                }
            });
            GenericStack[] assigned = IngredientMatching.assign(pedestalSlots, rest);
            if (assigned == null) {
                continue;
            }
            GenericStack reagent = new GenericStack(candidate, 1);
            List<GenericStack> pedestals = List.of(assigned);
            ItemStack made = produced(holder.value(), level, reagent, pedestals);
            AEItemKey madeKey = AEItemKey.of(made);
            if (madeKey == null) {
                continue;
            }
            GenericStack output = new GenericStack(madeKey, made.getCount());
            if (result != null && !output.equals(result)) {
                continue;
            }
            return new ApparatusPlan(
                    holder,
                    reagent,
                    pedestals,
                    Map.copyOf(items),
                    output,
                    holder.value().sourceCost());
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    static RecipeType<ImbuementRecipe> imbuementType() {
        return (RecipeType<ImbuementRecipe>) RecipeRegistry.IMBUEMENT_TYPE.get();
    }

    static List<RecipeHolder<ImbuementRecipe>> imbuementCandidates(Level level) {
        return IMBUEMENT_CACHE.get(level);
    }

    @Nullable
    static RecipeHolder<ImbuementRecipe> imbuementById(Level level, ResourceLocation recipe) {
        RecipeHolder<?> holder = level.getRecipeManager().byKey(recipe).orElse(null);
        if (holder == null || !(holder.value() instanceof ImbuementRecipe)) {
            return null;
        }
        return holder.value().getType() == imbuementType() ? castImbuement(holder) : null;
    }

    @Nullable
    static ImbuementPlan resolveImbuement(IPatternDetails pattern, Level level) {
        List<GenericStack> inputs = PatternStacks.condensedInputs(pattern);
        GenericStack result = PatternStacks.singleResult(pattern);
        if (inputs == null || result == null) {
            return null;
        }
        if (pattern instanceof ImbuementPattern imbuement) {
            RecipeHolder<ImbuementRecipe> holder = imbuementById(level, imbuement.recipe());
            return holder == null ? null : imbuementPlan(holder, level, inputs, result);
        }
        return Uniqueness.onlyMatch(
                imbuementCandidates(level), candidate -> imbuementPlan(candidate, level, inputs, result));
    }

    @Nullable
    static ImbuementPlan imbuementPlan(
            RecipeHolder<ImbuementRecipe> holder, Level level, List<GenericStack> inputs, GenericStack result) {
        EncodedIngredients expected = ArsRecipeIngredients.imbuement(holder.value(), level);
        if (expected == null || expected.inputs().isEmpty()) {
            return null;
        }
        EncodedIngredients reagentOnly =
                new EncodedIngredients(List.of(expected.inputs().get(0)), expected.outputs());
        GenericStack[] chosen = IngredientMatching.matchAssign(reagentOnly, inputs, result);
        boolean catalystsInPattern = false;
        if (chosen == null && expected.inputs().size() > 1) {
            chosen = IngredientMatching.matchAssign(expected, inputs, result);
            catalystsInPattern = true;
        }
        if (chosen == null || chosen.length < 1) {
            return null;
        }

        Map<AEItemKey, Long> expectedItems = expectedItems(inputs);
        if (expectedItems == null) {
            return null;
        }
        return new ImbuementPlan(
                holder,
                chosen[0],
                catalystsInPattern,
                expectedItems,
                result,
                holder.value().getSource());
    }

    static boolean producesExactly(
            EnchantingApparatusRecipe recipe,
            Level level,
            GenericStack reagent,
            List<GenericStack> pedestals,
            GenericStack result) {
        ItemStack produced = produced(recipe, level, reagent, pedestals);
        AEItemKey producedKey = AEItemKey.of(produced);
        return producedKey != null && producedKey.equals(result.what()) && produced.getCount() == result.amount();
    }

    static ItemStack produced(
            EnchantingApparatusRecipe recipe, Level level, GenericStack reagent, List<GenericStack> pedestals) {
        ItemStack reagentStack = toStack(reagent);
        if (reagentStack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        List<ItemStack> pedestalStacks = new ArrayList<>(pedestals.size());
        for (GenericStack pedestal : pedestals) {
            ItemStack stack = toStack(pedestal);
            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }
            pedestalStacks.add(stack);
        }

        ApparatusRecipeInput input = new ApparatusRecipeInput(reagentStack, pedestalStacks, null);
        if (!recipe.matches(input, level, null)) {
            return ItemStack.EMPTY;
        }
        return recipe.assemble(input, level.registryAccess());
    }

    static ItemStack toStack(GenericStack stack) {
        if (!(stack.what() instanceof AEItemKey key) || stack.amount() <= 0 || stack.amount() > Integer.MAX_VALUE) {
            return ItemStack.EMPTY;
        }
        return key.toStack((int) stack.amount());
    }

    @Nullable
    private static Map<AEItemKey, Long> expectedItems(List<GenericStack> inputs) {
        Map<AEItemKey, Long> expected = new LinkedHashMap<>();
        for (GenericStack stack : inputs) {
            if (!(stack.what() instanceof AEItemKey key)) {
                return null;
            }
            expected.merge(key, stack.amount(), Long::sum);
        }
        return Map.copyOf(expected);
    }

    @SuppressWarnings("unchecked")
    private static RecipeHolder<EnchantingApparatusRecipe> cast(RecipeHolder<?> holder) {
        return (RecipeHolder<EnchantingApparatusRecipe>) holder;
    }

    @SuppressWarnings("unchecked")
    private static RecipeHolder<ImbuementRecipe> castImbuement(RecipeHolder<?> holder) {
        return (RecipeHolder<ImbuementRecipe>) holder;
    }
}
