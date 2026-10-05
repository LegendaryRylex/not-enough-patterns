package dev.rylex.nep.compat.malum;

import appeng.api.stacks.GenericStack;
import dev.rylex.nep.Nep;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.NepIcons;
import dev.rylex.nep.client.MatrixScreen;
import dev.rylex.nep.client.ReadoutButton;
import dev.rylex.nep.util.MissingStacks;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class FocusedSpiritMatrixScreen extends MatrixScreen<FocusedSpiritMatrixMenu> {

    private static final ResourceLocation TEXTURE = Nep.id("textures/gui/focused_spirit_matrix.png");

    private static final int TITLE_ON_PANEL = 0x1C2740;
    private static final int READOUT_TEXT = 0xDCE6F7;
    private static final int READOUT_DIM = 0x8FA6CE;

    private static final int BAR_TRACK = 0x0C1730;
    private static final int CRAFT_FILL = 0x3E7FE0;

    private static final int READOUT_RIGHT = FocusedSpiritMatrixMenu.WIDTH - 12;

    private static final int BAR_WIDTH = READOUT_RIGHT - READOUT_X;

    private static final int RING_X = 88;
    private static final int RING_Y = 71;
    private static final int RING_SIZE = 36;
    private static final int RING_LIT_U = 200;
    private static final int RING_LIT_V = 0;

    /**
     * Rows of the hexagonal obelisk socket, as {@code x1, x2, y1, y2} spans relative to the slot's own 16x16 origin.
     */
    private static final int[] OBELISK_HIGHLIGHT = {
        7, 9, -4, -3,
        5, 11, -3, -2,
        3, 13, -2, -1,
        1, 15, -1, 0,
        -1, 17, 0, 14,
        1, 15, 14, 15,
        3, 13, 15, 16,
        5, 11, 16, 17,
        7, 9, 17, 18,
    };

    /** Drawing the lit ritual ring here rather than in {@code render} keeps it behind the slot's contents. */
    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        super.renderBg(graphics, partialTick, mouseX, mouseY);
        FocusedSpiritMatrixBlockEntity matrix = menu.matrix();
        if (matrix != null && !matrix.activeResult().isEmpty()) {
            graphics.blit(
                    texture(),
                    leftPos + RING_X,
                    topPos + RING_Y,
                    RING_LIT_U,
                    RING_LIT_V,
                    RING_SIZE,
                    RING_SIZE,
                    256,
                    256);
        }
    }

    @Override
    protected void renderSlotHighlight(GuiGraphics graphics, Slot slot, int mouseX, int mouseY, float partialTick) {
        if (!menu.isUpgradeSlot(slot)) {
            super.renderSlotHighlight(graphics, slot, mouseX, mouseY, partialTick);
            return;
        }
        if (!slot.isHighlightable()) {
            return;
        }
        int colour = getSlotColor(slot.index);
        for (int span = 0; span < OBELISK_HIGHLIGHT.length; span += 4) {
            graphics.fillGradient(
                    RenderType.guiOverlay(),
                    slot.x + OBELISK_HIGHLIGHT[span],
                    slot.y + OBELISK_HIGHLIGHT[span + 2],
                    slot.x + OBELISK_HIGHLIGHT[span + 1],
                    slot.y + OBELISK_HIGHLIGHT[span + 3],
                    colour,
                    colour,
                    0);
        }
    }

    @Override
    protected void drawTrailing(GuiGraphics graphics, Component text, int y, int occupiedUntil, int colour) {
        int x = READOUT_RIGHT - font.width(text);
        if (x < occupiedUntil + 6) {
            return;
        }
        graphics.drawString(font, text, x, y, colour, false);
    }

    @Nullable
    private ReadoutButton clearPending;

    @Nullable
    private ReadoutButton spiritRestock;

    @Nullable
    private Boolean shownRestock;

    public FocusedSpiritMatrixScreen(FocusedSpiritMatrixMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth = FocusedSpiritMatrixMenu.WIDTH;
        this.imageHeight = FocusedSpiritMatrixMenu.HEIGHT;
    }

    @Override
    protected ResourceLocation texture() {
        return TEXTURE;
    }

    @Override
    protected void init() {
        super.init();
        resetButtons();
        addHelpButton("nep/malum/focused-spirit-matrix.md");
        clearPending = addRightButton(
                2,
                NepIcons.REMOVE,
                "gui.nep.clear_pending",
                "gui.nep.clear_pending.hint",
                () -> sendButton(FocusedSpiritMatrixMenu.BUTTON_CLEAR_PENDING));
        addRightButton(
                3,
                NepIcons.DOWN,
                "gui.nep.clear_buffer",
                "gui.nep.clear_buffer.hint",
                () -> sendButton(FocusedSpiritMatrixMenu.BUTTON_CLEAR_BUFFER));
        shownRestock = null;
        spiritRestock = addRightButton(
                4,
                restockOn() ? NepIcons.UP : NepIcons.UP_BLOCKED,
                "gui.nep.focused_spirit_matrix.restock",
                "gui.nep.focused_spirit_matrix.restock.hint",
                () -> sendButton(FocusedSpiritMatrixMenu.BUTTON_SPIRIT_RESTOCK));
    }

    private boolean restockOn() {
        FocusedSpiritMatrixBlockEntity matrix = menu.matrix();
        return matrix != null && matrix.spiritRestock();
    }

    private void updateRestockButton() {
        if (spiritRestock == null) {
            return;
        }
        boolean on = restockOn();
        if (shownRestock != null && shownRestock == on) {
            return;
        }
        shownRestock = on;
        spiritRestock.setGlyph(on ? NepIcons.UP : NepIcons.UP_BLOCKED);
        spiritRestock.setTooltip(Tooltip.create(Component.empty()
                .append(Component.translatable("gui.nep.focused_spirit_matrix.restock"))
                .append("\n")
                .append(Component.translatable(
                                on
                                        ? "gui.nep.focused_spirit_matrix.restock.on"
                                        : "gui.nep.focused_spirit_matrix.restock.off")
                        .withStyle(ChatFormatting.YELLOW))
                .append("\n")
                .append(Component.translatable("gui.nep.focused_spirit_matrix.restock.hint")
                        .withStyle(ChatFormatting.GRAY))));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        FocusedSpiritMatrixBlockEntity matrix = menu.matrix();
        if (clearPending != null) {
            clearPending.active = matrix != null
                    && (matrix.hasPending() || matrix.refusal() != FocusedSpiritMatrixBlockEntity.Refusal.NONE);
        }
        updateRestockButton();
        tickButtons();
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(
                font, Component.translatable("gui.nep.focused_spirit_matrix.title"), 8, 7, TITLE_ON_PANEL, false);
        graphics.drawString(
                font,
                Component.translatable("gui.nep.focused_spirit_matrix.input"),
                FocusedSpiritMatrixMenu.INPUT_X,
                FocusedSpiritMatrixMenu.INPUT_Y - 10,
                TITLE_ON_PANEL,
                false);
        graphics.drawString(
                font,
                Component.translatable("gui.nep.focused_spirit_matrix.spirits"),
                FocusedSpiritMatrixMenu.SPIRIT_X,
                FocusedSpiritMatrixMenu.SPIRIT_Y - 10,
                TITLE_ON_PANEL,
                false);
        graphics.drawString(
                font,
                Component.translatable("gui.nep.focused_spirit_matrix.output"),
                FocusedSpiritMatrixMenu.OUTPUT_X,
                FocusedSpiritMatrixMenu.OUTPUT_Y - 10,
                TITLE_ON_PANEL,
                false);
        graphics.drawString(
                font,
                Component.translatable("container.inventory"),
                FocusedSpiritMatrixMenu.INV_X,
                FocusedSpiritMatrixMenu.INV_Y - 10,
                TITLE_ON_PANEL,
                false);

        FocusedSpiritMatrixBlockEntity matrix = menu.matrix();
        if (matrix == null) {
            return;
        }

        graphics.drawString(font, statusLine(matrix), READOUT_X, LINE_ONE_Y, statusColour(matrix), false);

        ItemStack making = matrix.activeResult();
        Component craftingLabel = Component.translatable("gui.nep.focused_spirit_matrix.crafting");
        graphics.drawString(font, craftingLabel, READOUT_X, LINE_TWO_Y, READOUT_DIM, false);
        int craftingX = READOUT_X + font.width(craftingLabel) + 4;
        long queued = matrix.pendingJobs();
        Component queuedText =
                queued > 0 ? Component.translatable("gui.nep.focused_spirit_matrix.queued", queued) : Component.empty();
        int nameLimit = READOUT_RIGHT - craftingX - (queued > 0 ? font.width(queuedText) + 6 : 0);
        Component craftingName = making.isEmpty()
                ? Component.translatable("gui.nep.focused_spirit_matrix.crafting.idle")
                : making.getHoverName();
        String craftingText = trim(craftingName, nameLimit);
        graphics.drawString(
                font, craftingText, craftingX, LINE_TWO_Y, making.isEmpty() ? READOUT_DIM : READOUT_TEXT, false);
        if (queued > 0) {
            drawTrailing(graphics, queuedText, LINE_TWO_Y, craftingX + font.width(craftingText), READOUT_DIM);
        }

        Component obeliskLine = obeliskLine(matrix);
        graphics.drawString(font, obeliskLine, READOUT_X, LINE_THREE_Y, READOUT_DIM, false);
        drawTrailing(
                graphics,
                Component.translatable("gui.nep.focused_spirit_matrix.craft_time", matrix.craftTicks()),
                LINE_THREE_Y,
                READOUT_X + font.width(obeliskLine),
                READOUT_DIM);

        graphics.fill(BAR_X, BAR_Y, BAR_X + BAR_WIDTH, BAR_Y + BAR_HEIGHT, 0xFF000000 | BAR_TRACK);
        int filled = Math.round(BAR_WIDTH * clamp01(matrix.craftProgress()));
        if (filled > 0) {
            graphics.fill(BAR_X, BAR_Y, BAR_X + filled, BAR_Y + BAR_HEIGHT, 0xFF000000 | CRAFT_FILL);
        }
    }

    private static Component obeliskLine(FocusedSpiritMatrixBlockEntity matrix) {
        if (!matrix.activeResult().isEmpty()
                && matrix.activeProcess() == FocusedSpiritMatrixBlockEntity.Process.FOCUSING) {
            return matrix.catalyzerCount() <= 0
                    ? Component.translatable("gui.nep.focused_spirit_matrix.catalyzers.none")
                    : Component.translatable(
                            "gui.nep.focused_spirit_matrix.catalyzers_line",
                            matrix.catalyzerCount(),
                            FocusedSpiritMatrixUpgrades.maxCatalyzers());
        }
        return matrix.upgradeCount() <= 0
                ? Component.translatable("gui.nep.focused_spirit_matrix.obelisks.none")
                : Component.translatable(
                        "gui.nep.focused_spirit_matrix.obelisks_line",
                        matrix.upgradeCount(),
                        FocusedSpiritMatrixUpgrades.maxObelisks());
    }

    private static Component faultLine(
            FocusedSpiritMatrixBlockEntity.Stall stall, FocusedSpiritMatrixBlockEntity.Refusal refusal) {
        String key =
                switch (stall) {
                    case NONE -> null;
                    case INGREDIENTS -> "gui.nep.focused_spirit_matrix.stall.ingredients";
                    case NO_IMPETUS -> "gui.nep.focused_spirit_matrix.stall.no_impetus";
                    case OUTPUT_FULL -> "gui.nep.focused_spirit_matrix.output_blocked";
                    case NO_RECIPE -> "gui.nep.focused_spirit_matrix.stall.no_recipe";
                    case FOCUSING_DISABLED -> "gui.nep.focused_spirit_matrix.stall.focusing_disabled";
                    case RUNEWORKING_DISABLED -> "gui.nep.focused_spirit_matrix.stall.runeworking_disabled";
                };
        if (key != null) {
            return Component.translatable(key);
        }
        String refused =
                switch (refusal) {
                    case NONE -> null;
                    case WRONG_PATTERN -> "gui.nep.focused_spirit_matrix.refused.wrong_pattern";
                    case NO_IMPETUS -> "gui.nep.focused_spirit_matrix.refused.no_impetus";
                    case FOCUSING_DISABLED -> "gui.nep.focused_spirit_matrix.refused.focusing_disabled";
                    case RUNEWORKING_DISABLED -> "gui.nep.focused_spirit_matrix.refused.runeworking_disabled";
                    case NO_ITEM_OUTPUT -> "gui.nep.focused_spirit_matrix.refused.no_item_output";
                    case UNKNOWN_RECIPE -> "gui.nep.focused_spirit_matrix.refused.unknown_recipe";
                    case MISMATCHED_INPUTS -> "gui.nep.focused_spirit_matrix.refused.mismatched_inputs";
                    case MIXED_RECIPES -> "gui.nep.focused_spirit_matrix.refused.mixed_recipes";
                    case ITEMS_ONLY -> "gui.nep.focused_spirit_matrix.refused.items_only";
                    case TOO_MANY_INPUTS -> "gui.nep.focused_spirit_matrix.refused.too_many_inputs";
                    case BUFFER_FULL -> "gui.nep.focused_spirit_matrix.refused.buffer_full";
                };
        return refused == null
                ? Component.translatable("gui.nep.focused_spirit_matrix.idle")
                : Component.translatable(refused);
    }

    private static List<Component> statusHint(FocusedSpiritMatrixBlockEntity matrix) {
        if (matrix.hasPowerFault()) {
            return List.of(Component.translatable("gui.nep.focused_spirit_matrix.stall.no_me_power.hint")
                    .withStyle(ChatFormatting.GRAY));
        }
        String key =
                switch (matrix.stall()) {
                    case NONE -> null;
                    case INGREDIENTS -> "gui.nep.focused_spirit_matrix.stall.ingredients.hint";
                    case NO_IMPETUS -> "gui.nep.focused_spirit_matrix.stall.no_impetus.hint";
                    case OUTPUT_FULL -> "gui.nep.focused_spirit_matrix.stall.output_full.hint";
                    case NO_RECIPE -> "gui.nep.focused_spirit_matrix.stall.no_recipe.hint";
                    case FOCUSING_DISABLED -> "gui.nep.focused_spirit_matrix.stall.focusing_disabled.hint";
                    case RUNEWORKING_DISABLED -> "gui.nep.focused_spirit_matrix.stall.runeworking_disabled.hint";
                };
        if (key == null && matrix.refusal() != FocusedSpiritMatrixBlockEntity.Refusal.NONE) {
            key = refusalHint(matrix.refusal());
        }
        if (key == null && matrix.isOutputBlocked()) {
            key = "gui.nep.focused_spirit_matrix.stall.output_full.hint";
        }
        return key == null ? List.of() : List.of(Component.translatable(key).withStyle(ChatFormatting.GRAY));
    }

    private static String refusalHint(FocusedSpiritMatrixBlockEntity.Refusal refusal) {
        return switch (refusal) {
            case BUFFER_FULL -> "gui.nep.focused_spirit_matrix.refused.buffer_full.hint";
            case TOO_MANY_INPUTS -> "gui.nep.focused_spirit_matrix.refused.too_many_inputs.hint";
            case NO_IMPETUS -> "gui.nep.focused_spirit_matrix.refused.no_impetus.hint";
            case FOCUSING_DISABLED -> "gui.nep.focused_spirit_matrix.refused.focusing_disabled.hint";
            case RUNEWORKING_DISABLED -> "gui.nep.focused_spirit_matrix.refused.runeworking_disabled.hint";
            default -> "gui.nep.focused_spirit_matrix.refused.hint";
        };
    }

    private static Component statusLine(FocusedSpiritMatrixBlockEntity matrix) {
        if (matrix.hasChannelFault()) {
            return Component.translatable("gui.nep.focused_spirit_matrix.no_network_channels");
        }
        if (matrix.hasPowerFault()) {
            return Component.translatable("gui.nep.focused_spirit_matrix.no_network_power");
        }
        if (matrix.stall() != FocusedSpiritMatrixBlockEntity.Stall.NONE
                || matrix.refusal() != FocusedSpiritMatrixBlockEntity.Refusal.NONE) {
            return faultLine(matrix.stall(), matrix.refusal());
        }
        if (matrix.isOutputBlocked()) {
            return Component.translatable("gui.nep.focused_spirit_matrix.output_blocked");
        }
        if (!matrix.missingInputs().isEmpty()) {
            return Component.translatable("gui.nep.focused_spirit_matrix.missing");
        }
        if (matrix.activeResult().isEmpty()) {
            return Component.translatable("gui.nep.focused_spirit_matrix.idle");
        }
        return Component.translatable(
                matrix.activeProcess() == FocusedSpiritMatrixBlockEntity.Process.FOCUSING
                        ? "gui.nep.focused_spirit_matrix.focusing"
                        : "gui.nep.focused_spirit_matrix.infusing");
    }

    private static boolean faulted(FocusedSpiritMatrixBlockEntity matrix) {
        return matrix.stall() != FocusedSpiritMatrixBlockEntity.Stall.NONE
                || matrix.refusal() != FocusedSpiritMatrixBlockEntity.Refusal.NONE
                || matrix.isOutputBlocked()
                || matrix.hasPowerFault()
                || !matrix.missingInputs().isEmpty();
    }

    private static int statusColour(FocusedSpiritMatrixBlockEntity matrix) {
        if (faulted(matrix)) {
            return READOUT_WARN;
        }
        return matrix.activeResult().isEmpty() ? READOUT_DIM : READOUT_TEXT;
    }

    @Override
    protected void renderReadoutTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        FocusedSpiritMatrixBlockEntity matrix = menu.matrix();
        if (matrix == null) {
            return;
        }
        if (menu.upgradeSlotEmpty()
                && within(
                        mouseX, mouseY, FocusedSpiritMatrixMenu.UPGRADE_X, FocusedSpiritMatrixMenu.UPGRADE_Y, 16, 16)) {
            graphics.renderComponentTooltip(font, emptyUpgradeTooltip(), mouseX, mouseY);
            return;
        }
        if (menu.catalyzerSlotEmpty()
                && within(
                        mouseX,
                        mouseY,
                        FocusedSpiritMatrixMenu.CATALYZER_X,
                        FocusedSpiritMatrixMenu.CATALYZER_Y,
                        16,
                        16)) {
            graphics.renderComponentTooltip(font, emptyCatalyzerTooltip(), mouseX, mouseY);
            return;
        }
        if (menu.impetusSlotEmpty()
                && within(
                        mouseX, mouseY, FocusedSpiritMatrixMenu.IMPETUS_X, FocusedSpiritMatrixMenu.IMPETUS_Y, 16, 16)) {
            graphics.renderComponentTooltip(font, emptyImpetusTooltip(), mouseX, mouseY);
            return;
        }
        int emptySpirit = hoveredEmptySpiritSlot(mouseX, mouseY);
        if (emptySpirit >= 0) {
            graphics.renderComponentTooltip(font, emptySpiritTooltip(emptySpirit), mouseX, mouseY);
            return;
        }
        if (!within(mouseX, mouseY, READOUT_X, LINE_ONE_Y, BAR_WIDTH, 32)) {
            return;
        }
        List<Component> lines = new ArrayList<>();
        lines.add(statusLine(matrix));
        lines.addAll(statusHint(matrix));
        ItemStack making = matrix.activeResult();
        if (!making.isEmpty()) {
            lines.add(Component.translatable("gui.nep.focused_spirit_matrix.crafting_tooltip", making.getHoverName())
                    .withStyle(ChatFormatting.GRAY));
        }
        long queued = matrix.pendingJobs();
        if (queued > 0) {
            lines.add(Component.translatable("gui.nep.focused_spirit_matrix.queued_tooltip", queued)
                    .withStyle(ChatFormatting.GRAY));
        }
        List<GenericStack> missing = matrix.missingInputs();
        if (!missing.isEmpty()) {
            lines.add(Component.translatable("gui.nep.focused_spirit_matrix.missing"));
            for (GenericStack stack : missing) {
                lines.add(MissingStacks.describe(stack).withStyle(ChatFormatting.GRAY));
            }
        }
        graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
    }

    private static List<Component> emptySpiritTooltip(int slot) {
        if (slot == SpiritBank.JOKER_SLOT) {
            return List.of(
                    Component.translatable("gui.nep.focused_spirit_matrix.joker_slot"),
                    Component.translatable("gui.nep.focused_spirit_matrix.joker_slot.hint")
                            .withStyle(ChatFormatting.GRAY));
        }
        return List.of(
                new ItemStack(SpiritBank.spiritFor(slot)).getHoverName(),
                Component.translatable("gui.nep.focused_spirit_matrix.spirit_slot.hint")
                        .withStyle(ChatFormatting.GRAY));
    }

    private int hoveredEmptySpiritSlot(int mouseX, int mouseY) {
        for (int slot = 0; slot < SpiritBank.SLOTS; slot++) {
            int x = FocusedSpiritMatrixMenu.SPIRIT_X + (slot % SpiritBank.COLUMNS) * 18;
            int y = FocusedSpiritMatrixMenu.SPIRIT_Y + (slot / SpiritBank.COLUMNS) * 18;
            if (within(mouseX, mouseY, x, y, 16, 16) && menu.spiritSlotEmpty(slot)) {
                return slot;
            }
        }
        return -1;
    }

    private static List<Component> emptyUpgradeTooltip() {
        int max = FocusedSpiritMatrixUpgrades.maxObelisks();
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("gui.nep.focused_spirit_matrix.upgrade_obelisk"));
        if (!FocusedSpiritMatrixUpgrades.obelisksEnabled()) {
            lines.add(Component.translatable("gui.nep.focused_spirit_matrix.upgrade_obelisk.disabled")
                    .withStyle(ChatFormatting.GRAY));
            return lines;
        }
        lines.add(Component.translatable("gui.nep.focused_spirit_matrix.upgrade_obelisk.capacity", max)
                .withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable(
                        "gui.nep.focused_spirit_matrix.upgrade_obelisk.speed",
                        FocusedSpiritMatrixUpgrades.craftTicks(max))
                .withStyle(ChatFormatting.GRAY));
        return lines;
    }

    private static List<Component> emptyImpetusTooltip() {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("gui.nep.focused_spirit_matrix.upgrade_impetus"));
        lines.add(Component.translatable(
                        FocusedSpiritMatrixUpgrades.focusingEnabled()
                                ? "gui.nep.focused_spirit_matrix.upgrade_impetus.hint"
                                : "gui.nep.focused_spirit_matrix.upgrade_impetus.disabled")
                .withStyle(ChatFormatting.GRAY));
        return lines;
    }

    private static List<Component> emptyCatalyzerTooltip() {
        int max = FocusedSpiritMatrixUpgrades.maxCatalyzers();
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("gui.nep.focused_spirit_matrix.upgrade_catalyzer"));
        if (!FocusedSpiritMatrixUpgrades.catalyzersEnabled()) {
            lines.add(Component.translatable("gui.nep.focused_spirit_matrix.upgrade_catalyzer.disabled")
                    .withStyle(ChatFormatting.GRAY));
            return lines;
        }
        lines.add(Component.translatable("gui.nep.focused_spirit_matrix.upgrade_catalyzer.capacity", max)
                .withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable(
                        "gui.nep.focused_spirit_matrix.upgrade_catalyzer.speed",
                        FocusedSpiritMatrixUpgrades.focusingTimeReductionPercent(900, max))
                .withStyle(ChatFormatting.GRAY));
        addCatalyzerStats(lines, max);
        return lines;
    }

    private static void addCatalyzerStats(List<Component> lines, int catalyzers) {
        lines.add(Component.translatable(
                        "gui.nep.focused_spirit_matrix.catalyzer_restoration",
                        percent(FocusedSpiritMatrixUpgrades.restorationChance(catalyzers)))
                .withStyle(ChatFormatting.GRAY));
        if (!NepConfig.malumFocusedSpiritMatrixConsumeImpetusDurability()) {
            lines.add(Component.translatable("gui.nep.focused_spirit_matrix.catalyzer_restoration.idle")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        lines.add(Component.translatable(
                        "gui.nep.focused_spirit_matrix.catalyzer_chain_focusing",
                        percent(FocusedSpiritMatrixUpgrades.chainFocusingChance(catalyzers)))
                .withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable(
                        "gui.nep.focused_spirit_matrix.catalyzer_fortune",
                        percent(FocusedSpiritMatrixUpgrades.fortuneChance(catalyzers)))
                .withStyle(ChatFormatting.GRAY));
    }

    private static int percent(double chance) {
        return (int) Math.round(chance * 100.0);
    }

    @Override
    protected List<Component> getTooltipFromContainerItem(ItemStack stack) {
        List<Component> lines = new ArrayList<>(super.getTooltipFromContainerItem(stack));
        if (menu.isCatalyzerSlot(hoveredSlot) && FocusedSpiritMatrixUpgrades.isCatalyzer(stack)) {
            int max = FocusedSpiritMatrixUpgrades.maxCatalyzers();
            int catalyzers = Math.min(stack.getCount(), max);
            lines.add(Component.translatable("gui.nep.focused_spirit_matrix.catalyzer_installed", catalyzers, max)
                    .withStyle(ChatFormatting.AQUA));
            lines.add(Component.translatable(
                            "gui.nep.focused_spirit_matrix.catalyzer_craft_time",
                            FocusedSpiritMatrixUpgrades.focusingTimeReductionPercent(900, catalyzers))
                    .withStyle(ChatFormatting.GRAY));
            addCatalyzerStats(lines, catalyzers);
            return lines;
        }
        if (!menu.isUpgradeSlot(hoveredSlot)) {
            return lines;
        }
        if (!FocusedSpiritMatrixUpgrades.isObelisk(stack)) {
            return lines;
        }
        int max = FocusedSpiritMatrixUpgrades.maxObelisks();
        int obelisks = Math.min(stack.getCount(), max);
        lines.add(Component.translatable("gui.nep.focused_spirit_matrix.upgrade_installed", obelisks, max)
                .withStyle(ChatFormatting.AQUA));
        lines.add(Component.translatable(
                        "gui.nep.focused_spirit_matrix.upgrade_craft_time",
                        FocusedSpiritMatrixUpgrades.craftTicks(obelisks),
                        FocusedSpiritMatrixUpgrades.craftTimeReductionPercent(obelisks))
                .withStyle(ChatFormatting.GRAY));
        return lines;
    }
}
