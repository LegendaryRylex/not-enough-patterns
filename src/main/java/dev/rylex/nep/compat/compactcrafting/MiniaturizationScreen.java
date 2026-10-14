package dev.rylex.nep.compat.compactcrafting;

import appeng.api.stacks.GenericStack;
import dev.rylex.nep.NepIcons;
import dev.rylex.nep.client.MatrixScreen;
import dev.rylex.nep.client.ReadoutButton;
import dev.rylex.nep.util.MissingStacks;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

abstract class MiniaturizationScreen<T extends AbstractContainerMenu> extends MatrixScreen<T> {

    protected static final int TITLE_ON_PANEL = 0x3A2E1C;
    protected static final int READOUT_TEXT = 0xF2E8D0;
    protected static final int READOUT_DIM = 0xC8A882;

    private static final int BAR_TRACK = 0x0D333D;
    private static final int CRAFT_FILL = 0x208BFF;

    @Nullable
    private ReadoutButton clearPending;

    protected MiniaturizationScreen(T menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
    }

    protected void addClearButtons(String helpPage, int clearPendingId, int clearBufferId) {
        resetButtons();
        addHelpButton(helpPage);
        clearPending = addRightButton(
                2,
                NepIcons.REMOVE,
                "gui.nep.clear_pending",
                "gui.nep.clear_pending.hint",
                () -> sendButton(clearPendingId));
        addRightButton(
                3, NepIcons.DOWN, "gui.nep.clear_buffer", "gui.nep.clear_buffer.hint", () -> sendButton(clearBufferId));
    }

    protected void tickClearButtons(boolean clearPendingActive) {
        if (clearPending != null) {
            clearPending.active = clearPendingActive;
        }
        tickButtons();
    }

    protected void drawTitles(
            GuiGraphics graphics, String prefix, int inputX, int inputY, int outputX, int outputY, int invX, int invY) {
        graphics.drawString(font, Component.translatable(prefix + ".title"), 8, 7, TITLE_ON_PANEL, false);
        graphics.drawString(
                font, Component.translatable(prefix + ".input"), inputX, inputY - 10, TITLE_ON_PANEL, false);
        graphics.drawString(
                font, Component.translatable(prefix + ".output"), outputX, outputY - 10, TITLE_ON_PANEL, false);
        graphics.drawString(
                font, Component.translatable("container.inventory"), invX, invY - 10, TITLE_ON_PANEL, false);
    }

    protected void drawReadout(
            GuiGraphics graphics,
            String prefix,
            Component status,
            int statusColour,
            Component fieldText,
            ItemStack making,
            long queued,
            int craftTicks,
            float craftProgress) {
        graphics.drawString(font, status, READOUT_X, LINE_ONE_Y, statusColour, false);
        drawTrailing(graphics, fieldText, LINE_ONE_Y, READOUT_X + font.width(status), READOUT_DIM);

        Component craftingLabel = Component.translatable(prefix + ".crafting");
        graphics.drawString(font, craftingLabel, READOUT_X, LINE_TWO_Y, READOUT_DIM, false);
        int craftingX = READOUT_X + font.width(craftingLabel) + 4;
        Component queuedText = queued > 0 ? Component.translatable(prefix + ".queued", queued) : Component.empty();
        int nameLimit = readoutRight() - craftingX - (queued > 0 ? font.width(queuedText) + 6 : 0);
        Component craftingName =
                making.isEmpty() ? Component.translatable(prefix + ".crafting.idle") : making.getHoverName();
        String craftingText = trim(craftingName, nameLimit);
        graphics.drawString(
                font, craftingText, craftingX, LINE_TWO_Y, making.isEmpty() ? READOUT_DIM : READOUT_TEXT, false);
        if (queued > 0) {
            drawTrailing(graphics, queuedText, LINE_TWO_Y, craftingX + font.width(craftingText), READOUT_DIM);
        }

        if (craftTicks > 0) {
            graphics.drawString(
                    font,
                    Component.translatable(prefix + ".craft_time", craftTicks),
                    READOUT_X,
                    LINE_THREE_Y,
                    READOUT_DIM,
                    false);
        }

        drawProgressBar(graphics, craftProgress, BAR_TRACK, CRAFT_FILL);
    }

    protected void drawReadoutTooltip(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            String prefix,
            Component status,
            List<Component> hint,
            ItemStack making,
            long queued,
            List<GenericStack> missing) {
        List<Component> lines = new ArrayList<>();
        lines.add(status);
        lines.addAll(hint);
        if (!making.isEmpty()) {
            lines.add(Component.translatable(prefix + ".crafting_tooltip", making.getHoverName())
                    .withStyle(ChatFormatting.GRAY));
        }
        if (queued > 0) {
            lines.add(Component.translatable(prefix + ".queued_tooltip", queued).withStyle(ChatFormatting.GRAY));
        }
        if (!missing.isEmpty()) {
            lines.add(Component.translatable(prefix + ".missing"));
            for (GenericStack stack : missing) {
                lines.add(MissingStacks.describe(stack).withStyle(ChatFormatting.GRAY));
            }
        }
        graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
    }

    protected boolean overReadout(int mouseX, int mouseY) {
        return within(mouseX, mouseY, READOUT_X, LINE_ONE_Y, barWidth(), READOUT_HEIGHT);
    }
}
