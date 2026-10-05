package dev.rylex.nep.client;

import dev.rylex.nep.Nep;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.decoder.DecoderModule;
import dev.rylex.nep.decoder.DecoderStatus;
import dev.rylex.nep.decoder.PatternDecoderBlockEntity;
import dev.rylex.nep.decoder.PatternDecoderMenu;
import dev.rylex.nep.decoder.PatternDecoding;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class PatternDecoderScreen extends MatrixScreen<PatternDecoderMenu> {

    private static final Identifier CHEST = Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");

    private static final int CHEST_HEADER = 17;
    private static final float CHEST_INVENTORY_V = 126.0F;
    private static final int CHEST_INVENTORY_HEIGHT = 96;
    private static final int BACKGROUND = 0xFFC6C6C6;
    private static final int LABEL = 0xFF404040;
    private static final int ACTIVE_FILL = 0xFF3F9A3F;
    private static final int MISSING_FILL = 0xFF000000;
    private static final int GHOST_FADE = 0xA08B8B8B;
    private static final int CROSS = 0xFFD8302A;
    private static final int SLOT_SIZE = 16;
    private static final int LAMP_SIZE = 5;
    private static final int LAMP_Y = 7;
    private static final int TOOLTIP_WIDTH = 180;
    private static final Component TITLE = Component.translatable("gui.nep.pattern_decoder.title");

    private enum ModuleState {
        ACTIVE("gui.nep.pattern_decoder.state.active", ChatFormatting.GREEN),
        INACTIVE("gui.nep.pattern_decoder.state.inactive", ChatFormatting.YELLOW),
        DISABLED("gui.nep.pattern_decoder.state.disabled", ChatFormatting.RED),
        NOT_INSTALLED("gui.nep.pattern_decoder.state.not_installed", ChatFormatting.DARK_RED);

        private final String key;
        private final ChatFormatting colour;

        ModuleState(String key, ChatFormatting colour) {
            this.key = key;
            this.colour = colour;
        }
    }

    public PatternDecoderScreen(PatternDecoderMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title, PatternDecoderMenu.WIDTH, PatternDecoderMenu.HEIGHT);
        this.inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected Identifier texture() {
        return CHEST;
    }

    @Override
    protected void init() {
        super.init();
        resetButtons();
        addHelpButton("nep/pattern-decoder.md");
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        tickButtons();
    }

    private ModuleState state(DecoderModule module) {
        if (!module.installed()) {
            return ModuleState.NOT_INSTALLED;
        }
        if (!PatternDecoding.required(module)) {
            return ModuleState.DISABLED;
        }
        return !menu.moduleIn(module).isEmpty() && menu.status() == DecoderStatus.ONLINE
                ? ModuleState.ACTIVE
                : ModuleState.INACTIVE;
    }

    @Override
    protected void extractPanel(GuiGraphicsExtractor graphics) {
        int top = CHEST_HEADER + PatternDecoderMenu.ROWS * 18;
        graphics.blit(RenderPipelines.GUI_TEXTURED, CHEST, leftPos, topPos, 0.0F, 0.0F, imageWidth, top, 256, 256);
        int gap = PatternDecoderMenu.INVENTORY_GAP;
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                CHEST,
                leftPos,
                topPos + top,
                0.0F,
                CHEST_HEADER - gap,
                imageWidth,
                gap,
                256,
                256);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                CHEST,
                leftPos,
                topPos + top + gap,
                0.0F,
                CHEST_INVENTORY_V,
                imageWidth,
                CHEST_INVENTORY_HEIGHT,
                256,
                256);
        for (int slot = PatternDecoderBlockEntity.SLOTS;
                slot < PatternDecoderMenu.ROWS * PatternDecoderMenu.COLUMNS;
                slot++) {
            int x = leftPos + PatternDecoderMenu.slotX(slot) - 1;
            int y = topPos + PatternDecoderMenu.slotY(slot) - 1;
            graphics.fill(x, y, x + 18, y + 18, BACKGROUND);
        }
        for (DecoderModule module : DecoderModule.values()) {
            int x = leftPos + PatternDecoderMenu.slotX(module.ordinal());
            int y = topPos + PatternDecoderMenu.slotY(module.ordinal());
            ModuleState state = state(module);
            if (state == ModuleState.NOT_INSTALLED) {
                graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, MISSING_FILL);
                continue;
            }
            if (state == ModuleState.ACTIVE) {
                graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, ACTIVE_FILL);
            }
            if (menu.moduleIn(module).isEmpty()) {
                graphics.fakeItem(ghostOf(module), x, y);
                graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, GHOST_FADE);
            }
        }
    }

    private static ItemStack ghostOf(DecoderModule module) {
        return new ItemStack(
                BuiltInRegistries.ITEM.getOptional(Nep.id(module.itemPath())).orElse(Items.AIR));
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, TITLE, titleLabelX, titleLabelY, LABEL, false);
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, LABEL, false);
        int lampX = lampX();
        graphics.fill(lampX - 1, LAMP_Y - 1, lampX + LAMP_SIZE + 1, LAMP_Y + LAMP_SIZE + 1, 0xFF373737);
        graphics.fill(lampX, LAMP_Y, lampX + LAMP_SIZE, LAMP_Y + LAMP_SIZE, lampColour(menu.status()));
    }

    @Override
    protected void extractSlots(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractSlots(graphics, mouseX, mouseY);
        for (DecoderModule module : DecoderModule.values()) {
            ModuleState state = state(module);
            if (state == ModuleState.DISABLED || state == ModuleState.NOT_INSTALLED) {
                drawCross(
                        graphics,
                        PatternDecoderMenu.slotX(module.ordinal()),
                        PatternDecoderMenu.slotY(module.ordinal()));
            }
        }
    }

    private int lampX() {
        return titleLabelX + font.width(TITLE) + 4;
    }

    private static int lampColour(DecoderStatus status) {
        return switch (status) {
            case ONLINE -> 0xFF4CD94C;
            case OFFLINE, NO_CHANNEL -> 0xFFE0A030;
            case NO_NETWORK -> 0xFFD8302A;
        };
    }

    private static void drawCross(GuiGraphicsExtractor graphics, int x, int y) {
        for (int step = 1; step < SLOT_SIZE - 1; step++) {
            graphics.fill(x + step, y + step, x + step + 2, y + step + 1, CROSS);
            graphics.fill(x + SLOT_SIZE - step - 2, y + step, x + SLOT_SIZE - step, y + step + 1, CROSS);
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (hoveredModule() == null) {
            super.extractTooltip(graphics, mouseX, mouseY);
        }
    }

    private DecoderModule hoveredModule() {
        if (hoveredSlot == null || hoveredSlot.index >= PatternDecoderBlockEntity.SLOTS) {
            return null;
        }
        return DecoderModule.values()[hoveredSlot.index];
    }

    @Override
    protected void extractReadoutTooltips(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        DecoderModule module = hoveredModule();
        if (module != null) {
            graphics.setTooltipForNextFrame(font, moduleTooltip(module), mouseX, mouseY);
            return;
        }
        if (within(mouseX, mouseY, lampX() - 1, LAMP_Y - 1, LAMP_SIZE + 2, LAMP_SIZE + 2)) {
            graphics.setTooltipForNextFrame(
                    font, Component.translatable(menu.status().key()), mouseX, mouseY);
        }
    }

    private List<FormattedCharSequence> moduleTooltip(DecoderModule module) {
        ModuleState state = state(module);
        List<FormattedCharSequence> lines = new ArrayList<>();
        lines.add(Component.translatable("item.nep." + module.itemPath()).getVisualOrderText());
        lines.add(Component.translatable(state.key).withStyle(state.colour).getVisualOrderText());
        lines.addAll(font.split(detail(module, state).withStyle(ChatFormatting.GRAY), TOOLTIP_WIDTH));
        return lines;
    }

    private MutableComponent detail(DecoderModule module, ModuleState state) {
        return switch (state) {
            case ACTIVE -> Component.translatable("gui.nep.pattern_decoder.detail.active", module.modName());
            case DISABLED ->
                NepConfig.requireDecoder()
                        ? Component.translatable("gui.nep.pattern_decoder.detail.ungated", module.modName())
                        : Component.translatable("gui.nep.pattern_decoder.detail.not_required");
            case NOT_INSTALLED -> Component.translatable("gui.nep.pattern_decoder.detail.not_installed");
            case INACTIVE -> inactiveDetail(module);
        };
    }

    private MutableComponent inactiveDetail(DecoderModule module) {
        if (!menu.moduleIn(module).isEmpty()) {
            return Component.translatable("gui.nep.pattern_decoder.detail.offline");
        }
        if (PatternDecoding.allows(menu.networkModules(), module)) {
            return Component.translatable("gui.nep.pattern_decoder.detail.shared", module.modName());
        }
        return Component.translatable("gui.nep.pattern_decoder.detail.empty", module.modName());
    }
}
