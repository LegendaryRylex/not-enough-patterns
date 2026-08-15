package dev.rylex.nep.compat.thunderbolt.mixin;

import appeng.me.service.PathingService;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.rylex.nep.compat.thunderbolt.ThunderboltChannels;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PathingService.class)
public abstract class PathingServiceChannelCountMixin {

    @ModifyExpressionValue(
            method = "onServerEndTick",
            at = @At(value = "INVOKE", target = "Lappeng/me/pathfinding/PathingCalculation;getChannelsInUse()I"))
    private int nep$countChannelsNotMachines(int machines) {
        int fed = ThunderboltChannels.consumeFedDemand();
        return fed < 0 ? machines : fed;
    }
}
