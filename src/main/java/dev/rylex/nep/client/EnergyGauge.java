package dev.rylex.nep.client;

import dev.rylex.nep.Nep;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public final class EnergyGauge {

    public static final int WIDTH = 15;
    public static final int HEIGHT = 51;

    private static final ResourceLocation TEXTURE = Nep.id("textures/gui/energy_gauge.png");

    private static final int SHEET_WIDTH = 32;
    private static final int SHEET_HEIGHT = 64;
    private static final int EMPTY_U = 0;
    private static final int FULL_U = 16;

    private EnergyGauge() {}

    public static void draw(GuiGraphics graphics, int x, int y, long stored, long capacity) {
        graphics.blit(TEXTURE, x, y, EMPTY_U, 0, WIDTH, HEIGHT, SHEET_WIDTH, SHEET_HEIGHT);
        int filled = capacity <= 0 ? 0 : (int) Math.min(HEIGHT, HEIGHT * Math.max(0L, stored) / capacity);
        if (filled > 0) {
            int top = HEIGHT - filled;
            graphics.blit(TEXTURE, x, y + top, FULL_U, top, WIDTH, filled, SHEET_WIDTH, SHEET_HEIGHT);
        }
    }
}
