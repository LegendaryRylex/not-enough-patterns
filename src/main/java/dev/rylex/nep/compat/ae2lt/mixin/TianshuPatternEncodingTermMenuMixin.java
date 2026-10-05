package dev.rylex.nep.compat.ae2lt.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.moakiee.ae2lt.menu.TianshuPatternEncodingTermMenu;
import dev.rylex.nep.pattern.encoding.EncodedPatternConverter;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * AE2LT 2.0.x reimplements {@code encode()} and reaches AE2's {@code encodePattern()} through an invoker, so nep's own
 * menu mixin never sees the result; 2.1.0 calls {@code super.encode()} instead and these injectors then find no target.
 */
@Mixin(TianshuPatternEncodingTermMenu.class)
public abstract class TianshuPatternEncodingTermMenuMixin {

    @ModifyReturnValue(method = "previewAe2EncodingCandidate", at = @At("RETURN"), require = 0)
    private ItemStack nep$convertEncodingCandidate(ItemStack candidate) {
        ItemStack converted = ((EncodedPatternConverter) this).nep$convertEncoded(candidate);
        return converted == null ? ItemStack.EMPTY : converted;
    }

    @Inject(method = "encodeServerWithOptions", at = @At("RETURN"), require = 0)
    private void nep$finishEncoding(Boolean interceptDuplicateUpload, CallbackInfo ci) {
        ((EncodedPatternConverter) this).nep$encodeFinished();
    }
}
