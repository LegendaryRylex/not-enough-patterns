package dev.rylex.nep.compat.malum.mixin;

import com.sammy.malum.core.handlers.SoulHarvestHandler;
import com.sammy.malum.core.systems.spirit.EntitySpiritDropData;
import dev.rylex.nep.compat.malum.PureSpiritDrops;
import java.util.List;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoulHarvestHandler.class)
public class SoulHarvestHandlerMixin {

    @Inject(method = "applySpiritLootBonuses", at = @At("RETURN"))
    private static void nep$dropPureSpirit(
            EntitySpiritDropData data,
            LivingEntity target,
            LivingEntity attacker,
            CallbackInfoReturnable<List<ItemStack>> cir) {
        PureSpiritDrops.appendPureSpirit(cir.getReturnValue(), target, attacker);
    }
}
