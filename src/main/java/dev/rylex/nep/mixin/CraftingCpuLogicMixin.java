package dev.rylex.nep.mixin;

import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.stacks.KeyCounter;
import appeng.crafting.execution.CraftingCpuLogic;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.rylex.nep.machine.PushingCpuContext;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(CraftingCpuLogic.class)
public abstract class CraftingCpuLogicMixin {

    @Shadow
    @Final
    CraftingCPUCluster cluster;

    @WrapOperation(
            method = "executeCrafting",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lappeng/api/networking/crafting/ICraftingProvider;pushPattern(Lappeng/api/crafting/IPatternDetails;[Lappeng/api/stacks/KeyCounter;)Z"))
    private boolean nep$markPushingCpu(
            ICraftingProvider provider, IPatternDetails details, KeyCounter[] inputs, Operation<Boolean> original) {
        PushingCpuContext.enter(cluster);
        try {
            return original.call(provider, details, inputs);
        } finally {
            PushingCpuContext.exit();
        }
    }
}
