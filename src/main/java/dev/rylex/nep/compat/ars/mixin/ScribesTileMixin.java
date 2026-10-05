package dev.rylex.nep.compat.ars.mixin;

import com.hollingsworth.arsnouveau.common.block.tile.ScribesTile;
import dev.rylex.nep.compat.ars.ArcaneLecternBlockEntity;
import dev.rylex.nep.compat.ars.ArcaneLecterns;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.BlockCapability;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScribesTile.class)
public abstract class ScribesTileMixin {

    @Inject(method = "takeNearby", at = @At("HEAD"), cancellable = true)
    private void nep$supplyFromLecterns(CallbackInfo ci) {
        if (ArcaneLecterns.supply((ScribesTile) (Object) this)) {
            ci.cancel();
        }
    }

    /** Arcane Lecterns serve the table through {@link ArcaneLecterns#supply}, so the table's own scan passes over them. */
    @Redirect(
            method = "takeNearby",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/Level;getCapability(Lnet/neoforged/neoforge/capabilities/BlockCapability;Lnet/minecraft/core/BlockPos;Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object nep$skipLecterns(Level level, BlockCapability<?, ?> capability, BlockPos pos, Object context) {
        if (level.getBlockEntity(pos) instanceof ArcaneLecternBlockEntity) {
            return null;
        }
        return level.getCapability(castCapability(capability), pos, context);
    }

    @SuppressWarnings("unchecked")
    private static BlockCapability<Object, Object> castCapability(BlockCapability<?, ?> capability) {
        return (BlockCapability<Object, Object>) capability;
    }
}
