package dev.rylex.nep.compat.advancedae.mixin;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.style.ScreenStyle;
import dev.rylex.nep.compat.advancedae.AdvProviderUpgradePanel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.pedroksl.advanced_ae.client.gui.AdvPatternProviderScreen;
import net.pedroksl.advanced_ae.gui.advpatternprovider.AdvPatternProviderMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AdvPatternProviderScreen.class)
public abstract class AdvPatternProviderScreenMixin extends AEBaseScreen<AdvPatternProviderMenu> {

    private AdvPatternProviderScreenMixin(
            AdvPatternProviderMenu menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void nep$addUpgradePanel(
            AdvPatternProviderMenu menu,
            Inventory playerInventory,
            Component title,
            ScreenStyle style,
            CallbackInfo ci) {
        AdvProviderUpgradePanel.add(style, menu, widgets);
    }
}
