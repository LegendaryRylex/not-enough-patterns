package dev.rylex.nep.compat.occultism;

import com.klikli_dev.occultism.Occultism;
import com.klikli_dev.occultism.config.OccultismServerConfig;
import com.klikli_dev.occultism.crafting.recipe.CrushingRecipe;
import com.klikli_dev.occultism.crafting.recipe.CrystallizeRecipe;
import com.klikli_dev.occultism.crafting.recipe.SpiritFireRecipe;
import com.klikli_dev.occultism.crafting.recipe.TieredSingleRecipeInput;
import com.klikli_dev.occultism.registry.OccultismRecipes;
import dev.rylex.nep.NepConfig;
import java.util.Locale;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

final class OccultismMatrixRecipes {
    private OccultismMatrixRecipes() {}

    enum Kind {
        SPIRIT_FIRE(null),
        CRUSHING(BoundSpirit.Work.CRUSHER),
        CRYSTALLIZING(BoundSpirit.Work.CRYSTALLIZER);

        @Nullable
        private final BoundSpirit.Work work;

        Kind(@Nullable BoundSpirit.Work work) {
            this.work = work;
        }

        @Nullable
        BoundSpirit.Work work() {
            return work;
        }

        String id() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    record Match(Kind kind, ResourceLocation recipe) {}

    record Craft(ItemStack result, int ticks) {}

    @Nullable
    static Match find(
            Level level,
            ItemStack input,
            Item output,
            @Nullable BoundSpirit crusher,
            @Nullable BoundSpirit crystallizer) {
        Match fire = scan(
                level, OccultismRecipes.SPIRIT_FIRE_TYPE.get(), new SingleRecipeInput(input), output, Kind.SPIRIT_FIRE);
        if (fire != null) {
            return fire;
        }
        if (crusher != null) {
            Match crushing = scan(
                    level,
                    OccultismRecipes.CRUSHING_TYPE.get(),
                    new TieredSingleRecipeInput(input, crusher.tier().level()),
                    output,
                    Kind.CRUSHING);
            if (crushing != null) {
                return crushing;
            }
        }
        if (crystallizer != null) {
            return scan(
                    level,
                    OccultismRecipes.CRYSTALLIZE_TYPE.get(),
                    new TieredSingleRecipeInput(input, crystallizer.tier().level()),
                    output,
                    Kind.CRYSTALLIZING);
        }
        return null;
    }

    @Nullable
    private static <I extends RecipeInput, R extends Recipe<I>> Match scan(
            Level level, RecipeType<R> type, I recipeInput, Item output, Kind kind) {
        for (RecipeHolder<R> holder : level.getRecipeManager().getAllRecipesFor(type)) {
            if (holder.value().matches(recipeInput, level)
                    && makes(holder.value().assemble(recipeInput, level.registryAccess()), output)) {
                return new Match(kind, holder.id());
            }
        }
        return null;
    }

    @Nullable
    static BoundSpirit.Work workNeeded(Level level, ItemStack input, Item output) {
        for (BoundSpirit.Work work : BoundSpirit.Work.values()) {
            for (BoundSpirit.Tier tier : BoundSpirit.Tier.values()) {
                BoundSpirit spirit = new BoundSpirit(tier, work);
                Match match = work == BoundSpirit.Work.CRUSHER
                        ? find(level, input, output, spirit, null)
                        : find(level, input, output, null, spirit);
                if (match != null && match.kind().work() == work) {
                    return work;
                }
            }
        }
        return null;
    }

