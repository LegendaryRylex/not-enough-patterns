package dev.rylex.nep.compat.advancedae;

import appeng.client.gui.WidgetContainer;
import appeng.client.gui.style.ScreenStyle;
import appeng.menu.AEBaseMenu;
import dev.rylex.nep.NepSlotSemantics;
import dev.rylex.nep.client.ImportUpgradePanels;
import dev.rylex.nep.mixin.ScreenStyleAccessor;

public final class AdvProviderUpgradePanel {

    private AdvProviderUpgradePanel() {}

    public static void add(ScreenStyle style, AEBaseMenu menu, WidgetContainer widgets) {
        if (style instanceof ScreenStyleAccessor accessor) {
            ImportUpgradePanels.add(menu.getSlots(NepSlotSemantics.IMPORT_UPGRADE), accessor.nep$widgets(), widgets);
        }
    }
}
