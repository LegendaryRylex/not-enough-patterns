package dev.rylex.nep.compat.actuallyadditions;

import de.ellpeck.actuallyadditions.mod.crafting.EmpowererRecipe;
import de.ellpeck.actuallyadditions.mod.crafting.LaserRecipe;
import de.ellpeck.actuallyadditions.mod.jei.JEIActuallyAdditionsPlugin;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.compat.jei.JeiTransferSource;
import dev.rylex.nep.pattern.encoding.EncodedIngredients;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class ActuallyAdditionsJeiCompat {
    private ActuallyAdditionsJeiCompat() {}

    public static JeiTransferSource source() {
        return new JeiTransferSource() {
            @Override
            public void addTransfers(TransferCollector collector) {
                collector.addUnwrapped(
                        JEIActuallyAdditionsPlugin.EMPOWERER,
                        ActuallyAdditionsJeiCompat::empowering,
                        ActuallyAdditionsJeiCompat::empoweringId);
                collector.addUnwrapped(
                        JEIActuallyAdditionsPlugin.LASER,
                        ActuallyAdditionsJeiCompat::laser,
                        ActuallyAdditionsJeiCompat::laserId);
            }

            @Override
            public void addCatalysts(IRecipeCatalystRegistration registration) {
                if (!NepConfig.actuallyAdditionsMatrix()) {
                    return;
                }
                ItemStack matrix = new ItemStack(NepActuallyAdditionsContent.MATRIX_ITEM.get());
                registration.addRecipeCatalyst(matrix, JEIActuallyAdditionsPlugin.EMPOWERER);
                registration.addRecipeCatalyst(matrix, JEIActuallyAdditionsPlugin.LASER);
            }
        };
    }

    @Nullable
    private static EncodedIngredients empowering(EmpowererRecipe recipe, Level level) {
        if (!NepConfig.actuallyAdditionsEmpowering() && !NepConfig.actuallyAdditionsMatrix()) {
            return null;
        }
        return ActuallyAdditionsRecipeIngredients.empowering(recipe);
    }

    @Nullable
    private static EncodedIngredients laser(LaserRecipe recipe, Level level) {
        if (!NepConfig.actuallyAdditionsAtomicReconstruction()) {
            return null;
        }
        return ActuallyAdditionsRecipeIngredients.laser(recipe, level);
    }

    @Nullable
    private static ResourceLocation empoweringId(EmpowererRecipe recipe, Level level) {
        for (RecipeHolder<EmpowererRecipe> holder : ActuallyAdditionsRecipeResolver.empoweringCandidates(level)) {
            if (holder.value() == recipe) {
                return holder.id();
            }
        }
        return null;
    }

    @Nullable
    private static ResourceLocation laserId(LaserRecipe recipe, Level level) {
        for (RecipeHolder<LaserRecipe> holder : ActuallyAdditionsRecipeResolver.laserCandidates(level)) {
            if (holder.value() == recipe) {
                return holder.id();
            }
        }
        return null;
    }
}