    @Nullable
    static Craft craft(
            Level level,
            Kind kind,
            ResourceLocation id,
            ItemStack input,
            @Nullable BoundSpirit spirit,
            int operations) {
        Recipe<?> recipe = recipe(level, id);
        if (kind == Kind.SPIRIT_FIRE) {
            if (!(recipe instanceof SpiritFireRecipe fire)) {
                return null;
            }
            ItemStack result = fire.assemble(new SingleRecipeInput(input), level.registryAccess());
            return result.isEmpty() ? null : new Craft(result, NepConfig.occultismMatrixSpiritFireTicks());
        }
        if (spirit == null) {
            return null;
        }
        TieredSingleRecipeInput tiered =
                new TieredSingleRecipeInput(input, spirit.tier().level());
        OccultismServerConfig.SpiritJobSettings.TierSpiritSettings settings = settings(spirit);
        int baseTicks;
        boolean ignoreMultiplier;
        ItemStack result;
        if (kind == Kind.CRUSHING && recipe instanceof CrushingRecipe crushing && crushing.matches(tiered, level)) {
            baseTicks = crushing.getCrushingTime();
            ignoreMultiplier = crushing.getIgnoreCrushingMultiplier();
            result = crushing.assemble(tiered, level.registryAccess());
        } else if (kind == Kind.CRYSTALLIZING
                && recipe instanceof CrystallizeRecipe crystallize
                && crystallize.matches(tiered, level)) {
            baseTicks = crystallize.getCrystallizeTime();
            ignoreMultiplier = crystallize.getIgnoreCrystallizeMultiplier();
            result = crystallize.assemble(tiered, level.registryAccess());
        } else {
            return null;
        }
        if (result.isEmpty()) {
            return null;
        }
        float multiplier =
                ignoreMultiplier ? 1.0F : settings.outputMultiplier.get().floatValue();
        result.setCount(Math.max(1, (int) (result.getCount() * Math.max(1, operations) * multiplier)));
        long ticks = Math.round(
                baseTicks * settings.timeMultiplier.get() * NepConfig.occultismMatrixCraftTimePercent() / 100.0);
        return new Craft(result, (int) Math.max(1, Math.min(Integer.MAX_VALUE, ticks)));
    }

    static int operations(@Nullable BoundSpirit spirit) {
        return spirit == null ? 1 : Math.max(1, settings(spirit).operationCount.get());
    }

    @Nullable
    static Kind kindOf(Level level, ResourceLocation id) {
        Recipe<?> recipe = recipe(level, id);
        if (recipe instanceof SpiritFireRecipe) {
            return Kind.SPIRIT_FIRE;
        }
        if (recipe instanceof CrushingRecipe) {
            return Kind.CRUSHING;
        }
        if (recipe instanceof CrystallizeRecipe) {
            return Kind.CRYSTALLIZING;
        }
        return null;
    }

    @Nullable
    static Ingredient ingredient(Level level, ResourceLocation id) {
        Recipe<?> recipe = recipe(level, id);
        return recipe == null ? null : singleIngredient(recipe);
    }

    @Nullable
    static Ingredient singleIngredient(Recipe<?> recipe) {
        if (recipe.getIngredients().size() != 1) {
            return null;
        }
        return recipe.getIngredients().get(0);
    }

    @Nullable
    private static Recipe<?> recipe(Level level, ResourceLocation id) {
        return level.getRecipeManager().byKey(id).map(RecipeHolder::value).orElse(null);
    }

    private static OccultismServerConfig.SpiritJobSettings.TierSpiritSettings settings(BoundSpirit spirit) {
        OccultismServerConfig.SpiritJobSettings jobs = Occultism.SERVER_CONFIG.spiritJobs;
        boolean crusher = spirit.work() == BoundSpirit.Work.CRUSHER;
        return switch (spirit.tier()) {
            case FOLIOT -> crusher ? jobs.crusherFoliot : jobs.crystallizerFoliot;
            case DJINNI -> crusher ? jobs.crusherDjinni : jobs.crystallizerDjinni;
            case AFRIT -> crusher ? jobs.crusherAfrit : jobs.crystallizerAfrit;
            case MARID -> crusher ? jobs.crusherMarid : jobs.crystallizerMarid;
        };
    }

    private static boolean makes(ItemStack result, Item output) {
        return !result.isEmpty() && result.is(output);
    }
}
