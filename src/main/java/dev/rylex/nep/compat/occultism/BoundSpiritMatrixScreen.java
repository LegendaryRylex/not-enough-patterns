package dev.rylex.nep.compat.occultism;

import appeng.api.stacks.GenericStack;
import dev.rylex.nep.Nep;
import dev.rylex.nep.NepIcons;
import dev.rylex.nep.client.MatrixScreen;
import dev.rylex.nep.client.ReadoutButton;
import dev.rylex.nep.util.MissingStacks;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class BoundSpiritMatrixScreen extends MatrixScreen<BoundSpiritMatrixMenu> {

    private static final Identifier TEXTURE = Nep.id("textures/gui/bound_spirit_matrix.png");

    private static final int TITLE_ON_PANEL = 0xFF3A2E1C;
    private static final int READOUT_TEXT = 0xFFF2E8D0;
    private static final int READOUT_DIM = 0xFFC8A882;

    private static final int BAR_TRACK = 0x18300A;
    private static final int CRAFT_FILL = 0x9BE03F;

    @Nullable
    private ReadoutButton clearPending;

    public BoundSpiritMatrixScreen(BoundSpiritMatrixMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title, BoundSpiritMatrixMenu.WIDTH, BoundSpiritMatrixMenu.HEIGHT);
    }

    @Override
    protected Identifier texture() {
        return TEXTURE;
    }

    @Override
    protected void init() {
        super.init();
        resetButtons();
        addHelpButton("nep/occultism/bound-spirit-matrix.md");
        clearPending = addRightButton(
                2,
                NepIcons.REMOVE,
                "gui.nep.clear_pending",
                "gui.nep.clear_pending.hint",
                () -> sendButton(BoundSpiritMatrixMenu.BUTTON_CLEAR_PENDING));
        addRightButton(
                3,
                NepIcons.DOWN,
                "gui.nep.clear_buffer",
                "gui.nep.clear_buffer.hint",
                () -> sendButton(BoundSpiritMatrixMenu.BUTTON_CLEAR_BUFFER));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        BoundSpiritMatrixBlockEntity matrix = menu.matrix();
        if (clearPending != null) {
            clearPending.active = matrix != null
                    && (matrix.hasPending() || matrix.refusal() != BoundSpiritMatrixBlockEntity.Refusal.NONE);
        }
        tickButtons();
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, Component.translatable("gui.nep.bound_spirit_matrix.title"), 8, 7, TITLE_ON_PANEL, false);
        graphics.text(
                font,
                Component.translatable("gui.nep.bound_spirit_matrix.input"),
                BoundSpiritMatrixMenu.INPUT_X,
                BoundSpiritMatrixMenu.INPUT_Y - 10,
                TITLE_ON_PANEL,
                false);
        graphics.text(
                font,
                Component.translatable("gui.nep.bound_spirit_matrix.output"),
                BoundSpiritMatrixMenu.OUTPUT_X,
                BoundSpiritMatrixMenu.OUTPUT_Y - 10,
                TITLE_ON_PANEL,
                false);
        graphics.text(
                font,
                Component.translatable("container.inventory"),
                BoundSpiritMatrixMenu.INV_X,
                BoundSpiritMatrixMenu.INV_Y - 10,
                TITLE_ON_PANEL,
                false);

        BoundSpiritMatrixBlockEntity matrix = menu.matrix();
        if (matrix == null) {
            return;
        }

        graphics.text(font, statusLine(matrix), READOUT_X, LINE_ONE_Y, statusColour(matrix), false);

        ItemStack making = matrix.activeResult();
        Component craftingLabel = Component.translatable("gui.nep.bound_spirit_matrix.crafting");
        graphics.text(font, craftingLabel, READOUT_X, LINE_TWO_Y, READOUT_DIM, false);
        int craftingX = READOUT_X + font.width(craftingLabel) + 4;
        long queued = matrix.pendingJobs();
        Component queuedText =
                queued > 0 ? Component.translatable("gui.nep.bound_spirit_matrix.queued", queued) : Component.empty();
        int nameLimit = READOUT_RIGHT - craftingX - (queued > 0 ? font.width(queuedText) + 6 : 0);
        Component craftingName = making.isEmpty()
                ? Component.translatable("gui.nep.bound_spirit_matrix.crafting.idle")
                : making.getHoverName();
        String craftingText = trim(craftingName, nameLimit);
        graphics.text(font, craftingText, craftingX, LINE_TWO_Y, making.isEmpty() ? READOUT_DIM : READOUT_TEXT, false);
        if (queued > 0) {
            drawTrailing(graphics, queuedText, LINE_TWO_Y, craftingX + font.width(craftingText), READOUT_DIM);
        }

        Component workLine = Component.translatable(
                making.isEmpty()
                        ? (matrix.hasChannelFault() || matrix.hasPowerFault()
                                ? "gui.nep.bound_spirit_matrix.work.offline"
                                : "gui.nep.bound_spirit_matrix.work.idle")
                        : "gui.nep.bound_spirit_matrix.work."
                                + matrix.activeKind().id());
        graphics.text(font, workLine, READOUT_X, LINE_THREE_Y, READOUT_DIM, false);
        if (!making.isEmpty()) {
            drawTrailing(
                    graphics,
                    Component.translatable("gui.nep.bound_spirit_matrix.craft_time", matrix.craftTicks()),
                    LINE_THREE_Y,
                    READOUT_X + font.width(workLine),
                    READOUT_DIM);
        }

        drawProgressBar(graphics, matrix.craftProgress(), BAR_TRACK, CRAFT_FILL);
    }

    private static Component faultLine(
            BoundSpiritMatrixBlockEntity.Stall stall, BoundSpiritMatrixBlockEntity.Refusal refusal) {
        String key =
                switch (stall) {
                    case NONE -> null;
                    case INGREDIENTS -> "gui.nep.bound_spirit_matrix.stall.ingredients";
                    case NO_SPIRIT -> "gui.nep.bound_spirit_matrix.stall.no_spirit";
                    case OUTPUT_FULL -> "gui.nep.bound_spirit_matrix.output_blocked";
                    case NO_RECIPE -> "gui.nep.bound_spirit_matrix.stall.no_recipe";
                };
        if (key != null) {
            return Component.translatable(key);
        }
        String refused =
                switch (refusal) {
                    case NONE -> null;
                    case NO_ITEM_OUTPUT -> "gui.nep.bound_spirit_matrix.refused.no_item_output";
                    case ONE_INPUT_ONLY -> "gui.nep.bound_spirit_matrix.refused.one_input_only";
                    case ITEMS_ONLY -> "gui.nep.bound_spirit_matrix.refused.items_only";
                    case UNKNOWN_RECIPE -> "gui.nep.bound_spirit_matrix.refused.unknown_recipe";
                    case NEEDS_CRUSHER -> "gui.nep.bound_spirit_matrix.refused.needs_crusher";
                    case NEEDS_CRYSTALLIZER -> "gui.nep.bound_spirit_matrix.refused.needs_crystallizer";
                    case SPIRIT_TOO_WEAK -> "gui.nep.bound_spirit_matrix.refused.spirit_too_weak";
                    case MIXED_RECIPES -> "gui.nep.bound_spirit_matrix.refused.mixed_recipes";
                    case BUFFER_FULL -> "gui.nep.bound_spirit_matrix.refused.buffer_full";
                };
        return refused == null
                ? Component.translatable("gui.nep.bound_spirit_matrix.idle")
                : Component.translatable(refused);
    }

    private static List<Component> statusHint(BoundSpiritMatrixBlockEntity matrix) {
        if (matrix.hasChannelFault()) {
            return List.of(Component.translatable("gui.nep.bound_spirit_matrix.no_network_channels.hint")
                    .withStyle(ChatFormatting.GRAY));
        }
        if (matrix.hasPowerFault()) {
            return List.of(Component.translatable("gui.nep.bound_spirit_matrix.stall.no_me_power.hint")
                    .withStyle(ChatFormatting.GRAY));
        }
        String key =
                switch (matrix.stall()) {
                    case NONE -> null;
                    case INGREDIENTS -> "gui.nep.bound_spirit_matrix.stall.ingredients.hint";
                    case NO_SPIRIT -> "gui.nep.bound_spirit_matrix.stall.no_spirit.hint";
                    case OUTPUT_FULL -> "gui.nep.bound_spirit_matrix.stall.output_full.hint";
                    case NO_RECIPE -> "gui.nep.bound_spirit_matrix.stall.no_recipe.hint";
                };
        if (key == null) {
            key = switch (matrix.refusal()) {
                case NONE -> null;
                case NEEDS_CRUSHER, NEEDS_CRYSTALLIZER, SPIRIT_TOO_WEAK ->
                    "gui.nep.bound_spirit_matrix.refused.spirit.hint";
                default -> "gui.nep.bound_spirit_matrix.refused.hint";
            };
        }
        if (key == null && matrix.isOutputBlocked()) {
            key = "gui.nep.bound_spirit_matrix.stall.output_full.hint";
        }
        return key == null ? List.of() : List.of(Component.translatable(key).withStyle(ChatFormatting.GRAY));
    }

    private static Component statusLine(BoundSpiritMatrixBlockEntity matrix) {
        if (matrix.hasChannelFault()) {
            return Component.translatable("gui.nep.bound_spirit_matrix.no_network_channels");
        }
        if (matrix.hasPowerFault()) {
            return Component.translatable("gui.nep.bound_spirit_matrix.no_network_power");
        }
        if (matrix.stall() != BoundSpiritMatrixBlockEntity.Stall.NONE
                || matrix.refusal() != BoundSpiritMatrixBlockEntity.Refusal.NONE) {
            return faultLine(matrix.stall(), matrix.refusal());
        }
        if (matrix.isOutputBlocked()) {
            return Component.translatable("gui.nep.bound_spirit_matrix.output_blocked");
        }
        if (!matrix.missingInputs().isEmpty()) {
            return Component.translatable("gui.nep.bound_spirit_matrix.missing");
        }
        if (matrix.activeResult().isEmpty()) {
            return Component.translatable("gui.nep.bound_spirit_matrix.idle");
        }
        return Component.translatable("gui.nep.bound_spirit_matrix.crafting_now");
    }

    private static boolean faulted(BoundSpiritMatrixBlockEntity matrix) {
        return matrix.stall() != BoundSpiritMatrixBlockEntity.Stall.NONE
                || matrix.refusal() != BoundSpiritMatrixBlockEntity.Refusal.NONE
                || matrix.isOutputBlocked()
                || matrix.hasPowerFault()
                || matrix.hasChannelFault()
                || !matrix.missingInputs().isEmpty();
    }

    private static int statusColour(BoundSpiritMatrixBlockEntity matrix) {
        if (faulted(matrix)) {
            return READOUT_WARN;
        }
        return matrix.activeResult().isEmpty() ? READOUT_DIM : READOUT_TEXT;
    }

    @Override
    protected void extractReadoutTooltips(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        BoundSpiritMatrixBlockEntity matrix = menu.matrix();
        if (matrix == null) {
            return;
        }
        if (bookSlotTooltip(graphics, matrix, BoundSpirit.Work.CRUSHER, BoundSpiritMatrixMenu.CRUSHER_Y, mouseX, mouseY)
                || bookSlotTooltip(
                        graphics,
                        matrix,
                        BoundSpirit.Work.CRYSTALLIZER,
                        BoundSpiritMatrixMenu.CRYSTALLIZER_Y,
                        mouseX,
                        mouseY)) {
            return;
        }
        if (!within(mouseX, mouseY, READOUT_X, LINE_ONE_Y, BAR_WIDTH, READOUT_HEIGHT)) {
            return;
        }
        List<Component> lines = new ArrayList<>();
        lines.add(statusLine(matrix));
        lines.addAll(statusHint(matrix));
        ItemStack making = matrix.activeResult();
        if (!making.isEmpty()) {
            lines.add(Component.translatable("gui.nep.bound_spirit_matrix.crafting_tooltip", making.getHoverName())
                    .withStyle(ChatFormatting.GRAY));
        }
        long queued = matrix.pendingJobs();
        if (queued > 0) {
            lines.add(Component.translatable("gui.nep.bound_spirit_matrix.queued_tooltip", queued)
                    .withStyle(ChatFormatting.GRAY));
        }
        List<GenericStack> missing = matrix.missingInputs();
        if (!missing.isEmpty()) {
            lines.add(Component.translatable("gui.nep.bound_spirit_matrix.missing"));
            for (GenericStack stack : missing) {
                lines.add(MissingStacks.describe(stack).withStyle(ChatFormatting.GRAY));
            }
        }
        graphics.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
    }

    private boolean bookSlotTooltip(
            GuiGraphicsExtractor graphics,
            BoundSpiritMatrixBlockEntity matrix,
            BoundSpirit.Work work,
            int y,
            int mouseX,
            int mouseY) {
        if (matrix.spirit(work) != null || !within(mouseX, mouseY, BoundSpiritMatrixMenu.BOOK_X, y, 16, 16)) {
            return false;
        }
        graphics.setComponentTooltipForNextFrame(
                font,
                List.of(
                        Component.translatable("gui.nep.bound_spirit_matrix.book." + work.getSerializedName()),
                        Component.translatable("gui.nep.bound_spirit_matrix.book." + work.getSerializedName() + ".hint")
                                .withStyle(ChatFormatting.GRAY)),
                mouseX,
                mouseY);
        return true;
    }
}
