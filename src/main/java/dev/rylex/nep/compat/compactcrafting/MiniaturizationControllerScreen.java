package dev.rylex.nep.compat.compactcrafting;

import appeng.api.stacks.GenericStack;
import dev.compactmods.crafting.api.field.MiniaturizationFieldSize;
import dev.rylex.nep.Nep;
import dev.rylex.nep.client.MatrixScreen;
import dev.rylex.nep.client.ReadoutButton;
import dev.rylex.nep.machine.RedstoneMode;
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

public class MiniaturizationControllerScreen extends MatrixScreen<MiniaturizationControllerMenu> {

    private static final ResourceLocation TEXTURE = Nep.id("textures/gui/miniaturization_matrix.png");

    private static final int TITLE_ON_PANEL = 0x3A2E1C;
    private static final int READOUT_TEXT = 0xF2E8D0;
    private static final int READOUT_DIM = 0xC8A882;

    private static final int BAR_TRACK = 0x0D333D;
    private static final int CRAFT_FILL = 0x208BFF;

    @Nullable
    private ReadoutButton clearPending;

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
        resetButtons();
        clearPending = addRightButton(
                1,
                "✗",
                "gui.nep.clear_pending",
                "gui.nep.clear_pending.hint",
                () -> sendButton(MiniaturizationControllerMenu.BUTTON_CLEAR_PENDING));
        redstoneModeButton = addRightButton(
                2,
                currentMode().glyph(),
                "gui.nep.redstone_mode",
                "gui.nep.redstone_mode.hint",
                () -> sendButton(MiniaturizationControllerMenu.BUTTON_REDSTONE_MODE));
        addRightButton(
                3,
                "↓",
                "gui.nep.clear_buffer",
                "gui.nep.clear_buffer.hint",
                () -> sendButton(MiniaturizationControllerMenu.BUTTON_CLEAR_BUFFER));
    }

    private RedstoneMode currentMode() {
        MiniaturizationControllerBlockEntity controller = menu.controller();
        return controller != null ? controller.redstoneMode() : RedstoneMode.OUTPUT;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        MiniaturizationControllerBlockEntity controller = menu.controller();
        if (clearPending != null) {
            clearPending.active = controller != null
                    && (controller.hasPending()
                            || controller.refusal() != MiniaturizationControllerBlockEntity.Refusal.NONE);
        }
        updateRedstoneButton(currentMode());
        tickButtons();
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(
                font, Component.translatable("gui.nep.miniaturization_controller.title"), 8, 7, TITLE_ON_PANEL, false);
        graphics.drawString(
                font,
                Component.translatable("gui.nep.miniaturization_controller.input"),
                MiniaturizationControllerMenu.INPUT_X,
                MiniaturizationControllerMenu.INPUT_Y - 10,
                TITLE_ON_PANEL,
                false);
        graphics.drawString(
                font,
                Component.translatable("gui.nep.miniaturization_controller.output"),
                MiniaturizationControllerMenu.OUTPUT_X,
                MiniaturizationControllerMenu.OUTPUT_Y - 10,
                TITLE_ON_PANEL,
                false);
        graphics.drawString(
                font,
                Component.translatable("container.inventory"),
                MiniaturizationControllerMenu.INV_X,
                MiniaturizationControllerMenu.INV_Y - 10,
                TITLE_ON_PANEL,
                false);

        MiniaturizationControllerBlockEntity controller = menu.controller();
        if (controller == null) {
            return;
        }

        Component status = statusLine(controller);
        graphics.drawString(font, status, READOUT_X, LINE_ONE_Y, statusColour(controller), false);
        drawTrailing(graphics, fieldLine(controller), LINE_ONE_Y, READOUT_X + font.width(status), READOUT_DIM);

        ItemStack making = controller.activeResult();
        Component craftingLabel = Component.translatable("gui.nep.miniaturization_controller.crafting");
        graphics.drawString(font, craftingLabel, READOUT_X, LINE_TWO_Y, READOUT_DIM, false);
        int craftingX = READOUT_X + font.width(craftingLabel) + 4;
        long queued = controller.pendingJobs();
        Component queuedText = queued > 0
                ? Component.translatable("gui.nep.miniaturization_controller.queued", queued)
                : Component.empty();
        int nameLimit = READOUT_RIGHT - craftingX - (queued > 0 ? font.width(queuedText) + 6 : 0);
        Component craftingName = making.isEmpty()
                ? Component.translatable("gui.nep.miniaturization_controller.crafting.idle")
                : making.getHoverName();
        String craftingText = trim(craftingName, nameLimit);
        graphics.drawString(
                font, craftingText, craftingX, LINE_TWO_Y, making.isEmpty() ? READOUT_DIM : READOUT_TEXT, false);
        if (queued > 0) {
            drawTrailing(graphics, queuedText, LINE_TWO_Y, craftingX + font.width(craftingText), READOUT_DIM);
        }

        if (controller.craftTicks() > 0) {
            graphics.drawString(
                    font,
                    Component.translatable("gui.nep.miniaturization_controller.craft_time", controller.craftTicks()),
                    READOUT_X,
                    LINE_THREE_Y,
                    READOUT_DIM,
                    false);
        }

        drawProgressBar(graphics, controller.craftProgress(), BAR_TRACK, CRAFT_FILL);
    }

    private static Component fieldLine(MiniaturizationControllerBlockEntity controller) {
        MiniaturizationFieldSize size = controller.boundFieldSize();
        return size == null
                ? Component.translatable("gui.nep.miniaturization_controller.no_field")
                : Component.translatable("gui.nep.miniaturization_controller.field", size.getDimensions());
    }

    private static Component statusLine(MiniaturizationControllerBlockEntity controller) {
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
        if (controller == null || !within(mouseX, mouseY, READOUT_X, LINE_ONE_Y, BAR_WIDTH, 32)) {
            return;
        }
        List<Component> lines = new ArrayList<>();
        lines.add(statusLine(controller));
        lines.addAll(statusHint(controller));
        ItemStack making = controller.activeResult();
        if (!making.isEmpty()) {
            lines.add(
                    Component.translatable("gui.nep.miniaturization_controller.crafting_tooltip", making.getHoverName())
                            .withStyle(ChatFormatting.GRAY));
        }
        long queued = controller.pendingJobs();
        if (queued > 0) {
            lines.add(Component.translatable("gui.nep.miniaturization_controller.queued_tooltip", queued)
                    .withStyle(ChatFormatting.GRAY));
        }
        List<GenericStack> missing = controller.missingInputs();
        if (!missing.isEmpty()) {
            lines.add(Component.translatable("gui.nep.miniaturization_controller.missing"));
            for (GenericStack stack : missing) {
                lines.add(MissingStacks.describe(stack).withStyle(ChatFormatting.GRAY));
            }
        }
        graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
    }
}
