package dev.rylex.nep.compat.occultism.mixin;

import com.klikli_dev.occultism.common.blockentity.GoldenSacrificialBowlBlockEntity;
import com.klikli_dev.occultism.common.ritual.Ritual;
import dev.rylex.nep.compat.occultism.RitualClaims;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Ritual.class)
public class RitualMixin {

    @Inject(method = "dropResult", at = @At("HEAD"), cancellable = true)
    private void nep$deliverToController(
            Level level,
            BlockPos goldenBowlPosition,
            GoldenSacrificialBowlBlockEntity blockEntity,
            Player castingPlayer,
            ItemStack stack,
            boolean realDrop,
            CallbackInfo ci) {
        if (RitualClaims.deliver(level, goldenBowlPosition, stack)) {
            ci.cancel();
        }
    }
}
