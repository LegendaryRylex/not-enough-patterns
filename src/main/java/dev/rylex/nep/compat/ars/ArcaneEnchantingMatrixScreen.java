package dev.rylex.nep.compat.ars;

import appeng.api.stacks.GenericStack;
import dev.rylex.nep.Nep;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.NepIcons;
import dev.rylex.nep.client.EnergyGauge;
import dev.rylex.nep.client.MatrixScreen;
import dev.rylex.nep.client.ReadoutButton;
import dev.rylex.nep.util.MissingStacks;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class ArcaneEnchantingMatrixScreen extends MatrixScreen<ArcaneEnchantingMatrixMenu> {

    private static final ResourceLocation TEXTURE = Nep.id("textures/gui/arcane_enchanting_matrix.png");
    private static final ResourceLocation SOURCE_GAUGE = Nep.id("textures/gui/source_gauge.png");

    private static final int TITLE_ON_PANEL = 0x3A1C36;
    private static final int READOUT_TEXT = 0xF2E8D0;
    private static final int READOUT_DIM = 0xC8A2BE;

    private static final int BAR_TRACK = 0x3A0D35;
    private static final int CRAFT_FILL = 0xE04FD0;
    private static final int GHOST_VEIL = 0xA08B7A8B;

    @Nullable
    private ReadoutButton clearPending;

    public ArcaneEnchantingMatrixScreen(ArcaneEnchantingMatrixMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth = ArcaneEnchantingMatrixMenu.WIDTH;
        this.imageHeight = ArcaneEnchantingMatrixMenu.HEIGHT;
    }

    @Override
    protected ResourceLocation texture() {
        return TEXTURE;
    }

    @Override
    protected int sheetHeight() {
        return ArcaneEnchantingMatrixMenu.SHEET_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        resetButtons();
        addHelpButton("nep/ars_nouveau/arcane-enchanting-matrix.md");
        clearPending = addRightButton(
                2,
                NepIcons.REMOVE,
                "gui.nep.clear_pending",
                "gui.nep.clear_pending.hint",
                () -> sendButton(ArcaneEnchantingMatrixMenu.BUTTON_CLEAR_PENDING));
        addRightButton(
                3,
                NepIcons.DOWN,
                "gui.nep.clear_buffer",
                "gui.nep.clear_buffer.hint",
                () -> sendButton(ArcaneEnchantingMatrixMenu.BUTTON_CLEAR_BUFFER));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        ArcaneEnchantingMatrixBlockEntity matrix = menu.matrix();
        if (clearPending != null) {
            clearPending.active = matrix != null
                    && (matrix.hasPending() || matrix.refusal() != ArcaneEnchantingMatrixBlockEntity.Refusal.NONE);
        }
        tickButtons();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        super.renderBg(graphics, partialTick, mouseX, mouseY);
        if (menu.accelerateSlotEmpty()) {
            drawGhost(
                    graphics,
                    ArcaneEnchantingMatrixUpgrades.accelerateIcon(),
                    ArcaneEnchantingMatrixMenu.ACCELERATE_X,
                    ArcaneEnchantingMatrixMenu.ACCELERATE_Y);
        }
        if (menu.dampenSlotEmpty()) {
            drawGhost(
                    graphics,
                    ArcaneEnchantingMatrixUpgrades.dampenIcon(),
                    ArcaneEnchantingMatrixMenu.DAMPEN_X,
                    ArcaneEnchantingMatrixMenu.DAMPEN_Y);
        }
        ArcaneEnchantingMatrixBlockEntity matrix = menu.matrix();
        if (matrix != null) {
            EnergyGauge.draw(
                    graphics,
                    SOURCE_GAUGE,
                    leftPos + ArcaneEnchantingMatrixMenu.GAUGE_X,
                    topPos + ArcaneEnchantingMatrixMenu.GAUGE_Y,
                    matrix.storedSource(),
                    MatrixSourceStore.CAPACITY);
        }
    }

    private void drawGhost(GuiGraphics graphics, ItemStack icon, int x, int y) {
        int left = leftPos + x;
        int top = topPos + y;
        graphics.renderFakeItem(icon, left, top);
        graphics.fill(RenderType.guiGhostRecipeOverlay(), left, top, left + 16, top + 16, GHOST_VEIL);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(
                font, Component.translatable("gui.nep.arcane_enchanting_matrix.title"), 8, 7, TITLE_ON_PANEL, false);
        graphics.drawString(
                font,
                Component.translatable("gui.nep.arcane_enchanting_matrix.input"),
                ArcaneEnchantingMatrixMenu.INPUT_X,
                ArcaneEnchantingMatrixMenu.INPUT_Y - 10,
                TITLE_ON_PANEL,
                false);
        graphics.drawString(
                font,
                Component.translatable("gui.nep.arcane_enchanting_matrix.catalysts"),
                ArcaneEnchantingMatrixMenu.CATALYST_X,
                ArcaneEnchantingMatrixMenu.CATALYST_Y - 10,
                TITLE_ON_PANEL,
                false);
        graphics.drawString(
                font,
                Component.translatable("gui.nep.arcane_enchanting_matrix.output"),
                ArcaneEnchantingMatrixMenu.OUTPUT_X,
                ArcaneEnchantingMatrixMenu.OUTPUT_Y - 10,
                TITLE_ON_PANEL,
                false);
        graphics.drawString(
                font,
                Component.translatable("container.inventory"),
                ArcaneEnchantingMatrixMenu.INV_X,
                ArcaneEnchantingMatrixMenu.INV_Y - 10,
                TITLE_ON_PANEL,
                false);

        ArcaneEnchantingMatrixBlockEntity matrix = menu.matrix();
        if (matrix == null) {
            return;
        }

        graphics.drawString(font, statusLine(matrix), READOUT_X, LINE_ONE_Y, statusColour(matrix), false);

        ItemStack making = matrix.activeResult();
        Component craftingLabel = Component.translatable("gui.nep.arcane_enchanting_matrix.crafting");
        graphics.drawString(font, craftingLabel, READOUT_X, LINE_TWO_Y, READOUT_DIM, false);
        int craftingX = READOUT_X + font.width(craftingLabel) + 4;
        long queued = matrix.pendingJobs();
        Component queuedText = queued > 0
                ? Component.translatable("gui.nep.arcane_enchanting_matrix.queued", queued)
                : Component.empty();
        int nameLimit = readoutRight() - craftingX - (queued > 0 ? font.width(queuedText) + 6 : 0);
        Component craftingName = making.isEmpty()
                ? Component.translatable("gui.nep.arcane_enchanting_matrix.crafting.idle")
                : making.getHoverName();
        String craftingText = trim(craftingName, nameLimit);
        graphics.drawString(
                font, craftingText, craftingX, LINE_TWO_Y, making.isEmpty() ? READOUT_DIM : READOUT_TEXT, false);
        if (queued > 0) {
            drawTrailing(graphics, queuedText, LINE_TWO_Y, craftingX + font.width(craftingText), READOUT_DIM);
        }

        Component sourceLine = Component.translatable(
                "gui.nep.arcane_enchanting_matrix.source_line",
                format(matrix.storedSource()),
                format(MatrixSourceStore.CAPACITY));
        graphics.drawString(font, sourceLine, READOUT_X, LINE_THREE_Y, READOUT_DIM, false);
        int ticks = making.isEmpty() ? matrix.nextCraftTicks() : matrix.activeTicks();
        if (ticks > 0) {
            drawTrailing(
                    graphics,
                    Component.translatable("gui.nep.arcane_enchanting_matrix.craft_time", ticks),
                    LINE_THREE_Y,
                    READOUT_X + font.width(sourceLine),
                    READOUT_DIM);
        }

        drawProgressBar(graphics, matrix.craftProgress(), BAR_TRACK, CRAFT_FILL);
    }

    private static Component faultLine(
            ArcaneEnchantingMatrixBlockEntity.Stall stall, ArcaneEnchantingMatrixBlockEntity.Refusal refusal) {
        String key =
                switch (stall) {
                    case NONE -> null;
                    case INGREDIENTS -> "gui.nep.arcane_enchanting_matrix.stall.ingredients";
                    case CATALYSTS -> "gui.nep.arcane_enchanting_matrix.stall.catalysts";
                    case CATALYST_SHELF_FULL -> "gui.nep.arcane_enchanting_matrix.stall.catalyst_shelf_full";
                    case SOURCE -> "gui.nep.arcane_enchanting_matrix.stall.source";
                    case OUTPUT_FULL -> "gui.nep.arcane_enchanting_matrix.output_blocked";
                    case NO_RECIPE -> "gui.nep.arcane_enchanting_matrix.stall.no_recipe";
                };
        if (key != null) {
            return Component.translatable(key);
        }
        String refused =
                switch (refusal) {
                    case NONE -> null;
                    case NOT_AN_ARS_PATTERN -> "gui.nep.arcane_enchanting_matrix.refused.wrong_pattern";
                    case NO_ITEM_OUTPUT -> "gui.nep.arcane_enchanting_matrix.refused.no_item_output";
                    case UNKNOWN_RECIPE -> "gui.nep.arcane_enchanting_matrix.refused.unknown_recipe";
                    case MIXED_RECIPES -> "gui.nep.arcane_enchanting_matrix.refused.mixed_recipes";
                    case ITEMS_ONLY -> "gui.nep.arcane_enchanting_matrix.refused.items_only";
                    case CATALYSTS_IN_PATTERN -> "gui.nep.arcane_enchanting_matrix.refused.catalysts_in_pattern";
                    case TOO_MANY_INPUTS -> "gui.nep.arcane_enchanting_matrix.refused.too_many_inputs";
                    case BUFFER_FULL -> "gui.nep.arcane_enchanting_matrix.refused.buffer_full";
                };
        return refused == null
                ? Component.translatable("gui.nep.arcane_enchanting_matrix.idle")
                : Component.translatable(refused);
    }

    private static List<Component> statusHint(ArcaneEnchantingMatrixBlockEntity matrix) {
        if (matrix.hasPowerFault()) {
            return List.of(Component.translatable("gui.nep.arcane_enchanting_matrix.stall.no_me_power.hint")
                    .withStyle(ChatFormatting.GRAY));
        }
        String key =
                switch (matrix.stall()) {
                    case NONE -> null;
                    case INGREDIENTS -> "gui.nep.arcane_enchanting_matrix.stall.ingredients.hint";
                    case CATALYSTS -> "gui.nep.arcane_enchanting_matrix.stall.catalysts.hint";
                    case CATALYST_SHELF_FULL -> "gui.nep.arcane_enchanting_matrix.stall.catalyst_shelf_full.hint";
                    case SOURCE -> "gui.nep.arcane_enchanting_matrix.stall.source.hint";
                    case OUTPUT_FULL -> "gui.nep.arcane_enchanting_matrix.stall.output_full.hint";
                    case NO_RECIPE -> "gui.nep.arcane_enchanting_matrix.stall.no_recipe.hint";
                };
        if (key == null && matrix.refusal() == ArcaneEnchantingMatrixBlockEntity.Refusal.CATALYSTS_IN_PATTERN) {
            key = "gui.nep.arcane_enchanting_matrix.refused.catalysts_in_pattern.hint";
        }
        if (key == null && matrix.refusal() != ArcaneEnchantingMatrixBlockEntity.Refusal.NONE) {
            key = "gui.nep.arcane_enchanting_matrix.refused.hint";
        }
        if (key == null && matrix.isOutputBlocked()) {
            key = "gui.nep.arcane_enchanting_matrix.stall.output_full.hint";
        }
        return key == null ? List.of() : List.of(Component.translatable(key).withStyle(ChatFormatting.GRAY));
    }

    private static Component statusLine(ArcaneEnchantingMatrixBlockEntity matrix) {
        if (matrix.hasChannelFault()) {
            return Component.translatable("gui.nep.arcane_enchanting_matrix.no_network_channels");
        }
        if (matrix.hasPowerFault()) {
            return Component.translatable("gui.nep.arcane_enchanting_matrix.no_network_power");
        }
        if (matrix.stall() != ArcaneEnchantingMatrixBlockEntity.Stall.NONE
                || matrix.refusal() != ArcaneEnchantingMatrixBlockEntity.Refusal.NONE) {
            return faultLine(matrix.stall(), matrix.refusal());
        }
        if (matrix.isOutputBlocked()) {
            return Component.translatable("gui.nep.arcane_enchanting_matrix.output_blocked");
        }
        if (!matrix.missingInputs().isEmpty()) {
            return Component.translatable("gui.nep.arcane_enchanting_matrix.missing");
        }
        if (matrix.activeResult().isEmpty()) {
            return Component.translatable("gui.nep.arcane_enchanting_matrix.idle");
        }
        return Component.translatable(
                matrix.activeKind() == ArcaneEnchantingMatrixBlockEntity.Kind.APPARATUS
                        ? "gui.nep.arcane_enchanting_matrix.enchanting_now"
                        : "gui.nep.arcane_enchanting_matrix.imbuing_now");
    }

    private static boolean faulted(ArcaneEnchantingMatrixBlockEntity matrix) {
        return matrix.stall() != ArcaneEnchantingMatrixBlockEntity.Stall.NONE
                || matrix.refusal() != ArcaneEnchantingMatrixBlockEntity.Refusal.NONE
                || matrix.isOutputBlocked()
                || matrix.hasPowerFault()
                || !matrix.missingInputs().isEmpty();
    }

    private static int statusColour(ArcaneEnchantingMatrixBlockEntity matrix) {
        if (faulted(matrix)) {
            return READOUT_WARN;
        }
        return matrix.activeResult().isEmpty() ? READOUT_DIM : READOUT_TEXT;
    }

    private static String format(long value) {
        if (value >= 1_000_000L) {
            return String.format("%.1fM", value / 1_000_000.0);
        }
        if (value >= 1_000L) {
            return String.format("%.1fK", value / 1_000.0);
        }
        return Long.toString(value);
    }

    @Override
    protected void renderReadoutTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        ArcaneEnchantingMatrixBlockEntity matrix = menu.matrix();
        if (matrix == null) {
            return;
        }
        if (within(
                mouseX,
                mouseY,
                ArcaneEnchantingMatrixMenu.GAUGE_X,
                ArcaneEnchantingMatrixMenu.GAUGE_Y,
                EnergyGauge.WIDTH,
                EnergyGauge.HEIGHT)) {
            graphics.renderComponentTooltip(font, sourceTooltip(matrix), mouseX, mouseY);
            return;
        }
        if (menu.accelerateSlotEmpty()
                && within(
                        mouseX,
                        mouseY,
                        ArcaneEnchantingMatrixMenu.ACCELERATE_X,
                        ArcaneEnchantingMatrixMenu.ACCELERATE_Y,
                        16,
                        16)) {
            graphics.renderComponentTooltip(font, emptyAccelerateTooltip(), mouseX, mouseY);
            return;
        }
        if (menu.dampenSlotEmpty()
                && within(
                        mouseX,
                        mouseY,
                        ArcaneEnchantingMatrixMenu.DAMPEN_X,
                        ArcaneEnchantingMatrixMenu.DAMPEN_Y,
                        16,
                        16)) {
            graphics.renderComponentTooltip(font, emptyDampenTooltip(), mouseX, mouseY);
            return;
        }
        if (within(
                        mouseX,
                        mouseY,
                        ArcaneEnchantingMatrixMenu.CATALYST_X,
                        ArcaneEnchantingMatrixMenu.CATALYST_Y,
                        ArcaneEnchantingMatrixMenu.CATALYST_COLUMNS * 18,
                        CatalystShelf.SLOTS / ArcaneEnchantingMatrixMenu.CATALYST_COLUMNS * 18)
                && (hoveredSlot == null || !hoveredSlot.hasItem())) {
            graphics.renderComponentTooltip(
                    font,
                    List.of(
                            Component.translatable("gui.nep.arcane_enchanting_matrix.catalysts"),
                            Component.translatable("gui.nep.arcane_enchanting_matrix.catalysts.hint")
                                    .withStyle(ChatFormatting.GRAY)),
                    mouseX,
                    mouseY);
            return;
        }
        if (!within(mouseX, mouseY, READOUT_X, LINE_ONE_Y, barWidth(), 32)) {
            return;
        }
        List<Component> lines = new ArrayList<>();
        lines.add(statusLine(matrix));
        lines.addAll(statusHint(matrix));
        ItemStack making = matrix.activeResult();
        if (!making.isEmpty()) {
            lines.add(Component.translatable("gui.nep.arcane_enchanting_matrix.crafting_tooltip", making.getHoverName())
                    .withStyle(ChatFormatting.GRAY));
        }
        long queued = matrix.pendingJobs();
        if (queued > 0) {
            lines.add(Component.translatable("gui.nep.arcane_enchanting_matrix.queued_tooltip", queued)
                    .withStyle(ChatFormatting.GRAY));
        }
        List<GenericStack> missing = matrix.missingInputs();
        if (!missing.isEmpty()) {
            lines.add(Component.translatable("gui.nep.arcane_enchanting_matrix.missing"));
            for (GenericStack stack : missing) {
                lines.add(MissingStacks.describe(stack).withStyle(ChatFormatting.GRAY));
            }
        }
        graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
    }

    private static List<Component> sourceTooltip(ArcaneEnchantingMatrixBlockEntity matrix) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("gui.nep.arcane_enchanting_matrix.source"));
        lines.add(Component.translatable(
                        "gui.nep.arcane_enchanting_matrix.source_amount",
                        format(matrix.storedSource()),
                        format(MatrixSourceStore.CAPACITY))
                .withStyle(ChatFormatting.GRAY));
        long needed = matrix.sourceNeeded();
        if (needed > 0) {
            lines.add(Component.translatable("gui.nep.arcane_enchanting_matrix.source_needed", format(needed))
                    .withStyle(ChatFormatting.GRAY));
        }
        lines.add(Component.translatable("gui.nep.arcane_enchanting_matrix.source.hint")
                .withStyle(ChatFormatting.DARK_GRAY));
        return lines;
    }

    private static List<Component> emptyAccelerateTooltip() {
        int max = ArcaneEnchantingMatrixUpgrades.maxAccelerate();
        return List.of(
                Component.translatable("gui.nep.arcane_enchanting_matrix.accelerate_slot"),
                Component.translatable(
                                "gui.nep.arcane_enchanting_matrix.accelerate_slot.hint",
                                max,
                                ArcaneEnchantingMatrixUpgrades.craftTimeReductionPercent(
                                        NepConfig.arsMatrixApparatusCraftTicks(), max))
                        .withStyle(ChatFormatting.GRAY));
    }

    private static List<Component> emptyDampenTooltip() {
        int max = ArcaneEnchantingMatrixUpgrades.maxDampen();
        return List.of(
                Component.translatable("gui.nep.arcane_enchanting_matrix.dampen_slot"),
                Component.translatable(
                                "gui.nep.arcane_enchanting_matrix.dampen_slot.hint",
                                max,
                                ArcaneEnchantingMatrixUpgrades.sourceDiscountPercent(max))
                        .withStyle(ChatFormatting.GRAY));
    }

    @Override
    protected List<Component> getTooltipFromContainerItem(ItemStack stack) {
        List<Component> lines = new ArrayList<>(super.getTooltipFromContainerItem(stack));
        ArcaneEnchantingMatrixBlockEntity matrix = menu.matrix();
        if (matrix == null) {
            return lines;
        }
        if (menu.isAccelerateSlot(hoveredSlot) && ArcaneEnchantingMatrixUpgrades.isAccelerate(stack)) {
            int max = ArcaneEnchantingMatrixUpgrades.maxAccelerate();
            int glyphs = Math.min(stack.getCount(), max);
            lines.add(Component.translatable("gui.nep.arcane_enchanting_matrix.glyphs_installed", glyphs, max)
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
            lines.add(Component.translatable(
                            "gui.nep.arcane_enchanting_matrix.accelerate_effect",
                            matrix.craftTicksFor(ArcaneEnchantingMatrixBlockEntity.Kind.APPARATUS),
                            matrix.craftTicksFor(ArcaneEnchantingMatrixBlockEntity.Kind.IMBUEMENT))
                    .withStyle(ChatFormatting.GRAY));
        } else if (menu.isDampenSlot(hoveredSlot) && ArcaneEnchantingMatrixUpgrades.isDampen(stack)) {
            int max = ArcaneEnchantingMatrixUpgrades.maxDampen();
            int glyphs = Math.min(stack.getCount(), max);
            lines.add(Component.translatable("gui.nep.arcane_enchanting_matrix.glyphs_installed", glyphs, max)
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
            lines.add(Component.translatable(
                            "gui.nep.arcane_enchanting_matrix.dampen_effect",
                            ArcaneEnchantingMatrixUpgrades.sourceDiscountPercent(glyphs))
                    .withStyle(ChatFormatting.GRAY));
        }
        return lines;
    }
}
