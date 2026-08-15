package dev.rylex.nep.client;

import dev.rylex.nep.NepIcons;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

public abstract class MatrixScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {

    protected static final int READOUT_WARN = 0xFFFF7A4A;

    protected static final int READOUT_X = 12;
    protected static final int READOUT_RIGHT = 164;
    protected static final int LINE_ONE_Y = 22;
    protected static final int LINE_TWO_Y = 32;
    protected static final int LINE_THREE_Y = 42;
    protected static final int BAR_X = 12;
    protected static final int BAR_Y = 52;
    protected static final int BAR_WIDTH = READOUT_RIGHT - READOUT_X;
    protected static final int BAR_HEIGHT = 4;

    public static final int HELP_BUTTON_INDEX = 1;

    private final List<ReadoutButton> buttons = new ArrayList<>();

    protected MatrixScreen(T menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
    }

    protected MatrixScreen(T menu, Inventory playerInv, Component title, int width, int height) {
        super(menu, playerInv, title, width, height);
    }

    protected abstract Identifier texture();

    protected int sheetHeight() {
        return 256;
    }

    protected abstract void extractReadoutTooltips(GuiGraphicsExtractor graphics, int mouseX, int mouseY);

    protected void resetButtons() {
        buttons.clear();
    }

    protected void tickButtons() {
        for (ReadoutButton button : buttons) {
            button.tick();
        }
    }

    protected int borderWidth() {
        return 4;
    }

    protected ReadoutButton addRightButton(
            int index, Component glyph, String nameKey, String hintKey, Runnable action) {
        ReadoutButton button = addRenderableWidget(new ReadoutButton(
                leftPos + imageWidth - index * ReadoutButton.SIZE - borderWidth() - 4 * index,
                topPos + borderWidth(),
                glyph,
                Component.translatable(nameKey),
                Component.translatable(hintKey),
                action));
        buttons.add(button);
        return button;
    }

    protected ReadoutButton addHelpButton(String page) {
        return addRightButton(
                HELP_BUTTON_INDEX, NepIcons.HELP, "gui.nep.help", "gui.nep.help.hint", () -> NepGuide.open(page));
    }

    protected void sendButton(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    protected static float clamp01(float value) {
        return Math.min(1.0F, Math.max(0.0F, value));
    }

    protected void drawTrailing(GuiGraphicsExtractor graphics, Component text, int y, int occupiedUntil, int colour) {
        int x = READOUT_RIGHT - font.width(text);
        if (x < occupiedUntil + 6) {
            return;
        }
        graphics.text(font, text, x, y, colour, false);
    }

    protected String trim(Component text, int width) {
        String plain = text.getString();
        if (font.width(plain) <= width) {
            return plain;
        }
        return font.plainSubstrByWidth(plain, Math.max(0, width - font.width("…"))) + "…";
    }

    protected void drawProgressBar(GuiGraphicsExtractor graphics, float progress, int track, int fill) {
        graphics.fill(BAR_X, BAR_Y, BAR_X + BAR_WIDTH, BAR_Y + BAR_HEIGHT, 0xFF000000 | track);
        int filled = Math.round(BAR_WIDTH * clamp01(progress));
        if (filled > 0) {
            graphics.fill(BAR_X, BAR_Y, BAR_X + filled, BAR_Y + BAR_HEIGHT, 0xFF000000 | fill);
        }
    }

    protected boolean within(int mouseX, int mouseY, int x, int y, int width, int height) {
        int localX = mouseX - leftPos;
        int localY = mouseY - topPos;
        return localX >= x && localX < x + width && localY >= y && localY < y + height;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                texture(),
                leftPos,
                topPos,
                0.0F,
                0.0F,
                imageWidth,
                imageHeight,
                256,
                sheetHeight());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        extractReadoutTooltips(graphics, mouseX, mouseY);
    }
}
