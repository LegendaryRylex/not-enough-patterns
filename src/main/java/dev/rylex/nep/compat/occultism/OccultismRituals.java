package dev.rylex.nep.compat.occultism;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import com.klikli_dev.occultism.crafting.recipe.RitualRecipe;
import com.klikli_dev.occultism.registry.OccultismRecipes;
import dev.rylex.nep.pattern.InputSubstitution;
import dev.rylex.nep.pattern.encoding.EncodedIngredients;
import dev.rylex.nep.pattern.encoding.IngredientMatching;
import dev.rylex.nep.util.Recipes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

final class OccultismRituals {
    private OccultismRituals() {}

    static final Identifier REPAIR = occultism("repair");

    private static final Set<Identifier> PATTERN_TYPES = Set.of(
            occultism("craft"),
            occultism("craft_with_spirit_name"),
            occultism("craft_miner_spirit"),
            occultism("upgrade"));

    private static final Map<Identifier, Integer> PENTACLE_TIERS = Map.of(
            occultism("craft_foliot"), 1,
            occultism("craft_djinni"), 2,
            occultism("contact_wild_spirit"), 2,
            occultism("craft_afrit"), 3,
            occultism("craft_marid"), 4,
            occultism("contact_eldritch_spirit"), 4);

    private static final int UNKNOWN_PENTACLE_TIER = 4;
    private static final int MAX_UNITS = 64;

    record Plan(Map<AEItemKey, Long> expectedItems, AEItemKey result, long resultCount) {}

    private static Identifier occultism(String path) {
        return Identifier.fromNamespaceAndPath("occultism", path);
    }

    static boolean patternable(RitualRecipe recipe, Level level) {
        return PATTERN_TYPES.contains(recipe.getRitualType())
                && !recipe.requiresItemUse()
                && !recipe.getResult().isEmpty();
    }

    static boolean repairs(RitualRecipe recipe) {
        return REPAIR.equals(recipe.getRitualType()) && !recipe.requiresItemUse() && !recipe.requiresSacrifice();
    }

    static int tierOf(RitualRecipe recipe) {
        return PENTACLE_TIERS.getOrDefault(recipe.getPentacleId(), UNKNOWN_PENTACLE_TIER);
    }

    static List<RecipeHolder<RitualRecipe>> all(Level level) {
        return Recipes.allOf(level, OccultismRecipes.RITUAL_TYPE.get());
    }

    static List<RecipeHolder<RitualRecipe>> candidates(Level level) {
        List<RecipeHolder<RitualRecipe>> candidates = new ArrayList<>();
        for (RecipeHolder<RitualRecipe> holder : all(level)) {
            if (patternable(holder.value(), level)) {
                candidates.add(holder);
            }
        }
        return candidates;
    }

    @Nullable
    static RecipeHolder<RitualRecipe> byId(Level level, @Nullable Identifier id) {
        if (id == null) {
            return null;
        }
        RecipeHolder<?> holder = Recipes.byId(level, id);
        return holder != null && holder.value() instanceof RitualRecipe recipe
                ? new RecipeHolder<>(holder.id(), recipe)
                : null;
    }

    static List<Predicate<ItemStack>> requirements(Level level, RitualRecipe recipe) {
        List<Predicate<ItemStack>> requirements = new ArrayList<>();
        requirements.add(recipe.getActivationItem());
        for (Ingredient ingredient : recipe.getIngredients()) {
            requirements.add(ingredient);
        }
        if (recipe.requiresSacrifice()) {
            requirements.add(stack -> SacrificeStandIns.accepts(level, recipe.getEntityToSacrifice(), stack));
        }
        return requirements;
    }

