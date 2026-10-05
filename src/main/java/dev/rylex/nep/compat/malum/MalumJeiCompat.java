package dev.rylex.nep.compat.malum;

import com.sammy.malum.common.recipe.RuneworkingRecipe;
import com.sammy.malum.common.recipe.SpiritFocusingRecipe;
import com.sammy.malum.common.recipe.SpiritInfusionRecipe;
import com.sammy.malum.common.recipe.spirit_repair.SpiritRepairRecipe;
import com.sammy.malum.compat.jei.JEIHandler;
import dev.rylex.nep.Nep;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.compat.jei.JeiTransferSource;
import dev.rylex.nep.compat.jei.ManualTransferHandler;
import dev.rylex.nep.compat.jei.PatternTransferHandler;
import dev.rylex.nep.pattern.encoding.EncodedIngredients;
import java.util.List;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class MalumJeiCompat {
    private MalumJeiCompat() {}

    private static final RecipeType<SpiritInfusionRecipe> SPIRIT_INFUSION =
            RecipeType.create("malum", "spirit_infusion", SpiritInfusionRecipe.class);

    private static final RecipeType<SpiritFocusingRecipe> SPIRIT_FOCUSING =
            RecipeType.create("malum", "spirit_focusing", SpiritFocusingRecipe.class);

    private static final ResourceLocation IMPETUS_REPAIR = Nep.id("fractured_matrix_impetus_restoration");

    @Nullable
    private static IJeiRuntime runtime;

    public static void addManualTransfers(
            IRecipeTransferRegistration registration, IRecipeTransferHandlerHelper helper) {
        addManualTransfer(
                registration,
                helper,
                SPIRIT_INFUSION,
                (recipe, level) -> MalumRecipeIngredients.manualSpiritInfusion(recipe),
                MalumJeiCompat::recipeId);
        addManualTransfer(
                registration,
                helper,
                SPIRIT_FOCUSING,
                (recipe, level) -> MalumRecipeIngredients.manualSpiritFocusing(recipe),
                MalumJeiCompat::focusingRecipeId);
        addManualTransfer(
                registration,
                helper,
                JEIHandler.RUNEWORKING,
                (recipe, level) -> MalumRecipeIngredients.manualRuneworking(recipe),
                MalumJeiCompat::runeworkingRecipeId);
    }

    private static <T> void addManualTransfer(
            IRecipeTransferRegistration registration,
            IRecipeTransferHandlerHelper helper,
            RecipeType<T> recipeType,
            ManualTransferHandler.Requirements<T> requirements,
            PatternTransferHandler.Identifier<T> identifier) {
        registration.addRecipeTransferHandler(
                new ManualTransferHandler<>(
                        FocusedSpiritMatrixMenu.class,
                        NepMalumContent.MATRIX_MENU.get(),
                        recipeType,
                        helper,
                        requirements,
                        identifier),
                recipeType);
    }

    public static JeiTransferSource source() {
        return new JeiTransferSource() {
            @Override
            public void addTransfers(TransferCollector collector) {
                collector.addUnwrapped(SPIRIT_INFUSION, MalumJeiCompat::spiritInfusion, MalumJeiCompat::recipeId);
                collector.addUnwrapped(
                        SPIRIT_FOCUSING, MalumJeiCompat::spiritFocusing, MalumJeiCompat::focusingRecipeId);
                collector.addUnwrapped(
                        JEIHandler.RUNEWORKING, MalumJeiCompat::runeworking, MalumJeiCompat::runeworkingRecipeId);
            }

            @Override
            public void addCatalysts(IRecipeCatalystRegistration registration) {
                if (!NepConfig.malumFocusedSpiritMatrix()) {
                    return;
                }
                ItemStack matrix = new ItemStack(NepMalumContent.MATRIX_ITEM.get());
                registration.addRecipeCatalyst(matrix, SPIRIT_INFUSION);
                if (NepConfig.malumFocusedSpiritMatrixRuneworking()) {
                    registration.addRecipeCatalyst(matrix, JEIHandler.RUNEWORKING);
                }
                if (NepConfig.malumFocusedSpiritMatrixFocusing()) {
                    registration.addRecipeCatalyst(matrix, SPIRIT_FOCUSING);
                    registration.addRecipeCatalyst(
                            new ItemStack(NepMalumContent.MATRIX_IMPETUS.get()), SPIRIT_FOCUSING);
                }
            }
        };
    }

    public static void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
        MalumClientCompat.onJeiRuntime(MalumJeiCompat::refreshImpetusRepair);
        refreshImpetusRepair();
    }

    /**
     * The gate is a JEI one rather than a recipe condition because the server config is not loaded when datapacks are
     * read.
     */
    private static void refreshImpetusRepair() {
        IJeiRuntime jeiRuntime = runtime;
        Level level = Minecraft.getInstance().level;
        if (jeiRuntime == null || level == null) {
            return;
        }
        RecipeHolder<?> holder = level.getRecipeManager().byKey(IMPETUS_REPAIR).orElse(null);
        if (holder == null || !(holder.value() instanceof SpiritRepairRecipe repair)) {
            return;
        }
        if (NepConfig.malumFocusedSpiritMatrixConsumeImpetusDurability()) {
            jeiRuntime.getRecipeManager().unhideRecipes(JEIHandler.SPIRIT_REPAIR, List.of(repair));
        } else {
            jeiRuntime.getRecipeManager().hideRecipes(JEIHandler.SPIRIT_REPAIR, List.of(repair));
        }
    }

    @Nullable
    private static EncodedIngredients spiritFocusing(SpiritFocusingRecipe recipe, Level level) {
        return NepConfig.malumSpiritFocusing() ? MalumRecipeIngredients.spiritFocusing(recipe) : null;
    }

    @Nullable
    private static ResourceLocation focusingRecipeId(SpiritFocusingRecipe recipe, Level level) {
        for (RecipeHolder<SpiritFocusingRecipe> holder : SpiritFocusingResolver.candidates(level)) {
            if (holder.value() == recipe) {
                return holder.id();
            }
        }
        return null;
    }

    @Nullable
    private static EncodedIngredients runeworking(RuneworkingRecipe recipe, Level level) {
        return NepConfig.malumRuneworking() ? MalumRecipeIngredients.runeworking(recipe) : null;
    }

    @Nullable
    private static ResourceLocation runeworkingRecipeId(RuneworkingRecipe recipe, Level level) {
        for (RecipeHolder<RuneworkingRecipe> holder : RuneworkingResolver.candidates(level)) {
            if (holder.value() == recipe) {
                return holder.id();
            }
        }
        return null;
    }

    @Nullable
    private static EncodedIngredients spiritInfusion(SpiritInfusionRecipe recipe, Level level) {
        return NepConfig.malumSpiritInfusion() ? MalumRecipeIngredients.spiritInfusion(recipe) : null;
    }

    @Nullable
    private static ResourceLocation recipeId(SpiritInfusionRecipe recipe, Level level) {
        for (RecipeHolder<SpiritInfusionRecipe> holder : SpiritInfusionResolver.candidates(level)) {
            if (holder.value() == recipe) {
                return holder.id();
            }
        }
        return null;
    }
}
