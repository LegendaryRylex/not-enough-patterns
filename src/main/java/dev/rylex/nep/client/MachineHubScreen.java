package dev.rylex.nep.client;

import dev.rylex.nep.Nep;
import dev.rylex.nep.hub.MachineHubMenu;
import dev.rylex.nep.hub.MachineHubState;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class MachineHubScreen extends MatrixScreen<MachineHubMenu> {

    private static final ResourceLocation TEXTURE = Nep.id("textures/gui/machine_hub.png");

    private static final int TITLE_ON_PANEL = 0x3A2E1C;
    private static final int READOUT_TEXT = 0xF2E8D0;
    private static final int READOUT_DIM = 0xC8A882;
    private static final int READOUT_ERROR = 0xFF5555;
    private static final int SCROLL_TRACK = 0xFF1A1410;
    private static final int SCROLL_THUMB = 0xFFC8A882;

    private static final int ROLE_X = 10;
    private static final int ICON_X = 24;
    private static final int NAME_X = 44;
    private static final int REMOVE_X = 170;
    private static final int GLYPH_WIDTH = 12;
    private static final int MINIMUM_THUMB = 12;

    private MachineHubState state = MachineHubState.empty();
    private int scroll;
    private boolean draggingThumb;

    public MachineHubScreen(MachineHubMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth = MachineHubMenu.WIDTH;
        this.imageHeight = MachineHubMenu.HEIGHT;
    }

    public void acceptState(MachineHubState state) {
        this.state = state;
        scroll = Math.min(scroll, maxScroll());
    }

    @Override
    protected ResourceLocation texture() {
        return TEXTURE;
    }

    @Override
    protected void init() {
        super.init();
        resetButtons();
        addRightButton(
                1,
                "✗",
                "gui.nep.machine_hub.clear",
                "gui.nep.machine_hub.clear.hint",
                () -> sendButton(MachineHubMenu.BUTTON_CLEAR));
        addRightButton(
                2,
                "⟳",
                "gui.nep.machine_hub.scan",
                "gui.nep.machine_hub.scan.hint",
                () -> sendButton(MachineHubMenu.BUTTON_SCAN));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        tickButtons();
    }

    private int maxScroll() {
        return Math.max(0, state.entries().size() - MachineHubMenu.VISIBLE_ROWS);
    }

    private int shownRows() {
        return Math.min(state.entries().size(), MachineHubMenu.VISIBLE_ROWS);
    }

    private int thumbHeight() {
        int total = state.entries().size();
        if (total <= MachineHubMenu.VISIBLE_ROWS) {
            return MachineHubMenu.SCROLL_HEIGHT;
        }
        return Math.max(MINIMUM_THUMB, MachineHubMenu.SCROLL_HEIGHT * MachineHubMenu.VISIBLE_ROWS / total);
    }

    private int thumbY() {
        int max = maxScroll();
        if (max == 0) {
            return MachineHubMenu.ROW_Y;
        }
        return MachineHubMenu.ROW_Y + (MachineHubMenu.SCROLL_HEIGHT - thumbHeight()) * scroll / max;
    }

    private void scrollTo(double mouseY) {
        int max = maxScroll();
        if (max == 0) {
            return;
        }
        int travel = MachineHubMenu.SCROLL_HEIGHT - thumbHeight();
        double top = mouseY - topPos - MachineHubMenu.ROW_Y - thumbHeight() / 2.0;
        scroll = Math.max(0, Math.min(max, (int) Math.round(top / travel * max)));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (maxScroll() > 0
                    && within(
                            (int) mouseX,
                            (int) mouseY,
                            MachineHubMenu.SCROLL_X,
                            MachineHubMenu.ROW_Y,
                            MachineHubMenu.SCROLL_WIDTH,
                            MachineHubMenu.SCROLL_HEIGHT)) {
                draggingThumb = true;
                scrollTo(mouseY);
                return true;
            }
            for (int row = 0; row < shownRows(); row++) {
                int y = MachineHubMenu.ROW_Y + row * MachineHubMenu.ROW_HEIGHT;
                if (within((int) mouseX, (int) mouseY, ROLE_X, y, GLYPH_WIDTH, MachineHubMenu.ROW_HEIGHT)) {
                    sendButton(MachineHubMenu.BUTTON_ROLE + scroll + row);
                    return true;
                }
                if (within((int) mouseX, (int) mouseY, REMOVE_X, y, GLYPH_WIDTH, MachineHubMenu.ROW_HEIGHT)) {
                    sendButton(MachineHubMenu.BUTTON_REMOVE + scroll + row);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingThumb && button == 0) {
            scrollTo(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            draggingThumb = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int max = maxScroll();
        if (max > 0
                && scrollY != 0
                && within(
                        (int) mouseX,
                        (int) mouseY,
                        MachineHubMenu.ROW_X,
                        MachineHubMenu.ROW_Y,
                        MachineHubMenu.SCROLL_X + MachineHubMenu.SCROLL_WIDTH - MachineHubMenu.ROW_X,
                        MachineHubMenu.SCROLL_HEIGHT)) {
            scroll = Math.max(0, Math.min(max, scroll - (int) Math.signum(scrollY)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, Component.translatable("gui.nep.machine_hub.title"), 8, 7, TITLE_ON_PANEL, false);

        List<MachineHubState.Entry> entries = state.entries();
        Component error = state.status().message();
        if (!state.enabled()) {
            graphics.drawString(
                    font,
                    Component.translatable("gui.nep.machine_hub.disabled"),
                    READOUT_X,
                    LINE_ONE_Y,
                    READOUT_WARN,
                    false);
        } else if (error != null) {
            graphics.drawString(font, error, READOUT_X, LINE_ONE_Y, READOUT_ERROR, false);
        } else if (entries.isEmpty()) {
            graphics.drawString(
                    font,
                    Component.translatable("gui.nep.machine_hub.empty"),
                    READOUT_X,
                    LINE_ONE_Y,
                    READOUT_DIM,
                    false);
        } else if (maxScroll() > 0) {
            graphics.drawString(
                    font,
                    Component.translatable(
                            "gui.nep.machine_hub.linked_scrolled", scroll + 1, scroll + shownRows(), entries.size()),
                    READOUT_X,
                    LINE_ONE_Y,
                    READOUT_DIM,
                    false);
        } else {
            graphics.drawString(
                    font,
                    Component.translatable("gui.nep.machine_hub.linked", entries.size()),
                    READOUT_X,
                    LINE_ONE_Y,
                    READOUT_DIM,
                    false);
        }

        for (int row = 0; row < shownRows(); row++) {
            drawRow(graphics, entries.get(scroll + row), MachineHubMenu.ROW_Y + row * MachineHubMenu.ROW_HEIGHT);
        }
        if (maxScroll() > 0) {
            drawScrollbar(graphics);
        }
    }

    private void drawScrollbar(GuiGraphics graphics) {
        int x = MachineHubMenu.SCROLL_X;
        int right = x + MachineHubMenu.SCROLL_WIDTH;
        graphics.fill(
                x, MachineHubMenu.ROW_Y, right, MachineHubMenu.ROW_Y + MachineHubMenu.SCROLL_HEIGHT, SCROLL_TRACK);
        int top = thumbY();
        graphics.fill(x + 1, top, right - 1, top + thumbHeight(), SCROLL_THUMB);
    }

    private void drawRow(GuiGraphics graphics, MachineHubState.Entry entry, int y) {
        graphics.drawString(font, entry.role().glyph(), ROLE_X, y + 3, READOUT_TEXT, false);
        if (!entry.icon().isEmpty()) {
            graphics.renderFakeItem(entry.icon(), ICON_X, y);
        }
        Component name = entry.icon().isEmpty()
                ? Component.translatable("gui.nep.machine_hub.unknown_block")
                : entry.icon().getHoverName();
        int colour = entry.healthy() ? READOUT_TEXT : READOUT_WARN;
        graphics.drawString(font, trim(name, REMOVE_X - NAME_X - 4), NAME_X, y + 3, colour, false);
        graphics.drawString(font, "✗", REMOVE_X, y + 3, READOUT_DIM, false);
    }

    @Override
    protected void renderReadoutTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        List<MachineHubState.Entry> entries = state.entries();
        for (int row = 0; row < shownRows(); row++) {
            int y = MachineHubMenu.ROW_Y + row * MachineHubMenu.ROW_HEIGHT;
            if (!within(mouseX, mouseY, MachineHubMenu.ROW_X, y, MachineHubMenu.ROW_WIDTH, MachineHubMenu.ROW_HEIGHT)) {
                continue;
            }
            graphics.renderComponentTooltip(
                    font, rowTooltip(entries.get(scroll + row), mouseX - leftPos), mouseX, mouseY);
            return;
        }
    }

    private List<Component> rowTooltip(MachineHubState.Entry entry, int localX) {
        List<Component> lines = new ArrayList<>();
        BlockPos pos = entry.pos();
        lines.add(Component.translatable(
                "gui.nep.machine_hub.entry",
                entry.icon().isEmpty()
                        ? Component.translatable("gui.nep.machine_hub.unknown_block")
                        : entry.icon().getHoverName(),
                Component.translatable("gui.nep.coords", pos.getX(), pos.getY(), pos.getZ())));
        lines.add(
                Component.translatable("gui.nep.machine_hub.role", entry.role().displayName())
                        .withStyle(ChatFormatting.GRAY));
        lines.add(contents(entry).withStyle(ChatFormatting.DARK_GRAY));
        switch (entry.issue()) {
            case NO_INVENTORY ->
                lines.add(Component.translatable("gui.nep.machine_hub.issue.no_inventory")
                        .withStyle(ChatFormatting.RED));
            case OUT_OF_RANGE ->
                lines.add(Component.translatable("gui.nep.machine_hub.issue.out_of_range")
                        .withStyle(ChatFormatting.RED));
            default -> {}
        }
        if (localX >= ROLE_X && localX < ROLE_X + GLYPH_WIDTH) {
            lines.add(Component.translatable("gui.nep.machine_hub.role.hint").withStyle(ChatFormatting.DARK_GRAY));
        } else if (localX >= REMOVE_X) {
            lines.add(Component.translatable("gui.nep.machine_hub.remove.hint").withStyle(ChatFormatting.DARK_GRAY));
        }
        return lines;
    }

    private static MutableComponent contents(MachineHubState.Entry entry) {
        if (entry.items() && entry.fluids()) {
            return Component.translatable("gui.nep.machine_hub.holds.both");
        }
        if (entry.items()) {
            return Component.translatable("gui.nep.machine_hub.holds.items");
        }
        if (entry.fluids()) {
            return Component.translatable("gui.nep.machine_hub.holds.fluids");
        }
        return Component.translatable("gui.nep.machine_hub.holds.nothing");
    }
}
