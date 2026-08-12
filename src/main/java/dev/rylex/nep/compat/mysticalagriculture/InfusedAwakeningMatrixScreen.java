package dev.rylex.nep.compat.mysticalagriculture;

import appeng.api.stacks.GenericStack;
import dev.rylex.nep.Nep;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.client.MatrixScreen;
import dev.rylex.nep.client.ReadoutButton;
import dev.rylex.nep.client.StackTint;
import dev.rylex.nep.client.TankGauge;
import dev.rylex.nep.machine.RedstoneMode;
import dev.rylex.nep.util.MissingStacks;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class InfusedAwakeningMatrixScreen extends MatrixScreen<InfusedAwakeningMatrixMenu> {

    private static final Identifier TEXTURE = Nep.id("textures/gui/infused_awakening_matrix.png");

    private static final int TITLE_ON_PANEL = 0xFF3A2E1C;
    private static final int READOUT_TEXT = 0xFFF2E8D0;
    private static final int READOUT_DIM = 0xFFC8A882;

    private static final int BAR_TRACK = 0x3D0D0D;
    private static final int CRAFT_FILL = 0xC74F14;
    private static final int TANK_FILL = 0x8CC820;

    @Nullable
    private ReadoutButton clearPending;

    public InfusedAwakeningMatrixScreen(InfusedAwakeningMatrixMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title, InfusedAwakeningMatrixMenu.WIDTH, InfusedAwakeningMatrixMenu.HEIGHT);
    }

    @Override
    protected Identifier texture() {
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
                () -> sendButton(InfusedAwakeningMatrixMenu.BUTTON_CLEAR_PENDING));
        redstoneModeButton = addRightButton(
                2,
                currentMode().glyph(),
                "gui.nep.redstone_mode",
                "gui.nep.redstone_mode.hint",
                () -> sendButton(InfusedAwakeningMatrixMenu.BUTTON_REDSTONE_MODE));
        addRightButton(
                3,
                "↓",
                "gui.nep.clear_buffer",
                "gui.nep.clear_buffer.hint",
                () -> sendButton(InfusedAwakeningMatrixMenu.BUTTON_CLEAR_BUFFER));
    }

    private RedstoneMode currentMode() {
        InfusedAwakeningMatrixBlockEntity matrix = menu.matrix();
        return matrix != null ? matrix.redstoneMode() : RedstoneMode.OUTPUT;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        InfusedAwakeningMatrixBlockEntity matrix = menu.matrix();
        if (clearPending != null) {
            clearPending.active = matrix != null
                    && (matrix.hasPending() || matrix.refusal() != InfusedAwakeningMatrixBlockEntity.Refusal.NONE);
        }
        updateRedstoneButton(currentMode());
        tickButtons();
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(
                font, Component.translatable("gui.nep.infused_awakening_matrix.title"), 8, 7, TITLE_ON_PANEL, false);
        graphics.text(
                font,
                Component.translatable("gui.nep.infused_awakening_matrix.input"),
                InfusedAwakeningMatrixMenu.INPUT_X,
                InfusedAwakeningMatrixMenu.INPUT_Y - 10,
                TITLE_ON_PANEL,
                false);
        graphics.text(
                font,
                Component.translatable("gui.nep.infused_awakening_matrix.output"),
                InfusedAwakeningMatrixMenu.OUTPUT_X,
                InfusedAwakeningMatrixMenu.OUTPUT_Y - 10,
                TITLE_ON_PANEL,
                false);
        graphics.text(
                font,
                Component.translatable("container.inventory"),
                InfusedAwakeningMatrixMenu.INV_X,
                InfusedAwakeningMatrixMenu.INV_Y - 10,
                TITLE_ON_PANEL,
                false);

        InfusedAwakeningMatrixBlockEntity matrix = menu.matrix();
        if (matrix == null) {
            return;
        }

        graphics.text(font, statusLine(matrix), READOUT_X, LINE_ONE_Y, statusColour(matrix), false);

        ItemStack making = matrix.activeResult();
        Component craftingLabel = Component.translatable("gui.nep.infused_awakening_matrix.crafting");
        graphics.text(font, craftingLabel, READOUT_X, LINE_TWO_Y, READOUT_DIM, false);
        int craftingX = READOUT_X + font.width(craftingLabel) + 4;
        long queued = matrix.pendingJobs();
        Component queuedText = queued > 0
                ? Component.translatable("gui.nep.infused_awakening_matrix.queued", queued)
                : Component.empty();
        int nameLimit = READOUT_RIGHT - craftingX - (queued > 0 ? font.width(queuedText) + 6 : 0);
        Component craftingName = making.isEmpty()
                ? Component.translatable("gui.nep.infused_awakening_matrix.crafting.idle")
                : making.getHoverName();
        String craftingText = trim(craftingName, nameLimit);
        graphics.text(font, craftingText, craftingX, LINE_TWO_Y, making.isEmpty() ? READOUT_DIM : READOUT_TEXT, false);
        if (queued > 0) {
            drawTrailing(graphics, queuedText, LINE_TWO_Y, craftingX + font.width(craftingText), READOUT_DIM);
        }

        Component tankLine =
                Component.translatable("gui.nep.infused_awakening_matrix.tanks_line", filledTanks(matrix), 4);
        graphics.text(font, tankLine, READOUT_X, LINE_THREE_Y, READOUT_DIM, false);
        drawTrailing(
                graphics,
                Component.translatable(
                        "gui.nep.infused_awakening_matrix.craft_time",
                        NepConfig.mysticalInfusedAwakeningMatrixCraftTicks()),
                LINE_THREE_Y,
                READOUT_X + font.width(tankLine),
                READOUT_DIM);

        drawProgressBar(graphics, matrix.craftProgress(), BAR_TRACK, CRAFT_FILL);

        drawTanks(graphics, matrix);
    }

    private void drawTanks(GuiGraphicsExtractor graphics, InfusedAwakeningMatrixBlockEntity matrix) {
        for (int index = 0; index < InfusedAwakeningMatrixBlockEntity.TANKS; index++) {
            EssenceTank tank = matrix.tank(index);
            if (tank.isEmpty()) {
                continue;
            }
            TankGauge.draw(
                    graphics,
                    InfusedAwakeningMatrixMenu.TANK_X + index * InfusedAwakeningMatrixMenu.TANK_SPACING,
                    InfusedAwakeningMatrixMenu.TANK_Y,
                    InfusedAwakeningMatrixMenu.TANK_WIDTH,
                    InfusedAwakeningMatrixMenu.TANK_HEIGHT,
                    tank.amount(),
                    EssenceTank.capacity(),
                    StackTint.of(tank.displayStack(), TANK_FILL));
        }
    }

    private static int filledTanks(InfusedAwakeningMatrixBlockEntity matrix) {
        int filled = 0;
        for (int index = 0; index < InfusedAwakeningMatrixBlockEntity.TANKS; index++) {
            if (!matrix.tank(index).isEmpty()) {
                filled++;
            }
        }
        return filled;
    }

    private static Component faultLine(
            InfusedAwakeningMatrixBlockEntity.Stall stall, InfusedAwakeningMatrixBlockEntity.Refusal refusal) {
        String key =
                switch (stall) {
                    case NONE -> null;
                    case INGREDIENTS -> "gui.nep.infused_awakening_matrix.stall.ingredients";
                    case ESSENCE -> "gui.nep.infused_awakening_matrix.stall.essence";
                    case OUTPUT_FULL -> "gui.nep.infused_awakening_matrix.output_blocked";
                    case NO_RECIPE -> "gui.nep.infused_awakening_matrix.stall.no_recipe";
                };
        if (key != null) {
            return Component.translatable(key);
        }
        String refused =
                switch (refusal) {
                    case NONE -> null;
                    case NOT_A_MYSTICAL_PATTERN -> "gui.nep.infused_awakening_matrix.refused.not_a_mystical_pattern";
                    case NO_ITEM_OUTPUT -> "gui.nep.infused_awakening_matrix.refused.no_item_output";
                    case UNKNOWN_RECIPE -> "gui.nep.infused_awakening_matrix.refused.unknown_recipe";
                    case MISMATCHED_INPUTS -> "gui.nep.infused_awakening_matrix.refused.mismatched_inputs";
                    case MIXED_RECIPES -> "gui.nep.infused_awakening_matrix.refused.mixed_recipes";
                    case MODULE_DISABLED -> "gui.nep.infused_awakening_matrix.refused.module_disabled";
                    case ITEMS_ONLY -> "gui.nep.infused_awakening_matrix.refused.items_only";
                    case TOO_MANY_INPUTS -> "gui.nep.infused_awakening_matrix.refused.too_many_inputs";
                    case BUFFER_FULL -> "gui.nep.infused_awakening_matrix.refused.buffer_full";
                };
        return refused == null
                ? Component.translatable("gui.nep.infused_awakening_matrix.idle")
                : Component.translatable(refused);
    }

    private static List<Component> statusHint(InfusedAwakeningMatrixBlockEntity matrix) {
        if (matrix.hasPowerFault()) {
            return List.of(Component.translatable("gui.nep.infused_awakening_matrix.stall.no_me_power.hint")
                    .withStyle(ChatFormatting.GRAY));
        }
        String key =
                switch (matrix.stall()) {
                    case NONE -> null;
                    case INGREDIENTS -> "gui.nep.infused_awakening_matrix.stall.ingredients.hint";
                    case ESSENCE -> "gui.nep.infused_awakening_matrix.stall.essence.hint";
                    case OUTPUT_FULL -> "gui.nep.infused_awakening_matrix.stall.output_full.hint";
                    case NO_RECIPE -> "gui.nep.infused_awakening_matrix.stall.no_recipe.hint";
                };
        if (key == null && matrix.refusal() != InfusedAwakeningMatrixBlockEntity.Refusal.NONE) {
            key = refusalHint(matrix.refusal());
        }
        if (key == null && matrix.isOutputBlocked()) {
            key = "gui.nep.infused_awakening_matrix.stall.output_full.hint";
        }
        return key == null ? List.of() : List.of(Component.translatable(key).withStyle(ChatFormatting.GRAY));
    }

    private static String refusalHint(InfusedAwakeningMatrixBlockEntity.Refusal refusal) {
        return switch (refusal) {
            case BUFFER_FULL -> "gui.nep.infused_awakening_matrix.refused.buffer_full.hint";
            case TOO_MANY_INPUTS -> "gui.nep.infused_awakening_matrix.refused.too_many_inputs.hint";
            case MODULE_DISABLED -> "gui.nep.infused_awakening_matrix.refused.module_disabled.hint";
            default -> "gui.nep.infused_awakening_matrix.refused.hint";
        };
    }

    private static Component statusLine(InfusedAwakeningMatrixBlockEntity matrix) {
        if (matrix.hasPowerFault()) {
            return Component.translatable("gui.nep.infused_awakening_matrix.no_network_power");
        }
        if (matrix.stall() != InfusedAwakeningMatrixBlockEntity.Stall.NONE
                || matrix.refusal() != InfusedAwakeningMatrixBlockEntity.Refusal.NONE) {
            return faultLine(matrix.stall(), matrix.refusal());
        }
        if (matrix.isOutputBlocked()) {
            return Component.translatable("gui.nep.infused_awakening_matrix.output_blocked");
        }
        if (!matrix.missingInputs().isEmpty()) {
            return Component.translatable("gui.nep.infused_awakening_matrix.missing");
        }
        if (matrix.activeResult().isEmpty()) {
            return Component.translatable("gui.nep.infused_awakening_matrix.idle");
        }
        return Component.translatable("gui.nep.infused_awakening_matrix.infusing");
    }

    private static boolean faulted(InfusedAwakeningMatrixBlockEntity matrix) {
        return matrix.stall() != InfusedAwakeningMatrixBlockEntity.Stall.NONE
                || matrix.refusal() != InfusedAwakeningMatrixBlockEntity.Refusal.NONE
                || matrix.isOutputBlocked()
                || matrix.hasPowerFault()
                || !matrix.missingInputs().isEmpty();
    }

    private static int statusColour(InfusedAwakeningMatrixBlockEntity matrix) {
        if (faulted(matrix)) {
            return READOUT_WARN;
        }
        return matrix.activeResult().isEmpty() ? READOUT_DIM : READOUT_TEXT;
    }

    @Override
    protected void extractReadoutTooltips(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        InfusedAwakeningMatrixBlockEntity matrix = menu.matrix();
        if (matrix == null) {
            return;
        }
        for (int index = 0; index < InfusedAwakeningMatrixBlockEntity.TANKS; index++) {
            int x = InfusedAwakeningMatrixMenu.TANK_X + index * InfusedAwakeningMatrixMenu.TANK_SPACING;
            if (!within(
                    mouseX,
                    mouseY,
                    x,
                    InfusedAwakeningMatrixMenu.TANK_Y,
                    InfusedAwakeningMatrixMenu.TANK_WIDTH,
                    InfusedAwakeningMatrixMenu.TANK_HEIGHT)) {
                continue;
            }
            graphics.setComponentTooltipForNextFrame(font, tankTooltip(matrix.tank(index)), mouseX, mouseY);
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
            lines.add(Component.translatable("gui.nep.infused_awakening_matrix.crafting_tooltip", making.getHoverName())
                    .withStyle(ChatFormatting.GRAY));
        }
        long queued = matrix.pendingJobs();
        if (queued > 0) {
            lines.add(Component.translatable("gui.nep.infused_awakening_matrix.queued_tooltip", queued)
                    .withStyle(ChatFormatting.GRAY));
        }
        List<GenericStack> missing = matrix.missingInputs();
        if (!missing.isEmpty()) {
            lines.add(Component.translatable("gui.nep.infused_awakening_matrix.missing"));
            for (GenericStack stack : missing) {
                lines.add(MissingStacks.describe(stack).withStyle(ChatFormatting.GRAY));
            }
        }
        graphics.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
    }

    private static List<Component> tankTooltip(EssenceTank tank) {
        List<Component> lines = new ArrayList<>();
        if (tank.isEmpty()) {
            lines.add(Component.translatable("gui.nep.infused_awakening_matrix.tank.empty"));
            lines.add(Component.translatable("gui.nep.infused_awakening_matrix.tank.empty.hint", EssenceTank.capacity())
                    .withStyle(ChatFormatting.GRAY));
            return lines;
        }
        ItemStack essence = tank.displayStack();
        lines.add(essence.getHoverName()
                .copy()
                .withStyle(
                        style -> style.withColor(TextColor.fromRgb(StackTint.of(essence, READOUT_TEXT) & 0xFFFFFF))));
        lines.add(Component.translatable(
                        "gui.nep.infused_awakening_matrix.tank.amount", tank.amount(), EssenceTank.capacity())
                .withStyle(ChatFormatting.GRAY));
        return lines;
    }
}
