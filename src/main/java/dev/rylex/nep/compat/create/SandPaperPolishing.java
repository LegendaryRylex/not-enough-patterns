package dev.rylex.nep.compat.create;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.AllTags;
import com.simibubi.create.content.equipment.sandPaper.SandPaperPolishingRecipe;
import com.simibubi.create.content.kinetics.deployer.DeployerApplicationRecipe;
import com.simibubi.create.content.kinetics.deployer.ItemApplicationRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.util.RecipeCache;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

final class SandPaperPolishing {
    private SandPaperPolishing() {}

    private static final RecipeCache<Map<ResourceLocation, RecipeHolder<DeployerApplicationRecipe>>> TABLE =
            RecipeCache.of(SandPaperPolishing::build);

    static void clearCache() {
        TABLE.clear();
    }

    static List<RecipeHolder<? extends ItemApplicationRecipe>> recipes(Level level) {
        return NepConfig.createDeploying() ? List.copyOf(table(level).values()) : List.of();
    }

    @Nullable
    static RecipeHolder<DeployerApplicationRecipe> byId(ResourceLocation id, Level level) {
        return NepConfig.createDeploying() ? table(level).get(id) : null;
    }

    @Nullable
    static RecipeHolder<DeployerApplicationRecipe> substitute(RecipeHolder<?> polishing, Level level) {
        return byId(polishing.id(), level);
    }

    private static Map<ResourceLocation, RecipeHolder<DeployerApplicationRecipe>> table(Level level) {
        return TABLE.get(level);
    }

    private static Map<ResourceLocation, RecipeHolder<DeployerApplicationRecipe>> build(Level level) {
        Map<ResourceLocation, RecipeHolder<DeployerApplicationRecipe>> converted = new LinkedHashMap<>();
        List<RecipeHolder<SandPaperPolishingRecipe>> polishing = level.getRecipeManager()
                .getAllRecipesFor(
                        AllRecipeTypes.SANDPAPER_POLISHING.<SingleRecipeInput, SandPaperPolishingRecipe>getType());

        for (RecipeHolder<SandPaperPolishingRecipe> holder : polishing) {
            SandPaperPolishingRecipe recipe = holder.value();
            List<ProcessingOutput> results = recipe.getRollableResults();
            if (recipe.getIngredients().isEmpty() || results.size() != 1) {
                continue;
            }
            ItemApplicationRecipe.Builder<DeployerApplicationRecipe> builder =
                    new ItemApplicationRecipe.Builder<>(DeployerApplicationRecipe::new, holder.id());
            DeployerApplicationRecipe application = builder.require(
                            recipe.getIngredients().get(0))
                    .require(AllTags.AllItemTags.SANDPAPER.tag)
                    .output(results.get(0))
                    .toolNotConsumed()
                    .build();
            converted.put(holder.id(), new RecipeHolder<>(holder.id(), application));
        }
        return Map.copyOf(converted);
    }
}
