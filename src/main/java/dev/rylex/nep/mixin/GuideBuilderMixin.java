package dev.rylex.nep.mixin;

import dev.rylex.nep.client.NepConfigValueTag;
import dev.rylex.nep.client.NepGuide;
import guideme.Guide;
import guideme.GuideBuilder;
import guideme.compiler.TagCompiler;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GuideBuilder.class)
public abstract class GuideBuilderMixin {

    @Shadow
    @Final
    private ResourceLocation id;

    @Inject(method = "build", at = @At("HEAD"))
    private void nep$addConfigValueTag(CallbackInfoReturnable<Guide> cir) {
        if (NepGuide.AE2_GUIDE.equals(id)) {
            ((GuideBuilder) (Object) this).extension(TagCompiler.EXTENSION_POINT, new NepConfigValueTag());
        }
    }
}
