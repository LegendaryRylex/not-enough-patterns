package dev.rylex.nep.compat.apothic;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import dev.rylex.nep.pattern.EnchantingPattern;
import dev.rylex.nep.pattern.PatternStacks;
import dev.rylex.nep.util.RecipeCache;
import dev.rylex.nep.util.Uniqueness;
import dev.shadowsoffire.apothic_enchanting.Ench;
import dev.shadowsoffire.apothic_enchanting.table.EnchantingStatRegistry;
import dev.shadowsoffire.apothic_enchanting.table.infusion.InfusionRecipe;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.Tags;
import org.jetbrains.annotations.Nullable;

final class InfusionRecipeResolver {
    private InfusionRecipeResolver() {}

    static final int MAX_STAT = 100;

    private static final RecipeCache<List<RecipeHolder<InfusionRecipe>>> CANDIDATE_CACHE =
            RecipeCache.of(level -> List.copyOf(level.getRecipeManager().getAllRecipesFor(Ench.RecipeTypes.INFUSION)));

    record Stats(int eterna, int quanta, int arcana) {}

    record Plan(
            RecipeHolder<InfusionRecipe> holder,
            AEItemKey input,
            int eterna,
            int quanta,
            int arcana,
            int fuel,
            int bottles,
            long experienceFluid,
            Map<AEKey, Long> expected,
            GenericStack result) {}

    static void clearCache() {
        CANDIDATE_CACHE.clear();
    }

    static List<RecipeHolder<InfusionRecipe>> candidates(Level level) {
        return CANDIDATE_CACHE.get(level);
    }

    @Nullable
    static Plan resolve(IPatternDetails pattern, Level level, InfusionCosts.Rates rates) {
        List<GenericStack> inputs = PatternStacks.condensedInputs(pattern);
        GenericStack result = PatternStacks.singleResult(pattern);
        if (inputs == null || result == null || !(result.what() instanceof AEItemKey resultKey)) {
            return null;
        }

        Map<AEKey, Long> provided = new LinkedHashMap<>();
        for (GenericStack stack : inputs) {
            provided.merge(stack.what(), stack.amount(), Long::sum);
        }

        if (pattern instanceof EnchantingPattern enchanting) {
            RecipeHolder<InfusionRecipe> holder = byId(level, enchanting.recipe());
            return holder == null ? null : plan(holder, provided, resultKey, result, rates);
        }

        return Uniqueness.onlyMatch(
                candidates(level), candidate -> plan(candidate, provided, resultKey, result, rates));
    }

    @Nullable
    private static RecipeHolder<InfusionRecipe> byId(Level level, ResourceLocation recipe) {
        for (RecipeHolder<InfusionRecipe> candidate : candidates(level)) {
            if (candidate.id().equals(recipe)) {
                return candidate;
            }
        }
        return null;
    }

    @Nullable
    private static Plan plan(
            RecipeHolder<InfusionRecipe> holder,
            Map<AEKey, Long> provided,
            AEItemKey resultKey,
            GenericStack result,
            InfusionCosts.Rates rates) {
        InfusionRecipe recipe = holder.value();
        ItemStack output = recipe.getOutput();
        if (!resultKey.matches(output) || result.amount() != output.getCount()) {
            return null;
        }

        Stats stats = statsFor(recipe);
        if (stats == null) {
            return null;
        }
        int eterna = stats.eterna();
        int quanta = stats.quanta();
        int arcana = stats.arcana();

        int bottles = InfusionCosts.bottleCost(recipe, rates);
        long experienceFluid = InfusionCosts.fluidCost(recipe, rates);
        for (AEKey key : provided.keySet()) {
            if (!(key instanceof AEItemKey candidate)) {
                continue;
            }
            ItemStack stack = candidate.toStack();
            if (!recipe.matches(stack, eterna, quanta, arcana)) {
                continue;
            }
            if (!coversCosts(provided, candidate, bottles, experienceFluid)) {
                continue;
            }
            return new Plan(
                    holder,
                    candidate,
                    eterna,
                    quanta,
                    arcana,
                    InfusionCosts.LAPIS,
                    bottles,
                    experienceFluid,
                    Map.copyOf(provided),
                    result);
        }
        return null;
    }

    private static boolean coversCosts(Map<AEKey, Long> provided, AEItemKey input, int bottles, long experienceFluid) {
        long fuelSeen = 0;
        long bottlesSeen = 0;
        long fluidSeen = 0;
        for (Map.Entry<AEKey, Long> entry : provided.entrySet()) {
            long amount = entry.getValue() - (entry.getKey().equals(input) ? 1 : 0);
            if (amount < 0) {
                return false;
            }
            if (amount == 0) {
                continue;
            }
            if (entry.getKey() instanceof AEFluidKey fluidKey) {
                if (!ExperienceFluids.isExperience(fluidKey)) {
                    return false;
                }
                fluidSeen += amount;
                continue;
            }
            if (!(entry.getKey() instanceof AEItemKey itemKey)) {
                return false;
            }
            ItemStack stack = itemKey.toStack();
            if (stack.is(Items.EXPERIENCE_BOTTLE)) {
                bottlesSeen += amount;
            } else if (stack.is(Tags.Items.ENCHANTING_FUELS)) {
                fuelSeen += amount;
            } else {
                return false;
            }
        }
        if (fuelSeen != InfusionCosts.LAPIS) {
            return false;
        }
        if (fluidSeen > 0) {
            return bottlesSeen == 0 && fluidSeen == experienceFluid;
        }
        return bottlesSeen == bottles;
    }

    @Nullable
    static Stats statsFor(InfusionRecipe recipe) {
        EnchantingStatRegistry.Stats requirements = recipe.getRequirements();
        EnchantingStatRegistry.Stats maximums = recipe.getMaxRequirements();
        int eterna = stat(requirements.eterna(), maximums.eterna());
        int quanta = stat(requirements.quanta(), maximums.quanta());
        int arcana = stat(requirements.arcana(), maximums.arcana());
        return eterna < 0 || quanta < 0 || arcana < 0 ? null : new Stats(eterna, quanta, arcana);
    }

    private static int stat(float minimum, float maximum) {
        int value = Math.max(Mth.ceil(minimum), 0);
        if (value > MAX_STAT) {
            return -1;
        }
        return maximum > -1.0F && value > maximum ? -1 : value;
    }
}
