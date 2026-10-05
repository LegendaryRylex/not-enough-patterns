package dev.rylex.nep.compat.compactcrafting;

import appeng.api.stacks.GenericStack;
import dev.rylex.nep.Nep;
import dev.rylex.nep.NepIcons;
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

public class MiniaturizationMatrixScreen extends MatrixScreen<MiniaturizationMatrixMenu> {

    private static final ResourceLocation TEXTURE = Nep.id("textures/gui/miniaturization_matrix.png");

    private static final int TITLE_ON_PANEL = 0x3A2E1C;
    private static final int READOUT_TEXT = 0xF2E8D0;
    private static final int READOUT_DIM = 0xC8A882;

    private static final int BAR_TRACK = 0x0D333D;
    private static final int CRAFT_FILL = 0x208BFF;

    @Nullable
    private ReadoutButton clearPending;

    public MiniaturizationMatrixScreen(MiniaturizationMatrixMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth = MiniaturizationMatrixMenu.WIDTH;
        this.imageHeight = MiniaturizationMatrixMenu.HEIGHT;
    }

    @Override
    protected ResourceLocation texture() {
        return TEXTURE;
    }

    @Override
    protected void init() {
        super.init();
        resetButtons();
        addHelpButton("nep/compactcrafting/miniaturization-matrix.md");
        clearPending = addRightButton(
                2,
                NepIcons.REMOVE,
                "gui.nep.clear_pending",
                "gui.nep.clear_pending.hint",
                () -> sendButton(MiniaturizationMatrixMenu.BUTTON_CLEAR_PENDING));
        addRightButton(
                3,
                NepIcons.DOWN,
                "gui.nep.clear_buffer",
                "gui.nep.clear_buffer.hint",
                () -> sendButton(MiniaturizationMatrixMenu.BUTTON_CLEAR_BUFFER));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        MiniaturizationMatrixBlockEntity matrix = menu.matrix();
        if (clearPending != null) {
            clearPending.active = matrix != null
                    && (matrix.hasPending() || matrix.refusal() != MiniaturizationMatrixBlockEntity.Refusal.NONE);
        }
        tickButtons();
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(
                font, Component.translatable("gui.nep.miniaturization_matrix.title"), 8, 7, TITLE_ON_PANEL, false);
        graphics.drawString(
                font,
                Component.translatable("gui.nep.miniaturization_matrix.input"),
                MiniaturizationMatrixMenu.INPUT_X,
                MiniaturizationMatrixMenu.INPUT_Y - 10,
                TITLE_ON_PANEL,
                false);
        graphics.drawString(
                font,
                Component.translatable("gui.nep.miniaturization_matrix.output"),
                MiniaturizationMatrixMenu.OUTPUT_X,
                MiniaturizationMatrixMenu.OUTPUT_Y - 10,
                TITLE_ON_PANEL,
                false);
        graphics.drawString(
                font,
                Component.translatable("container.inventory"),
                MiniaturizationMatrixMenu.INV_X,
                MiniaturizationMatrixMenu.INV_Y - 10,
                TITLE_ON_PANEL,
                false);

        MiniaturizationMatrixBlockEntity matrix = menu.matrix();
        if (matrix == null) {
            return;
        }

        Component status = statusLine(matrix);
        graphics.drawString(font, status, READOUT_X, LINE_ONE_Y, statusColour(matrix), false);
        drawTrailing(
                graphics,
                Component.translatable(
                        "gui.nep.miniaturization_matrix.field",
                        MiniaturizationMatrixBlockEntity.maximumFieldSize().getDimensions()),
                LINE_ONE_Y,
                READOUT_X + font.width(status),
                READOUT_DIM);

        ItemStack making = matrix.activeResult();
        Component craftingLabel = Component.translatable("gui.nep.miniaturization_matrix.crafting");
        graphics.drawString(font, craftingLabel, READOUT_X, LINE_TWO_Y, READOUT_DIM, false);
        int craftingX = READOUT_X + font.width(craftingLabel) + 4;
        long queued = matrix.pendingJobs();
        Component queuedText = queued > 0
                ? Component.translatable("gui.nep.miniaturization_matrix.queued", queued)
                : Component.empty();
        int nameLimit = readoutRight() - craftingX - (queued > 0 ? font.width(queuedText) + 6 : 0);
        Component craftingName = making.isEmpty()
                ? Component.translatable("gui.nep.miniaturization_matrix.crafting.idle")
                : making.getHoverName();
        String craftingText = trim(craftingName, nameLimit);
        graphics.drawString(
                font, craftingText, craftingX, LINE_TWO_Y, making.isEmpty() ? READOUT_DIM : READOUT_TEXT, false);
        if (queued > 0) {
            drawTrailing(graphics, queuedText, LINE_TWO_Y, craftingX + font.width(craftingText), READOUT_DIM);
        }

        if (matrix.craftTicks() > 0) {
            graphics.drawString(
                    font,
                    Component.translatable("gui.nep.miniaturization_matrix.craft_time", matrix.craftTicks()),
                    READOUT_X,
                    LINE_THREE_Y,
                    READOUT_DIM,
                    false);
        }

        drawProgressBar(graphics, matrix.craftProgress(), BAR_TRACK, CRAFT_FILL);
    }

