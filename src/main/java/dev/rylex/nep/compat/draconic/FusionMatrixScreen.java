package dev.rylex.nep.compat.draconic;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import com.brandon3055.brandonscore.api.TechLevel;
import dev.rylex.nep.Nep;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.NepIcons;
import dev.rylex.nep.client.EnergyGauge;
import dev.rylex.nep.client.MatrixScreen;
import dev.rylex.nep.client.ReadoutButton;
import dev.rylex.nep.client.RetainedHighlight;
import dev.rylex.nep.pattern.RetainedInputs;
import dev.rylex.nep.util.MissingStacks;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class FusionMatrixScreen extends MatrixScreen<FusionMatrixMenu> {

    private static final ResourceLocation TEXTURE = Nep.id("textures/gui/fusion_matrix.png");

    private static final int TITLE_ON_PANEL = 0x3A2E1C;
    private static final int READOUT_TEXT = 0xF2E8D0;
    private static final int READOUT_DIM = 0xC8A882;

    private static final int BAR_TRACK = 0x3A1A0C;
    private static final int CHARGE_FILL = 0xC64A0C;
    private static final int CRAFT_FILL = 0xFFB020;

    private static final int GAUGE_X = 152;
    private static final int GAUGE_Y = 73;

    private static final int UPGRADE_TIP_X = FusionMatrixMenu.UPGRADE_X;
    private static final int UPGRADE_TIP_Y = FusionMatrixMenu.UPGRADE_Y;
    private static final int UPGRADE_TIP_SIZE = 16;

    @Nullable
    private ReadoutButton clearPending;

    private Set<AEItemKey> retainedKeys = Set.of();

    public FusionMatrixScreen(FusionMatrixMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth = FusionMatrixMenu.WIDTH;
        this.imageHeight = FusionMatrixMenu.HEIGHT;
    }

    @Override
    protected ResourceLocation texture() {
        return TEXTURE;
    }

    @Override
    protected void init() {
        super.init();
        resetButtons();
        addHelpButton("nep/draconicevolution/fusion-matrix.md");
        clearPending = addRightButton(
                2,
                NepIcons.REMOVE,
                "gui.nep.clear_pending",
                "gui.nep.clear_pending.hint",
                () -> sendButton(FusionMatrixMenu.BUTTON_CLEAR_PENDING));
        addRightButton(
                3,
                NepIcons.DOWN,
                "gui.nep.clear_buffer",
                "gui.nep.clear_buffer.hint",
                () -> sendButton(FusionMatrixMenu.BUTTON_CLEAR_BUFFER));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        FusionMatrixBlockEntity matrix = menu.matrix();
        retainedKeys = matrix == null ? Set.of() : matrix.retainedInputKeys();
        if (clearPending != null) {
            clearPending.active =
                    matrix != null && (matrix.hasPending() || matrix.refusal() != FusionMatrixBlockEntity.Refusal.NONE);
        }
        tickButtons();
    }

    @Override
    protected void renderSlot(GuiGraphics graphics, Slot slot) {
        super.renderSlot(graphics, slot);
        if (isRetained(slot)) {
            RetainedHighlight.draw(graphics, slot.x, slot.y);
        }
    }

    private boolean isRetained(@Nullable Slot slot) {
        if (slot == null || !menu.isInputSlot(slot) || retainedKeys.isEmpty() || !slot.hasItem()) {
            return false;
        }
        AEItemKey key = AEItemKey.of(slot.getItem());
        return key != null && retainedKeys.contains(key);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, Component.translatable("gui.nep.fusion_matrix.title"), 8, 7, TITLE_ON_PANEL, false);
        graphics.drawString(
                font,
                Component.translatable("gui.nep.fusion_matrix.input"),
                FusionMatrixMenu.INPUT_X,
                FusionMatrixMenu.INPUT_Y - 10,
                TITLE_ON_PANEL,
                false);
        graphics.drawString(
                font,
                Component.translatable("gui.nep.fusion_matrix.output"),
                FusionMatrixMenu.OUTPUT_X,
                FusionMatrixMenu.OUTPUT_Y - 10,
                TITLE_ON_PANEL,
                false);
        graphics.drawString(
                font,
                Component.translatable("container.inventory"),
                FusionMatrixMenu.INV_X,
                FusionMatrixMenu.INV_Y - 10,
                TITLE_ON_PANEL,
                false);

        FusionMatrixBlockEntity matrix = menu.matrix();
        if (matrix == null) {
            return;
        }

        Component status = statusLine(matrix);
        graphics.drawString(font, status, READOUT_X, LINE_ONE_Y, statusColour(matrix), false);
        drawTrailing(graphics, coresLine(matrix), LINE_ONE_Y, READOUT_X + font.width(status), coresColour(matrix));

        ItemStack making = matrix.activeResult();
        Component craftingLabel = Component.translatable("gui.nep.fusion_matrix.crafting");
        graphics.drawString(font, craftingLabel, READOUT_X, LINE_TWO_Y, READOUT_DIM, false);
        int craftingX = READOUT_X + font.width(craftingLabel) + 4;
        long queued = matrix.pendingJobs();
        Component queuedText =
                queued > 0 ? Component.translatable("gui.nep.fusion_matrix.queued", queued) : Component.empty();
        int nameLimit = READOUT_RIGHT - craftingX - (queued > 0 ? font.width(queuedText) + 6 : 0);
        Component craftingName = making.isEmpty()
                ? Component.translatable("gui.nep.fusion_matrix.crafting.idle")
                : making.getHoverName();
        String craftingText = trim(craftingName, nameLimit);
        graphics.drawString(
                font, craftingText, craftingX, LINE_TWO_Y, making.isEmpty() ? READOUT_DIM : READOUT_TEXT, false);
        if (queued > 0) {
            drawTrailing(graphics, queuedText, LINE_TWO_Y, craftingX + font.width(craftingText), READOUT_DIM);
        }

        Component energyLine = making.isEmpty()
                ? Component.translatable(
                        "gui.nep.fusion_matrix.buffer_line",
                        format(matrix.storedEnergy()),
                        format(matrix.energyCapacity()))
                : Component.translatable(
                        "gui.nep.fusion_matrix.charge_line",
                        format(matrix.chargedEnergy()),
                        format(matrix.chargeCost()));
        graphics.drawString(font, energyLine, READOUT_X, LINE_THREE_Y, READOUT_DIM, false);
        drawTrailing(
                graphics,
                Component.translatable("gui.nep.fusion_matrix.craft_time", matrix.craftTicks()),
                LINE_THREE_Y,
                READOUT_X + font.width(energyLine),
                READOUT_DIM);

        graphics.fill(BAR_X, BAR_Y, BAR_X + BAR_WIDTH, BAR_Y + BAR_HEIGHT, 0xFF000000 | BAR_TRACK);
        int filled = Math.round(BAR_WIDTH * clamp01(matrix.craftProgress()));
        if (filled > 0) {
            graphics.fill(
                    BAR_X,
                    BAR_Y,
                    BAR_X + filled,
                    BAR_Y + BAR_HEIGHT,
                    0xFF000000 | (matrix.isCharging() ? CHARGE_FILL : CRAFT_FILL));
        }

        EnergyGauge.draw(graphics, GAUGE_X, GAUGE_Y, matrix.storedEnergy(), matrix.energyCapacity());
    }

    private static Component coresLine(FusionMatrixBlockEntity matrix) {
        TechLevel tier = matrix.upgradeTier();
        return tier == null
                ? Component.translatable("gui.nep.fusion_matrix.cores.none")
                : Component.translatable("gui.nep.fusion_matrix.cores", tier.getDisplayName(), matrix.upgradeCount());
    }

    private static int coresColour(FusionMatrixBlockEntity matrix) {
        TechLevel tier = matrix.upgradeTier();
        if (tier == null) {
            return READOUT_DIM;
        }
        Integer colour = tier.getTextColour().getColor();
        return colour == null ? READOUT_TEXT : colour;
    }

    private static Component faultLine(FusionMatrixBlockEntity.Stall stall, FusionMatrixBlockEntity.Refusal refusal) {
        String key =
                switch (stall) {
                    case NONE -> null;
                    case INGREDIENTS -> "gui.nep.fusion_matrix.stall.ingredients";
                    case OUTPUT_FULL -> "gui.nep.fusion_matrix.output_blocked";
                    case TIER -> "gui.nep.fusion_matrix.stall.tier";
                    case NO_RECIPE -> "gui.nep.fusion_matrix.stall.no_recipe";
                };
        if (key != null) {
            return Component.translatable(key);
        }
        String refused =
                switch (refusal) {
                    case NONE -> null;
                    case NOT_A_FUSION_PATTERN -> "gui.nep.fusion_matrix.refused.not_a_fusion_pattern";
                    case NO_ITEM_OUTPUT -> "gui.nep.fusion_matrix.refused.no_item_output";
                    case UNKNOWN_RECIPE -> "gui.nep.fusion_matrix.refused.unknown_recipe";
                    case TIER_TOO_HIGH -> "gui.nep.fusion_matrix.refused.tier_too_high";
                    case ITEMS_ONLY -> "gui.nep.fusion_matrix.refused.items_only";
                    case TOO_MANY_INPUTS -> "gui.nep.fusion_matrix.refused.too_many_inputs";
                    case BUFFER_FULL -> "gui.nep.fusion_matrix.refused.buffer_full";
                };
        return refused == null ? Component.translatable("gui.nep.fusion_matrix.idle") : Component.translatable(refused);
    }

    private static List<Component> statusHint(FusionMatrixBlockEntity matrix) {
        if (matrix.hasPowerFault()) {
            return List.of(Component.translatable("gui.nep.fusion_matrix.stall.no_me_power.hint")
                    .withStyle(ChatFormatting.GRAY));
        }
        if (matrix.hasEnergyFault()) {
            return List.of(Component.translatable("gui.nep.fusion_matrix.stall.no_energy.hint")
                    .withStyle(ChatFormatting.GRAY));
        }
        String key =
                switch (matrix.stall()) {
                    case NONE -> null;
                    case INGREDIENTS -> "gui.nep.fusion_matrix.stall.ingredients.hint";
                    case OUTPUT_FULL -> "gui.nep.fusion_matrix.stall.output_full.hint";
                    case TIER -> "gui.nep.fusion_matrix.stall.tier.hint";
                    case NO_RECIPE -> "gui.nep.fusion_matrix.stall.no_recipe.hint";
                };
        if (key == null && matrix.refusal() != FusionMatrixBlockEntity.Refusal.NONE) {
            key = refusalHint(matrix.refusal());
        }
        if (key == null && matrix.isOutputBlocked()) {
            key = "gui.nep.fusion_matrix.stall.output_full.hint";
        }
        return key == null ? List.of() : List.of(Component.translatable(key).withStyle(ChatFormatting.GRAY));
    }

    private static String refusalHint(FusionMatrixBlockEntity.Refusal refusal) {
        return switch (refusal) {
            case TIER_TOO_HIGH -> "gui.nep.fusion_matrix.refused.tier_too_high.hint";
            case BUFFER_FULL -> "gui.nep.fusion_matrix.refused.buffer_full.hint";
            case TOO_MANY_INPUTS -> "gui.nep.fusion_matrix.refused.too_many_inputs.hint";
            default -> "gui.nep.fusion_matrix.refused.hint";
        };
    }

    private static Component statusLine(FusionMatrixBlockEntity matrix) {
        if (matrix.hasChannelFault()) {
            return Component.translatable("gui.nep.fusion_matrix.no_network_channels");
        }
        if (matrix.hasPowerFault()) {
            return Component.translatable("gui.nep.fusion_matrix.no_network_power");
        }
        if (matrix.hasEnergyFault()) {
            return Component.translatable("gui.nep.fusion_matrix.no_energy");
        }
        if (matrix.stall() != FusionMatrixBlockEntity.Stall.NONE
                || matrix.refusal() != FusionMatrixBlockEntity.Refusal.NONE) {
            return faultLine(matrix.stall(), matrix.refusal());
        }
        if (matrix.isOutputBlocked()) {
            return Component.translatable("gui.nep.fusion_matrix.output_blocked");
        }
        if (!matrix.missingInputs().isEmpty()) {
            return Component.translatable("gui.nep.fusion_matrix.missing");
        }
        if (matrix.activeResult().isEmpty()) {
            return Component.translatable("gui.nep.fusion_matrix.idle");
        }
        return Component.translatable(
                matrix.isCharging() ? "gui.nep.fusion_matrix.charging" : "gui.nep.fusion_matrix.fusing");
    }

    private static boolean faulted(FusionMatrixBlockEntity matrix) {
        return matrix.stall() != FusionMatrixBlockEntity.Stall.NONE
                || matrix.refusal() != FusionMatrixBlockEntity.Refusal.NONE
                || matrix.isOutputBlocked()
                || matrix.hasPowerFault()
                || matrix.hasEnergyFault()
                || !matrix.missingInputs().isEmpty();
    }

    private static int statusColour(FusionMatrixBlockEntity matrix) {
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
        FusionMatrixBlockEntity matrix = menu.matrix();
        if (matrix == null) {
            return;
        }
        if (menu.upgradeSlotEmpty()
                && within(mouseX, mouseY, UPGRADE_TIP_X, UPGRADE_TIP_Y, UPGRADE_TIP_SIZE, UPGRADE_TIP_SIZE)) {
            graphics.renderComponentTooltip(font, emptyUpgradeTooltip(), mouseX, mouseY);
            return;
        }
        if (within(mouseX, mouseY, GAUGE_X, GAUGE_Y, EnergyGauge.WIDTH, EnergyGauge.HEIGHT)) {
            List<Component> gauge = new ArrayList<>();
            gauge.add(Component.translatable("gui.nep.fusion_matrix.energy"));
            gauge.add(Component.translatable(
                            "gui.nep.fusion_matrix.charge",
                            format(matrix.storedEnergy()),
                            format(matrix.energyCapacity()))
                    .withStyle(ChatFormatting.GRAY));
            if (matrix.isHoldingSwappedEnergy()) {
                gauge.add(Component.translatable("gui.nep.fusion_matrix.energy_held")
                        .withStyle(ChatFormatting.YELLOW));
            }
            graphics.renderComponentTooltip(font, gauge, mouseX, mouseY);
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
            lines.add(Component.translatable("gui.nep.fusion_matrix.crafting_tooltip", making.getHoverName())
                    .withStyle(ChatFormatting.GRAY));
        }
        long queued = matrix.pendingJobs();
        if (queued > 0) {
            lines.add(Component.translatable("gui.nep.fusion_matrix.queued_tooltip", queued)
                    .withStyle(ChatFormatting.GRAY));
        }
        List<GenericStack> missing = matrix.missingInputs();
        if (!missing.isEmpty()) {
            lines.add(Component.translatable("gui.nep.fusion_matrix.missing"));
            for (GenericStack stack : missing) {
                lines.add(MissingStacks.describe(stack).withStyle(ChatFormatting.GRAY));
            }
        }
        graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
    }

    private List<Component> emptyUpgradeTooltip() {
        int max = FusionMatrixUpgrades.maxCores();
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("gui.nep.fusion_matrix.upgrade_core"));
        if (max <= 0) {
            lines.add(Component.translatable("gui.nep.fusion_matrix.upgrade_core.disabled")
                    .withStyle(ChatFormatting.GRAY));
            return lines;
        }
        lines.add(Component.translatable("gui.nep.fusion_matrix.upgrade_core.capacity", max)
                .withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable(
                        "gui.nep.fusion_matrix.upgrade_core.wyvern",
                        format(FusionMatrixUpgrades.capacityTarget(TechLevel.WYVERN)))
                .withStyle(TechLevel.WYVERN.getTextColour()));
        lines.add(Component.translatable(
                        "gui.nep.fusion_matrix.upgrade_core.draconic",
                        format(FusionMatrixUpgrades.capacityTarget(TechLevel.DRACONIC)),
                        NepConfig.draconicFusionMatrixDraconicCraftTimeReductionPercent())
                .withStyle(TechLevel.DRACONIC.getTextColour()));
        lines.add(Component.translatable(
                        "gui.nep.fusion_matrix.upgrade_core.chaotic",
                        format(FusionMatrixUpgrades.capacityTarget(TechLevel.CHAOTIC)),
                        NepConfig.draconicFusionMatrixChaoticEnergyCostReductionPercent(),
                        NepConfig.draconicFusionMatrixChaoticMinimumCraftTicks())
                .withStyle(TechLevel.CHAOTIC.getTextColour()));
        return lines;
    }

    @Override
    protected List<Component> getTooltipFromContainerItem(ItemStack stack) {
        List<Component> lines = new ArrayList<>(super.getTooltipFromContainerItem(stack));
        if (isRetained(hoveredSlot)) {
            lines.add(RetainedInputs.slotNote());
            return lines;
        }
        if (!menu.isUpgradeSlot(hoveredSlot)) {
            return lines;
        }
        TechLevel tier = FusionMatrixUpgrades.tierOf(stack);
        if (tier == null) {
            return lines;
        }
        int max = FusionMatrixUpgrades.maxCores();
        int cores = Math.min(stack.getCount(), max);
        lines.add(Component.translatable("gui.nep.fusion_matrix.upgrade_installed", cores, max)
                .withStyle(tier.getTextColour()));
        lines.add(Component.translatable(
                        "gui.nep.fusion_matrix.upgrade_buffer", format(FusionMatrixUpgrades.capacity(tier, cores)))
                .withStyle(ChatFormatting.GRAY));
        int craftTicks = FusionMatrixUpgrades.craftTicks(tier, cores);
        if (craftTicks != NepConfig.draconicFusionMatrixCraftTicks()) {
            lines.add(Component.translatable("gui.nep.fusion_matrix.upgrade_craft_time", craftTicks)
                    .withStyle(ChatFormatting.GRAY));
        }
        int discount = tier == TechLevel.CHAOTIC ? FusionMatrixUpgrades.energyCostReductionPercent(cores) : 0;
        if (discount > 0) {
            lines.add(Component.translatable("gui.nep.fusion_matrix.upgrade_energy_cost", discount)
                    .withStyle(ChatFormatting.GRAY));
        }
        return lines;
    }
}