    @Nullable
    static Plan plan(Level level, RitualRecipe recipe, List<GenericStack> inputs, GenericStack result) {
        if (!patternable(recipe, level) || !(result.what() instanceof AEItemKey resultKey)) {
            return null;
        }
        ItemStack expected = recipe.getResult();
        if (resultKey.getItem() != expected.getItem() || result.amount() != expected.getCount()) {
            return null;
        }
        List<ItemStack> units = new ArrayList<>();
        Map<AEItemKey, Long> items = new HashMap<>();
        for (GenericStack input : inputs) {
            if (!(input.what() instanceof AEItemKey key) || input.amount() <= 0) {
                return null;
            }
            items.merge(key, input.amount(), Long::sum);
            for (long i = 0; i < input.amount(); i++) {
                if (units.size() >= MAX_UNITS) {
                    return null;
                }
                units.add(key.toStack());
            }
        }
        if (assign(requirements(level, recipe), units) == null) {
            return null;
        }
        return new Plan(Map.copyOf(items), resultKey, result.amount());
    }

    /** Pairs every requirement with a distinct unit, by index into {@code units}, or {@code null} when no pairing covers them all. */
    @Nullable
    static int[] assign(List<Predicate<ItemStack>> requirements, List<ItemStack> units) {
        if (requirements.size() != units.size()) {
            return null;
        }
        int[] chosen = new int[requirements.size()];
        boolean[] used = new boolean[units.size()];
        return assign(requirements, units, 0, chosen, used) ? chosen : null;
    }

    private static boolean assign(
            List<Predicate<ItemStack>> requirements, List<ItemStack> units, int index, int[] chosen, boolean[] used) {
        if (index == requirements.size()) {
            return true;
        }
        for (int unit = 0; unit < units.size(); unit++) {
            if (!used[unit] && requirements.get(index).test(units.get(unit))) {
                used[unit] = true;
                chosen[index] = unit;
                if (assign(requirements, units, index + 1, chosen, used)) {
                    return true;
                }
                used[unit] = false;
            }
        }
        return false;
    }

    @Nullable
    static EncodedIngredients ingredients(Level level, RitualRecipe recipe) {
        if (!patternable(recipe, level)) {
            return null;
        }
        List<List<GenericStack>> inputs = new ArrayList<>();
        inputs.add(options(stacks(recipe.getActivationItem(), level)));
        for (Ingredient ingredient : recipe.getIngredients()) {
            inputs.add(options(stacks(ingredient, level)));
        }
        if (recipe.requiresSacrifice()) {
            inputs.add(options(SacrificeStandIns.examples(level, recipe.getEntityToSacrifice())));
        }
        for (List<GenericStack> slot : inputs) {
            if (slot.isEmpty()) {
                return null;
            }
        }
        GenericStack result = GenericStack.fromItemStack(recipe.getResult());
        return result == null ? null : new EncodedIngredients(inputs, List.of(result));
    }

    private static List<ItemStack> stacks(Ingredient ingredient, Level level) {
        List<ItemStack> stacks = new ArrayList<>();
        for (AEItemKey key : IngredientMatching.itemOptions(ingredient, level)) {
            stacks.add(key.toStack());
        }
        return stacks;
    }

    private static List<GenericStack> options(List<ItemStack> stacks) {
        List<GenericStack> options = new ArrayList<>();
        for (ItemStack stack : stacks) {
            GenericStack option = GenericStack.fromItemStack(stack.copyWithCount(1));
            if (option != null && !options.contains(option)) {
                options.add(option);
            }
        }
        return options;
    }

    @Nullable
    static InputSubstitution substitution(Identifier id, Level level) {
        RecipeHolder<RitualRecipe> holder = byId(level, id);
        if (holder == null) {
            return null;
        }
        List<Predicate<ItemStack>> requirements = requirements(level, holder.value());
        return (template, candidate, ignored) -> {
            if (!(template instanceof AEItemKey wanted)
                    || !(candidate instanceof AEItemKey offered)
                    || wanted.getItem() != offered.getItem()) {
                return false;
            }
            ItemStack wantedStack = wanted.toStack();
            ItemStack offeredStack = offered.toStack();
            for (Predicate<ItemStack> requirement : requirements) {
                if (requirement.test(wantedStack) && requirement.test(offeredStack)) {
                    return true;
                }
            }
            return false;
        };
    }
}
