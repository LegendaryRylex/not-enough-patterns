package dev.rylex.nep.compat.malum.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.sammy.malum.common.block.curiosities.spirit_altar.SpiritAltarBlockEntity;
import dev.rylex.nep.compat.malum.SpiritReclaimWrap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SpiritAltarBlockEntity.class)
public class SpiritAltarBlockEntityMixin {

    @WrapOperation(
            method = "craft",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/server/level/ServerLevel;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean nep$returnInfusionToProvider(ServerLevel level, Entity entity, Operation<Boolean> original) {
        return SpiritReclaimWrap.addFreshEntity((SpiritAltarBlockEntity) (Object) this, false, level, entity, original);
    }
}
