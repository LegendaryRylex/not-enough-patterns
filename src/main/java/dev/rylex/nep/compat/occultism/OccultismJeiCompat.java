package dev.rylex.nep.compat.occultism;

import com.klikli_dev.occultism.crafting.recipe.RitualRecipe;
import com.klikli_dev.occultism.integration.jei.impl.JeiRecipeTypes;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.compat.jei.JeiTransferSource;
import dev.rylex.nep.compat.jei.ManualTransferHandler;
import dev.rylex.nep.machine.ManualRequirement;
import dev.rylex.nep.pattern.encoding.EncodedIngredients;
import dev.rylex.nep.util.Recipes;
import java.util.List;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class OccultismJeiCompat {
    private OccultismJeiCompat() {}

    public static void addManualTransfers(
            IRecipeTransferRegistration registration, IRecipeTransferHandlerHelper helper) {
        addManualTransfer(registration, helper, JeiRecipeTypes.SPIRIT_FIRE);
        addManualTransfer(registration, helper, JeiRecipeTypes.CRUSHING);
        addManualTransfer(registration, helper, JeiRecipeTypes.CRYSTALLIZE);
    }

    private static <R extends Recipe<?>> void addManualTransfer(
            IRecipeTransferRegistration registration,
            IRecipeTransferHandlerHelper helper,
            IRecipeType<RecipeHolder<R>> type) {
        registration.addRecipeTransferHandler(
                new ManualTransferHandler<>(
                        BoundSpiritMatrixMenu.class,
                        NepOccultismContent.MATRIX_MENU.get(),
                        type,
                        helper,
                        OccultismJeiCompat::requirements,
                        (holder, level) -> Recipes.idOf(holder)),
                type);
    }

    @Nullable
    private static List<ManualRequirement> requirements(RecipeHolder<? extends Recipe<?>> holder, Level level) {
        Ingredient ingredient = OccultismMatrixRecipes.ingredient(level, Recipes.idOf(holder));
        return ingredient == null ? null : List.of(new ManualRequirement(ingredient, 1, true));
    }

    @Nullable
    private static EncodedIngredients ritual(RecipeHolder<RitualRecipe> holder, Level level) {
        return NepConfig.occultismRitualController() ? OccultismRituals.ingredients(level, holder.value()) : null;
    }

    public static JeiTransferSource source() {
        return new JeiTransferSource() {
            @Override
            public void addTransfers(TransferCollector collector) {
                collector.add(JeiRecipeTypes.RITUAL, OccultismJeiCompat::ritual);
            }

            @Override
            public void addCatalysts(IRecipeCatalystRegistration registration) {
                if (NepConfig.occultismMatrix()) {
                    ItemStack matrix = new ItemStack(NepOccultismContent.MATRIX_ITEM.get());
                    registration.addCraftingStation(JeiRecipeTypes.SPIRIT_FIRE, matrix);
                    registration.addCraftingStation(JeiRecipeTypes.CRUSHING, matrix);
                    registration.addCraftingStation(JeiRecipeTypes.CRYSTALLIZE, matrix);
                }
                if (NepConfig.occultismRitualController()) {
                    registration.addCraftingStation(
                            JeiRecipeTypes.RITUAL, new ItemStack(NepOccultismContent.CONTROLLER_ITEM.get()));
                }
                if (NepConfig.occultismSpiritProvider()) {
                    ItemStack provider = new ItemStack(NepOccultismContent.SPIRIT_PROVIDER_ITEM.get());
                    registration.addCraftingStation(JeiRecipeTypes.CRUSHING, provider);
                    registration.addCraftingStation(JeiRecipeTypes.CRYSTALLIZE, provider);
                    registration.addCraftingStation(JeiRecipeTypes.SPIRIT_TRADE, provider);
                }
            }
        };
    }
}
