package dev.rylex.nep.compat.draconic.mixin;

import com.brandon3055.draconicevolution.api.crafting.IFusionInventory;
import com.brandon3055.draconicevolution.api.crafting.IFusionRecipe;
import com.brandon3055.draconicevolution.api.crafting.IFusionStateMachine;
import com.brandon3055.draconicevolution.blocks.tileentity.TileFusionCraftingCore;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.rylex.nep.compat.draconic.FusionReclaimer;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TileFusionCraftingCore.class)
public class TileFusionCraftingCoreMixin {

    @WrapOperation(
            method = "tick",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lcom/brandon3055/draconicevolution/api/crafting/IFusionRecipe;tickFusionState(Lcom/brandon3055/draconicevolution/api/crafting/IFusionStateMachine;Lcom/brandon3055/draconicevolution/api/crafting/IFusionInventory;Lnet/minecraft/world/level/Level;)V"))
    private void nep$reclaimOnceTheOutputExists(
            IFusionRecipe recipe,
            IFusionStateMachine machine,
            IFusionInventory inventory,
            Level level,
            Operation<Void> original) {
        TileFusionCraftingCore core = (TileFusionCraftingCore) (Object) this;
        FusionReclaimer.beginFusionState(core);
        try {
            original.call(recipe, machine, inventory, level);
        } finally {
            FusionReclaimer.endFusionState(core);
        }
    }

    @Inject(method = "completeCraft", at = @At("RETURN"))
    private void nep$reclaimAfterCraft(CallbackInfo ci) {
        FusionReclaimer.onCraftCompleted((TileFusionCraftingCore) (Object) this);
    }

    @Inject(method = "cancelCraft", at = @At("RETURN"))
    private void nep$reclaimAfterCancel(CallbackInfo ci) {
        FusionReclaimer.onCraftCancelled((TileFusionCraftingCore) (Object) this);
    }
}
