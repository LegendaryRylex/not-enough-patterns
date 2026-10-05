package dev.rylex.nep.compat.ars;

import dev.rylex.nep.Nep;
import dev.rylex.nep.NepIcons;
import dev.rylex.nep.client.MatrixScreen;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector2i;
import org.joml.Vector2ic;

public class RitualConductorScreen extends MatrixScreen<RitualConductorMenu> {

    private static final ResourceLocation TEXTURE = Nep.id("textures/gui/ritual_conductor.png");

    private static final int TITLE_ON_PANEL = 0x3A2E1C;
    private static final int READOUT_TEXT = 0xF2E8D0;
    private static final int READOUT_DIM = 0xC8A882;
    private static final int READOUT_ERROR = 0xFF5555;
    private static final int READOUT_GOOD = 0x9BE07A;
    private static final int READOUT_COST = 0x55FFFF;
    private static final int READOUT_ARROW = 0xE8A23A;
    private static final int SCROLL_TRACK = 0xFF1A1410;
    private static final int SCROLL_THUMB = 0xFFC8A882;
    private static final int ROW_SELECTED = 0x50FFD27A;
    private static final int ROW_HOVER = 0x28FFFFFF;
    private static final int SETTING_HOVER = 0x14FFFFFF;
    private static final int CELL_HOVER = 0x60FFFFFF;

    private static final int ICON_X = RitualConductorMenu.ROW_X + 1;
    private static final int NAME_X = RitualConductorMenu.ROW_X + 20;
    private static final int CARD_TEXT_X = RitualConductorMenu.PANE_X + 20;
    private static final int MINIMUM_THUMB = 12;
    private static final int ARROW_REACH = 3;
    private static final int TOOLTIP_WIDTH = 170;
    private static final int TOOLTIP_GAP = 6;
    private static final int TOOLTIP_MARGIN = 4;

    private enum Setting {
        REPEAT("gui.nep.ritual_conductor.cadence", "gui.nep.ritual_conductor.cadence.hint"),
        WEATHER("gui.nep.ritual_conductor.weather", "gui.nep.ritual_conductor.weather.hint"),
        TIME("gui.nep.ritual_conductor.daylight", "gui.nep.ritual_conductor.daylight.hint"),
        OUTPUT("gui.nep.ritual_conductor.collect", "gui.nep.ritual_conductor.collect.hint");

        private final String labelKey;
        private final String hintKey;

        Setting(String labelKey, String hintKey) {
            this.labelKey = labelKey;
            this.hintKey = hintKey;
        }

        int y() {
            return RitualConductorMenu.SETTING_Y + ordinal() * RitualConductorMenu.SETTING_HEIGHT;
        }
    }

    private enum Zone {
        BACK,
        VALUE,
        FORWARD
    }

    private final List<ResourceLocation> rituals = ArsRituals.sorted();

    private RitualConductorState state = RitualConductorState.empty();
    private int scroll;
    private boolean draggingThumb;
    private boolean scrolledToSelection;
    private int backArrowX;
    private int forwardArrowX;

    public RitualConductorScreen(RitualConductorMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth = RitualConductorMenu.WIDTH;
        this.imageHeight = RitualConductorMenu.HEIGHT;
    }

    public void acceptState(RitualConductorState updated) {
        this.state = updated;
        scroll = Math.min(scroll, maxScroll());
        if (!scrolledToSelection) {
            scrolledToSelection = true;
            revealSelection();
        }
    }

    private void revealSelection() {
        int selected = selectedIndex();
        if (selected >= 0 && (selected < scroll || selected >= scroll + RitualConductorMenu.VISIBLE_ROWS)) {
            scroll = Math.max(0, Math.min(maxScroll(), selected - RitualConductorMenu.VISIBLE_ROWS / 2));
        }
    }

    private int selectedIndex() {
        Optional<ResourceLocation> selected = state.ritual();
        return selected.map(rituals::indexOf).orElse(-1);
    }

    @Override
    protected ResourceLocation texture() {
        return TEXTURE;
    }

