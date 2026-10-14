package dev.rylex.nep.compat.occultism;

import appeng.api.stacks.GenericStack;
import dev.rylex.nep.Nep;
import dev.rylex.nep.NepIcons;
import dev.rylex.nep.client.MatrixScreen;
import dev.rylex.nep.client.ReadoutButton;
import dev.rylex.nep.compat.occultism.RitualControllerBlockEntity.Phase;
import dev.rylex.nep.compat.occultism.RitualControllerBlockEntity.Refusal;
import dev.rylex.nep.compat.occultism.RitualControllerBlockEntity.Stall;
import dev.rylex.nep.util.MissingStacks;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class RitualControllerScreen extends MatrixScreen<RitualControllerMenu> {

    private static final Identifier TEXTURE = Nep.id("textures/gui/ritual_controller.png");
    private static final String KEY = "gui.nep.ritual_controller.";

    private static final int TITLE_ON_PANEL = 0xFF2A2433;
    private static final int READOUT_TEXT = 0xFFEAF7E6;
    private static final int READOUT_DIM = 0xFFA9C2A4;

    private static final int BAR_TRACK = 0x16301A;
    private static final int RITUAL_FILL = 0x6BE07A;

    @Nullable
    private ReadoutButton clearPending;

    @Nullable
    private ReadoutButton repair;

    public RitualControllerScreen(RitualControllerMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title, RitualControllerMenu.WIDTH, RitualControllerMenu.HEIGHT);
    }

    @Override
    protected Identifier texture() {
        return TEXTURE;
    }

    @Override
    protected void init() {
        super.init();
        resetButtons();
        addHelpButton("nep/occultism/ritual-controller.md");
        clearPending = addRightButton(
                2,
                NepIcons.REMOVE,
                "gui.nep.clear_pending",
                "gui.nep.clear_pending.hint",
                () -> sendButton(RitualControllerMenu.BUTTON_CLEAR_PENDING));
        addRightButton(
                3,
                NepIcons.DOWN,
                "gui.nep.clear_buffer",
                "gui.nep.clear_buffer.hint",
                () -> sendButton(RitualControllerMenu.BUTTON_CLEAR_BUFFER));
        repair = addRightButton(
                4,
                NepIcons.CONFIG,
                KEY + "repair",
                KEY + "repair.hint",
                () -> sendButton(RitualControllerMenu.BUTTON_TOGGLE_REPAIR));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        RitualControllerBlockEntity controller = menu.controller();
        if (clearPending != null) {
            clearPending.active =
                    controller != null && (controller.hasPending() || controller.refusal() != Refusal.NONE);
        }
        if (repair != null) {
            repair.setGlyph(controller != null && controller.repairEnabled() ? NepIcons.UP : NepIcons.CONFIG);
        }
        tickButtons();
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, Component.translatable(KEY + "title"), 8, 7, TITLE_ON_PANEL, false);
        graphics.text(
                font,
                Component.translatable(KEY + "input"),
                RitualControllerMenu.INPUT_X,
                RitualControllerMenu.INPUT_Y - 10,
                TITLE_ON_PANEL,
                false);
        graphics.text(
                font,
                Component.translatable(KEY + "output"),
                RitualControllerMenu.OUTPUT_X,
                RitualControllerMenu.OUTPUT_Y - 10,
                TITLE_ON_PANEL,
                false);
        graphics.text(
                font,
                Component.translatable("container.inventory"),
                RitualControllerMenu.INV_X,
                RitualControllerMenu.INV_Y - 10,
                TITLE_ON_PANEL,
                false);

        RitualControllerBlockEntity controller = menu.controller();
        if (controller == null) {
            return;
        }

        Component status = statusLine(controller);
        graphics.text(font, status, READOUT_X, LINE_ONE_Y, statusColour(controller), false);
        drawTrailing(graphics, spiritLine(controller), LINE_ONE_Y, READOUT_X + font.width(status), READOUT_DIM);

        ItemStack making = controller.activeResult();
        Component craftingLabel = Component.translatable(KEY + (controller.repairing() ? "repairing" : "crafting"));
        graphics.text(font, craftingLabel, READOUT_X, LINE_TWO_Y, READOUT_DIM, false);
        int craftingX = READOUT_X + font.width(craftingLabel) + 4;
        long queued = controller.pendingJobs();
        Component queuedText = queued > 0 ? Component.translatable(KEY + "queued", queued) : Component.empty();
        int nameLimit = READOUT_RIGHT - craftingX - (queued > 0 ? font.width(queuedText) + 6 : 0);
        Component craftingName =
                making.isEmpty() ? Component.translatable(KEY + "crafting.idle") : making.getHoverName();
        String craftingText = trim(craftingName, nameLimit);
        graphics.text(font, craftingText, craftingX, LINE_TWO_Y, making.isEmpty() ? READOUT_DIM : READOUT_TEXT, false);
        if (queued > 0) {
            drawTrailing(graphics, queuedText, LINE_TWO_Y, craftingX + font.width(craftingText), READOUT_DIM);
        }

        Component bowl = bowlLine(controller);
        graphics.text(font, bowl, READOUT_X, LINE_THREE_Y, READOUT_DIM, false);
        drawTrailing(
                graphics,
                Component.translatable(KEY + (controller.repairEnabled() ? "repair.on" : "repair.off")),
                LINE_THREE_Y,
                READOUT_X + font.width(bowl),
                READOUT_DIM);
        drawProgressBar(graphics, controller.craftProgress(), BAR_TRACK, RITUAL_FILL);
    }

    private static Component spiritLine(RitualControllerBlockEntity controller) {
        BoundSpirit spirit = controller.spirit();
        return spirit == null
                ? Component.translatable(KEY + "no_book")
                : Component.translatable(KEY + "tier", spirit.tier().displayName());
    }

    private static Component bowlLine(RitualControllerBlockEntity controller) {
        BlockPos bowl = controller.bowlPos();
        return bowl == null
                ? Component.translatable(KEY + "no_bowl")
                : Component.translatable(KEY + "bowl", bowl.getX(), bowl.getY(), bowl.getZ());
    }

    private static Component statusLine(RitualControllerBlockEntity controller) {
        if (controller.hasChannelFault()) {
            return Component.translatable(KEY + "no_network_channels");
        }
        if (controller.hasPowerFault()) {
            return Component.translatable(KEY + "no_network_power");
        }
        String fault = faultKey(controller);
        if (fault != null) {
            return Component.translatable(fault);
        }
        if (controller.isOutputBlocked()) {
            return Component.translatable(KEY + "output_blocked");
        }
        if (!controller.missingInputs().isEmpty()) {
            return Component.translatable(KEY + "missing");
        }
        return Component.translatable(controller.phase() == Phase.IDLE ? KEY + "idle" : KEY + "running");
    }

    @Nullable
    private static String faultKey(RitualControllerBlockEntity controller) {
        String stall = stallKey(controller.stall());
        return stall != null ? stall : refusalKey(controller.refusal());
    }

    @Nullable
    private static String stallKey(Stall stall) {
        return switch (stall) {
            case NONE -> null;
            case INGREDIENTS -> KEY + "stall.ingredients";
            case OUTPUT_FULL -> KEY + "output_blocked";
            case BOWL_BUSY -> KEY + "stall.bowl_busy";
            case CONDITIONS -> KEY + "stall.conditions";
            case INTERRUPTED -> KEY + "stall.interrupted";
            case BOWLS_OCCUPIED -> KEY + "stall.bowls_occupied";
            case TOO_FEW_BOWLS -> KEY + "stall.too_few_bowls";
            case WRONG_PENTACLE -> KEY + "stall.wrong_pentacle";
            case TIER_TOO_LOW -> KEY + "stall.tier_too_low";
            case UNLOADED -> KEY + "stall.unloaded";
            case NO_BOWL -> KEY + "stall.no_bowl";
            case NO_BOOK -> KEY + "stall.no_book";
            case NO_RECIPE -> KEY + "stall.no_recipe";
        };
    }

    @Nullable
    private static String refusalKey(Refusal refusal) {
        return switch (refusal) {
            case NONE -> null;
            case NOT_A_RITUAL_PATTERN -> KEY + "refused.not_a_pattern";
            case NO_ITEM_OUTPUT -> KEY + "refused.no_item_output";
            case UNKNOWN_RECIPE -> KEY + "refused.unknown_recipe";
            case NO_BOOK -> KEY + "refused.no_book";
            case TIER_TOO_LOW -> KEY + "refused.tier_too_low";
            case MIXED_RECIPES -> KEY + "refused.mixed_recipes";
            case ITEMS_ONLY -> KEY + "refused.items_only";
            case TOO_MANY_INPUTS -> KEY + "refused.too_many_inputs";
            case BUFFER_FULL -> KEY + "refused.buffer_full";
        };
    }

    private static List<Component> statusHint(RitualControllerBlockEntity controller) {
        if (controller.hasChannelFault()) {
            return List.of(
                    Component.translatable(KEY + "no_network_channels.hint").withStyle(ChatFormatting.GRAY));
        }
        if (controller.hasPowerFault()) {
            return List.of(Component.translatable(KEY + "no_network_power.hint").withStyle(ChatFormatting.GRAY));
        }
        String key = faultKey(controller);
        if (key == null && controller.isOutputBlocked()) {
            key = KEY + "output_blocked";
        }
        return key == null
                ? List.of()
                : List.of(Component.translatable(key + ".hint").withStyle(ChatFormatting.GRAY));
    }

    private static int statusColour(RitualControllerBlockEntity controller) {
        if (faultKey(controller) != null
                || controller.isOutputBlocked()
                || controller.hasPowerFault()
                || controller.hasChannelFault()
                || !controller.missingInputs().isEmpty()) {
            return READOUT_WARN;
        }
        return controller.phase() == Phase.IDLE ? READOUT_DIM : READOUT_TEXT;
    }

    @Override
    protected void extractReadoutTooltips(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        RitualControllerBlockEntity controller = menu.controller();
        if (controller == null || !within(mouseX, mouseY, READOUT_X, LINE_ONE_Y, BAR_WIDTH, READOUT_HEIGHT)) {
            return;
        }
        List<Component> lines = new ArrayList<>();
        lines.add(statusLine(controller));
        lines.addAll(statusHint(controller));
        lines.add(spiritLine(controller).copy().withStyle(ChatFormatting.GRAY));
        lines.add(bowlLine(controller).copy().withStyle(ChatFormatting.GRAY));
        long queued = controller.pendingJobs();
        if (queued > 0) {
            lines.add(Component.translatable(KEY + "queued_tooltip", queued).withStyle(ChatFormatting.GRAY));
        }
        List<GenericStack> missing = controller.missingInputs();
        if (!missing.isEmpty()) {
            lines.add(Component.translatable(KEY + "missing"));
            for (GenericStack stack : missing) {
                lines.add(MissingStacks.describe(stack).withStyle(ChatFormatting.GRAY));
            }
        }
        graphics.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
    }
}
