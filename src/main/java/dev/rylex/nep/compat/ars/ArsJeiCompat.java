package dev.rylex.nep.compat.ars;

import com.hollingsworth.arsnouveau.client.jei.JEIArsNouveauPlugin;
import com.hollingsworth.arsnouveau.common.crafting.recipes.EnchantingApparatusRecipe;
import com.hollingsworth.arsnouveau.common.crafting.recipes.ImbuementRecipe;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.compat.jei.JeiTransferSource;
import dev.rylex.nep.compat.jei.ManualTransferHandler;
import dev.rylex.nep.pattern.encoding.EncodedIngredients;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class ArsJeiCompat {
    private ArsJeiCompat() {}

    public static void addManualTransfers(
            IRecipeTransferRegistration registration, IRecipeTransferHandlerHelper helper) {
        RecipeType<RecipeHolder<EnchantingApparatusRecipe>> apparatus =
                JEIArsNouveauPlugin.ENCHANTING_APP_RECIPE_TYPE.get();
        registration.addRecipeTransferHandler(
                new ManualTransferHandler<>(
                        ArcaneEnchantingMatrixMenu.class,
                        NepArsContent.MATRIX_MENU.get(),
                        apparatus,
                        helper,
                        (holder, level) -> ArsRecipeIngredients.apparatusRequirements(holder.value()),
                        (holder, level) -> holder.id()),
                apparatus);
        RecipeType<RecipeHolder<ImbuementRecipe>> imbuement = JEIArsNouveauPlugin.IMBUEMENT_RECIPE_TYPE.get();
        registration.addRecipeTransferHandler(
                new ManualTransferHandler<>(
                        ArcaneEnchantingMatrixMenu.class,
                        NepArsContent.MATRIX_MENU.get(),
                        imbuement,
                        helper,
                        (holder, level) -> ArsRecipeIngredients.imbuementRequirements(holder.value()),
                        (holder, level) -> holder.id()),
                imbuement);
    }

    public static JeiTransferSource source() {
        return new JeiTransferSource() {
            @Override
            public void addTransfers(TransferCollector collector) {
                collector.add(JEIArsNouveauPlugin.ENCHANTING_APP_RECIPE_TYPE.get(), ArsJeiCompat::apparatus);
                collector.add(JEIArsNouveauPlugin.IMBUEMENT_RECIPE_TYPE.get(), ArsJeiCompat::imbuement);
                collector.addViewing(JEIArsNouveauPlugin.ENCHANTING_RECIPE_TYPE.get(), ArsJeiCompat::openReagent);
                collector.addViewing(JEIArsNouveauPlugin.ARMOR_RECIPE_TYPE.get(), ArsJeiCompat::openReagent);
            }

            @Override
            public void addCatalysts(IRecipeCatalystRegistration registration) {
                if (!NepConfig.arsMatrix()) {
                    return;
                }
                ItemStack matrix = new ItemStack(NepArsContent.MATRIX_ITEM.get());
                registration.addRecipeCatalyst(matrix, JEIArsNouveauPlugin.ENCHANTING_APP_RECIPE_TYPE.get());
                registration.addRecipeCatalyst(matrix, JEIArsNouveauPlugin.ENCHANTING_RECIPE_TYPE.get());
                registration.addRecipeCatalyst(matrix, JEIArsNouveauPlugin.ARMOR_RECIPE_TYPE.get());
                registration.addRecipeCatalyst(matrix, JEIArsNouveauPlugin.IMBUEMENT_RECIPE_TYPE.get());
            }
        };
    }

    @Nullable
    private static EncodedIngredients apparatus(RecipeHolder<EnchantingApparatusRecipe> holder, Level level) {
        if (!NepConfig.arsEnchantingApparatus()) {
            return null;
        }
        if (holder.value().getType() != ArsRecipeResolver.apparatusType()) {
            return null;
        }
        return ArsRecipeIngredients.apparatus(holder.value(), level);
    }

    /** Encodes the reagent JEI is showing, since one enchantment or armor upgrade recipe covers many items. */
    @Nullable
    private static <R extends EnchantingApparatusRecipe> EncodedIngredients openReagent(
            RecipeHolder<R> holder, IRecipeSlotsView slots, Level level) {
        if (!NepConfig.arsEnchantingApparatus()) {
            return null;
        }
        ItemStack shown = slots.getSlotViews(RecipeIngredientRole.INPUT).stream()
                .findFirst()
                .flatMap(IRecipeSlotView::getDisplayedItemStack)
                .orElse(ItemStack.EMPTY);
        if (shown.isEmpty()) {
            return null;
        }
        ItemStack reagent = shown.copyWithCount(1);
        reagent.remove(DataComponents.CUSTOM_NAME);
        return ArsRecipeIngredients.openReagent(holder.value(), reagent, level);
    }

    @Nullable
    private static EncodedIngredients imbuement(RecipeHolder<ImbuementRecipe> holder, Level level) {
        if (!NepConfig.arsImbuementChamber()) {
            return null;
        }
        if (holder.value().getType() != ArsRecipeResolver.imbuementType()) {
            return null;
        }
        return ArsRecipeIngredients.imbuement(holder.value(), level);
    }
}
