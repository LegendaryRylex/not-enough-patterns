package dev.rylex.nep.mixin;

import appeng.me.GridNode;
import appeng.me.pathfinding.IPathItem;
import appeng.me.pathfinding.PathingCalculation;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.rylex.nep.machine.ChannelDemand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(PathingCalculation.class)
public abstract class PathingCalculationMixin {

    @Shadow
    private int channelsInUse;

    @ModifyExpressionValue(
            method = "enqueue",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lappeng/me/pathfinding/IPathItem;hasFlag(Lappeng/api/networking/GridFlags;)Z",
                            ordinal = 0))
    private boolean nep$claimNoPathPreference(boolean dense, @Local(argsOnly = true) IPathItem pathItem) {
        return dense && !(pathItem instanceof GridNode node && node.getOwner() instanceof ChannelDemand);
    }

    @ModifyExpressionValue(
            method = "tryUseChannel(Lappeng/me/GridNode;)Z",
            at = @At(value = "INVOKE", target = "Lappeng/me/GridNode;getMaxChannels()I"))
    private int nep$reserveWholeDemand(int max, @Local(argsOnly = true) GridNode start) {
        return max - (nep$demandOf(start) - 1);
    }

    @ModifyArg(
            method = "tryUseChannel(Lappeng/me/GridNode;)Z",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lit/unimi/dsi/fastutil/objects/Reference2IntOpenHashMap;addTo(Ljava/lang/Object;I)I"),
            index = 1)
    private int nep$allocateWholeDemand(int increment, @Local(argsOnly = true) GridNode start) {
        return nep$demandOf(start);
    }

    @WrapOperation(
            method = "propagateAssignments",
            at = @At(value = "INVOKE", target = "Lappeng/me/GridNode;propagateChannelsUpwards(Z)I"))
    private int nep$countWholeDemand(GridNode node, boolean hasChannel, Operation<Integer> original) {
        int used = original.call(node, hasChannel);
        if (!hasChannel) {
            return used;
        }
        int extra = nep$demandOf(node) - 1;
        if (extra <= 0) {
            return used;
        }
        node.incrementChannelCount(extra);
        channelsInUse += extra;
        return used + extra;
    }

    private static int nep$demandOf(GridNode node) {
        return Math.max(1, ChannelDemand.of(node));
    }
}
