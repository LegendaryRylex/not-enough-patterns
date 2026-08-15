package dev.rylex.nep.mixin;

import appeng.api.networking.IGridNode;
import appeng.api.networking.pathing.ChannelMode;
import appeng.me.service.AdHocNetworkError;
import appeng.me.service.PathingService;
import dev.rylex.nep.machine.ChannelDemand;
import java.util.Set;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PathingService.class)
public abstract class PathingServiceMixin {

    @Shadow
    @Final
    private Set<IGridNode> nodesNeedingChannels;

    @Shadow
    private AdHocNetworkError adHocNetworkError;

    @Shadow
    private ChannelMode channelMode;

    @Inject(method = "calculateAdHocChannels", at = @At("RETURN"), cancellable = true)
    private void nep$countWholeDemand(CallbackInfoReturnable<Integer> cir) {
        int channels = cir.getReturnValueI();
        if (channels == 0) {
            return;
        }
        int extra = 0;
        for (IGridNode node : nodesNeedingChannels) {
            extra += Math.max(1, ChannelDemand.of(node)) - 1;
        }
        if (extra == 0) {
            return;
        }
        int total = channels + extra;
        if (total > channelMode.getAdHocNetworkChannels()) {
            adHocNetworkError = AdHocNetworkError.TOO_MANY_CHANNELS;
            cir.setReturnValue(0);
        } else {
            cir.setReturnValue(total);
        }
    }
}
