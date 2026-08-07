package dev.rylex.nep.client;

import dev.rylex.nep.Nep;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * A tank fill drawn from a greyscale strip multiplied by the contents' own colour, the same way a mod
 * tints one texture per variant. The strip carries the shading and grain; nothing in it is specific to
 * what is being held, so repainting the texture restyles every tank without touching code, and nothing
 * in it stands for a quantity, since the well is a fixed height while capacity is configurable. The
 * bottom of the strip is sourced so the pattern stays fixed to the well as the level moves, and the
 * surface row is blitted over the fill line lightened, which reads as a meniscus.
 */
public final class TankGauge {

    private static final ResourceLocation TEXTURE = Nep.id("textures/gui/essence_tank.png");

    private static final int SHEET_WIDTH = 8;
    private static final int SHEET_HEIGHT = 64;
    private static final int STRIP_HEIGHT = 51;
    private static final int SURFACE_V = 52;
    private static final float SURFACE_LIFT = 0.35F;

    private TankGauge() {}

    public static void draw(
            GuiGraphics graphics, int x, int y, int width, int height, long amount, long capacity, int tint) {
        if (amount <= 0) {
            return;
        }
        int span = Math.min(height, STRIP_HEIGHT);
        int filled = capacity <= 0 ? span : (int) Math.min(span, Math.max(1L, span * amount / capacity));
        int top = y + height - filled;
        setTint(graphics, tint, 0.0F);
        graphics.blit(TEXTURE, x, top, 0, STRIP_HEIGHT - filled, width, filled, SHEET_WIDTH, SHEET_HEIGHT);
        setTint(graphics, tint, SURFACE_LIFT);
        graphics.blit(TEXTURE, x, top, 0, SURFACE_V, width, 1, SHEET_WIDTH, SHEET_HEIGHT);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void setTint(GuiGraphics graphics, int tint, float lift) {
        graphics.setColor(channel(tint >> 16, lift), channel(tint >> 8, lift), channel(tint, lift), 1.0F);
    }

    private static float channel(int shifted, float lift) {
        float value = (shifted & 0xFF) / 255.0F;
        return value + (1.0F - value) * lift;
    }
}
