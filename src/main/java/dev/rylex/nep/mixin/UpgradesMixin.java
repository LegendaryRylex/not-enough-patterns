package dev.rylex.nep.mixin;

import appeng.api.upgrades.Upgrades;
import dev.rylex.nep.NepItems;
import dev.rylex.nep.provider.ImportCardUpgrades;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ItemLike;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Upgrades.class)
public abstract class UpgradesMixin {

    @Inject(method = "getTooltipLinesForCard", at = @At("HEAD"), cancellable = true)
    private static void nep$importCardSupportedBy(ItemLike card, CallbackInfoReturnable<List<Component>> cir) {
        if (card.asItem() == NepItems.IMPORT_CARD.get()) {
            cir.setReturnValue(ImportCardUpgrades.supportedBy());
        }
    }
}
