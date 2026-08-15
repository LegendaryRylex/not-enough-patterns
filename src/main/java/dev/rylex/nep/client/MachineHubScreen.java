package dev.rylex.nep.client;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import dev.rylex.nep.Nep;
import dev.rylex.nep.NepIcons;
import dev.rylex.nep.hub.HubFilter;
import dev.rylex.nep.hub.MachineHubMenu;
import dev.rylex.nep.hub.MachineHubState;
import dev.rylex.nep.net.HubLinkEditPayload;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

public class MachineHubScreen extends MatrixScreen<MachineHubMenu> {

    private static final ResourceLocation TEXTURE = Nep.id("textures/gui/machine_hub.png");

    private static final int TITLE_ON_PANEL = 0x3A2E1C;
    private static final int READOUT_TEXT = 0xF2E8D0;
    private static final int READOUT_DIM = 0xC8A882;
    private static final int READOUT_ERROR = 0xFF5555;
    private static final int SCROLL_TRACK = 0xFF1A1410;
    private static final int SCROLL_THUMB = 0xFFC8A882;

    private static final int SLOT_BORDER = 0xFF2E271C;
    private static final int SLOT_FACE = 0xFF8A8172;
    private static final int SLOT_HIGHLIGHT = 0xFFE6DDC6;
    private static final int CHIP_FACE = 0xFF3A342B;
    private static final int CHIP_HOVER = 0xFF574E40;
    private static final int PANEL_BACKDROP = 0xFF141210;

    private static final int ROLE_X = 11;
    private static final int ICON_X = 25;
    private static final int NAME_X = 45;
    private static final int CONFIG_X = 171;
    private static final int REMOVE_X = 187;
    private static final int GLYPH_WIDTH = 12;
    private static final int MINIMUM_THUMB = 12;

    private static final int PANEL_LEFT = 9;
    private static final int PANEL_RIGHT = 207;
    private static final int PANEL_TOP = 20;
    private static final int PANEL_BOTTOM = 205;
    private static final int PANEL_LABEL_X = 15;
    private static final int PANEL_VALUE_X = 81;
    private static final int PANEL_HEADER_Y = 24;
    private static final int PANEL_COORDS_Y = 42;
    private static final int PANEL_ROLE_Y = 56;
    private static final int PANEL_FACE_Y = 72;
    private static final int PANEL_PRIORITY_Y = 86;
    private static final int PANEL_INSERT_LABEL_Y = 104;
    private static final int PANEL_INSERT_SLOTS_Y = 122;
    private static final int PANEL_RETURN_LABEL_Y = 148;
    private static final int PANEL_RETURN_SLOTS_Y = 166;
    private static final int PANEL_BACK_Y = 190;

    private static final int CHIP_WIDTH = 108;
    private static final int CHIP_HEIGHT = 12;
    private static final int STEP_WIDTH = 12;
    private static final int PRIORITY_WIDTH = 72;
    private static final int PRIORITY_X = PANEL_VALUE_X + (CHIP_WIDTH - PRIORITY_WIDTH) / 2;
    private static final int PRIORITY_DOWN_X = PRIORITY_X;
    private static final int PRIORITY_CENTRE_X = PRIORITY_X + PRIORITY_WIDTH / 2;
    private static final int PRIORITY_UP_X = PRIORITY_X + PRIORITY_WIDTH - STEP_WIDTH;
    private static final int MODE_WIDTH = 36;
    private static final int MODE_X = PANEL_RIGHT - 4 - MODE_WIDTH;
    private static final int BACK_WIDTH = 48;
    private static final int FILTER_SLOT = 18;
    private static final int FILTER_X = MachineHubMenu.PLAYER_INVENTORY_X;

    private static final int PRIORITY_STEP = 1;
    private static final int PRIORITY_FAST_STEP = 10;

    private MachineHubState state = MachineHubState.empty();
    private int scroll;
    private boolean draggingThumb;

    @Nullable
    private BlockPos panelPos;

    @Nullable
    private ReadoutButton clearButton;

    @Nullable
    private ReadoutButton scanButton;

    public MachineHubScreen(MachineHubMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth = MachineHubMenu.WIDTH;
        this.imageHeight = MachineHubMenu.HEIGHT;
        this.inventoryLabelX = MachineHubMenu.PLAYER_INVENTORY_X;
        this.inventoryLabelY = MachineHubMenu.PLAYER_INVENTORY_Y - 11;
    }

