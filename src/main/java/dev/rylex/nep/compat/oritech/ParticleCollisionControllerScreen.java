package dev.rylex.nep.compat.oritech;

import appeng.api.stacks.GenericStack;
import dev.rylex.nep.Nep;
import dev.rylex.nep.NepIcons;
import dev.rylex.nep.client.MatrixScreen;
import dev.rylex.nep.client.ReadoutButton;
import dev.rylex.nep.compat.oritech.ParticleCollisionControllerBlockEntity.Phase;
import dev.rylex.nep.compat.oritech.ParticleCollisionControllerBlockEntity.Refusal;
import dev.rylex.nep.compat.oritech.ParticleCollisionControllerBlockEntity.Stall;
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

public class ParticleCollisionControllerScreen extends MatrixScreen<ParticleCollisionControllerMenu> {

    private static final Identifier TEXTURE = Nep.id("textures/gui/particle_collision_controller.png");
    private static final String KEY = "gui.nep.particle_collision_controller.";

    private static final int TITLE_ON_PANEL = 0xFF1E2A26;
    private static final int READOUT_TEXT = 0xFFE6F2EE;
    private static final int READOUT_DIM = 0xFF9EBFB6;

    private static final int BAR_TRACK = 0x0E2A24;
    private static final int SPEED_FILL = 0x29C39D;

    @Nullable
    private ReadoutButton clearPending;

