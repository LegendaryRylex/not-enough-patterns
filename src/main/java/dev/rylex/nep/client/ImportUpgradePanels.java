package dev.rylex.nep.client;

import appeng.client.gui.WidgetContainer;
import appeng.client.gui.style.WidgetStyle;
import appeng.core.localization.GuiText;
import dev.rylex.nep.NepItems;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;

public final class ImportUpgradePanels {

    public static final String WIDGET_ID = "nepImportUpgrades";
    public static final String STOCK_WIDGET_ID = "upgrades";

    private ImportUpgradePanels() {}

    public static void add(List<Slot> slots, Map<String, WidgetStyle> widgetStyles, WidgetContainer widgets) {
        var stock = widgetStyles.get(STOCK_WIDGET_ID);
        if (slots.isEmpty() || stock == null) {
            return;
        }

        widgetStyles.put(WIDGET_ID, stock);
        widgets.add(WIDGET_ID, new StackedUpgradesPanel(slots, ImportUpgradePanels::compatibleUpgrades, widgets));
    }

    public static List<Component> compatibleUpgrades() {
        var lines = new ArrayList<Component>();
        lines.add(GuiText.CompatibleUpgrades.text());
        lines.add(GuiText.CompatibleUpgrade.text(NepItems.IMPORT_CARD.get().getDescription(), 1)
                .withStyle(ChatFormatting.GRAY));
        return lines;
    }
}
