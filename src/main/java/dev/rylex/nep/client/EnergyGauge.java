package dev.rylex.nep.client;

import dev.rylex.nep.Nep;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public final class EnergyGauge {

    public static final int WIDTH = 15;
    public static final int HEIGHT = 51;

    private static final Identifier TEXTURE = Nep.id("textures/gui/energy_gauge.png");

    private static final int SHEET_WIDTH = 32;
    private static final int SHEET_HEIGHT = 64;
    private static final int EMPTY_U = 0;
    private static final int FULL_U = 16;

    private EnergyGauge() {}

    public static void draw(GuiGraphicsExtractor graphics, int x, int y, long stored, long capacity) {
        draw(graphics, TEXTURE, x, y, stored, capacity);
    }

    public static void draw(
            GuiGraphicsExtractor graphics, Identifier texture, int x, int y, long stored, long capacity) {
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                texture,
                x,
                y,
                (float) EMPTY_U,
                0.0F,
                WIDTH,
                HEIGHT,
                SHEET_WIDTH,
                SHEET_HEIGHT);
        int filled = capacity <= 0 ? 0 : (int) Math.min(HEIGHT, HEIGHT * Math.max(0L, stored) / capacity);
        if (filled > 0) {
            int top = HEIGHT - filled;
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    texture,
                    x,
                    y + top,
                    (float) FULL_U,
                    (float) top,
                    WIDTH,
                    filled,
                    SHEET_WIDTH,
                    SHEET_HEIGHT);
        }
    }
}