    public ParticleCollisionControllerScreen(
            ParticleCollisionControllerMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title, ParticleCollisionControllerMenu.WIDTH, ParticleCollisionControllerMenu.HEIGHT);
    }

    @Override
    protected Identifier texture() {
        return TEXTURE;
    }

    @Override
    protected void init() {
        super.init();
        resetButtons();
        addHelpButton("nep/oritech/particle-collision-controller.md");
        clearPending = addRightButton(
                2,
                NepIcons.REMOVE,
                "gui.nep.clear_pending",
                "gui.nep.clear_pending.hint",
                () -> sendButton(ParticleCollisionControllerMenu.BUTTON_CLEAR_PENDING));
        addRightButton(
                3,
                NepIcons.DOWN,
                "gui.nep.clear_buffer",
                "gui.nep.clear_buffer.hint",
                () -> sendButton(ParticleCollisionControllerMenu.BUTTON_CLEAR_BUFFER));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        ParticleCollisionControllerBlockEntity controller = menu.controller();
        if (clearPending != null) {
            clearPending.active =
                    controller != null && (controller.hasPending() || controller.refusal() != Refusal.NONE);
        }
        tickButtons();
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, Component.translatable(KEY + "title"), 8, 7, TITLE_ON_PANEL, false);
        graphics.text(
                font,
                Component.translatable(KEY + "input"),
                ParticleCollisionControllerMenu.INPUT_X,
                ParticleCollisionControllerMenu.INPUT_Y - 10,
                TITLE_ON_PANEL,
                false);
        graphics.text(
                font,
                Component.translatable(KEY + "output"),
                ParticleCollisionControllerMenu.OUTPUT_X,
                ParticleCollisionControllerMenu.OUTPUT_Y - 10,
                TITLE_ON_PANEL,
                false);
        graphics.text(
                font,
                Component.translatable("container.inventory"),
                ParticleCollisionControllerMenu.INV_X,
                ParticleCollisionControllerMenu.INV_Y - 10,
                TITLE_ON_PANEL,
                false);

        ParticleCollisionControllerBlockEntity controller = menu.controller();
        if (controller == null) {
            return;
        }

        Component status = statusLine(controller);
        graphics.text(font, status, READOUT_X, LINE_ONE_Y, statusColour(controller), false);
        drawTrailing(graphics, topSpeedLine(controller), LINE_ONE_Y, READOUT_X + font.width(status), READOUT_DIM);

        ItemStack making = controller.activeResult();
        Component craftingLabel = Component.translatable(KEY + "crafting");
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

        graphics.text(font, detailLine(controller), READOUT_X, LINE_THREE_Y, READOUT_DIM, false);
        drawProgressBar(graphics, controller.craftProgress(), BAR_TRACK, SPEED_FILL);
    }

    private static Component topSpeedLine(ParticleCollisionControllerBlockEntity controller) {
        if (controller.fieldHeld()) {
            return controller.speedLimit() == Long.MAX_VALUE
                    ? Component.translatable(KEY + "field_held")
                    : Component.translatable(KEY + "field_capped", controller.speedLimit());
        }
        return controller.topSpeed() > 0
                ? Component.translatable(KEY + "top_speed", controller.topSpeed())
                : Component.translatable(KEY + "unmeasured");
    }

    private static Component detailLine(ParticleCollisionControllerBlockEntity controller) {
        if (controller.phase() != Phase.IDLE) {
            return Component.translatable(
                    KEY + "speed", controller.speed(), ParticleEnergy.launchSpeedFor(controller.requiredSpeed()));
        }
        return Component.translatable(
                KEY + "links", controller.linkCount(), ParticleCollisionControllerBlockEntity.LINKS);
    }

    private static Component statusLine(ParticleCollisionControllerBlockEntity controller) {
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
        return Component.translatable(
                switch (controller.phase()) {
                    case IDLE -> KEY + "idle";
                    case LAUNCHING -> KEY + "phase.launching";
                    case ACCELERATING -> KEY + "phase.accelerating";
                    case COLLIDING -> KEY + "phase.colliding";
                });
    }

    @Nullable
    private static String faultKey(ParticleCollisionControllerBlockEntity controller) {
        String stall = stallKey(controller.stall());
        return stall != null ? stall : refusalKey(controller.refusal());
    }

    @Nullable
    private static String stallKey(Stall stall) {
        return switch (stall) {
            case NONE -> null;
            case INGREDIENTS -> KEY + "stall.ingredients";
            case OUTPUT_FULL -> KEY + "output_blocked";
            case NOT_ACCELERATING -> KEY + "stall.not_accelerating";
            case PARTNER_BUSY -> KEY + "stall.partner_busy";
            case ACCELERATOR_BUSY -> KEY + "stall.accelerator_busy";
            case UNLOADED -> KEY + "stall.unloaded";
            case NO_RING -> KEY + "stall.no_ring";
            case NO_COLLISION -> KEY + "stall.no_collision";
            case PARTICLE_LOST -> KEY + "stall.particle_lost";
            case TOO_SLOW -> KEY + "stall.too_slow";
            case MISSING_ACCELERATOR -> KEY + "stall.missing_accelerator";
            case NO_LINKS -> KEY + "stall.no_links";
            case NO_RECIPE -> KEY + "stall.no_recipe";
        };
    }

    @Nullable
    private static String refusalKey(Refusal refusal) {
        return switch (refusal) {
            case NONE -> null;
            case NOT_A_COLLISION_PATTERN -> KEY + "refused.not_a_pattern";
            case NO_ITEM_OUTPUT -> KEY + "refused.no_item_output";
            case UNKNOWN_RECIPE -> KEY + "refused.unknown_recipe";
            case MIXED_RECIPES -> KEY + "refused.mixed_recipes";
            case ITEMS_ONLY -> KEY + "refused.items_only";
            case TOO_MANY_INPUTS -> KEY + "refused.too_many_inputs";
            case TOO_FAST -> KEY + "refused.too_fast";
            case BUFFER_FULL -> KEY + "refused.buffer_full";
        };
    }

    private static List<Component> statusHint(ParticleCollisionControllerBlockEntity controller) {
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

    private static int statusColour(ParticleCollisionControllerBlockEntity controller) {
        if (faultKey(controller) != null
                || controller.isOutputBlocked()
                || controller.hasPowerFault()
                || !controller.missingInputs().isEmpty()) {
            return READOUT_WARN;
        }
        return controller.phase() == Phase.IDLE ? READOUT_DIM : READOUT_TEXT;
    }

    @Override
    protected void extractReadoutTooltips(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        ParticleCollisionControllerBlockEntity controller = menu.controller();
        if (controller == null || !within(mouseX, mouseY, READOUT_X, LINE_ONE_Y, BAR_WIDTH, 32)) {
            return;
        }
        List<Component> lines = new ArrayList<>();
        lines.add(statusLine(controller));
        lines.addAll(statusHint(controller));
        lines.add(Component.translatable(
                        KEY + "top_speed_tooltip",
                        controller.topSpeed() > 0
                                ? Component.literal(String.valueOf(controller.topSpeed()))
                                : Component.translatable(KEY + "unmeasured"))
                .withStyle(ChatFormatting.GRAY));
        if (controller.fieldHeld()) {
            lines.add(Component.translatable(KEY + "field_held_tooltip").withStyle(ChatFormatting.GRAY));
        }
        ItemStack making = controller.activeResult();
        if (!making.isEmpty()) {
            lines.add(
                    Component.translatable(KEY + "crafting_tooltip", making.getHoverName(), controller.requiredSpeed())
                            .withStyle(ChatFormatting.GRAY));
        }
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
