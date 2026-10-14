package dev.rylex.nep.compat.malum.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.sammy.malum.common.block.curiosities.runic_workbench.RunicWorkbenchBlockEntity;
import com.sammy.malum.common.recipe.RuneworkingRecipe;
import dev.rylex.nep.compat.malum.RunicWorkbenchState;
import dev.rylex.nep.compat.malum.SpiritReclaimWrap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(RunicWorkbenchBlockEntity.class)
public class RunicWorkbenchBlockEntityMixin implements RunicWorkbenchState {

    @Shadow
    protected RuneworkingRecipe.RunicWorkbenchRecipeInput input;

    @Override
    public boolean nep$isCrafting() {
        return input != null;
    }

    @WrapOperation(
            method = "craft",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/server/level/ServerLevel;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean nep$returnRuneToProvider(ServerLevel level, Entity entity, Operation<Boolean> original) {
        return SpiritReclaimWrap.addFreshEntity(
                (RunicWorkbenchBlockEntity) (Object) this, false, level, entity, original);
    }
}
