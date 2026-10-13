package dev.rylex.nep.compat.oritech;

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

public class ParticleCollisionMatrixScreen extends MatrixScreen<ParticleCollisionMatrixMenu> {

    private static final ResourceLocation TEXTURE = Nep.id("textures/gui/particle_collision_matrix.png");

    private static final int TITLE_ON_PANEL = 0x1C3A2E;
    private static final int READOUT_TEXT = 0xD0F2E8;
    private static final int READOUT_DIM = 0x82C8A8;

    private static final int BAR_TRACK = 0x0D3A2E;
    private static final int CRAFT_FILL = 0x3FE0B8;

    private static final int GAUGE_X = 152;
    private static final int GAUGE_Y = 73;

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
                        format(matrix.storedEnergy()),
                        format(matrix.energyCapacity()))
                : Component.translatable(
                        "gui.nep.particle_collision_matrix.charge_line",
                        format(matrix.craftPaid()),
                        format(matrix.craftCost()));
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
        String key =
                switch (stall) {
                    case NONE -> null;
                    case INGREDIENTS -> "gui.nep.particle_collision_matrix.stall.ingredients";
                    case OUTPUT_FULL -> "gui.nep.particle_collision_matrix.output_blocked";
                    case NO_RECIPE -> "gui.nep.particle_collision_matrix.stall.no_recipe";
                };
        if (key != null) {
            return Component.translatable(key);
        }
        String refused =
                switch (refusal) {
                    case NONE -> null;
                    case NOT_A_COLLISION_PATTERN -> "gui.nep.particle_collision_matrix.refused.wrong_pattern";
                    case NO_ITEM_OUTPUT -> "gui.nep.particle_collision_matrix.refused.no_item_output";
                    case UNKNOWN_RECIPE -> "gui.nep.particle_collision_matrix.refused.unknown_recipe";
                    case MIXED_RECIPES -> "gui.nep.particle_collision_matrix.refused.mixed_recipes";
                    case ITEMS_ONLY -> "gui.nep.particle_collision_matrix.refused.items_only";
                    case TOO_MANY_INPUTS -> "gui.nep.particle_collision_matrix.refused.too_many_inputs";
                    case BUFFER_FULL -> "gui.nep.particle_collision_matrix.refused.buffer_full";
                };
        return refused == null
                ? Component.translatable("gui.nep.particle_collision_matrix.idle")
                : Component.translatable(refused);
    }

    private static List<Component> statusHint(ParticleCollisionMatrixBlockEntity matrix) {
        if (matrix.hasPowerFault()) {
            return List.of(Component.translatable("gui.nep.particle_collision_matrix.stall.no_me_power.hint")
                    .withStyle(ChatFormatting.GRAY));
        }
        if (matrix.hasEnergyFault()) {
            return List.of(Component.translatable("gui.nep.particle_collision_matrix.stall.no_energy.hint")
                    .withStyle(ChatFormatting.GRAY));
        }
        String key =
                switch (matrix.stall()) {
                    case NONE -> null;
                    case INGREDIENTS -> "gui.nep.particle_collision_matrix.stall.ingredients.hint";
                    case OUTPUT_FULL -> "gui.nep.particle_collision_matrix.stall.output_full.hint";
                    case NO_RECIPE -> "gui.nep.particle_collision_matrix.stall.no_recipe.hint";
                };
        if (key == null && matrix.refusal() != ParticleCollisionMatrixBlockEntity.Refusal.NONE) {
            key = "gui.nep.particle_collision_matrix.refused.hint";
        }
        if (key == null && matrix.isOutputBlocked()) {
            key = "gui.nep.particle_collision_matrix.stall.output_full.hint";
        }
        return key == null ? List.of() : List.of(Component.translatable(key).withStyle(ChatFormatting.GRAY));
    }

    private static Component statusLine(ParticleCollisionMatrixBlockEntity matrix) {
        if (matrix.hasChannelFault()) {
            return Component.translatable("gui.nep.particle_collision_matrix.no_network_channels");
        }
        if (matrix.hasPowerFault()) {
            return Component.translatable("gui.nep.particle_collision_matrix.no_network_power");
        }
        if (matrix.hasEnergyFault()) {
            return Component.translatable("gui.nep.particle_collision_matrix.no_energy");
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
                || matrix.hasPowerFault()
                || matrix.hasEnergyFault()
                || !matrix.missingInputs().isEmpty();
    }

    private static int statusColour(ParticleCollisionMatrixBlockEntity matrix) {
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