    public void acceptState(MachineHubState state) {
        this.state = state;
        scroll = Math.min(scroll, maxScroll());
        if (panelEntry() == null) {
            panelPos = null;
        }
    }

    @Override
    protected ResourceLocation texture() {
        return TEXTURE;
    }

    @Override
    protected int sheetHeight() {
        return MachineHubMenu.SHEET_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        resetButtons();
        addHelpButton("nep/machine-hub.md");
        clearButton = addRightButton(
                2,
                NepIcons.REMOVE,
                "gui.nep.machine_hub.clear",
                "gui.nep.machine_hub.clear.hint",
                () -> sendButton(MachineHubMenu.BUTTON_CLEAR));
        scanButton = addRightButton(
                3,
                NepIcons.SCAN,
                "gui.nep.machine_hub.scan",
                "gui.nep.machine_hub.scan.hint",
                () -> sendButton(MachineHubMenu.BUTTON_SCAN));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        tickButtons();
        boolean listing = panelPos == null;
        if (clearButton != null) {
            clearButton.visible = listing;
        }
        if (scanButton != null) {
            scanButton.visible = listing;
        }
    }

    @Nullable
    private MachineHubState.Entry panelEntry() {
        if (panelPos == null) {
            return null;
        }
        for (MachineHubState.Entry entry : state.entries()) {
            if (entry.pos().equals(panelPos)) {
                return entry;
            }
        }
        return null;
    }

    private int panelIndex() {
        if (panelPos == null) {
            return -1;
        }
        List<MachineHubState.Entry> entries = state.entries();
        for (int index = 0; index < entries.size(); index++) {
            if (entries.get(index).pos().equals(panelPos)) {
                return index;
            }
        }
        return -1;
    }

