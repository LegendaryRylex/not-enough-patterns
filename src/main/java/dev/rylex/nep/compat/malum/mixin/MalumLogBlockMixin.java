package dev.rylex.nep.compat.malum.mixin;

import com.sammy.malum.common.block.nature.MalumLogBLock;
import com.sammy.malum.core.systems.spirit.SpiritTypeProperty;
import com.sammy.malum.core.systems.spirit.type.SpiritLike;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Carving stores only the spirit's path in the blockstate, so a spirit the totem property never learned about throws
 * out of setValue and takes the server with it.
 */
@Mixin(MalumLogBLock.class)
public class MalumLogBlockMixin {

    @Inject(method = "createTotemPole", at = @At("HEAD"), cancellable = true)
    private void nep$refuseUncarvableSpirits(
            ServerLevel level,
            BlockPos pos,
            Direction direction,
            SpiritLike spirit,
            CallbackInfoReturnable<Boolean> cir) {
        if (!SpiritTypeProperty.SPIRIT_TYPE.getPossibleValues().contains(spirit.getName())) {
            cir.setReturnValue(false);
        }
    }
}
