package dev.rylex.nep.compat.emi.mixin;

import appeng.integration.modules.emi.EmiEncodePatternHandler;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.jemi.JemiRecipe;
import dev.rylex.nep.compat.viewer.EncodableCategories;
import net.neoforged.fml.ModList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "appeng.integration.modules.emi.AbstractRecipeHandler")
public abstract class AbstractRecipeHandlerMixin {

    @Inject(method = "supportsRecipe", at = @At("HEAD"), cancellable = true)
    private void nep$declineTypedRecipes(EmiRecipe recipe, CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof EmiEncodePatternHandler)
                || !ModList.get().isLoaded("jei")) {
            return;
        }
        if (recipe instanceof JemiRecipe<?>
                && EncodableCategories.contains(recipe.getCategory().getId())) {
            cir.setReturnValue(false);
        }
    }
}
