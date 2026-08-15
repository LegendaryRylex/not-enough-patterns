package dev.rylex.nep.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public final class ReadoutButton extends AbstractButton {

    public static final int SIZE = 13;

    private static final int PRESS_GLOW_TICKS = 5;

    private static final int BEVEL = 2;
    private static final int GLYPH_SIZE = 8;
    private static final int GLYPH_INSET = BEVEL + (SIZE - BEVEL - 1 - GLYPH_SIZE) / 2;

    private static final int SHADOW = 0xFF2E271C;
    private static final int FACE = 0xFF8A8172;
    private static final int FACE_HOVER = 0xFFB09A72;
    private static final int HIGHLIGHT = 0xFFE6DDC6;
    private static final int GLYPH = 0xFF3A2E1C;
    private static final int GLYPH_HOVER = 0xFF8C2A2A;

    private Component glyph;
    private final Runnable action;
    private int pressGlow;

    public ReadoutButton(int x, int y, Component glyph, Component title, Component description, Runnable action) {
        super(x, y, SIZE, SIZE, title);
        this.glyph = glyph;
        this.action = action;
        setTooltip(Tooltip.create(Component.empty().append(title).append("\n").append(description)));
    }

    public void setGlyph(Component glyph) {
        this.glyph = glyph;
    }

    @Override
    public void onPress() {
        pressGlow = PRESS_GLOW_TICKS;
        action.run();
    }

    public void tick() {
        if (pressGlow > 0) {
            pressGlow--;
        }
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int left = getX();
        int top = getY();
        int right = left + width;
        int bottom = top + height;
        boolean lit = active
                && (isHovered()
                        || pressGlow > 0
                        || (isFocused()
                                && Minecraft.getInstance().getLastInputType().isKeyboard()));
        graphics.fill(left, top, right, bottom, SHADOW);
        graphics.fill(left + 1, top + 1, right - 1, bottom - 1, lit ? FACE_HOVER : FACE);
        graphics.fill(left + 1, top + 1, right - 1, top + 2, HIGHLIGHT);
        graphics.fill(left + 1, top + 1, left + 2, bottom - 1, HIGHLIGHT);
        Font font = Minecraft.getInstance().font;
        graphics.drawString(font, glyph, left + GLYPH_INSET, top + GLYPH_INSET, lit ? GLYPH_HOVER : GLYPH, false);
        if (!active) {
            graphics.fill(left + 1, top + 1, right - 1, bottom - 1, 0x80CBBE9E);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
