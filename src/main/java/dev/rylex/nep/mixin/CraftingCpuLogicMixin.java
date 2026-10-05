package dev.rylex.nep.mixin;

import appeng.api.config.Actionable;
import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.crafting.execution.CraftingCpuLogic;
import appeng.crafting.inv.ListCraftingInventory;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.rylex.nep.machine.CraftedOutputs;
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

    @WrapOperation(
            method = "insert",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lappeng/crafting/inv/ListCraftingInventory;extract(Lappeng/api/stacks/AEKey;JLappeng/api/config/Actionable;)J"))
    private long nep$awaitCraftedForm(
            ListCraftingInventory waitingFor, AEKey what, long amount, Actionable mode, Operation<Long> original) {
        long direct = original.call(waitingFor, what, amount, mode);
        if (direct > 0) {
            return direct;
        }
        AEKey declared = CraftedOutputs.declaredFor(what);
        return declared == null ? 0L : original.call(waitingFor, declared, amount, mode);
    }

    @WrapOperation(
            method = "insert",
            at = @At(value = "INVOKE", target = "Lappeng/api/stacks/AEKey;matches(Lappeng/api/stacks/GenericStack;)Z"))
    private boolean nep$deliverCraftedForm(AEKey what, GenericStack finalOutput, Operation<Boolean> original) {
        if (original.call(what, finalOutput)) {
            return true;
        }
        AEKey declared = CraftedOutputs.declaredFor(what);
        return declared != null && original.call(declared, finalOutput);
    }
}