    @Override
    protected int sheetHeight() {
        return RitualConductorMenu.HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        resetButtons();
        addHelpButton("nep/ars_nouveau/ritual-conductor.md");
        addRightButton(
                2,
                NepIcons.REMOVE,
                "gui.nep.ritual_conductor.clear",
                "gui.nep.ritual_conductor.clear.hint",
                () -> sendButton(RitualConductorMenu.BUTTON_CLEAR));
        int widestLabel = 0;
        for (Setting setting : Setting.values()) {
            widestLabel = Math.max(widestLabel, font.width(Component.translatable(setting.labelKey)));
        }
        backArrowX = RitualConductorMenu.PANE_X + widestLabel + 6;
        forwardArrowX = RitualConductorMenu.PANE_RIGHT - 1 - font.width(">");
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        tickButtons();
    }

    private int maxScroll() {
        return Math.max(0, rituals.size() - RitualConductorMenu.VISIBLE_ROWS);
    }

    private int shownRows() {
        return Math.min(rituals.size(), RitualConductorMenu.VISIBLE_ROWS);
    }

    private int thumbHeight() {
        if (rituals.size() <= RitualConductorMenu.VISIBLE_ROWS) {
            return RitualConductorMenu.SCROLL_HEIGHT;
        }
        return Math.max(
                MINIMUM_THUMB, RitualConductorMenu.SCROLL_HEIGHT * RitualConductorMenu.VISIBLE_ROWS / rituals.size());
    }

    private int thumbY() {
        int max = maxScroll();
        if (max == 0) {
            return RitualConductorMenu.ROW_Y;
        }
        return RitualConductorMenu.ROW_Y + (RitualConductorMenu.SCROLL_HEIGHT - thumbHeight()) * scroll / max;
    }

    private void scrollTo(double mouseY) {
        int max = maxScroll();
        if (max == 0) {
            return;
        }
        int travel = RitualConductorMenu.SCROLL_HEIGHT - thumbHeight();
        double top = mouseY - topPos - RitualConductorMenu.ROW_Y - thumbHeight() / 2.0;
        scroll = Math.max(0, Math.min(max, (int) Math.round(top / travel * max)));
    }

    private static int augmentX(int index) {
        return RitualConductorMenu.AUGMENT_X
                + (index % RitualConductorMenu.AUGMENT_COLUMNS) * RitualConductorMenu.AUGMENT_SIZE;
    }

    private static int augmentY(int index) {
        return RitualConductorMenu.AUGMENT_Y
                + (index / RitualConductorMenu.AUGMENT_COLUMNS) * RitualConductorMenu.AUGMENT_SIZE;
    }

    private boolean overAugment(int mouseX, int mouseY, int index) {
        return within(
                mouseX,
                mouseY,
                augmentX(index),
                augmentY(index),
                RitualConductorMenu.AUGMENT_SIZE,
                RitualConductorMenu.AUGMENT_SIZE);
    }

    private boolean overRow(int mouseX, int mouseY, int row) {
        return within(
                mouseX,
                mouseY,
                RitualConductorMenu.ROW_X,
                RitualConductorMenu.ROW_Y + row * RitualConductorMenu.ROW_HEIGHT,
                RitualConductorMenu.ROW_WIDTH,
                RitualConductorMenu.ROW_HEIGHT);
    }

    private boolean overSetting(int mouseX, int mouseY, Setting setting) {
        return within(
                mouseX,
                mouseY,
                RitualConductorMenu.PANE_X - ARROW_REACH,
                setting.y() - 2,
                RitualConductorMenu.PANE_RIGHT - RitualConductorMenu.PANE_X + ARROW_REACH,
                RitualConductorMenu.SETTING_HEIGHT);
    }

    private boolean overCard(int mouseX, int mouseY) {
        return within(
                mouseX,
                mouseY,
                RitualConductorMenu.PANE_X,
                RitualConductorMenu.CARD_Y - 1,
                RitualConductorMenu.PANE_RIGHT - RitualConductorMenu.PANE_X,
                20);
    }

    private Optional<Zone> zoneAt(int mouseX, int mouseY, Setting setting) {
        if (!overSetting(mouseX, mouseY, setting)) {
            return Optional.empty();
        }
        int localX = mouseX - leftPos;
        if (localX < backArrowX - ARROW_REACH) {
            return Optional.empty();
        }
        if (localX < backArrowX + font.width("<") + ARROW_REACH) {
            return Optional.of(Zone.BACK);
        }
        if (localX >= forwardArrowX - ARROW_REACH) {
            return Optional.of(Zone.FORWARD);
        }
        return Optional.of(Zone.VALUE);
    }

