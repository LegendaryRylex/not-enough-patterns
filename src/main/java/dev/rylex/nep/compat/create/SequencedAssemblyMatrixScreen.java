package dev.rylex.nep.compat.create;

import appeng.api.stacks.GenericStack;
import dev.rylex.nep.Nep;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.client.EnergyGauge;
import dev.rylex.nep.machine.MachineFluidInput;
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
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

public class SequencedAssemblyMatrixScreen extends AssemblyReadoutScreen<SequencedAssemblyMatrixMenu> {

    private static final ResourceLocation TEXTURE = Nep.id("textures/gui/sequenced_assembly_matrix.png");
    private static final int READOUT_DIM = 0x8C8577;
    private static final int BAR_TRACK = 0x2E271C;
    private static final int BAR_FILL = 0xE8C64B;
    private static final int READOUT_RIGHT = SequencedAssemblyMatrixMenu.WIDTH - 12;

    private List<MakingEntry> makingEntries = List.of();

    public SequencedAssemblyMatrixScreen(SequencedAssemblyMatrixMenu menu, Inventory playerInv, Component title) {
        super(
                menu,
                playerInv,
                title,
                SequencedAssemblyMatrixMenu.TANK_X,
                SequencedAssemblyMatrixMenu.TANK_Y,
                SequencedAssemblyMatrixMenu.TANK_STEP,
                SequencedAssemblyMatrixMenu.TANK_WIDTH,
                SequencedAssemblyMatrixMenu.TANK_HEIGHT);
        this.imageWidth = SequencedAssemblyMatrixMenu.WIDTH;
        this.imageHeight = SequencedAssemblyMatrixMenu.HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        clearPending = addRightButton(
                1,
                "✗",
                "gui.nep.clear_pending",
                "gui.nep.clear_pending.hint",
                () -> sendButton(SequencedAssemblyMatrixMenu.BUTTON_CLEAR_PENDING));
        redstoneModeButton = addRightButton(
                2,
                currentMode().glyph(),
                "gui.nep.redstone_mode",
                "gui.nep.redstone_mode.hint",
                () -> sendButton(SequencedAssemblyMatrixMenu.BUTTON_REDSTONE_MODE));
        addRightButton(
                3,
                "↓",
                "gui.nep.clear_buffer",
                "gui.nep.clear_buffer.hint",
                () -> sendButton(SequencedAssemblyMatrixMenu.BUTTON_CLEAR_BUFFER));
    }

