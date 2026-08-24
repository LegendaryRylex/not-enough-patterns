package dev.rylex.nep.compat.create;

import com.simibubi.create.content.equipment.sandPaper.SandPaperPolishingRecipe;
import com.simibubi.create.content.fluids.transfer.FillingRecipe;
import com.simibubi.create.content.kinetics.deployer.DeployerApplicationRecipe;
import com.simibubi.create.content.kinetics.deployer.ManualApplicationRecipe;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import dev.rylex.nep.compat.jei.JeiTransferSource;
import java.util.List;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;

public final class CreateJeiCompat {
    private CreateJeiCompat() {}

    public static JeiTransferSource source() {
        return new JeiTransferSource() {
            @Override
            public void addTransfers(TransferCollector collector) {
                collector.add(
                        RecipeType.<CraftingRecipe>createRecipeHolderType(CreatePatternSources.MECHANICAL_CRAFTING),
                        CreateRecipeIngredients::mechanicalCrafting);
                collector.add(
                        RecipeType.<CraftingRecipe>createRecipeHolderType(CreatePatternSources.AUTOMATIC_SHAPED),
                        CreateRecipeIngredients::mechanicalCrafting);
                collector.add(
                        RecipeType.<SequencedAssemblyRecipe>createRecipeHolderType(
                                CreatePatternSources.SEQUENCED_ASSEMBLY),
                        CreateRecipeIngredients::sequencedAssembly);
                collector.add(
                        RecipeType.<DeployerApplicationRecipe>createRecipeHolderType(CreatePatternSources.DEPLOYING),
                        CreateRecipeIngredients::itemApplication);
                collector.add(
                        RecipeType.<ManualApplicationRecipe>createRecipeHolderType(
                                CreatePatternSources.ITEM_APPLICATION),
                        CreateRecipeIngredients::displayedItemApplication);
                collector.add(
                        RecipeType.<SandPaperPolishingRecipe>createRecipeHolderType(
                                CreatePatternSources.SANDPAPER_POLISHING),
                        CreateRecipeIngredients::sandPaperPolishing);
                collector.add(
                        RecipeType.<FillingRecipe>createRecipeHolderType(CreatePatternSources.SPOUT_FILLING),
                        CreateRecipeIngredients::spoutFilling);
            }

            @Override
            public void addRecipes(IRecipeRegistration registration, Level level) {
                List<RecipeHolder<DeployerApplicationRecipe>> stripping = LogStripping.deployerRecipes(level);
                if (!stripping.isEmpty()) {
                    registration.addRecipes(
                            RecipeType.<DeployerApplicationRecipe>createRecipeHolderType(
                                    CreatePatternSources.DEPLOYING),
                            stripping);
                }
            }

            @Override
            public void addCatalysts(IRecipeCatalystRegistration registration) {
                registration.addRecipeCatalysts(
                        RecipeType.<SequencedAssemblyRecipe>createRecipeHolderType(
                                CreatePatternSources.SEQUENCED_ASSEMBLY),
                        NepCreateContent.CONTROLLER_ITEM.get(),
                        NepCreateContent.MATRIX_ITEM.get());
            }
        };
    }
}
