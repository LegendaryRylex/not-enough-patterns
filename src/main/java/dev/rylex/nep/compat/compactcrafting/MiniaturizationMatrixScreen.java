package dev.rylex.nep.compat.compactcrafting;

import dev.rylex.nep.Nep;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.Nullable;

public class MiniaturizationMatrixScreen extends MiniaturizationScreen<MiniaturizationMatrixMenu> {

    private static final ResourceLocation TEXTURE = Nep.id("textures/gui/miniaturization_matrix.png");

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
        addClearButtons(
                "nep/compactcrafting/miniaturization-matrix.md",
                MiniaturizationMatrixMenu.BUTTON_CLEAR_PENDING,
                MiniaturizationMatrixMenu.BUTTON_CLEAR_BUFFER);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        MiniaturizationMatrixBlockEntity matrix = menu.matrix();
        tickClearButtons(matrix != null
                && (matrix.hasPending() || matrix.refusal() != MiniaturizationMatrixBlockEntity.Refusal.NONE));
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        drawTitles(
                graphics,
                "gui.nep.miniaturization_matrix",
                MiniaturizationMatrixMenu.INPUT_X,
                MiniaturizationMatrixMenu.INPUT_Y,
                MiniaturizationMatrixMenu.OUTPUT_X,
                MiniaturizationMatrixMenu.OUTPUT_Y,
                MiniaturizationMatrixMenu.INV_X,
                MiniaturizationMatrixMenu.INV_Y);

        MiniaturizationMatrixBlockEntity matrix = menu.matrix();
        if (matrix == null) {
            return;
        }

        drawReadout(
                graphics,
                "gui.nep.miniaturization_matrix",
                statusLine(matrix),
                statusColour(matrix),
                Component.translatable(
                        "gui.nep.miniaturization_matrix.field",
                        MiniaturizationMatrixBlockEntity.maximumFieldSize().getDimensions()),
                matrix.activeResult(),
                matrix.pendingJobs(),
                matrix.craftTicks(),
                matrix.craftProgress());
    }

    private static Component faultLine(
            MiniaturizationMatrixBlockEntity.Stall stall, MiniaturizationMatrixBlockEntity.Refusal refusal) {
        String key = stallKey(stall);
        if (key != null) {
            return Component.translatable(key);
        }
        String refused = refusalKey(refusal);
        return refused == null
                ? Component.translatable("gui.nep.miniaturization_matrix.idle")
                : Component.translatable(refused);
    }

    @Nullable
    private static String stallKey(MiniaturizationMatrixBlockEntity.Stall stall) {
        return switch (stall) {
            case NONE -> null;
            case INGREDIENTS -> "gui.nep.miniaturization_matrix.stall.ingredients";
            case OUTPUT_FULL -> "gui.nep.miniaturization_matrix.output_blocked";
            case FIELD_TOO_LARGE -> "gui.nep.miniaturization_matrix.stall.field_too_large";
            case NO_RECIPE -> "gui.nep.miniaturization_matrix.stall.no_recipe";
        };
    }

    @Nullable
    private static String stallHintKey(MiniaturizationMatrixBlockEntity.Stall stall) {
        return stall == MiniaturizationMatrixBlockEntity.Stall.OUTPUT_FULL
                ? "gui.nep.miniaturization_matrix.stall.output_full.hint"
                : stallKey(stall) == null ? null : stallKey(stall) + ".hint";
    }

    @Nullable
    private static String refusalKey(MiniaturizationMatrixBlockEntity.Refusal refusal) {
        return switch (refusal) {
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
    }

    private static List<Component> statusHint(MiniaturizationMatrixBlockEntity matrix) {
        if (matrix.hasChannelFault()) {
            return List.of(Component.translatable("gui.nep.miniaturization_matrix.no_network_channels.hint")
                    .withStyle(ChatFormatting.GRAY));
        }
        if (matrix.hasPowerFault()) {
            return List.of(Component.translatable("gui.nep.miniaturization_matrix.stall.no_me_power.hint")
                    .withStyle(ChatFormatting.GRAY));
        }
        String key = stallHintKey(matrix.stall());
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
            case FIELD_TOO_LARGE, BUFFER_FULL, TOO_MANY_INPUTS -> refusalKey(refusal) + ".hint";
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
                || matrix.hasChannelFault()
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
        if (matrix == null || !overReadout(mouseX, mouseY)) {
            return;
        }
        drawReadoutTooltip(
                graphics,
                mouseX,
                mouseY,
                "gui.nep.miniaturization_matrix",
                statusLine(matrix),
                statusHint(matrix),
                matrix.activeResult(),
                matrix.pendingJobs(),
                matrix.missingInputs());
    }
}