    private RedstoneMode currentMode() {
        SequencedAssemblyMatrixBlockEntity matrix = menu.matrix();
        return matrix != null ? matrix.redstoneMode() : RedstoneMode.OUTPUT;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        SequencedAssemblyMatrixBlockEntity matrix = menu.matrix();
        if (clearPending != null) {
            clearPending.active = matrix != null && matrix.hasPending();
        }
        updateRedstoneButton(currentMode());
        List<ItemStack> making = matrix == null ? List.of() : matrix.makingNow();
        List<MakingEntry> entries = new ArrayList<>(making.size());
        for (ItemStack stack : making) {
            entries.add(new MakingEntry(
                    stack.copyWithCount(1),
                    Component.translatable("gui.nep.sequenced_assembly_matrix.count", stack.getCount()),
                    stack.getHoverName()));
        }
        makingEntries = entries;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0
                && overTanks(mouseX, mouseY)
                && MachineFluidInput.canFillFrom(menu.getCarried())
                && minecraft != null
                && minecraft.gameMode != null) {
            sendButton(SequencedAssemblyMatrixMenu.BUTTON_FILL_TANK);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private FluidStack tankFluid(int tank) {
        SequencedAssemblyMatrixBlockEntity matrix = menu.matrix();
        return matrix == null ? FluidStack.EMPTY : matrix.getFluids().getFluidInTank(tank);
    }

    private int tankCapacity() {
        SequencedAssemblyMatrixBlockEntity matrix = menu.matrix();
        return matrix == null
                ? NepConfig.createSequencedAssemblyMatrixTankCapacity()
                : Math.max(1, matrix.getFluids().getTankCapacity(0));
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        SequencedAssemblyMatrixBlockEntity matrix = menu.matrix();
        if (matrix != null) {
            drawTanks(graphics, this::tankFluid, tankCapacity());
            if (SequencedAssemblyMatrixBlockEntity.NEW_AGE_LOADED) {
                EnergyGauge.draw(
                        graphics,
                        leftPos + SequencedAssemblyMatrixMenu.ENERGY_X,
                        topPos + SequencedAssemblyMatrixMenu.ENERGY_Y,
                        matrix.energyStored(),
                        matrix.energyCapacity());
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        clearHotspots();
        graphics.drawString(
                font, Component.translatable("gui.nep.sequenced_assembly_matrix.title"), 8, 6, PANEL_TEXT, false);

        SequencedAssemblyMatrixBlockEntity matrix = menu.matrix();
        int flags = matrix == null ? 0 : matrix.statusFlags();

        Component status = MatrixReadout.status(flags, makingEntries.isEmpty());
        graphics.drawString(font, status, 12, 21, MatrixReadout.faulted(flags) ? READOUT_FAULT : READOUT_TEXT, false);
        addStatusHotspot(matrix, flags, status);
        drawTrailing(graphics, meDrainText(matrix), 21, 12 + font.width(status), READOUT_DIM);
        drawMakingRow(
                graphics,
                Component.translatable("gui.nep.sequenced_assembly_matrix.making"),
                Component.translatable("gui.nep.sequenced_assembly_matrix.making.idle"),
                30,
                READOUT_RIGHT,
                makingEntries);
        Component speed = speedText(matrix);
        graphics.drawString(font, speed, 12, 46, READOUT_DIM, false);
        drawTrailing(graphics, stressText(matrix), 46, 12 + font.width(speed), READOUT_DIM);
        drawProgress(graphics, matrix);
        if (matrix != null) {
            addTankHotspots(this::tankFluid, tankCapacity(), "gui.nep.sequenced_assembly_matrix");
            if (SequencedAssemblyMatrixBlockEntity.NEW_AGE_LOADED) {
                addHotspot(
                        SequencedAssemblyMatrixMenu.ENERGY_X,
                        SequencedAssemblyMatrixMenu.ENERGY_Y,
                        EnergyGauge.WIDTH,
                        EnergyGauge.HEIGHT,
                        () -> List.of(
                                Component.translatable("gui.nep.sequenced_assembly_matrix.energy_buffer"),
                                MatrixReadout.energy(matrix.energyStored(), matrix.energyCapacity())
                                        .copy()
                                        .withStyle(ChatFormatting.GRAY)));
            }
        }

        graphics.drawString(
                font,
                Component.translatable("gui.nep.sequenced_assembly_matrix.input"),
                SequencedAssemblyMatrixMenu.INPUT_X,
                SequencedAssemblyMatrixMenu.INPUT_Y - 10,
                PANEL_TEXT,
                false);
        graphics.drawString(
                font,
                Component.translatable("gui.nep.sequenced_assembly_matrix.output"),
                SequencedAssemblyMatrixMenu.OUTPUT_X,
                SequencedAssemblyMatrixMenu.OUTPUT_Y - 10,
                PANEL_TEXT,
                false);
        graphics.drawString(
                font,
                Component.translatable("container.inventory"),
                SequencedAssemblyMatrixMenu.INV_X,
                SequencedAssemblyMatrixMenu.INV_Y - 10,
                PANEL_TEXT,
                false);
    }

    private void addStatusHotspot(@Nullable SequencedAssemblyMatrixBlockEntity matrix, int flags, Component status) {
        if (matrix == null) {
            return;
        }
        boolean blocked = (flags & SequencedAssemblyMatrixBlockEntity.FLAG_OUTPUT_BLOCKED) != 0;
        boolean starved = (flags & SequencedAssemblyMatrixBlockEntity.FLAG_STARVED) != 0;
        boolean noEnergy = (flags & SequencedAssemblyMatrixBlockEntity.FLAG_NO_ENERGY) != 0;
        if (!blocked && !starved && !noEnergy) {
            return;
        }
        addHotspot(12, 21, font.width(status), font.lineHeight, () -> {
            List<Component> lines = new ArrayList<>();
            if (blocked) {
                lines.add(Component.translatable("gui.nep.sequenced_assembly_matrix.status.output_blocked.hint")
                        .withStyle(ChatFormatting.GRAY));
            }
            if (noEnergy) {
                lines.add(Component.translatable(
                                "gui.nep.sequenced_assembly_matrix.status.no_energy.hint",
                                MatrixReadout.count(matrix.energyStored()),
                                MatrixReadout.count(matrix.energyPending()))
                        .withStyle(ChatFormatting.GRAY));
            }
            if (starved) {
                lines.add(Component.translatable("gui.nep.sequenced_assembly_matrix.missing")
                        .withStyle(ChatFormatting.GRAY));
                for (GenericStack stack : matrix.missingInputs()) {
                    lines.add(MissingStacks.describe(stack).withStyle(ChatFormatting.RED));
                }
                lines.add(Component.translatable("gui.nep.sequenced_assembly_matrix.missing.hint")
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
            return lines;
        });
    }

    private void drawTrailing(GuiGraphics graphics, Component text, int y, int occupiedUntil, int color) {
        int x = READOUT_RIGHT - font.width(text);
        if (x < occupiedUntil + 6) {
            return;
        }
        graphics.drawString(font, text, x, y, color, false);
    }

    private static Component meDrainText(@Nullable SequencedAssemblyMatrixBlockEntity matrix) {
        return MatrixReadout.meDrain(
                matrix == null ? NepConfig.createSequencedAssemblyMatrixIdleMeDrain() : matrix.meDrain());
    }

    private static Component speedText(@Nullable SequencedAssemblyMatrixBlockEntity matrix) {
        return MatrixReadout.speed(
                matrix == null ? 0.0F : matrix.getSpeed(), matrix == null ? 0.0F : matrix.operatingFraction());
    }

    private static Component stressText(@Nullable SequencedAssemblyMatrixBlockEntity matrix) {
        return MatrixReadout.stress(
                matrix == null ? 0 : matrix.stressDraw(), matrix == null ? 0.0F : matrix.processingFraction());
    }

    private void drawProgress(GuiGraphics graphics, @Nullable SequencedAssemblyMatrixBlockEntity matrix) {
        int left = 12;
        int right = READOUT_RIGHT;
        graphics.fill(left, 55, right, 57, 0xFF000000 | BAR_TRACK);
        float progress = matrix == null ? 0.0F : matrix.craftProgress();
        if (progress <= 0.0F) {
            return;
        }
        int filled = Math.max(1, Math.round((right - left) * Math.min(1.0F, progress)));
        graphics.fill(left, 55, left + filled, 57, 0xFF000000 | BAR_FILL);
    }
}