    private void select(ResourceLocation ritual) {
        boolean deselect = state.ritual().filter(ritual::equals).isPresent();
        PacketDistributor.sendToServer(new SelectRitualPayload(deselect ? Optional.empty() : Optional.of(ritual)));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 && button != 1) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        int x = (int) mouseX;
        int y = (int) mouseY;

        if (button == 0
                && maxScroll() > 0
                && within(
                        x,
                        y,
                        RitualConductorMenu.SCROLL_X,
                        RitualConductorMenu.ROW_Y,
                        RitualConductorMenu.SCROLL_WIDTH,
                        RitualConductorMenu.SCROLL_HEIGHT)) {
            draggingThumb = true;
            scrollTo(mouseY);
            return true;
        }

        if (button == 0) {
            for (int row = 0; row < shownRows(); row++) {
                if (overRow(x, y, row)) {
                    select(rituals.get(scroll + row));
                    return true;
                }
            }
            for (int index = 0; index < ArsRituals.MAX_AUGMENTS; index++) {
                if (overAugment(x, y, index)) {
                    sendButton(RitualConductorMenu.BUTTON_SET_AUGMENT + index);
                    return true;
                }
            }
        }

        for (Setting setting : Setting.values()) {
            Optional<Zone> zone = zoneAt(x, y, setting);
            if (zone.isPresent()) {
                int id = settingButton(setting, zone.get(), button == 1);
                if (id >= 0) {
                    sendButton(id);
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private static int settingButton(Setting setting, Zone zone, boolean rightClick) {
        boolean forward = zone == Zone.FORWARD || (zone == Zone.VALUE && !rightClick);
        return switch (setting) {
            case REPEAT -> {
                if (zone == Zone.VALUE) {
                    yield -1;
                }
                if (zone == Zone.FORWARD) {
                    yield rightClick
                            ? RitualConductorMenu.BUTTON_INTERVAL_UP_COARSE
                            : RitualConductorMenu.BUTTON_INTERVAL_UP;
                }
                yield rightClick
                        ? RitualConductorMenu.BUTTON_INTERVAL_DOWN_COARSE
                        : RitualConductorMenu.BUTTON_INTERVAL_DOWN;
            }
            case WEATHER -> forward ? RitualConductorMenu.BUTTON_WEATHER : RitualConductorMenu.BUTTON_WEATHER_BACK;
            case TIME -> forward ? RitualConductorMenu.BUTTON_DAYLIGHT : RitualConductorMenu.BUTTON_DAYLIGHT_BACK;
            case OUTPUT -> RitualConductorMenu.BUTTON_COLLECT;
        };
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
                        RitualConductorMenu.ROW_X,
                        RitualConductorMenu.ROW_Y,
                        RitualConductorMenu.SCROLL_X + RitualConductorMenu.SCROLL_WIDTH - RitualConductorMenu.ROW_X,
                        RitualConductorMenu.SCROLL_HEIGHT)) {
            scroll = Math.max(0, Math.min(max, scroll - (int) Math.signum(scrollY)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(
                font, Component.translatable("gui.nep.ritual_conductor.title"), 8, 7, TITLE_ON_PANEL, false);

        drawHeader(graphics);
        drawRituals(graphics, mouseX, mouseY);
        drawCard(graphics);
        drawSettings(graphics, mouseX, mouseY);
        drawAugments(graphics, mouseX, mouseY);
    }

    private void drawHeader(GuiGraphics graphics) {
        Component headline;
        int colour;
        if (!state.enabled()) {
            headline = Component.translatable("gui.nep.ritual_conductor.disabled");
            colour = READOUT_WARN;
        } else if (state.powered()) {
            headline = Component.translatable("gui.nep.ritual_conductor.paused");
            colour = READOUT_WARN;
        } else {
            headline = Component.translatable(state.state().key());
            colour = switch (state.state()) {
                case RUNNING -> READOUT_GOOD;
                case NO_BRAZIER -> READOUT_ERROR;
                case STARVED -> READOUT_WARN;
                case UNCONFIGURED, HELD, PAUSED -> READOUT_DIM;
                default -> READOUT_TEXT;
            };
        }
        String shown = trim(headline, barWidth());
        graphics.drawString(font, shown, READOUT_X, RitualConductorMenu.HEADER_Y, colour, false);
        int occupied = READOUT_X + font.width(shown);

        Optional<BlockPos> brazier = state.brazier();
        if (state.state() == RitualConductorBlockEntity.ConductorState.HELD && state.secondsUntilNextRun() > 0) {
            drawTrailing(
                    graphics,
                    Component.translatable("gui.nep.ritual_conductor.next_in", state.secondsUntilNextRun()),
                    RitualConductorMenu.HEADER_Y,
                    occupied,
                    READOUT_DIM);
        } else if (brazier.isPresent()) {
            BlockPos pos = brazier.get();
            drawTrailing(
                    graphics,
                    Component.translatable(
                            "gui.nep.ritual_conductor.status.brazier", pos.getX(), pos.getY(), pos.getZ()),
                    RitualConductorMenu.HEADER_Y,
                    occupied,
                    READOUT_DIM);
        } else if (state.state() != RitualConductorBlockEntity.ConductorState.NO_BRAZIER) {
            drawTrailing(
                    graphics,
                    Component.translatable("gui.nep.ritual_conductor.status.no_brazier"),
                    RitualConductorMenu.HEADER_Y,
                    occupied,
                    READOUT_ERROR);
        }
    }

    private void drawRituals(GuiGraphics graphics, int mouseX, int mouseY) {
        int selected = selectedIndex();
        for (int row = 0; row < shownRows(); row++) {
            int index = scroll + row;
            int y = RitualConductorMenu.ROW_Y + row * RitualConductorMenu.ROW_HEIGHT;
            ResourceLocation ritual = rituals.get(index);
            if (index == selected) {
                fillRow(graphics, y, ROW_SELECTED);
            } else if (overRow(mouseX, mouseY, row)) {
                fillRow(graphics, y, ROW_HOVER);
            }
            ItemStack tablet = ArsRituals.tabletStack(ritual);
            if (!tablet.isEmpty()) {
                graphics.renderFakeItem(tablet, ICON_X, y - 1);
            }
            graphics.drawString(
                    font,
                    fitName(
                            ArsRituals.shortName(ritual),
                            RitualConductorMenu.ROW_X + RitualConductorMenu.ROW_WIDTH - NAME_X - 1),
                    NAME_X,
                    y + 3,
                    index == selected ? READOUT_TEXT : READOUT_DIM,
                    false);
        }
        if (maxScroll() > 0) {
            drawScrollbar(graphics);
        }
    }

    private String fitName(Component name, int width) {
        String plain = name.getString();
        int colon = plain.lastIndexOf(": ");
        if (font.width(plain) <= width || colon <= 0) {
            return trim(name, width);
        }
        String variant = plain.substring(colon);
        String head = font.plainSubstrByWidth(
                plain.substring(0, colon), Math.max(0, width - font.width(variant) - font.width("…")));
        return head + "…" + variant;
    }

    private static void fillRow(GuiGraphics graphics, int y, int colour) {
        graphics.fill(
                RitualConductorMenu.ROW_X,
                y,
                RitualConductorMenu.ROW_X + RitualConductorMenu.ROW_WIDTH,
                y + RitualConductorMenu.ROW_HEIGHT,
                colour);
    }

    private void drawScrollbar(GuiGraphics graphics) {
        int x = RitualConductorMenu.SCROLL_X;
        int right = x + RitualConductorMenu.SCROLL_WIDTH;
        graphics.fill(
                x,
                RitualConductorMenu.ROW_Y,
                right,
                RitualConductorMenu.ROW_Y + RitualConductorMenu.SCROLL_HEIGHT,
                SCROLL_TRACK);
        int top = thumbY();
        graphics.fill(x + 1, top, right - 1, top + thumbHeight(), SCROLL_THUMB);
    }

    private void drawCard(GuiGraphics graphics) {
        int width = RitualConductorMenu.PANE_RIGHT - CARD_TEXT_X;
        Optional<ResourceLocation> chosen = state.ritual();
        if (chosen.isEmpty()) {
            graphics.drawString(
                    font,
                    trim(
                            Component.translatable("gui.nep.ritual_conductor.card.empty"),
                            RitualConductorMenu.PANE_RIGHT - RitualConductorMenu.PANE_X),
                    RitualConductorMenu.PANE_X,
                    RitualConductorMenu.CARD_Y + 5,
                    READOUT_DIM,
                    false);
            return;
        }
        ResourceLocation ritual = chosen.get();
        ItemStack tablet = ArsRituals.tabletStack(ritual);
        if (!tablet.isEmpty()) {
            graphics.renderFakeItem(tablet, RitualConductorMenu.PANE_X, RitualConductorMenu.CARD_Y + 1);
        }
        graphics.drawString(
                font,
                fitName(ArsRituals.shortName(ritual), width),
                CARD_TEXT_X,
                RitualConductorMenu.CARD_Y,
                READOUT_TEXT,
                false);
        int cost = ArsRituals.sourceCost(ritual);
        Component costLine = cost > 0
                ? Component.translatable("gui.nep.ritual_conductor.card.cost", cost)
                : Component.translatable("gui.nep.ritual_conductor.card.free");
        graphics.drawString(
                font, trim(costLine, width), CARD_TEXT_X, RitualConductorMenu.CARD_Y + 10, READOUT_COST, false);
    }

    private void drawSettings(GuiGraphics graphics, int mouseX, int mouseY) {
        int valueLeft = backArrowX + font.width("<") + ARROW_REACH;
        int valueRight = forwardArrowX - ARROW_REACH;
        for (Setting setting : Setting.values()) {
            int y = setting.y();
            Optional<Zone> hovered = zoneAt(mouseX, mouseY, setting);
            if (overSetting(mouseX, mouseY, setting)) {
                graphics.fill(
                        RitualConductorMenu.PANE_X - ARROW_REACH,
                        y - 2,
                        RitualConductorMenu.PANE_RIGHT,
                        y + RitualConductorMenu.SETTING_HEIGHT - 2,
                        SETTING_HOVER);
            }
            graphics.drawString(
                    font, Component.translatable(setting.labelKey), RitualConductorMenu.PANE_X, y, READOUT_DIM, false);
            graphics.drawString(
                    font,
                    "<",
                    backArrowX,
                    y,
                    hovered.filter(Zone.BACK::equals).isPresent() ? READOUT_TEXT : READOUT_ARROW,
                    false);
            graphics.drawString(
                    font,
                    ">",
                    forwardArrowX,
                    y,
                    hovered.filter(Zone.FORWARD::equals).isPresent() ? READOUT_TEXT : READOUT_ARROW,
                    false);
            String value = trim(settingValue(setting), valueRight - valueLeft);
            graphics.drawString(
                    font,
                    value,
                    valueLeft + (valueRight - valueLeft - font.width(value)) / 2,
                    y,
                    setting == Setting.OUTPUT && state.collectOutput() ? READOUT_GOOD : READOUT_TEXT,
                    false);
        }
    }

    private Component settingValue(Setting setting) {
        return switch (setting) {
            case REPEAT -> cadenceText();
            case WEATHER -> Component.translatable(state.weather().key());
            case TIME -> Component.translatable(state.daylight().key());
            case OUTPUT ->
                Component.translatable(
                        state.collectOutput()
                                ? "gui.nep.ritual_conductor.collect.on"
                                : "gui.nep.ritual_conductor.collect.off");
        };
    }

    private Component cadenceText() {
        int seconds = state.runInterval();
        if (seconds <= 0) {
            return Component.translatable("gui.nep.ritual_conductor.cadence.continuous");
        }
        if (seconds % 60 == 0) {
            return Component.translatable("gui.nep.ritual_conductor.cadence.minutes", seconds / 60);
        }
        return Component.translatable("gui.nep.ritual_conductor.cadence.seconds", seconds);
    }

    private void drawAugments(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(
                font,
                Component.translatable("gui.nep.ritual_conductor.augments"),
                RitualConductorMenu.AUGMENT_X,
                RitualConductorMenu.AUGMENT_LABEL_Y,
                READOUT_DIM,
                false);

        List<ItemStack> augments = state.augments();
        for (int index = 0; index < ArsRituals.MAX_AUGMENTS; index++) {
            int x = augmentX(index);
            int y = augmentY(index);
            if (overAugment(mouseX, mouseY, index)) {
                graphics.fill(
                        x + 1,
                        y + 1,
                        x + RitualConductorMenu.AUGMENT_SIZE - 1,
                        y + RitualConductorMenu.AUGMENT_SIZE - 1,
                        CELL_HOVER);
            }
            if (index < augments.size() && !augments.get(index).isEmpty()) {
                graphics.renderFakeItem(augments.get(index), x + 1, y + 1);
            }
        }
    }

    private void renderWrapped(GuiGraphics graphics, List<Component> lines, int mouseX, int mouseY) {
        renderWrapped(graphics, lines, DefaultTooltipPositioner.INSTANCE, mouseX, mouseY);
    }

    private void renderWrapped(
            GuiGraphics graphics, List<Component> lines, ClientTooltipPositioner positioner, int mouseX, int mouseY) {
        List<FormattedCharSequence> wrapped = new ArrayList<>();
        for (Component line : lines) {
            wrapped.addAll(font.split(line, TOOLTIP_WIDTH));
        }
        graphics.renderTooltip(font, wrapped, positioner, mouseX, mouseY);
    }

    private Vector2ic besideSettings(int screenWidth, int screenHeight, int mouseX, int mouseY, int width, int height) {
        Vector2ic placed = DefaultTooltipPositioner.INSTANCE.positionTooltip(
                screenWidth, screenHeight, mouseX, mouseY, width, height);
        int left = leftPos + RitualConductorMenu.PANE_X - ARROW_REACH - TOOLTIP_GAP - width;
        if (left >= TOOLTIP_MARGIN) {
            return new Vector2i(left, placed.y());
        }
        int right = leftPos + imageWidth + TOOLTIP_GAP;
        return right + width <= screenWidth - TOOLTIP_MARGIN ? new Vector2i(right, placed.y()) : placed;
    }

    @Override
    protected void renderReadoutTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        for (int row = 0; row < shownRows(); row++) {
            if (overRow(mouseX, mouseY, row)) {
                renderWrapped(graphics, ritualTooltip(rituals.get(scroll + row)), mouseX, mouseY);
                return;
            }
        }

        Optional<ResourceLocation> chosen = state.ritual();
        if (chosen.isPresent() && overCard(mouseX, mouseY)) {
            renderWrapped(graphics, ritualTooltip(chosen.get()), mouseX, mouseY);
            return;
        }

        List<ItemStack> augments = state.augments();
        for (int index = 0; index < ArsRituals.MAX_AUGMENTS; index++) {
            if (!overAugment(mouseX, mouseY, index)) {
                continue;
            }
            List<Component> lines = new ArrayList<>();
            ItemStack held = index < augments.size() ? augments.get(index) : ItemStack.EMPTY;
            if (held.isEmpty()) {
                lines.add(Component.translatable("gui.nep.ritual_conductor.augments.slot"));
                lines.add(Component.translatable("gui.nep.ritual_conductor.augments.set")
                        .withStyle(ChatFormatting.GRAY));
            } else {
                lines.add(held.getHoverName());
                lines.add(Component.translatable("gui.nep.ritual_conductor.augments.clear")
                        .withStyle(ChatFormatting.GRAY));
            }
            renderWrapped(graphics, lines, mouseX, mouseY);
            return;
        }

        for (Setting setting : Setting.values()) {
            if (overSetting(mouseX, mouseY, setting)) {
                renderWrapped(
                        graphics,
                        List.of(
                                Component.translatable(setting.labelKey),
                                Component.translatable(setting.hintKey).withStyle(ChatFormatting.GRAY)),
                        this::besideSettings,
                        mouseX,
                        mouseY);
                return;
            }
        }
    }

    private List<Component> ritualTooltip(ResourceLocation ritual) {
        List<Component> lines = new ArrayList<>();
        lines.add(ArsRituals.displayName(ritual));
        Component description = ArsRituals.description(ritual);
        if (description != null) {
            lines.add(description.copy().withStyle(ChatFormatting.GRAY));
        }
        int cost = ArsRituals.sourceCost(ritual);
        if (cost > 0) {
            lines.add(Component.translatable("gui.nep.ritual_conductor.source_cost", cost)
                    .withStyle(ChatFormatting.AQUA));
        }
        lines.add(Component.translatable(
                        state.ritual().filter(ritual::equals).isPresent()
                                ? "gui.nep.ritual_conductor.deselect.hint"
                                : "gui.nep.ritual_conductor.select.hint")
                .withStyle(ChatFormatting.DARK_GRAY));
        return lines;
    }
}
