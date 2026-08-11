package dev.rylex.nep.mixin;

import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.style.WidgetStyle;
import java.util.Map;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ScreenStyle.class)
public interface ScreenStyleAccessor {

    @Accessor("widgets")
    Map<String, WidgetStyle> nep$widgets();
}
