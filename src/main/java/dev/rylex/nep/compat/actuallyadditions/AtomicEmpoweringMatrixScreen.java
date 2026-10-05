package dev.rylex.nep.compat.actuallyadditions;

import appeng.api.stacks.GenericStack;
import dev.rylex.nep.Nep;
import dev.rylex.nep.NepIcons;
import dev.rylex.nep.client.EnergyGauge;
import dev.rylex.nep.client.MatrixScreen;
import dev.rylex.nep.client.ReadoutButton;
import dev.rylex.nep.util.MissingStacks;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class AtomicEmpoweringMatrixScreen extends MatrixScreen<AtomicEmpoweringMatrixMenu> {

    private static final ResourceLocation TEXTURE = Nep.id("textures/gui/atomic_empowering_matrix.png");

    private static final int TITLE_ON_PANEL = 0x3A2E1C;
    private static final int READOUT_TEXT = 0xF2E8D0;
    private static final int READOUT_DIM = 0xC8A882;

    private static final int BAR_TRACK = 0x2A0D45;
    private static final int CRAFT_FILL = 0xB36BFF;

    private static final int GAUGE_X = 152;
    private static final int GAUGE_Y = 73;

    @Nullable
    private ReadoutButton clearPending;

    public AtomicEmpoweringMatrixScreen(AtomicEmpoweringMatrixMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth = AtomicEmpoweringMatrixMenu.WIDTH;
        this.imageHeight = AtomicEmpoweringMatrixMenu.HEIGHT;
    }

    @Override
    protected ResourceLocation texture() {
        return TEXTURE;
    }

    @Override
    protected void init() {
        super.init();
        resetButtons();
        addHelpButton("nep/actuallyadditions/atomic-empowering-matrix.md");
        clearPending = addRightButton(
                2,
                NepIcons.REMOVE,
                "gui.nep.clear_pending",
                "gui.nep.clear_pending.hint",
                () -> sendButton(AtomicEmpoweringMatrixMenu.BUTTON_CLEAR_PENDING));
        addRightButton(
                3,
                NepIcons.DOWN,
                "gui.nep.clear_buffer",
                "gui.nep.clear_buffer.hint",
                () -> sendButton(AtomicEmpoweringMatrixMenu.BUTTON_CLEAR_BUFFER));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        AtomicEmpoweringMatrixBlockEntity matrix = menu.matrix();
        if (clearPending != null) {
            clearPending.active = matrix != null
                    && (matrix.hasPending() || matrix.refusal() != AtomicEmpoweringMatrixBlockEntity.Refusal.NONE);
        }
        tickButtons();
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(
                font, Component.translatable("gui.nep.atomic_empowering_matrix.title"), 8, 7, TITLE_ON_PANEL, false);
        graphics.drawString(
                font,
                Component.translatable("gui.nep.atomic_empowering_matrix.input"),
                AtomicEmpoweringMatrixMenu.INPUT_X,
                AtomicEmpoweringMatrixMenu.INPUT_Y - 10,
                TITLE_ON_PANEL,
                false);
        graphics.drawString(
                font,
                Component.translatable("gui.nep.atomic_empowering_matrix.output"),
                AtomicEmpoweringMatrixMenu.OUTPUT_X,
                AtomicEmpoweringMatrixMenu.OUTPUT_Y - 10,
                TITLE_ON_PANEL,
                false);
        graphics.drawString(
                font,
                Component.translatable("container.inventory"),
                AtomicEmpoweringMatrixMenu.INV_X,
                AtomicEmpoweringMatrixMenu.INV_Y - 10,
                TITLE_ON_PANEL,
                false);

        AtomicEmpoweringMatrixBlockEntity matrix = menu.matrix();
        if (matrix == null) {
            return;
        }

        graphics.drawString(font, statusLine(matrix), READOUT_X, LINE_ONE_Y, statusColour(matrix), false);

        ItemStack making = matrix.activeResult();
        Component craftingLabel = Component.translatable("gui.nep.atomic_empowering_matrix.crafting");
        graphics.drawString(font, craftingLabel, READOUT_X, LINE_TWO_Y, READOUT_DIM, false);
        int craftingX = READOUT_X + font.width(craftingLabel) + 4;
        long queued = matrix.pendingJobs();
        Component queuedText = queued > 0
                ? Component.translatable("gui.nep.atomic_empowering_matrix.queued", queued)
                : Component.empty();
        int nameLimit = readoutRight() - craftingX - (queued > 0 ? font.width(queuedText) + 6 : 0);
        Component craftingName = making.isEmpty()
                ? Component.translatable("gui.nep.atomic_empowering_matrix.crafting.idle")
                : making.getHoverName();
        String craftingText = trim(craftingName, nameLimit);
        graphics.drawString(
                font, craftingText, craftingX, LINE_TWO_Y, making.isEmpty() ? READOUT_DIM : READOUT_TEXT, false);
        if (queued > 0) {
            drawTrailing(graphics, queuedText, LINE_TWO_Y, craftingX + font.width(craftingText), READOUT_DIM);
        }

        Component energyLine = making.isEmpty()
                ? Component.translatable(
                        "gui.nep.atomic_empowering_matrix.buffer_line",
                        format(matrix.storedEnergy()),
                        format(matrix.energyCapacity()))
                : Component.translatable(
                        "gui.nep.atomic_empowering_matrix.charge_line",
                        format(matrix.craftPaid()),
                        format(matrix.craftCost()));
        graphics.drawString(font, energyLine, READOUT_X, LINE_THREE_Y, READOUT_DIM, false);
        drawTrailing(
                graphics,
                Component.translatable("gui.nep.atomic_empowering_matrix.craft_time", matrix.craftTicks()),
                LINE_THREE_Y,
                READOUT_X + font.width(energyLine),
                READOUT_DIM);

        drawProgressBar(graphics, matrix.craftProgress(), BAR_TRACK, CRAFT_FILL);

        EnergyGauge.draw(graphics, GAUGE_X, GAUGE_Y, matrix.storedEnergy(), matrix.energyCapacity());
    }

    private static Component faultLine(
            AtomicEmpoweringMatrixBlockEntity.Stall stall, AtomicEmpoweringMatrixBlockEntity.Refusal refusal) {
        String key =
                switch (stall) {
                    case NONE -> null;
                    case INGREDIENTS -> "gui.nep.atomic_empowering_matrix.stall.ingredients";
                    case OUTPUT_FULL -> "gui.nep.atomic_empowering_matrix.output_blocked";
                    case NO_RECIPE -> "gui.nep.atomic_empowering_matrix.stall.no_recipe";
                };
        if (key != null) {
            return Component.translatable(key);
        }
        String refused =
                switch (refusal) {
                    case NONE -> null;
                    case NOT_AN_ACTUALLY_ADDITIONS_PATTERN -> "gui.nep.atomic_empowering_matrix.refused.wrong_pattern";
                    case NO_ITEM_OUTPUT -> "gui.nep.atomic_empowering_matrix.refused.no_item_output";
                    case UNKNOWN_RECIPE -> "gui.nep.atomic_empowering_matrix.refused.unknown_recipe";
                    case MIXED_RECIPES -> "gui.nep.atomic_empowering_matrix.refused.mixed_recipes";
                    case ITEMS_ONLY -> "gui.nep.atomic_empowering_matrix.refused.items_only";
                    case TOO_MANY_INPUTS -> "gui.nep.atomic_empowering_matrix.refused.too_many_inputs";
                    case BUFFER_FULL -> "gui.nep.atomic_empowering_matrix.refused.buffer_full";
                };
        return refused == null
                ? Component.translatable("gui.nep.atomic_empowering_matrix.idle")
                : Component.translatable(refused);
    }

    private static List<Component> statusHint(AtomicEmpoweringMatrixBlockEntity matrix) {
        if (matrix.hasPowerFault()) {
            return List.of(Component.translatable("gui.nep.atomic_empowering_matrix.stall.no_me_power.hint")
                    .withStyle(ChatFormatting.GRAY));
        }
        if (matrix.hasEnergyFault()) {
            return List.of(Component.translatable("gui.nep.atomic_empowering_matrix.stall.no_energy.hint")
                    .withStyle(ChatFormatting.GRAY));
        }
        String key =
                switch (matrix.stall()) {
                    case NONE -> null;
                    case INGREDIENTS -> "gui.nep.atomic_empowering_matrix.stall.ingredients.hint";
                    case OUTPUT_FULL -> "gui.nep.atomic_empowering_matrix.stall.output_full.hint";
                    case NO_RECIPE -> "gui.nep.atomic_empowering_matrix.stall.no_recipe.hint";
                };
        if (key == null && matrix.refusal() != AtomicEmpoweringMatrixBlockEntity.Refusal.NONE) {
            key = "gui.nep.atomic_empowering_matrix.refused.hint";
        }
        if (key == null && matrix.isOutputBlocked()) {
            key = "gui.nep.atomic_empowering_matrix.stall.output_full.hint";
        }
        return key == null ? List.of() : List.of(Component.translatable(key).withStyle(ChatFormatting.GRAY));
    }

    private static Component statusLine(AtomicEmpoweringMatrixBlockEntity matrix) {
        if (matrix.hasChannelFault()) {
            return Component.translatable("gui.nep.atomic_empowering_matrix.no_network_channels");
        }
        if (matrix.hasPowerFault()) {
            return Component.translatable("gui.nep.atomic_empowering_matrix.no_network_power");
        }
        if (matrix.hasEnergyFault()) {
            return Component.translatable("gui.nep.atomic_empowering_matrix.no_energy");
        }
        if (matrix.stall() != AtomicEmpoweringMatrixBlockEntity.Stall.NONE
                || matrix.refusal() != AtomicEmpoweringMatrixBlockEntity.Refusal.NONE) {
            return faultLine(matrix.stall(), matrix.refusal());
        }
        if (matrix.isOutputBlocked()) {
            return Component.translatable("gui.nep.atomic_empowering_matrix.output_blocked");
        }
        if (!matrix.missingInputs().isEmpty()) {
            return Component.translatable("gui.nep.atomic_empowering_matrix.missing");
        }
        if (matrix.activeResult().isEmpty()) {
            return Component.translatable("gui.nep.atomic_empowering_matrix.idle");
        }
        return Component.translatable("gui.nep.atomic_empowering_matrix.crafting_now");
    }

    private static boolean faulted(AtomicEmpoweringMatrixBlockEntity matrix) {
        return matrix.stall() != AtomicEmpoweringMatrixBlockEntity.Stall.NONE
                || matrix.refusal() != AtomicEmpoweringMatrixBlockEntity.Refusal.NONE
                || matrix.isOutputBlocked()
                || matrix.hasPowerFault()
                || matrix.hasEnergyFault()
                || !matrix.missingInputs().isEmpty();
    }

    private static int statusColour(AtomicEmpoweringMatrixBlockEntity matrix) {
        if (faulted(matrix)) {
            return READOUT_WARN;
        }
        return matrix.activeResult().isEmpty() ? READOUT_DIM : READOUT_TEXT;
    }

    private static String format(long value) {
        if (value >= 1_000_000_000L) {
            return String.format("%.1fG", value / 1_000_000_000.0);
        }
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
        AtomicEmpoweringMatrixBlockEntity matrix = menu.matrix();
        if (matrix == null) {
            return;
        }
        if (within(mouseX, mouseY, GAUGE_X, GAUGE_Y, EnergyGauge.WIDTH, EnergyGauge.HEIGHT)) {
            graphics.renderComponentTooltip(
                    font,
                    List.of(
                            Component.translatable("gui.nep.atomic_empowering_matrix.energy"),
                            Component.translatable(
                                            "gui.nep.atomic_empowering_matrix.charge",
                                            format(matrix.storedEnergy()),
                                            format(matrix.energyCapacity()))
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
            lines.add(Component.translatable("gui.nep.atomic_empowering_matrix.crafting_tooltip", making.getHoverName())
                    .withStyle(ChatFormatting.GRAY));
        }
        long queued = matrix.pendingJobs();
        if (queued > 0) {
            lines.add(Component.translatable("gui.nep.atomic_empowering_matrix.queued_tooltip", queued)
                    .withStyle(ChatFormatting.GRAY));
        }
        List<GenericStack> missing = matrix.missingInputs();
        if (!missing.isEmpty()) {
            lines.add(Component.translatable("gui.nep.atomic_empowering_matrix.missing"));
            for (GenericStack stack : missing) {
                lines.add(MissingStacks.describe(stack).withStyle(ChatFormatting.GRAY));
            }
        }
        graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
    }
}
