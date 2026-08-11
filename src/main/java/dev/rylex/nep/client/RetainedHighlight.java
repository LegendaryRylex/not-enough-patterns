package dev.rylex.nep.client;

import dev.rylex.nep.pattern.RetainedInputs;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class RetainedHighlight {
    private RetainedHighlight() {}

    private static final int SIZE = 16;

    public static void draw(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.fill(x, y, x + SIZE, y + 1, RetainedInputs.HIGHLIGHT);
        graphics.fill(x, y + SIZE - 1, x + SIZE, y + SIZE, RetainedInputs.HIGHLIGHT);
        graphics.fill(x, y + 1, x + 1, y + SIZE - 1, RetainedInputs.HIGHLIGHT);
        graphics.fill(x + SIZE - 1, y + 1, x + SIZE, y + SIZE - 1, RetainedInputs.HIGHLIGHT);
    }
}
