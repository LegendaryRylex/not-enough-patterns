package dev.rylex.nep.compat.mysticalagriculture;

import com.blakebr0.mysticalagriculture.api.crafting.IAwakeningRecipe;
import com.blakebr0.mysticalagriculture.api.crafting.IInfusionRecipe;
import com.blakebr0.mysticalagriculture.compat.jei.category.AwakeningCategory;
import com.blakebr0.mysticalagriculture.compat.jei.category.InfusionCategory;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.compat.jei.JeiTransferSource;
import dev.rylex.nep.compat.jei.ManualTransferHandler;
import dev.rylex.nep.pattern.encoding.EncodedIngredients;
import dev.rylex.nep.util.Recipes;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class MysticalJeiCompat {
    private MysticalJeiCompat() {}

    public static void addManualTransfers(
            IRecipeTransferRegistration registration, IRecipeTransferHandlerHelper helper) {
        registration.addRecipeTransferHandler(
                new ManualTransferHandler<>(
                        InfusedAwakeningMatrixMenu.class,
                        NepMysticalContent.MATRIX_MENU.get(),
                        InfusionCategory.RECIPE_TYPE,
                        helper,
                        (holder, level) -> MysticalRecipeIngredients.manualInfusionRequirements(holder.value()),
                        (holder, level) -> Recipes.idOf(holder)),
                InfusionCategory.RECIPE_TYPE);
        registration.addRecipeTransferHandler(
                new ManualTransferHandler<>(
                        InfusedAwakeningMatrixMenu.class,
                        NepMysticalContent.MATRIX_MENU.get(),
                        AwakeningCategory.RECIPE_TYPE,
                        helper,
                        (holder, level) -> MysticalRecipeIngredients.manualAwakeningRequirements(holder.value()),
                        (holder, level) -> Recipes.idOf(holder)),
                AwakeningCategory.RECIPE_TYPE);
    }

    public static JeiTransferSource source() {
        return new JeiTransferSource() {
            @Override
            public void addTransfers(TransferCollector collector) {
                collector.add(InfusionCategory.RECIPE_TYPE, MysticalJeiCompat::infusion);
                collector.add(AwakeningCategory.RECIPE_TYPE, MysticalJeiCompat::awakening);
            }

            @Override
            public void addCatalysts(IRecipeCatalystRegistration registration) {
                if (!NepConfig.mysticalInfusedAwakeningMatrix()) {
                    return;
                }
                ItemStack matrix = new ItemStack(NepMysticalContent.MATRIX_ITEM.get());
                registration.addCraftingStation(InfusionCategory.RECIPE_TYPE, matrix);
                registration.addCraftingStation(AwakeningCategory.RECIPE_TYPE, matrix);
            }
        };
    }

    @Nullable
    private static EncodedIngredients infusion(RecipeHolder<IInfusionRecipe> holder, Level level) {
        return NepConfig.mysticalInfusion() ? MysticalRecipeIngredients.infusion(holder.value(), level) : null;
    }

    @Nullable
    private static EncodedIngredients awakening(RecipeHolder<IAwakeningRecipe> holder, Level level) {
        return NepConfig.mysticalAwakening() ? MysticalRecipeIngredients.awakening(holder.value(), level) : null;
    }
}