    private static Component faultLine(
            MiniaturizationMatrixBlockEntity.Stall stall, MiniaturizationMatrixBlockEntity.Refusal refusal) {
        String key =
                switch (stall) {
                    case NONE -> null;
                    case INGREDIENTS -> "gui.nep.miniaturization_matrix.stall.ingredients";
                    case OUTPUT_FULL -> "gui.nep.miniaturization_matrix.output_blocked";
                    case FIELD_TOO_LARGE -> "gui.nep.miniaturization_matrix.stall.field_too_large";
                    case NO_RECIPE -> "gui.nep.miniaturization_matrix.stall.no_recipe";
                };
        if (key != null) {
            return Component.translatable(key);
        }
        String refused =
                switch (refusal) {
                    case NONE -> null;
                    case NOT_A_MINIATURIZATION_PATTERN -> "gui.nep.miniaturization_matrix.refused.not_a_pattern";
                    case NO_ITEM_OUTPUT -> "gui.nep.miniaturization_matrix.refused.no_item_output";
                    case UNKNOWN_RECIPE -> "gui.nep.miniaturization_matrix.refused.unknown_recipe";
                    case MIXED_RECIPES -> "gui.nep.miniaturization_matrix.refused.mixed_recipes";
                    case FIELD_TOO_LARGE -> "gui.nep.miniaturization_matrix.refused.field_too_large";
                    case ITEMS_ONLY -> "gui.nep.miniaturization_matrix.refused.items_only";
                    case TOO_MANY_INPUTS -> "gui.nep.miniaturization_matrix.refused.too_many_inputs";
                    case BUFFER_FULL -> "gui.nep.miniaturization_matrix.refused.buffer_full";
                };
        return refused == null
                ? Component.translatable("gui.nep.miniaturization_matrix.idle")
                : Component.translatable(refused);
    }

    private static List<Component> statusHint(MiniaturizationMatrixBlockEntity matrix) {
        if (matrix.hasPowerFault()) {
            return List.of(Component.translatable("gui.nep.miniaturization_matrix.stall.no_me_power.hint")
                    .withStyle(ChatFormatting.GRAY));
        }
        String key =
                switch (matrix.stall()) {
                    case NONE -> null;
                    case INGREDIENTS -> "gui.nep.miniaturization_matrix.stall.ingredients.hint";
                    case OUTPUT_FULL -> "gui.nep.miniaturization_matrix.stall.output_full.hint";
                    case FIELD_TOO_LARGE -> "gui.nep.miniaturization_matrix.stall.field_too_large.hint";
                    case NO_RECIPE -> "gui.nep.miniaturization_matrix.stall.no_recipe.hint";
                };
        if (key == null && matrix.refusal() != MiniaturizationMatrixBlockEntity.Refusal.NONE) {
            key = refusalHint(matrix.refusal());
        }
        if (key == null && matrix.isOutputBlocked()) {
            key = "gui.nep.miniaturization_matrix.stall.output_full.hint";
        }
        return key == null ? List.of() : List.of(Component.translatable(key).withStyle(ChatFormatting.GRAY));
    }

