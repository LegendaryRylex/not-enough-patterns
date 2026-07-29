package dev.rylex.nep.mixin;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.implementations.PatternProviderScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.style.WidgetStyle;
import appeng.client.gui.widgets.UpgradesPanel;
import appeng.core.localization.GuiText;
import appeng.menu.implementations.PatternProviderMenu;
import dev.rylex.nep.NepItems;
import dev.rylex.nep.NepSlotSemantics;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.fml.ModList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PatternProviderScreen.class)
public abstract class PatternProviderScreenMixin extends AEBaseScreen<PatternProviderMenu> {

    @Unique
    private static final String NEP$WIDGET_ID = "nepImportUpgrades";

    @Unique
    private static final String NEP$STOCK_WIDGET_ID = "upgrades";

    @Unique
    private static final int NEP$PANEL_SPACING = 32;

    private PatternProviderScreenMixin(
            PatternProviderMenu menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void nep$addUpgradePanel(
            PatternProviderMenu menu, Inventory playerInventory, Component title, ScreenStyle style, CallbackInfo ci) {
        var upgradeSlots = menu.getSlots(NepSlotSemantics.IMPORT_UPGRADE);
        if (upgradeSlots.isEmpty() || !(style instanceof ScreenStyleAccessor accessor)) {
            return;
        }

        var widgetStyles = accessor.nep$widgets();
        var stock = widgetStyles.get(NEP$STOCK_WIDGET_ID);
        if (stock == null) {
            return;
        }

        widgetStyles.put(NEP$WIDGET_ID, nep$panelStyle(stock));
        widgets.add(NEP$WIDGET_ID, new UpgradesPanel(upgradeSlots, this::nep$getCompatibleUpgrades));
    }

    @Unique
    private WidgetStyle nep$panelStyle(WidgetStyle stock) {
        int offset = nep$stackedPanelOffset();
        if (offset == 0) {
            return stock;
        }

        var moved = new WidgetStyle();
        moved.setLeft(stock.getLeft());
        moved.setRight(stock.getRight());
        moved.setTop((stock.getTop() == null ? 0 : stock.getTop()) + stock.getHeight() + offset);
        moved.setWidth(stock.getWidth());
        moved.setHeight(stock.getHeight());
        moved.setHideEdge(stock.isHideEdge());
        return moved;
    }

    @Unique
    private int nep$stackedPanelOffset() {
        var mods = ModList.get();
        int offset = 0;
        if (mods.isLoaded("appflux") || mods.isLoaded("mesoulcard")) {
            offset += NEP$PANEL_SPACING;
        }
        if (mods.isLoaded("ae2helpers")) {
            offset += NEP$PANEL_SPACING;
        }
        return offset;
    }

    @Unique
    private List<Component> nep$getCompatibleUpgrades() {
        var lines = new ArrayList<Component>();
        lines.add(GuiText.CompatibleUpgrades.text());
        lines.add(GuiText.CompatibleUpgrade.text(NepItems.IMPORT_CARD.get().getDescription(), 1)
                .withStyle(ChatFormatting.GRAY));
        return lines;
    }
}
