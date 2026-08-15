package dev.rylex.nep.compat.thunderbolt.mixin;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.pathing.ChannelMode;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.moakiee.ae2lt.grid.BorrowedCapacityCalculator;
import dev.rylex.nep.compat.thunderbolt.ThunderboltChannels;
import java.util.List;
import java.util.Set;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(BorrowedCapacityCalculator.class)
public abstract class BorrowedCapacityCalculatorMixin {

    private static final String DINIC = "Lcom/moakiee/ae2lt/grid/BorrowedCapacityCalculator$Dinic;";

    @ModifyArg(method = "solve", at = @At(value = "INVOKE", target = DINIC + "addEdge(III)V", ordinal = 5), index = 2)
    private static int nep$sinkTakesWholeDemand(int one, @Local(ordinal = 0) IGridNode node) {
        return ThunderboltChannels.sinkCapacity(node);
    }

    @ModifyArg(method = "solve", at = @At(value = "INVOKE", target = DINIC + "maxFlow(III)I"), index = 2)
    private static int nep$targetWholeDemand(int sinks) {
        return ThunderboltChannels.claimedCapacity();
    }

    @WrapOperation(
            method = "assignChannels",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lcom/moakiee/ae2lt/grid/BorrowedCapacityCalculator;solve"
                                    + "(Lappeng/api/networking/IGrid;Ljava/util/List;Ljava/util/Set;"
                                    + "Lappeng/api/networking/pathing/ChannelMode;)"
                                    + "Lcom/moakiee/ae2lt/grid/BorrowedCapacityCalculator$Result;"))
    private static BorrowedCapacityCalculator.Result nep$freePartFedMachines(
            IGrid grid,
            List<IGridNode> controllers,
            Set<IGridNode> network,
            ChannelMode mode,
            Operation<BorrowedCapacityCalculator.Result> original) {
        return ThunderboltChannels.solveUntilNobodyIsPartFed(
                () -> original.call(grid, controllers, network, mode), network);
    }
}