    private void sendEdit(HubLinkEditPayload.Action action, int value, @Nullable AEKey key) {
        int index = panelIndex();
        if (index >= 0) {
            PacketDistributor.sendToServer(new HubLinkEditPayload(index, action, value, key));
        }
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
        MachineHubState.Entry entry = panelEntry();
        if (entry != null) {
            if (clickPanel(entry, (int) mouseX, (int) mouseY, button)) {
                return true;
            }
            if (within(
                    (int) mouseX,
                    (int) mouseY,
                    PANEL_LEFT,
                    PANEL_TOP,
                    PANEL_RIGHT - PANEL_LEFT,
                    PANEL_BOTTOM - PANEL_TOP)) {
                return true;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }
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
                if (within((int) mouseX, (int) mouseY, CONFIG_X, y, GLYPH_WIDTH, MachineHubMenu.ROW_HEIGHT)) {
                    panelPos = state.entries().get(scroll + row).pos();
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

    private boolean clickPanel(MachineHubState.Entry entry, int mouseX, int mouseY, int button) {
        if (button != 0 && button != 1) {
            return false;
        }
        if (button == 0 && within(mouseX, mouseY, PANEL_LABEL_X, PANEL_BACK_Y, BACK_WIDTH, CHIP_HEIGHT)) {
            panelPos = null;
            return true;
        }
        if (button == 0 && within(mouseX, mouseY, PANEL_VALUE_X, PANEL_ROLE_Y, CHIP_WIDTH, CHIP_HEIGHT)) {
            sendButton(MachineHubMenu.BUTTON_ROLE + panelIndex());
            return true;
        }
        if (button == 0 && within(mouseX, mouseY, PANEL_VALUE_X, PANEL_FACE_Y, CHIP_WIDTH, CHIP_HEIGHT)) {
            sendEdit(HubLinkEditPayload.Action.SET_FACE, nextFace(entry.face()), null);
            return true;
        }
        if (button == 0 && within(mouseX, mouseY, PRIORITY_DOWN_X, PANEL_PRIORITY_Y, STEP_WIDTH, CHIP_HEIGHT)) {
            sendEdit(HubLinkEditPayload.Action.SET_PRIORITY, entry.priority() - priorityStep(), null);
            return true;
        }
        if (button == 0 && within(mouseX, mouseY, PRIORITY_UP_X, PANEL_PRIORITY_Y, STEP_WIDTH, CHIP_HEIGHT)) {
            sendEdit(HubLinkEditPayload.Action.SET_PRIORITY, entry.priority() + priorityStep(), null);
            return true;
        }
        if (button == 0 && within(mouseX, mouseY, MODE_X, PANEL_INSERT_LABEL_Y, MODE_WIDTH, CHIP_HEIGHT)) {
            sendEdit(HubLinkEditPayload.Action.TOGGLE_INSERT_MODE, 0, null);
            return true;
        }
        if (button == 0 && within(mouseX, mouseY, MODE_X, PANEL_RETURN_LABEL_Y, MODE_WIDTH, CHIP_HEIGHT)) {
            sendEdit(HubLinkEditPayload.Action.TOGGLE_RETURN_MODE, 0, null);
            return true;
        }
        int insertSlot = filterSlotAt(mouseX, mouseY, PANEL_INSERT_SLOTS_Y);
        if (insertSlot >= 0) {
            sendEdit(HubLinkEditPayload.Action.SET_INSERT_FILTER, insertSlot, keyFromCursor(button));
            return true;
        }
        int returnSlot = filterSlotAt(mouseX, mouseY, PANEL_RETURN_SLOTS_Y);
        if (returnSlot >= 0) {
            sendEdit(HubLinkEditPayload.Action.SET_RETURN_FILTER, returnSlot, keyFromCursor(button));
            return true;
        }
        return false;
    }

    private int priorityStep() {
        return hasShiftDown() ? PRIORITY_FAST_STEP : PRIORITY_STEP;
    }

    @Nullable
    private AEKey keyFromCursor(int button) {
        if (button == 1) {
            return null;
        }
        GenericStack carried = GenericStack.fromItemStack(menu.getCarried());
        return carried == null ? null : carried.what();
    }

    public List<GhostTarget> ghostTargets() {
        MachineHubState.Entry entry = panelEntry();
        if (entry == null) {
            return List.of();
        }
        List<GhostTarget> targets = new ArrayList<>();
        addGhostTargets(
                targets,
                PANEL_INSERT_SLOTS_Y,
                HubLinkEditPayload.Action.SET_INSERT_FILTER,
                entry.role().accepts());
        addGhostTargets(
                targets,
                PANEL_RETURN_SLOTS_Y,
                HubLinkEditPayload.Action.SET_RETURN_FILTER,
                entry.role().provides());
        return targets;
    }

    private void addGhostTargets(List<GhostTarget> out, int slotsY, HubLinkEditPayload.Action action, boolean used) {
        if (!used) {
            return;
        }
        for (int slot = 0; slot < HubFilter.SIZE; slot++) {
            int index = slot;
            out.add(new GhostTarget(
                    new Rect2i(leftPos + FILTER_X + slot * FILTER_SLOT, topPos + slotsY, FILTER_SLOT, FILTER_SLOT),
                    key -> sendEdit(action, index, key)));
        }
    }

    private int filterSlotAt(int mouseX, int mouseY, int slotsY) {
        for (int slot = 0; slot < HubFilter.SIZE; slot++) {
            if (within(mouseX, mouseY, FILTER_X + slot * FILTER_SLOT, slotsY, FILTER_SLOT, FILTER_SLOT)) {
                return slot;
            }
        }
        return -1;
    }

    private static int nextFace(@Nullable Direction face) {
        return face == null ? 0 : face.ordinal() + 1;
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
        if (panelPos == null
                && max > 0
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
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (panelPos != null && minecraft != null && minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            panelPos = null;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, Component.translatable("gui.nep.machine_hub.title"), 8, 7, TITLE_ON_PANEL, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TITLE_ON_PANEL, false);

        MachineHubState.Entry panel = panelEntry();
        if (panel != null) {
            drawPanel(graphics, panel, mouseX, mouseY);
            return;
        }
        drawList(graphics);
    }

    private void drawList(GuiGraphics graphics) {
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
        graphics.drawString(font, trim(name, CONFIG_X - NAME_X - 4), NAME_X, y + 3, colour, false);
        boolean configured = !entry.insertFilter().isEmpty()
                || !entry.returnFilter().isEmpty()
                || entry.face() != null
                || entry.priority() != 0;
        graphics.drawString(font, NepIcons.CONFIG, CONFIG_X, y + 3, configured ? SCROLL_THUMB : READOUT_DIM, false);
        graphics.drawString(font, NepIcons.REMOVE, REMOVE_X, y + 3, READOUT_DIM, false);
    }

    private void drawPanel(GuiGraphics graphics, MachineHubState.Entry entry, int mouseX, int mouseY) {
        graphics.fill(PANEL_LEFT, PANEL_TOP, PANEL_RIGHT, PANEL_BOTTOM, PANEL_BACKDROP);

        if (!entry.icon().isEmpty()) {
            graphics.renderFakeItem(entry.icon(), PANEL_LABEL_X, PANEL_HEADER_Y - 2);
        }
        Component name = entry.icon().isEmpty()
                ? Component.translatable("gui.nep.machine_hub.unknown_block")
                : entry.icon().getHoverName();
        graphics.drawString(
                font,
                trim(name, PANEL_RIGHT - PANEL_LABEL_X - 26),
                PANEL_LABEL_X + 20,
                PANEL_HEADER_Y + 2,
                READOUT_TEXT,
                false);
        BlockPos pos = entry.pos();
        graphics.drawString(
                font,
                Component.translatable("gui.nep.coords", pos.getX(), pos.getY(), pos.getZ()),
                PANEL_LABEL_X,
                PANEL_COORDS_Y,
                READOUT_DIM,
                false);

        drawLabel(graphics, "gui.nep.machine_hub.panel.role", PANEL_ROLE_Y);
        drawChip(graphics, PANEL_VALUE_X, PANEL_ROLE_Y, CHIP_WIDTH, entry.role().displayName(), mouseX, mouseY);
        graphics.drawString(font, entry.role().glyph(), PANEL_LABEL_X + 48, PANEL_ROLE_Y + 2, READOUT_TEXT, false);

        drawLabel(graphics, "gui.nep.machine_hub.panel.face", PANEL_FACE_Y);
        drawChip(graphics, PANEL_VALUE_X, PANEL_FACE_Y, CHIP_WIDTH, faceName(entry.face()), mouseX, mouseY);

        drawLabel(graphics, "gui.nep.machine_hub.panel.priority", PANEL_PRIORITY_Y);
        drawChip(graphics, PRIORITY_DOWN_X, PANEL_PRIORITY_Y, STEP_WIDTH, Component.literal("−"), mouseX, mouseY);
        String priority = String.valueOf(entry.priority());
        graphics.drawString(
                font,
                priority,
                PRIORITY_CENTRE_X - font.width(priority) / 2,
                PANEL_PRIORITY_Y + 2,
                READOUT_TEXT,
                false);
        drawChip(graphics, PRIORITY_UP_X, PANEL_PRIORITY_Y, STEP_WIDTH, Component.literal("+"), mouseX, mouseY);

        drawFilter(
                graphics,
                entry.insertFilter(),
                "gui.nep.machine_hub.panel.insert_filter",
                PANEL_INSERT_LABEL_Y,
                PANEL_INSERT_SLOTS_Y,
                entry.role().accepts(),
                mouseX,
                mouseY);
        drawFilter(
                graphics,
                entry.returnFilter(),
                "gui.nep.machine_hub.panel.return_filter",
                PANEL_RETURN_LABEL_Y,
                PANEL_RETURN_SLOTS_Y,
                entry.role().provides(),
                mouseX,
                mouseY);

        drawChip(
                graphics,
                PANEL_LABEL_X,
                PANEL_BACK_Y,
                BACK_WIDTH,
                Component.translatable("gui.nep.machine_hub.panel.back"),
                mouseX,
                mouseY);
    }

    private void drawFilter(
            GuiGraphics graphics,
            HubFilter filter,
            String labelKey,
            int labelY,
            int slotsY,
            boolean used,
            int mouseX,
            int mouseY) {
        graphics.drawString(
                font,
                Component.translatable(labelKey),
                PANEL_LABEL_X,
                labelY + 2,
                used ? READOUT_TEXT : READOUT_DIM,
                false);
        drawChip(
                graphics,
                MODE_X,
                labelY,
                MODE_WIDTH,
                Component.translatable(
                        filter.allow() ? "gui.nep.machine_hub.panel.allow" : "gui.nep.machine_hub.panel.deny"),
                mouseX,
                mouseY);
        for (int slot = 0; slot < HubFilter.SIZE; slot++) {
            int x = FILTER_X + slot * FILTER_SLOT;
            graphics.fill(x, slotsY, x + FILTER_SLOT, slotsY + FILTER_SLOT, SLOT_BORDER);
            graphics.fill(x + 1, slotsY + 1, x + FILTER_SLOT, slotsY + FILTER_SLOT, SLOT_HIGHLIGHT);
            graphics.fill(x + 1, slotsY + 1, x + FILTER_SLOT - 1, slotsY + FILTER_SLOT - 1, SLOT_FACE);
            AEKey key = filter.keys().get(slot);
            if (key != null) {
                graphics.renderFakeItem(display(key), x + 1, slotsY + 1);
            }
        }
        if (!used) {
            graphics.fill(FILTER_X, slotsY, FILTER_X + HubFilter.SIZE * FILTER_SLOT, slotsY + FILTER_SLOT, 0xA0141210);
        }
    }

    private void drawLabel(GuiGraphics graphics, String key, int y) {
        graphics.drawString(font, Component.translatable(key), PANEL_LABEL_X, y + 2, READOUT_DIM, false);
    }

    private void drawChip(GuiGraphics graphics, int x, int y, int width, Component text, int mouseX, int mouseY) {
        boolean hovered = within(mouseX, mouseY, x, y, width, CHIP_HEIGHT);
        graphics.fill(x, y, x + width, y + CHIP_HEIGHT, hovered ? CHIP_HOVER : CHIP_FACE);
        String plain = trim(text, width - 6);
        graphics.drawString(font, plain, x + (width - font.width(plain)) / 2, y + 2, READOUT_TEXT, false);
    }

    private static ItemStack display(AEKey key) {
        return key instanceof AEItemKey itemKey ? itemKey.toStack(1) : GenericStack.wrapInItemStack(key, 1);
    }

    private static Component faceName(@Nullable Direction face) {
        return face == null
                ? Component.translatable("gui.nep.machine_hub.face.any")
                : Component.translatable("gui.nep.machine_hub.face." + face.getSerializedName());
    }

    @Override
    protected void renderReadoutTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        MachineHubState.Entry panel = panelEntry();
        if (panel != null) {
            renderPanelTooltips(graphics, panel, mouseX, mouseY);
            return;
        }
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

    private void renderPanelTooltips(GuiGraphics graphics, MachineHubState.Entry entry, int mouseX, int mouseY) {
        if (within(mouseX, mouseY, PANEL_VALUE_X, PANEL_FACE_Y, CHIP_WIDTH, CHIP_HEIGHT)) {
            graphics.renderComponentTooltip(
                    font,
                    List.of(Component.translatable("gui.nep.machine_hub.face.hint")
                            .withStyle(ChatFormatting.GRAY)),
                    mouseX,
                    mouseY);
            return;
        }
        if (within(mouseX, mouseY, PRIORITY_X, PANEL_PRIORITY_Y, PRIORITY_WIDTH, CHIP_HEIGHT)) {
            graphics.renderComponentTooltip(
                    font,
                    List.of(Component.translatable("gui.nep.machine_hub.priority.hint")
                            .withStyle(ChatFormatting.GRAY)),
                    mouseX,
                    mouseY);
            return;
        }
        if (filterTooltip(
                        graphics,
                        entry.insertFilter(),
                        PANEL_INSERT_SLOTS_Y,
                        entry.role().accepts(),
                        mouseX,
                        mouseY)
                || filterTooltip(
                        graphics,
                        entry.returnFilter(),
                        PANEL_RETURN_SLOTS_Y,
                        entry.role().provides(),
                        mouseX,
                        mouseY)) {
            return;
        }
        if (within(mouseX, mouseY, PANEL_VALUE_X, PANEL_ROLE_Y, CHIP_WIDTH, CHIP_HEIGHT)) {
            graphics.renderComponentTooltip(
                    font,
                    List.of(Component.translatable("gui.nep.machine_hub.role.hint")
                            .withStyle(ChatFormatting.GRAY)),
                    mouseX,
                    mouseY);
        }
    }

    private boolean filterTooltip(
            GuiGraphics graphics, HubFilter filter, int slotsY, boolean used, int mouseX, int mouseY) {
        int slot = filterSlotAt(mouseX, mouseY, slotsY);
        if (slot < 0) {
            return false;
        }
        List<Component> lines = new ArrayList<>();
        AEKey key = filter.keys().get(slot);
        if (key != null) {
            lines.add(key.getDisplayName());
        }
        if (!used) {
            lines.add(
                    Component.translatable("gui.nep.machine_hub.filter.unused").withStyle(ChatFormatting.RED));
        }
        lines.add(Component.translatable("gui.nep.machine_hub.filter.hint").withStyle(ChatFormatting.DARK_GRAY));
        graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
        return true;
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
        if (entry.face() != null) {
            lines.add(Component.translatable("gui.nep.machine_hub.face", faceName(entry.face()))
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        if (entry.priority() != 0) {
            lines.add(Component.translatable("gui.nep.machine_hub.priority", entry.priority())
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
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
        } else if (localX >= CONFIG_X && localX < CONFIG_X + GLYPH_WIDTH) {
            lines.add(Component.translatable("gui.nep.machine_hub.config.hint").withStyle(ChatFormatting.DARK_GRAY));
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
