package dev.rylex.nep.compat.create;

import dev.rylex.nep.machine.RedstoneMode;
import dev.rylex.nep.menu.MachineMenu;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

abstract class AssemblyReadoutScreen<T extends MachineMenu> extends AbstractContainerScreen<T> {

    protected static final int READOUT_TEXT = 0xE0D6BC;
    protected static final int READOUT_FAULT = 0xFF6B6B;
    protected static final int PANEL_TEXT = 0x3A2E1C;

    protected record MakingEntry(ItemStack icon, Component count, Component hover) {}

    private record Hotspot(int x, int y, int w, int h, Supplier<List<Component>> lines) {}

    private final List<Hotspot> hotspots = new ArrayList<>();
    private final List<ReadoutButton> readoutButtons = new ArrayList<>();
    private final int tankX;
    private final int tankY;
    private final int tankStep;
    private final int tankWidth;
    private final int tankHeight;

    @Nullable
    protected ReadoutButton clearPending;

    @Nullable
    protected ReadoutButton redstoneModeButton;

    @Nullable
    private RedstoneMode lastTooltipMode;

    protected AssemblyReadoutScreen(
            T menu,
            Inventory playerInv,
            Component title,
            int tankX,
            int tankY,
            int tankStep,
            int tankWidth,
            int tankHeight) {
        super(menu, playerInv, title);
        this.tankX = tankX;
        this.tankY = tankY;
        this.tankStep = tankStep;
        this.tankWidth = tankWidth;
        this.tankHeight = tankHeight;
    }

    @Override
    protected void init() {
        super.init();
        readoutButtons.clear();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        for (ReadoutButton button : readoutButtons) {
            button.tick();
        }
    }

    protected void sendButton(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    protected ReadoutButton addRightButton(int index, String glyph, String nameKey, String hintKey, Runnable action) {
        ReadoutButton button = addRenderableWidget(new ReadoutButton(
                leftPos + imageWidth - index * ReadoutButton.SIZE - 4 - 4 * index,
                topPos + 3,
                glyph,
                Component.translatable(nameKey),
                Component.translatable(hintKey).withStyle(ChatFormatting.GRAY),
                action));
        readoutButtons.add(button);
        return button;
    }

    protected void updateRedstoneButton(RedstoneMode mode) {
        if (redstoneModeButton == null || mode == lastTooltipMode) {
            return;
        }
        lastTooltipMode = mode;
        redstoneModeButton.setGlyph(mode.glyph());
        redstoneModeButton.setTooltip(Tooltip.create(Component.empty()
                .append(Component.translatable("gui.nep.redstone_mode"))
                .append("\n")
                .append(Component.translatable(mode.key()).withStyle(ChatFormatting.YELLOW))
                .append("\n")
                .append(Component.translatable("gui.nep.redstone_mode.hint").withStyle(ChatFormatting.GRAY))));
    }

    protected void clearHotspots() {
        hotspots.clear();
    }

    protected void addHotspot(int x, int y, int w, int h, Supplier<List<Component>> lines) {
        hotspots.add(new Hotspot(x, y, w, h, lines));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        int relX = mouseX - leftPos;
        int relY = mouseY - topPos;
        for (Hotspot spot : hotspots) {
            if (relX >= spot.x() && relX < spot.x() + spot.w() && relY >= spot.y() && relY < spot.y() + spot.h()) {
                graphics.renderComponentTooltip(font, spot.lines().get(), mouseX, mouseY);
                break;
            }
        }
    }

    protected boolean overTanks(double mouseX, double mouseY) {
        double relX = mouseX - leftPos;
        double relY = mouseY - topPos;
        int x1 = tankX + (AssemblyFluidBuffer.TANKS - 1) * tankStep + tankWidth;
        return relX >= tankX && relX < x1 && relY >= tankY && relY < tankY + tankHeight;
    }

    protected void drawTanks(GuiGraphics graphics, IntFunction<FluidStack> tank, int capacity) {
        for (int t = 0; t < AssemblyFluidBuffer.TANKS; t++) {
            FluidGauge.draw(
                    graphics,
                    tank.apply(t),
                    leftPos + tankX + t * tankStep,
                    topPos + tankY + tankHeight,
                    tankWidth,
                    tankHeight,
                    capacity);
        }
    }

    protected void addTankHotspots(IntFunction<FluidStack> tank, int capacity, String langPrefix) {
        for (int t = 0; t < AssemblyFluidBuffer.TANKS; t++) {
            FluidStack held = tank.apply(t);
            addHotspot(tankX + t * tankStep, tankY, tankWidth, tankHeight, () -> {
                List<Component> lines = new ArrayList<>();
                if (held.isEmpty()) {
                    lines.add(Component.translatable(langPrefix + ".tank.empty").withStyle(ChatFormatting.GRAY));
                } else {
                    lines.add(held.getHoverName());
                    lines.add(Component.translatable(langPrefix + ".tank.amount", held.getAmount(), capacity)
                            .withStyle(ChatFormatting.GRAY));
                }
                return lines;
            });
        }
    }

    protected void drawMakingRow(
            GuiGraphics graphics,
            Component label,
            Component idleText,
            int iconY,
            int rightLimit,
            List<MakingEntry> entries) {
        int textY = iconY + 4;
        graphics.drawString(font, label, 12, textY, READOUT_TEXT, false);
        int x = 12 + font.width(label) + 4;
        if (entries.isEmpty()) {
            graphics.drawString(font, idleText, x, textY, READOUT_TEXT, false);
            return;
        }
        for (MakingEntry entry : entries) {
            int entryWidth = 17 + font.width(entry.count());
            if (x + entryWidth > rightLimit) {
                graphics.drawString(font, "…", x, textY, READOUT_TEXT, false);
                break;
            }
            graphics.renderItem(entry.icon(), x, iconY);
            graphics.drawString(font, entry.count(), x + 17, textY, READOUT_TEXT, false);
            addHotspot(x, iconY, 16, 16, () -> List.of(entry.hover()));
            x += entryWidth + 6;
        }
    }
}
