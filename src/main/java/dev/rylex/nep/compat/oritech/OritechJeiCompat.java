package dev.rylex.nep.compat.oritech;

import dev.rylex.nep.NepConfig;
import dev.rylex.nep.compat.jei.JeiTransferSource;
import dev.rylex.nep.compat.jei.ManualTransferHandler;
import dev.rylex.nep.pattern.encoding.EncodedIngredients;
import dev.rylex.nep.util.Recipes;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.recipe.types.IRecipeHolderType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.init.recipes.OritechRecipe;
import rearth.oritech.init.recipes.RecipeContent;

public final class OritechJeiCompat {
    private OritechJeiCompat() {}

    private static final IRecipeHolderType<OritechRecipe> PARTICLE_COLLISION =
            IRecipeHolderType.create(RecipeContent.PARTICLE_COLLISION.get());

    public static void addManualTransfers(
            IRecipeTransferRegistration registration, IRecipeTransferHandlerHelper helper) {
        registration.addRecipeTransferHandler(
                new ManualTransferHandler<>(
                        ParticleCollisionMatrixMenu.class,
                        NepOritechContent.MATRIX_MENU.get(),
                        PARTICLE_COLLISION,
                        helper,
                        (holder, level) -> OritechRecipeIngredients.manualRequirements(holder.value()),
                        (holder, level) -> Recipes.idOf(holder)),
                PARTICLE_COLLISION);
    }

    public static JeiTransferSource source() {
        return new JeiTransferSource() {
            @Override
            public void addTransfers(TransferCollector collector) {
                collector.add(PARTICLE_COLLISION, OritechJeiCompat::collision);
            }

            @Override
            public void addCatalysts(IRecipeCatalystRegistration registration) {
                if (NepConfig.oritechParticleCollisionMatrix()) {
                    registration.addCraftingStation(
                            PARTICLE_COLLISION, new ItemStack(NepOritechContent.MATRIX_ITEM.get()));
                }
                if (NepConfig.oritechParticleCollisionController()) {
                    registration.addCraftingStation(
                            PARTICLE_COLLISION, new ItemStack(NepOritechContent.CONTROLLER_ITEM.get()));
                }
            }
        };
    }

    @Nullable
    private static EncodedIngredients collision(RecipeHolder<OritechRecipe> holder, Level level) {
        return NepConfig.oritechParticleCollision() ? OritechRecipeIngredients.collision(holder.value(), level) : null;
    }
}
