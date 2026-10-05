package dev.rylex.nep.compat.malum.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.sammy.malum.common.block.curiosities.spirit_crucible.SpiritCrucibleCoreBlockEntity;
import com.sammy.malum.core.systems.artifice.ArtificeAttributeData;
import dev.rylex.nep.compat.malum.ImpetusWard;
import dev.rylex.nep.compat.malum.SpiritReclaimer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SpiritCrucibleCoreBlockEntity.class)
public class SpiritCrucibleCoreBlockEntityMixin {

    @WrapOperation(
            method = "craft",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lcom/sammy/malum/common/item/augment/ShieldingApparatusItem;shieldImpetus(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lcom/sammy/malum/core/systems/artifice/ArtificeAttributeData;)Z"))
    private boolean nep$preserveImpetus(
            Level level, BlockPos pos, ArtificeAttributeData attributes, Operation<Boolean> original) {
        return ImpetusWard.shields((SpiritCrucibleCoreBlockEntity) (Object) this)
                || original.call(level, pos, attributes);
    }

    @WrapOperation(
            method = "craft",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/server/level/ServerLevel;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean nep$returnFocusingToProvider(ServerLevel level, Entity entity, Operation<Boolean> original) {
        if (entity instanceof ItemEntity item) {
            ItemStack remaining =
                    SpiritReclaimer.reclaimKeeping((SpiritCrucibleCoreBlockEntity) (Object) this, item.getItem());
            if (remaining.isEmpty()) {
                return true;
            }
            item.setItem(remaining);
        }
        return original.call(level, entity);
    }

    @Unique
    private boolean nep$recipeRestored;

    /** Malum only restores the recipe on load when the level is set, which it never is during chunk load. */
    @Inject(method = "serverTick", at = @At("HEAD"))
    private void nep$restoreRecipeAfterLoad(ServerLevel level, CallbackInfo callback) {
        if (nep$recipeRestored) {
            return;
        }
        nep$recipeRestored = true;
        SpiritCrucibleCoreBlockEntity crucible = (SpiritCrucibleCoreBlockEntity) (Object) this;
        if (crucible.recipe == null && SpiritReclaimer.pending(crucible)) {
            crucible.updateRecipe();
        }
    }

    @Inject(method = "craft", at = @At("RETURN"))
    private void nep$endReclaim(ServerLevel level, CallbackInfo callback) {
        SpiritReclaimer.forget((SpiritCrucibleCoreBlockEntity) (Object) this);
    }
}
