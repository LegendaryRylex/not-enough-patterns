package dev.rylex.nep.compat.compactcrafting;

import dev.compactmods.crafting.api.field.MiniaturizationFieldSize;
import dev.rylex.nep.Nep;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.Nullable;

public class MiniaturizationControllerScreen extends MiniaturizationScreen<MiniaturizationControllerMenu> {

    private static final ResourceLocation TEXTURE = Nep.id("textures/gui/miniaturization_matrix.png");

    public MiniaturizationControllerScreen(MiniaturizationControllerMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth = MiniaturizationControllerMenu.WIDTH;
        this.imageHeight = MiniaturizationControllerMenu.HEIGHT;
    }

    @Override
    protected ResourceLocation texture() {
        return TEXTURE;
    }

    @Override
    protected void init() {
        super.init();
        addClearButtons(
                "nep/compactcrafting/miniaturization-controller.md",
                MiniaturizationControllerMenu.BUTTON_CLEAR_PENDING,
                MiniaturizationControllerMenu.BUTTON_CLEAR_BUFFER);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        MiniaturizationControllerBlockEntity controller = menu.controller();
        tickClearButtons(controller != null
                && (controller.hasPending()
                        || controller.refusal() != MiniaturizationControllerBlockEntity.Refusal.NONE));
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        drawTitles(
                graphics,
                "gui.nep.miniaturization_controller",
                MiniaturizationControllerMenu.INPUT_X,
                MiniaturizationControllerMenu.INPUT_Y,
                MiniaturizationControllerMenu.OUTPUT_X,
                MiniaturizationControllerMenu.OUTPUT_Y,
                MiniaturizationControllerMenu.INV_X,
                MiniaturizationControllerMenu.INV_Y);

        MiniaturizationControllerBlockEntity controller = menu.controller();
        if (controller == null) {
            return;
        }

        drawReadout(
                graphics,
                "gui.nep.miniaturization_controller",
                statusLine(controller),
                statusColour(controller),
                fieldLine(controller),
                controller.activeResult(),
                controller.pendingJobs(),
                controller.craftTicks(),
                controller.craftProgress());
    }

    private static Component fieldLine(MiniaturizationControllerBlockEntity controller) {
        MiniaturizationFieldSize size = controller.boundFieldSize();
        return size == null
                ? Component.translatable("gui.nep.miniaturization_controller.no_field")
                : Component.translatable("gui.nep.miniaturization_controller.field", size.getDimensions());
    }

    private static Component statusLine(MiniaturizationControllerBlockEntity controller) {
        if (controller.hasChannelFault()) {
            return Component.translatable("gui.nep.miniaturization_controller.no_network_channels");
        }
        if (controller.hasPowerFault()) {
            return Component.translatable("gui.nep.miniaturization_controller.no_network_power");
        }
        if (controller.stall() != MiniaturizationControllerBlockEntity.Stall.NONE
                || controller.refusal() != MiniaturizationControllerBlockEntity.Refusal.NONE) {
            return faultLine(controller.stall(), controller.refusal());
        }
        if (controller.isOutputBlocked()) {
            return Component.translatable("gui.nep.miniaturization_controller.output_blocked");
        }
        if (!controller.missingInputs().isEmpty()) {
            return Component.translatable("gui.nep.miniaturization_controller.missing");
        }
        return phaseLine(controller.phase());
    }

    private static Component phaseLine(MiniaturizationControllerBlockEntity.Phase phase) {
        return Component.translatable(
                switch (phase) {
                    case IDLE -> "gui.nep.miniaturization_controller.idle";
                    case BUILDING -> "gui.nep.miniaturization_controller.phase.building";
                    case SCANNING -> "gui.nep.miniaturization_controller.phase.scanning";
                    case CATALYST -> "gui.nep.miniaturization_controller.phase.catalyst";
                    case CRAFTING -> "gui.nep.miniaturization_controller.phase.crafting";
                    case COLLECTING -> "gui.nep.miniaturization_controller.phase.collecting";
                });
    }

    private static Component faultLine(
            MiniaturizationControllerBlockEntity.Stall stall, MiniaturizationControllerBlockEntity.Refusal refusal) {
        String key = stallKey(stall);
        if (key != null) {
            return Component.translatable(key);
        }
        String refused = refusalKey(refusal);
        return refused == null
                ? Component.translatable("gui.nep.miniaturization_controller.idle")
                : Component.translatable(refused);
    }

