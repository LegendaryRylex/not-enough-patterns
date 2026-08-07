package dev.rylex.nep.compat.create.mixin;

import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.kinetics.deployer.BeltDeployerCallbacks;
import com.simibubi.create.content.kinetics.deployer.DeployerBlockEntity;
import dev.rylex.nep.compat.create.DeployerReclaimer;
import net.minecraft.world.item.crafting.Recipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BeltDeployerCallbacks.class)
public class BeltDeployerCallbacksMixin {

    @Inject(method = "activate", at = @At("RETURN"))
    private static void nep$reclaimAfterCraft(
            TransportedItemStack transported,
            TransportedItemStackHandlerBehaviour handler,
            DeployerBlockEntity blockEntity,
            Recipe<?> recipe,
            CallbackInfo ci) {
        DeployerReclaimer.onCrafted(blockEntity);
    }
}
