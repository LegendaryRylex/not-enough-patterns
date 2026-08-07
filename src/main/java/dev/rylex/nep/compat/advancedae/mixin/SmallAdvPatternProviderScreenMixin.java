package dev.rylex.nep.compat.advancedae.mixin;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.UpgradesPanel;
import dev.rylex.nep.NepSlotSemantics;
import dev.rylex.nep.client.ImportUpgradePanels;
import dev.rylex.nep.mixin.ScreenStyleAccessor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.fml.ModList;
import net.pedroksl.advanced_ae.client.gui.SmallAdvPatternProviderScreen;
import net.pedroksl.advanced_ae.gui.advpatternprovider.SmallAdvPatternProviderMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SmallAdvPatternProviderScreen.class)
public abstract class SmallAdvPatternProviderScreenMixin extends AEBaseScreen<SmallAdvPatternProviderMenu> {

    private SmallAdvPatternProviderScreenMixin(
            SmallAdvPatternProviderMenu menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void nep$addUpgradePanel(
            SmallAdvPatternProviderMenu menu,
            Inventory playerInventory,
            Component title,
            ScreenStyle style,
            CallbackInfo ci) {
        var upgradeSlots = menu.getSlots(NepSlotSemantics.IMPORT_UPGRADE);
        if (upgradeSlots.isEmpty() || !(style instanceof ScreenStyleAccessor accessor)) {
            return;
        }

        var widgetStyles = accessor.nep$widgets();
        var stock = widgetStyles.get(ImportUpgradePanels.STOCK_WIDGET_ID);
        if (stock == null) {
            return;
        }

        widgetStyles.put(
                ImportUpgradePanels.WIDGET_ID, ImportUpgradePanels.stackedStyle(stock, nep$stackedPanelOffset()));
        widgets.add(
                ImportUpgradePanels.WIDGET_ID,
                new UpgradesPanel(upgradeSlots, ImportUpgradePanels::compatibleUpgrades));
    }

    @Unique
    private static int nep$stackedPanelOffset() {
        return ModList.get().isLoaded("appflux") ? ImportUpgradePanels.PANEL_SPACING : 0;
    }
}