    private static String refusalHint(MiniaturizationMatrixBlockEntity.Refusal refusal) {
        return switch (refusal) {
            case FIELD_TOO_LARGE -> "gui.nep.miniaturization_matrix.refused.field_too_large.hint";
            case BUFFER_FULL -> "gui.nep.miniaturization_matrix.refused.buffer_full.hint";
            case TOO_MANY_INPUTS -> "gui.nep.miniaturization_matrix.refused.too_many_inputs.hint";
            default -> "gui.nep.miniaturization_matrix.refused.hint";
        };
    }

    private static Component statusLine(MiniaturizationMatrixBlockEntity matrix) {
        if (matrix.hasChannelFault()) {
            return Component.translatable("gui.nep.miniaturization_matrix.no_network_channels");
        }
        if (matrix.hasPowerFault()) {
            return Component.translatable("gui.nep.miniaturization_matrix.no_network_power");
        }
        if (matrix.stall() != MiniaturizationMatrixBlockEntity.Stall.NONE
                || matrix.refusal() != MiniaturizationMatrixBlockEntity.Refusal.NONE) {
            return faultLine(matrix.stall(), matrix.refusal());
        }
        if (matrix.isOutputBlocked()) {
            return Component.translatable("gui.nep.miniaturization_matrix.output_blocked");
        }
        if (!matrix.missingInputs().isEmpty()) {
            return Component.translatable("gui.nep.miniaturization_matrix.missing");
        }
        if (matrix.activeResult().isEmpty()) {
            return Component.translatable("gui.nep.miniaturization_matrix.idle");
        }
        return Component.translatable("gui.nep.miniaturization_matrix.crafting_now");
    }

    private static boolean faulted(MiniaturizationMatrixBlockEntity matrix) {
        return matrix.stall() != MiniaturizationMatrixBlockEntity.Stall.NONE
                || matrix.refusal() != MiniaturizationMatrixBlockEntity.Refusal.NONE
                || matrix.isOutputBlocked()
                || matrix.hasPowerFault()
                || !matrix.missingInputs().isEmpty();
    }

    private static int statusColour(MiniaturizationMatrixBlockEntity matrix) {
        if (faulted(matrix)) {
            return READOUT_WARN;
        }
        return matrix.activeResult().isEmpty() ? READOUT_DIM : READOUT_TEXT;
    }

    @Override
    protected void renderReadoutTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        MiniaturizationMatrixBlockEntity matrix = menu.matrix();
        if (matrix == null || !within(mouseX, mouseY, READOUT_X, LINE_ONE_Y, barWidth(), 32)) {
            return;
        }
        List<Component> lines = new ArrayList<>();
        lines.add(statusLine(matrix));
        lines.addAll(statusHint(matrix));
        ItemStack making = matrix.activeResult();
        if (!making.isEmpty()) {
            lines.add(Component.translatable("gui.nep.miniaturization_matrix.crafting_tooltip", making.getHoverName())
                    .withStyle(ChatFormatting.GRAY));
        }
        long queued = matrix.pendingJobs();
        if (queued > 0) {
            lines.add(Component.translatable("gui.nep.miniaturization_matrix.queued_tooltip", queued)
                    .withStyle(ChatFormatting.GRAY));
        }
        List<GenericStack> missing = matrix.missingInputs();
        if (!missing.isEmpty()) {
            lines.add(Component.translatable("gui.nep.miniaturization_matrix.missing"));
            for (GenericStack stack : missing) {
                lines.add(MissingStacks.describe(stack).withStyle(ChatFormatting.GRAY));
            }
        }
        graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
    }
}
