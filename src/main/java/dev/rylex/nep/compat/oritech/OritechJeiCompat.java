package dev.rylex.nep.compat.oritech;

import dev.rylex.nep.NepConfig;
import dev.rylex.nep.compat.jei.JeiTransferSource;
import dev.rylex.nep.compat.jei.ManualTransferHandler;
import dev.rylex.nep.pattern.encoding.EncodedIngredients;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.init.recipes.OritechRecipe;
import rearth.oritech.init.recipes.RecipeContent;

public final class OritechJeiCompat {
    private OritechJeiCompat() {}

    private static final RecipeType<OritechRecipe> PARTICLE_COLLISION = RecipeType.create(
            RecipeContent.PARTICLE_COLLISION.getIdentifier().getNamespace(),
            RecipeContent.PARTICLE_COLLISION.getIdentifier().getPath(),
            OritechRecipe.class);

    public static void addManualTransfers(
            IRecipeTransferRegistration registration, IRecipeTransferHandlerHelper helper) {
        registration.addRecipeTransferHandler(
                new ManualTransferHandler<>(
                        ParticleCollisionMatrixMenu.class,
                        NepOritechContent.MATRIX_MENU.get(),
                        PARTICLE_COLLISION,
                        helper,
                        (recipe, level) -> OritechRecipeIngredients.manualRequirements(recipe),
                        OritechJeiCompat::collisionId),
                PARTICLE_COLLISION);
    }

    public static JeiTransferSource source() {
        return new JeiTransferSource() {
            @Override
            public void addTransfers(TransferCollector collector) {
                collector.addUnwrapped(PARTICLE_COLLISION, OritechJeiCompat::collision, OritechJeiCompat::collisionId);
            }

            @Override
            public void addCatalysts(IRecipeCatalystRegistration registration) {
                if (NepConfig.oritechParticleCollisionMatrix()) {
                    registration.addRecipeCatalyst(
                            new ItemStack(NepOritechContent.MATRIX_ITEM.get()), PARTICLE_COLLISION);
                }
                if (NepConfig.oritechParticleCollisionController()) {
                    registration.addRecipeCatalyst(
                            new ItemStack(NepOritechContent.CONTROLLER_ITEM.get()), PARTICLE_COLLISION);
                }
            }
        };
    }

    @Nullable
    private static EncodedIngredients collision(OritechRecipe recipe, Level level) {
        return NepConfig.oritechParticleCollision() ? OritechRecipeIngredients.collision(recipe) : null;
    }

    private record IdIndex(List<RecipeHolder<OritechRecipe>> source, Map<OritechRecipe, ResourceLocation> ids) {}

    private static volatile IdIndex idIndex = new IdIndex(List.of(), Map.of());

    @Nullable
    private static ResourceLocation collisionId(OritechRecipe recipe, Level level) {
        List<RecipeHolder<OritechRecipe>> candidates = OritechRecipeResolver.collisionCandidates(level);
        IdIndex index = idIndex;
        if (index.source() != candidates) {
            Map<OritechRecipe, ResourceLocation> ids = new IdentityHashMap<>();
            for (RecipeHolder<OritechRecipe> holder : candidates) {
                ids.putIfAbsent(holder.value(), holder.id());
            }
            index = new IdIndex(candidates, ids);
            idIndex = index;
        }
        return index.ids().get(recipe);
    }
}
