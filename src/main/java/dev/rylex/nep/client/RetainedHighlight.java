package dev.rylex.nep.client;

import dev.rylex.nep.pattern.RetainedInputs;
import net.minecraft.client.gui.GuiGraphics;

public final class RetainedHighlight {
    private RetainedHighlight() {}

    private static final int SIZE = 16;

    public static void draw(GuiGraphics graphics, int x, int y) {
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 300.0F);
        graphics.hLine(x, x + SIZE - 1, y, RetainedInputs.HIGHLIGHT);
        graphics.hLine(x, x + SIZE - 1, y + SIZE - 1, RetainedInputs.HIGHLIGHT);
        graphics.vLine(x, y, y + SIZE - 1, RetainedInputs.HIGHLIGHT);
        graphics.vLine(x + SIZE - 1, y, y + SIZE - 1, RetainedInputs.HIGHLIGHT);
        graphics.pose().popPose();
    }
}