    @Nullable
    private static String stallKey(MiniaturizationControllerBlockEntity.Stall stall) {
        return switch (stall) {
            case NONE -> null;
            case INGREDIENTS -> "gui.nep.miniaturization_controller.stall.ingredients";
            case OUTPUT_FULL -> "gui.nep.miniaturization_controller.output_blocked";
            case NO_FIELD -> "gui.nep.miniaturization_controller.stall.no_field";
            case FIELD_DISABLED -> "gui.nep.miniaturization_controller.stall.field_disabled";
            case FIELD_UNLOADED -> "gui.nep.miniaturization_controller.stall.field_unloaded";
            case FIELD_BUSY -> "gui.nep.miniaturization_controller.stall.field_busy";
            case FIELD_OCCUPIED -> "gui.nep.miniaturization_controller.stall.field_occupied";
            case FIELD_TOO_SMALL -> "gui.nep.miniaturization_controller.stall.field_too_small";
            case INSIDE_FIELD -> "gui.nep.miniaturization_controller.stall.inside_field";
            case NO_MATCH -> "gui.nep.miniaturization_controller.stall.no_match";
            case NO_RECIPE -> "gui.nep.miniaturization_controller.stall.no_recipe";
        };
    }

    @Nullable
    private static String refusalKey(MiniaturizationControllerBlockEntity.Refusal refusal) {
        return switch (refusal) {
            case NONE -> null;
            case NOT_A_MINIATURIZATION_PATTERN -> "gui.nep.miniaturization_controller.refused.not_a_pattern";
            case NO_ITEM_OUTPUT -> "gui.nep.miniaturization_controller.refused.no_item_output";
            case UNKNOWN_RECIPE -> "gui.nep.miniaturization_controller.refused.unknown_recipe";
            case MIXED_RECIPES -> "gui.nep.miniaturization_controller.refused.mixed_recipes";
            case ITEMS_ONLY -> "gui.nep.miniaturization_controller.refused.items_only";
            case TOO_MANY_INPUTS -> "gui.nep.miniaturization_controller.refused.too_many_inputs";
            case BUFFER_FULL -> "gui.nep.miniaturization_controller.refused.buffer_full";
        };
    }

    private static List<Component> statusHint(MiniaturizationControllerBlockEntity controller) {
        if (controller.hasChannelFault()) {
            return List.of(Component.translatable("gui.nep.miniaturization_controller.no_network_channels.hint")
                    .withStyle(ChatFormatting.GRAY));
        }
        if (controller.hasPowerFault()) {
            return List.of(Component.translatable("gui.nep.miniaturization_controller.stall.no_me_power.hint")
                    .withStyle(ChatFormatting.GRAY));
        }
        String key = stallKey(controller.stall());
        key = key == null ? null : key + ".hint";
        if (key == null && controller.refusal() != MiniaturizationControllerBlockEntity.Refusal.NONE) {
            String refused = refusalKey(controller.refusal());
            key = refused == null ? null : refused + ".hint";
        }
        if (key == null && controller.isOutputBlocked()) {
            key = "gui.nep.miniaturization_controller.output_blocked.hint";
        }
        return key == null ? List.of() : List.of(Component.translatable(key).withStyle(ChatFormatting.GRAY));
    }

    private static boolean faulted(MiniaturizationControllerBlockEntity controller) {
        return controller.stall() != MiniaturizationControllerBlockEntity.Stall.NONE
                || controller.refusal() != MiniaturizationControllerBlockEntity.Refusal.NONE
                || controller.isOutputBlocked()
                || controller.hasPowerFault()
                || controller.hasChannelFault()
                || !controller.missingInputs().isEmpty();
    }

    private static int statusColour(MiniaturizationControllerBlockEntity controller) {
        if (faulted(controller)) {
            return READOUT_WARN;
        }
        return controller.phase() == MiniaturizationControllerBlockEntity.Phase.IDLE ? READOUT_DIM : READOUT_TEXT;
    }

    @Override
    protected void renderReadoutTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        MiniaturizationControllerBlockEntity controller = menu.controller();
        if (controller == null || !overReadout(mouseX, mouseY)) {
            return;
        }
        drawReadoutTooltip(
                graphics,
                mouseX,
                mouseY,
                "gui.nep.miniaturization_controller",
                statusLine(controller),
                statusHint(controller),
                controller.activeResult(),
                controller.pendingJobs(),
                controller.missingInputs());
    }
}
