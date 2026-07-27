package dev.rylex.nep.compat.jei;

import appeng.menu.me.items.PatternEncodingTermMenu;
import net.minecraft.world.inventory.MenuType;

public interface PatternMenuBinder {
    <T extends PatternEncodingTermMenu> void bind(Class<T> menuClass, MenuType<T> menuType);
}
