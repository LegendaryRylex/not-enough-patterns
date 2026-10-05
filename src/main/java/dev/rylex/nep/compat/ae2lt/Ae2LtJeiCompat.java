package dev.rylex.nep.compat.ae2lt;

import com.moakiee.ae2lt.menu.TianshuPatternEncodingTermMenu;
import com.moakiee.ae2lt.menu.TianshuWirelessPatternEncodingTermMenu;
import dev.rylex.nep.compat.jei.PatternMenuBinder;

public final class Ae2LtJeiCompat {
    private Ae2LtJeiCompat() {}

    public static void bindPatternMenus(PatternMenuBinder binder) {
        binder.bind(TianshuPatternEncodingTermMenu.class, TianshuPatternEncodingTermMenu.TYPE);
        binder.bind(TianshuWirelessPatternEncodingTermMenu.class, TianshuWirelessPatternEncodingTermMenu.TYPE);
    }
}
