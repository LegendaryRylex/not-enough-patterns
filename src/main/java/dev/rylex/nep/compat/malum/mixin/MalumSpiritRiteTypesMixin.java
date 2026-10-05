package dev.rylex.nep.compat.malum.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.sammy.malum.common.block.curiosities.totem.TotemBaseBlockEntity;
import com.sammy.malum.core.systems.rite.SpiritRiteType;
import com.sammy.malum.registry.common.magic.rite.MalumSpiritRiteTypes;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Malum walks its own DeferredRegister rather than the rite registry, so a rite registered by any other mod is never
 * offered to a totem base.
 */
@Mixin(MalumSpiritRiteTypes.class)
public class MalumSpiritRiteTypesMixin {

    @ModifyReturnValue(method = "getRite", at = @At("RETURN"))
    private static SpiritRiteType nep$matchAddonRites(
            SpiritRiteType found, ServerLevel level, TotemBaseBlockEntity totemBase) {
        if (found != null) {
            return found;
        }
        for (SpiritRiteType rite : MalumSpiritRiteTypes.RITE_REGISTRY) {
            if (rite.matches(level, totemBase)) {
                return rite;
            }
        }
        return null;
    }
}
