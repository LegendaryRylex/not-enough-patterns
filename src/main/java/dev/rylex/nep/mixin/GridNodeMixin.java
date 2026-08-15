package dev.rylex.nep.mixin;

import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridNode;
import appeng.me.GridNode;
import dev.rylex.nep.machine.ChannelDemand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GridNode.class)
public abstract class GridNodeMixin {

    @Shadow
    public abstract int getUsedChannels();

    @Shadow
    public abstract boolean hasFlag(GridFlags flag);

    @Inject(method = "meetsChannelRequirements", at = @At("RETURN"), cancellable = true)
    private void nep$requireWholeDemand(CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() || !hasFlag(GridFlags.REQUIRE_CHANNEL)) {
            return;
        }
        if (getUsedChannels() < ChannelDemand.of((IGridNode) (Object) this)) {
            cir.setReturnValue(false);
        }
    }
}
