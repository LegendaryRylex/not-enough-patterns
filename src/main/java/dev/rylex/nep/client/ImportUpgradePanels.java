package dev.rylex.nep.client;

import appeng.client.gui.style.WidgetStyle;
import appeng.core.localization.GuiText;
import dev.rylex.nep.NepItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public final class ImportUpgradePanels {

    public static final String WIDGET_ID = "nepImportUpgrades";
    public static final String STOCK_WIDGET_ID = "upgrades";
    public static final int PANEL_SPACING = 32;

    private ImportUpgradePanels() {}

    public static WidgetStyle stackedStyle(WidgetStyle stock, int offset) {
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

    public static List<Component> compatibleUpgrades() {
        var lines = new ArrayList<Component>();
        lines.add(GuiText.CompatibleUpgrades.text());
        lines.add(GuiText.CompatibleUpgrade.text(NepItems.IMPORT_CARD.get().getDescription(), 1)
                .withStyle(ChatFormatting.GRAY));
        return lines;
    }
}
