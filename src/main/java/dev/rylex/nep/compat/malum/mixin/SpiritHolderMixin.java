package dev.rylex.nep.compat.malum.mixin;

import com.sammy.malum.core.systems.registry.SpiritHolder;
import dev.rylex.nep.Nep;
import dev.rylex.nep.compat.malum.NepTotemSpirits;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(SpiritHolder.class)
public abstract class SpiritHolderMixin {

    @ModifyVariable(
            method =
                    "getSpiritType(Lnet/minecraft/resources/ResourceLocation;)Lcom/sammy/malum/core/systems/registry/SpiritHolder;",
            at = @At("HEAD"),
            argsOnly = true)
    private static ResourceLocation nep$claimReservedPaths(ResourceLocation spirit) {
        if (!NepTotemSpirits.isNepPath(spirit.getPath())) {
            return spirit;
        }
        String namespace = spirit.getNamespace();
        return namespace.equals("minecraft") || namespace.equals("malum") ? Nep.id(spirit.getPath()) : spirit;
    }
}
