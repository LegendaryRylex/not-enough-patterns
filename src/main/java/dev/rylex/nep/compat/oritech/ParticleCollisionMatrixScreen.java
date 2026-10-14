package dev.rylex.nep.compat.oritech;

import appeng.api.stacks.GenericStack;
import dev.rylex.nep.Nep;
import dev.rylex.nep.NepIcons;
import dev.rylex.nep.client.EnergyGauge;
import dev.rylex.nep.client.MatrixScreen;
import dev.rylex.nep.client.ReadoutButton;
import dev.rylex.nep.client.ScreenFormat;
import dev.rylex.nep.util.MissingStacks;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class ParticleCollisionMatrixScreen extends MatrixScreen<ParticleCollisionMatrixMenu> {

    private static final String OUTPUT_FULL_HINT = "gui.nep.particle_collision_matrix.stall.output_full.hint";
    private static final ResourceLocation TEXTURE = Nep.id("textures/gui/particle_collision_matrix.png");

    private static final int TITLE_ON_PANEL = 0x1C3A2E;
    private static final int READOUT_TEXT = 0xD0F2E8;
    private static final int READOUT_DIM = 0x82C8A8;

    private static final int BAR_TRACK = 0x0D3A2E;
    private static final int CRAFT_FILL = 0x3FE0B8;

    private static final int GAUGE_X = 152;
    private static final int GAUGE_Y = 99;

    private static final int STRIP_LEFT = 29;
    private static final int STRIP_RIGHT = 166;
    private static final int STRIP_TOP = 63;
    private static final int STRIP_HEIGHT = 20;
    private static final int STRIP_TEXT_Y = 69;
    private static final int ICON_X = 32;
    private static final int ICON_Y = 65;
    private static final int ICON_SIZE = 16;
    private static final int ICON_PITCH = 18;
    private static final int ICON_HOVER = 0x40FFFFFF;

    private int layoutRevision = -1;

    @Nullable
    private ParticleCollisionMatrixBlockEntity layoutMatrix;

    private List<AddonEntry> layoutEntries = List.of();
    private List<ItemStack> layoutIcons = List.of();
    private int layoutShown;
    private int layoutOverflow;

    @Nullable
    private Component layoutTotals;

    private int layoutTotalsX;

    @Nullable
    private ReadoutButton clearPending;

    public ParticleCollisionMatrixScreen(ParticleCollisionMatrixMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth = ParticleCollisionMatrixMenu.WIDTH;
        this.imageHeight = ParticleCollisionMatrixMenu.HEIGHT;
    }

    @Override
    protected ResourceLocation texture() {
        return TEXTURE;
    }

    @Override
    protected int sheetHeight() {
        return 282;
    }

    @Override
    protected void init() {
        super.init();
        resetButtons();
        addHelpButton("nep/oritech/particle-collision-matrix.md");
        clearPending = addRightButton(
                2,
                NepIcons.REMOVE,
                "gui.nep.clear_pending",
                "gui.nep.clear_pending.hint",
                () -> sendButton(ParticleCollisionMatrixMenu.BUTTON_CLEAR_PENDING));
        addRightButton(
                3,
                NepIcons.DOWN,
                "gui.nep.clear_buffer",
                "gui.nep.clear_buffer.hint",
                () -> sendButton(ParticleCollisionMatrixMenu.BUTTON_CLEAR_BUFFER));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        ParticleCollisionMatrixBlockEntity matrix = menu.matrix();
        if (clearPending != null) {
            clearPending.active = matrix != null
                    && (matrix.hasPending() || matrix.refusal() != ParticleCollisionMatrixBlockEntity.Refusal.NONE);
        }
        tickButtons();
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(
                font, Component.translatable("gui.nep.particle_collision_matrix.title"), 8, 7, TITLE_ON_PANEL, false);
        graphics.drawString(
                font,
                Component.translatable("gui.nep.particle_collision_matrix.input"),
                ParticleCollisionMatrixMenu.INPUT_X,
                ParticleCollisionMatrixMenu.INPUT_Y - 10,
                TITLE_ON_PANEL,
                false);
        graphics.drawString(
                font,
                Component.translatable("gui.nep.particle_collision_matrix.output"),
                ParticleCollisionMatrixMenu.OUTPUT_X,
                ParticleCollisionMatrixMenu.OUTPUT_Y - 10,
                TITLE_ON_PANEL,
                false);
        graphics.drawString(
                font,
                Component.translatable("container.inventory"),
                ParticleCollisionMatrixMenu.INV_X,
                ParticleCollisionMatrixMenu.INV_Y - 10,
                TITLE_ON_PANEL,
                false);

        ParticleCollisionMatrixBlockEntity matrix = menu.matrix();
        if (matrix == null) {
            return;
        }

        graphics.drawString(font, statusLine(matrix), READOUT_X, LINE_ONE_Y, statusColour(matrix), false);

        drawAddonStrip(graphics, matrix, mouseX, mouseY);

        ItemStack making = matrix.activeResult();
        Component craftingLabel = Component.translatable("gui.nep.particle_collision_matrix.crafting");
        graphics.drawString(font, craftingLabel, READOUT_X, LINE_TWO_Y, READOUT_DIM, false);
        int craftingX = READOUT_X + font.width(craftingLabel) + 4;
        long queued = matrix.pendingJobs();
        Component queuedText = queued > 0
                ? Component.translatable("gui.nep.particle_collision_matrix.queued", queued)
                : Component.empty();
        int nameLimit = readoutRight() - craftingX - (queued > 0 ? font.width(queuedText) + 6 : 0);
        Component craftingName = making.isEmpty()
                ? Component.translatable("gui.nep.particle_collision_matrix.crafting.idle")
                : making.getHoverName();
        String craftingText = trim(craftingName, nameLimit);
        graphics.drawString(
                font, craftingText, craftingX, LINE_TWO_Y, making.isEmpty() ? READOUT_DIM : READOUT_TEXT, false);
        if (queued > 0) {
            drawTrailing(graphics, queuedText, LINE_TWO_Y, craftingX + font.width(craftingText), READOUT_DIM);
        }

        Component energyLine = making.isEmpty()
                ? Component.translatable(
                        "gui.nep.particle_collision_matrix.buffer_line",
                        ScreenFormat.format(matrix.storedEnergy()),
                        ScreenFormat.format(matrix.energyCapacity()))
                : Component.translatable(
                        "gui.nep.particle_collision_matrix.charge_line",
                        ScreenFormat.format(matrix.craftPaid()),
                        ScreenFormat.format(matrix.craftCost()));
        graphics.drawString(font, energyLine, READOUT_X, LINE_THREE_Y, READOUT_DIM, false);
        drawTrailing(
                graphics,
                matrix.craftSpeed() > 0
                        ? Component.translatable("gui.nep.particle_collision_matrix.speed", matrix.craftSpeed())
                        : Component.translatable("gui.nep.particle_collision_matrix.craft_time", matrix.craftTicks()),
                LINE_THREE_Y,
                READOUT_X + font.width(energyLine),
                READOUT_DIM);

        drawProgressBar(graphics, matrix.craftProgress(), BAR_TRACK, CRAFT_FILL);

        EnergyGauge.draw(graphics, GAUGE_X, GAUGE_Y, matrix.storedEnergy(), matrix.energyCapacity());
    }

    private static Component faultLine(
            ParticleCollisionMatrixBlockEntity.Stall stall, ParticleCollisionMatrixBlockEntity.Refusal refusal) {
        String key = stallKey(stall);
        if (key != null) {
            return Component.translatable(key);
        }
        String refused = refusalKey(refusal);
        return refused == null
                ? Component.translatable("gui.nep.particle_collision_matrix.idle")
                : Component.translatable(refused);
    }

    @Nullable
    private static String stallKey(ParticleCollisionMatrixBlockEntity.Stall stall) {
        return switch (stall) {
            case NONE -> null;
            case INGREDIENTS -> "gui.nep.particle_collision_matrix.stall.ingredients";
            case OUTPUT_FULL -> "gui.nep.particle_collision_matrix.output_blocked";
            case NO_RECIPE -> "gui.nep.particle_collision_matrix.stall.no_recipe";
        };
    }

    @Nullable
    private static String stallHintKey(ParticleCollisionMatrixBlockEntity.Stall stall) {
        if (stall == ParticleCollisionMatrixBlockEntity.Stall.OUTPUT_FULL) {
            return OUTPUT_FULL_HINT;
        }
        String key = stallKey(stall);
        return key == null ? null : key + ".hint";
    }

    @Nullable
    private static String refusalKey(ParticleCollisionMatrixBlockEntity.Refusal refusal) {
        return switch (refusal) {
            case NONE -> null;
            case NOT_A_COLLISION_PATTERN -> "gui.nep.particle_collision_matrix.refused.wrong_pattern";
            case NO_ITEM_OUTPUT -> "gui.nep.particle_collision_matrix.refused.no_item_output";
            case UNKNOWN_RECIPE -> "gui.nep.particle_collision_matrix.refused.unknown_recipe";
            case MIXED_RECIPES -> "gui.nep.particle_collision_matrix.refused.mixed_recipes";
            case ITEMS_ONLY -> "gui.nep.particle_collision_matrix.refused.items_only";
            case TOO_MANY_INPUTS -> "gui.nep.particle_collision_matrix.refused.too_many_inputs";
            case BUFFER_FULL -> "gui.nep.particle_collision_matrix.refused.buffer_full";
        };
    }

    private record Fault(boolean active, String line, String hint) {}

    private static List<Fault> powerFaults(ParticleCollisionMatrixBlockEntity matrix) {
        return List.of(
                new Fault(
                        matrix.hasChannelFault(),
                        "gui.nep.particle_collision_matrix.no_network_channels",
                        "gui.nep.particle_collision_matrix.no_network_channels.hint"),
                new Fault(
                        matrix.hasPowerFault(),
                        "gui.nep.particle_collision_matrix.no_network_power",
                        "gui.nep.particle_collision_matrix.stall.no_me_power.hint"),
                new Fault(
                        matrix.hasEnergyFault(),
                        "gui.nep.particle_collision_matrix.no_energy",
                        "gui.nep.particle_collision_matrix.stall.no_energy.hint"));
    }

    private static List<Component> statusHint(ParticleCollisionMatrixBlockEntity matrix) {
        for (Fault fault : powerFaults(matrix)) {
            if (fault.active()) {
                return fault.hint() == null
                        ? List.of()
                        : List.of(Component.translatable(fault.hint()).withStyle(ChatFormatting.GRAY));
            }
        }
        String key = stallHintKey(matrix.stall());
        if (key == null && matrix.refusal() != ParticleCollisionMatrixBlockEntity.Refusal.NONE) {
            key = "gui.nep.particle_collision_matrix.refused.hint";
        }
        if (key == null && matrix.isOutputBlocked()) {
            key = OUTPUT_FULL_HINT;
        }
        return key == null ? List.of() : List.of(Component.translatable(key).withStyle(ChatFormatting.GRAY));
    }

    private static Component statusLine(ParticleCollisionMatrixBlockEntity matrix) {
        for (Fault fault : powerFaults(matrix)) {
            if (fault.active()) {
                return Component.translatable(fault.line());
            }
        }
        if (matrix.stall() != ParticleCollisionMatrixBlockEntity.Stall.NONE
                || matrix.refusal() != ParticleCollisionMatrixBlockEntity.Refusal.NONE) {
            return faultLine(matrix.stall(), matrix.refusal());
        }
        if (matrix.isOutputBlocked()) {
            return Component.translatable("gui.nep.particle_collision_matrix.output_blocked");
        }
        if (!matrix.missingInputs().isEmpty()) {
            return Component.translatable("gui.nep.particle_collision_matrix.missing");
        }
        if (matrix.activeResult().isEmpty()) {
            return Component.translatable("gui.nep.particle_collision_matrix.idle");
        }
        return Component.translatable("gui.nep.particle_collision_matrix.crafting_now");
    }

    private static boolean faulted(ParticleCollisionMatrixBlockEntity matrix) {
        return matrix.stall() != ParticleCollisionMatrixBlockEntity.Stall.NONE
                || matrix.refusal() != ParticleCollisionMatrixBlockEntity.Refusal.NONE
                || matrix.isOutputBlocked()
                || powerFaults(matrix).stream().anyMatch(Fault::active)
                || !matrix.missingInputs().isEmpty();
    }

    private static int statusColour(ParticleCollisionMatrixBlockEntity matrix) {
        if (faulted(matrix)) {
            return READOUT_WARN;
        }
        return matrix.activeResult().isEmpty() ? READOUT_DIM : READOUT_TEXT;
    }

    private void refreshLayout(ParticleCollisionMatrixBlockEntity matrix) {
        if (matrix == layoutMatrix && matrix.addonRevision() == layoutRevision) {
            return;
        }
        layoutMatrix = matrix;
        layoutRevision = matrix.addonRevision();
        layoutEntries = List.copyOf(matrix.addonEntries());
        layoutIcons = layoutEntries.stream().map(AddonEntry::stack).toList();

        Component totals = addonBadge(matrix);
        int totalsX = totals == null ? readoutRight() : readoutRight() - font.width(totals);
        int needed = layoutEntries.size() * ICON_PITCH - (ICON_PITCH - ICON_SIZE);
        boolean totalsFit = totals != null && ICON_X + needed + 6 <= totalsX;
        layoutTotals = totalsFit ? totals : null;
        layoutTotalsX = totalsX;

        int capacity =
                totalsFit ? layoutEntries.size() : (readoutRight() - ICON_X + (ICON_PITCH - ICON_SIZE)) / ICON_PITCH;
        if (layoutEntries.size() <= capacity) {
            layoutShown = layoutEntries.size();
            layoutOverflow = 0;
        } else {
            layoutShown = Math.max(0, capacity - 1);
            layoutOverflow = layoutEntries.size() - layoutShown;
        }
    }

    private int hoveredIcon(int mouseX, int mouseY) {
        int cells = layoutShown + (layoutOverflow > 0 ? 1 : 0);
        for (int i = 0; i < cells; i++) {
            if (within(mouseX, mouseY, ICON_X + i * ICON_PITCH, ICON_Y, ICON_SIZE, ICON_SIZE)) {
                return i;
            }
        }
        return -1;
    }

    private void drawAddonStrip(
            GuiGraphics graphics, ParticleCollisionMatrixBlockEntity matrix, int mouseX, int mouseY) {
        refreshLayout(matrix);
        if (layoutEntries.isEmpty()) {
            graphics.drawString(
                    font,
                    Component.translatable("gui.nep.particle_collision_matrix.addons.none"),
                    ICON_X,
                    STRIP_TEXT_Y,
                    READOUT_DIM,
                    false);
            return;
        }
        int hovered = hoveredIcon(mouseX, mouseY);
        for (int i = 0; i < layoutShown; i++) {
            int x = ICON_X + i * ICON_PITCH;
            if (i == hovered) {
                graphics.fill(x, ICON_Y, x + ICON_SIZE, ICON_Y + ICON_SIZE, ICON_HOVER);
            }
            graphics.renderItem(layoutIcons.get(i), x, ICON_Y);
        }
        if (layoutOverflow > 0) {
            Component more = Component.translatable("gui.nep.particle_collision_matrix.addons.more", layoutOverflow);
            int x = ICON_X + layoutShown * ICON_PITCH;
            graphics.drawString(font, more, x + (ICON_SIZE - font.width(more)) / 2, STRIP_TEXT_Y, READOUT_TEXT, false);
        }
        if (layoutTotals != null) {
            graphics.drawString(font, layoutTotals, layoutTotalsX, STRIP_TEXT_Y, READOUT_TEXT, false);
        }
    }

    private static List<Component> contribution(AddonEntry entry) {
        List<Component> lines = new ArrayList<>();
        if (entry.speed() != 1.0F) {
            lines.add(Component.translatable(
                    "gui.nep.particle_collision_matrix.addon.speed", String.format("%.2f", 1.0F / entry.speed())));
        }
        if (entry.efficiency() != 1.0F) {
            lines.add(Component.translatable(
                    "gui.nep.particle_collision_matrix.addons.efficiency", Math.round(entry.efficiency() * 100.0F)));
        }
        if (entry.chambers() > 0) {
            lines.add(Component.translatable("gui.nep.particle_collision_matrix.addon.chambers", entry.chambers()));
        }
        if (entry.capacity() > 0) {
            lines.add(Component.translatable(
                    "gui.nep.particle_collision_matrix.addons.capacity", ScreenFormat.format(entry.capacity())));
        }
        if (entry.insert() > 0) {
            lines.add(Component.translatable(
                    "gui.nep.particle_collision_matrix.addon.insert", ScreenFormat.format(entry.insert())));
        }
        if (lines.isEmpty()) {
            lines.add(Component.translatable("gui.nep.particle_collision_matrix.addon.passive"));
        }
        lines.replaceAll(line -> ((MutableComponent) line).withStyle(ChatFormatting.GRAY));
        return lines;
    }

    private static List<Component> entryTooltip(AddonEntry entry) {
        List<Component> lines = new ArrayList<>();
        lines.add(entry.name());
        lines.addAll(contribution(entry));
        return lines;
    }

    private List<Component> overflowTooltip() {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("gui.nep.particle_collision_matrix.addons.more_title", layoutOverflow));
        for (AddonEntry entry : layoutEntries.subList(layoutShown, layoutEntries.size())) {
            lines.add(entry.name());
            lines.addAll(contribution(entry));
        }
        return lines;
    }

    private static Component extenderLine(ParticleCollisionMatrixBlockEntity matrix) {
        return matrix.layeredExtenders()
                ? Component.translatable(
                                "gui.nep.particle_collision_matrix.addons.extender_layers", matrix.extenderLimit() + 1)
                        .withStyle(ChatFormatting.GRAY)
                : Component.translatable("gui.nep.particle_collision_matrix.addons.extenders", matrix.extenderLimit())
                        .withStyle(ChatFormatting.GRAY);
    }

    @Nullable
    private static Component addonBadge(ParticleCollisionMatrixBlockEntity matrix) {
        if (matrix.addonCount() == 0) {
            return null;
        }
        List<Component> parts = new ArrayList<>();
        if (matrix.speedMultiplier() != 1.0F) {
            parts.add(Component.translatable("gui.nep.particle_collision_matrix.addon_speed", speedFactor(matrix)));
        }
        if (matrix.efficiencyMultiplier() != 1.0F) {
            parts.add(Component.translatable(
                    "gui.nep.particle_collision_matrix.addon_efficiency", efficiencyPercent(matrix)));
        }
        if (matrix.extraChambers() > 0) {
            parts.add(
                    Component.translatable("gui.nep.particle_collision_matrix.addon_chambers", matrix.extraChambers()));
        }
        if (parts.isEmpty()) {
            return Component.translatable("gui.nep.particle_collision_matrix.addon_count", matrix.addonCount());
        }
        MutableComponent badge = Component.empty();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) {
                badge.append(" ");
            }
            badge.append(parts.get(i));
        }
        return badge;
    }

    private static String speedFactor(ParticleCollisionMatrixBlockEntity matrix) {
        return String.format("%.2f", 1.0F / matrix.speedMultiplier());
    }

    private static String efficiencyPercent(ParticleCollisionMatrixBlockEntity matrix) {
        return Long.toString(Math.round(matrix.efficiencyMultiplier() * 100.0F));
    }

    private static List<Component> addonTooltip(ParticleCollisionMatrixBlockEntity matrix) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("gui.nep.particle_collision_matrix.addons", matrix.addonCount()));
        lines.add(extenderLine(matrix));
        if (matrix.speedMultiplier() != 1.0F) {
            lines.add(Component.translatable(
                            "gui.nep.particle_collision_matrix.addons.speed", speedFactor(matrix), matrix.craftTicks())
                    .withStyle(ChatFormatting.GRAY));
        }
        if (matrix.efficiencyMultiplier() != 1.0F) {
            lines.add(Component.translatable(
                            "gui.nep.particle_collision_matrix.addons.efficiency", efficiencyPercent(matrix))
                    .withStyle(ChatFormatting.GRAY));
        }
        if (matrix.extraChambers() > 0) {
            lines.add(Component.translatable(
                            "gui.nep.particle_collision_matrix.addons.chambers", matrix.extraChambers() + 1)
                    .withStyle(ChatFormatting.GRAY));
        }
        if (matrix.addonCapacityBonus() > 0) {
            lines.add(Component.translatable(
                            "gui.nep.particle_collision_matrix.addons.capacity",
                            ScreenFormat.format(matrix.addonCapacityBonus()))
                    .withStyle(ChatFormatting.GRAY));
        }
        return lines;
    }

    @Override
    protected void renderReadoutTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        ParticleCollisionMatrixBlockEntity matrix = menu.matrix();
        if (matrix == null) {
            return;
        }
        if (within(mouseX, mouseY, GAUGE_X, GAUGE_Y, EnergyGauge.WIDTH, EnergyGauge.HEIGHT)) {
            graphics.renderComponentTooltip(
                    font,
                    List.of(
                            Component.translatable("gui.nep.particle_collision_matrix.energy"),
                            Component.translatable(
                                            "gui.nep.particle_collision_matrix.charge",
                                            ScreenFormat.format(matrix.storedEnergy()),
                                            ScreenFormat.format(matrix.energyCapacity()))
                                    .withStyle(ChatFormatting.GRAY)),
                    mouseX,
                    mouseY);
            return;
        }
        if (within(
                        mouseX,
                        mouseY,
                        ParticleCollisionMatrixMenu.CORE_X,
                        ParticleCollisionMatrixMenu.CORE_Y,
                        ICON_SIZE,
                        ICON_SIZE)
                && matrix.coreSlot().getStackInSlot(0).isEmpty()
                && menu.getCarried().isEmpty()) {
            graphics.renderComponentTooltip(
                    font,
                    List.of(
                            Component.translatable("gui.nep.particle_collision_matrix.core"),
                            Component.translatable("gui.nep.particle_collision_matrix.core.hint")
                                    .withStyle(ChatFormatting.GRAY)),
                    mouseX,
                    mouseY);
            return;
        }
        if (within(mouseX, mouseY, STRIP_LEFT, STRIP_TOP, STRIP_RIGHT - STRIP_LEFT, STRIP_HEIGHT)) {
            refreshLayout(matrix);
            int hovered = hoveredIcon(mouseX, mouseY);
            List<Component> lines = hovered < 0
                    ? addonTooltip(matrix)
                    : hovered < layoutShown ? entryTooltip(layoutEntries.get(hovered)) : overflowTooltip();
            graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
            return;
        }
        if (!within(mouseX, mouseY, READOUT_X, LINE_ONE_Y, barWidth(), READOUT_HEIGHT)) {
            return;
        }
        List<Component> lines = new ArrayList<>();
        lines.add(statusLine(matrix));
        lines.addAll(statusHint(matrix));
        ItemStack making = matrix.activeResult();
        if (!making.isEmpty()) {
            lines.add(
                    Component.translatable("gui.nep.particle_collision_matrix.crafting_tooltip", making.getHoverName())
                            .withStyle(ChatFormatting.GRAY));
        }
        long queued = matrix.pendingJobs();
        if (queued > 0) {
            lines.add(Component.translatable("gui.nep.particle_collision_matrix.queued_tooltip", queued)
                    .withStyle(ChatFormatting.GRAY));
        }
        List<GenericStack> missing = matrix.missingInputs();
        if (!missing.isEmpty()) {
            lines.add(Component.translatable("gui.nep.particle_collision_matrix.missing"));
            for (GenericStack stack : missing) {
                lines.add(MissingStacks.describe(stack).withStyle(ChatFormatting.GRAY));
            }
        }
        graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
    }
}
