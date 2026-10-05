package dev.rylex.nep.compat.draconic;

import com.brandon3055.draconicevolution.api.DraconicAPI;
import com.brandon3055.draconicevolution.api.crafting.IFusionRecipe;
import dev.rylex.nep.compat.jei.JeiTransferSource;
import dev.rylex.nep.compat.jei.ManualTransferHandler;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.world.item.crafting.RecipeHolder;

public final class DraconicJeiCompat {
    private DraconicJeiCompat() {}

    @SuppressWarnings("unchecked")
    private static RecipeType<RecipeHolder<IFusionRecipe>> fusionRecipeType() {
        return RecipeType.createFromVanilla(
                (net.minecraft.world.item.crafting.RecipeType<IFusionRecipe>) DraconicAPI.FUSION_RECIPE_TYPE.get());
    }

    public static void addManualTransfers(
            IRecipeTransferRegistration registration, IRecipeTransferHandlerHelper helper) {
        RecipeType<RecipeHolder<IFusionRecipe>> type = fusionRecipeType();
        registration.addRecipeTransferHandler(
                new ManualTransferHandler<>(
                        FusionMatrixMenu.class,
                        NepDraconicContent.MATRIX_MENU.get(),
                        type,
                        helper,
                        (holder, level) -> DraconicRecipeIngredients.manualRequirements(holder.value()),
                        (holder, level) -> holder.id()),
                type);
    }

    public static JeiTransferSource source() {
        return new JeiTransferSource() {
            @Override
            public void addTransfers(TransferCollector collector) {
                collector.add(fusionRecipeType(), DraconicRecipeIngredients::fusion);
            }

            @Override
            public void addCatalysts(IRecipeCatalystRegistration registration) {
                registration.addRecipeCatalysts(fusionRecipeType(), NepDraconicContent.MATRIX_ITEM.get());
            }
        };
    }
}
