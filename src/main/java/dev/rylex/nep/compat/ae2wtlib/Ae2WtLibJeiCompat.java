package dev.rylex.nep.compat.ae2wtlib;

import de.mari_023.ae2wtlib.wet.WETMenu;
import dev.rylex.nep.compat.jei.PatternMenuBinder;

public final class Ae2WtLibJeiCompat {
    private Ae2WtLibJeiCompat() {}

    public static void bindPatternMenus(PatternMenuBinder binder) {
        binder.bind(WETMenu.class, WETMenu.TYPE);
    }
}
