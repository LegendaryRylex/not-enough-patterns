package dev.rylex.nep.mixin;

import appeng.api.stacks.GenericStack;
import appeng.integration.modules.itemlists.EncodingHelper;
import appeng.menu.me.items.PatternEncodingTermMenu;
import dev.rylex.nep.pattern.encoding.PatternOrigin;
import dev.rylex.nep.pattern.encoding.PatternRecipeHolder;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EncodingHelper.class)
public abstract class EncodingHelperMixin {

    @Inject(method = "encodeProcessingRecipe", at = @At("RETURN"))
    private static void nep$markRecipeViewerTransfer(
            PatternEncodingTermMenu menu,
            List<List<GenericStack>> genericIngredients,
            List<GenericStack> genericResults,
            CallbackInfo ci) {
        ((PatternRecipeHolder) menu).nep$setOrigin(PatternOrigin.RECIPE_VIEWER);
    }
}
