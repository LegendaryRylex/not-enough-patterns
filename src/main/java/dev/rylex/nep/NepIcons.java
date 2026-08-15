package dev.rylex.nep;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class NepIcons {

    public static final ResourceLocation FONT = Nep.id("icons");

    public static final Component INPUT = icon("\uE000");
    public static final Component OUTPUT = icon("\uE001");
    public static final Component BOTH = icon("\uE002");
    public static final Component CONFIG = icon("\uE003");
    public static final Component REMOVE = icon("\uE004");
    public static final Component SCAN = icon("\uE005");
    public static final Component DOWN = icon("\uE006");
    public static final Component HELP = icon("\uE007");

    private NepIcons() {}

    private static Component icon(String glyph) {
        return Component.literal(glyph).withStyle(style -> style.withFont(FONT));
    }
}
