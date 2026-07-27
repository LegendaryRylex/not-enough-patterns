package dev.rylex.nep.compat.create;

import appeng.api.stacks.GenericStack;
import dev.rylex.nep.Nep;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.machine.MachineFluidInput;
import dev.rylex.nep.machine.RedstoneMode;
import dev.rylex.nep.util.MissingStacks;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

public class SequencedAssemblyControllerScreen extends AssemblyReadoutScreen<SequencedAssemblyControllerMenu> {

    private static final ResourceLocation TEXTURE = Nep.id("textures/gui/sequenced_assembly_controller.png");
    private static final int READOUT_OK = 0x66DD66;
    private static final int READOUT_RIGHT = 188;

    private SequencedAssemblyState state = SequencedAssemblyState.empty();
    private List<MakingEntry> makingEntries = List.of();

    public SequencedAssemblyControllerScreen(
            SequencedAssemblyControllerMenu menu, Inventory playerInv, Component title) {
        super(
                menu,
                playerInv,
                title,
                SequencedAssemblyControllerMenu.TANK_X,
                SequencedAssemblyControllerMenu.TANK_Y,
                SequencedAssemblyControllerMenu.TANK_STEP,
                SequencedAssemblyControllerMenu.TANK_WIDTH,
                SequencedAssemblyControllerMenu.TANK_HEIGHT);
        this.imageWidth = SequencedAssemblyControllerMenu.WIDTH;
        this.imageHeight = SequencedAssemblyControllerMenu.HEIGHT;
    }

    void acceptState(SequencedAssemblyState state) {
        this.state = state;
        List<MakingEntry> entries = new ArrayList<>(state.making().size());
        for (SequencedAssemblyState.Making m : state.making()) {
            entries.add(new MakingEntry(
                    m.output(),
                    Component.translatable("gui.nep.sequenced_assembly.count", m.count()),
                    m.output().getHoverName()));
        }
        this.makingEntries = entries;
    }

    @Override
    protected void init() {
        super.init();
        clearPending = addRightButton(
                1,
                "✗",
                "gui.nep.clear_pending",
                "gui.nep.clear_pending.hint",
                () -> sendButton(SequencedAssemblyControllerMenu.BUTTON_CLEAR_PENDING));
        redstoneModeButton = addRightButton(
                2,
                RedstoneMode.byOrdinal(state.redstoneMode()).glyph(),
                "gui.nep.redstone_mode",
                "gui.nep.redstone_mode.hint",
                () -> sendButton(SequencedAssemblyControllerMenu.BUTTON_REDSTONE_MODE));
        addRightButton(
                3,
                "↓",
                "gui.nep.clear_buffer",
                "gui.nep.clear_buffer.hint",
                () -> sendButton(SequencedAssemblyControllerMenu.BUTTON_CLEAR_BUFFER));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (clearPending != null) {
            clearPending.active = !state.making().isEmpty();
        }
        updateRedstoneButton(RedstoneMode.byOrdinal(state.redstoneMode()));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0
                && overTanks(mouseX, mouseY)
                && MachineFluidInput.canFillFrom(menu.getCarried())
                && minecraft != null
                && minecraft.gameMode != null) {
            sendButton(SequencedAssemblyControllerMenu.BUTTON_FILL_TANK);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private FluidStack stagedFluid(int tank) {
        List<FluidStack> staged = state.fluids();
        return tank < staged.size() ? staged.get(tank) : FluidStack.EMPTY;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(
                TEXTURE,
                leftPos,
                topPos,
                0,
                0,
                imageWidth,
                imageHeight,
                256,
                SequencedAssemblyControllerMenu.TEXTURE_HEIGHT);
        drawTanks(graphics, this::stagedFluid, NepConfig.createSequencedAssemblyTankCapacity());
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        clearHotspots();
        graphics.drawString(font, Component.translatable("gui.nep.sequenced_assembly.title"), 8, 6, PANEL_TEXT, false);

        boolean halted = state.halted();
        boolean blocked = state.outputBlocked();
        Component status;
        if (blocked) {
            status = Component.translatable("gui.nep.sequenced_assembly.status.output_blocked");
        } else if (halted) {
            status = Component.translatable("gui.nep.sequenced_assembly.status.halted");
        } else {
            status = switch (state.status()) {
                case 1 -> Component.translatable("gui.nep.sequenced_assembly.status.ready");
                case 2 -> Component.translatable("gui.nep.sequenced_assembly.status.problem");
                default -> Component.translatable("gui.nep.sequenced_assembly.status.none");
            };
        }
        graphics.drawString(font, status, 12, 22, halted || blocked ? READOUT_FAULT : READOUT_TEXT, false);
        if (halted || blocked) {
            addHotspot(12, 22, font.width(status), font.lineHeight, () -> statusTooltip(halted, blocked));
        }
        drawMakingRow(
                graphics,
                Component.translatable("gui.nep.sequenced_assembly.making"),
                Component.translatable("gui.nep.sequenced_assembly.making.idle"),
                31,
                READOUT_RIGHT,
                makingEntries);
        drawStations(graphics, 49);
        addTankHotspots(
                this::stagedFluid, NepConfig.createSequencedAssemblyTankCapacity(), "gui.nep.sequenced_assembly");

        graphics.drawString(
                font,
                Component.translatable("gui.nep.sequenced_assembly.input"),
                SequencedAssemblyControllerMenu.INGREDIENT_X,
                SequencedAssemblyControllerMenu.INGREDIENT_Y - 10,
                PANEL_TEXT,
                false);
        graphics.drawString(
                font,
                Component.translatable("gui.nep.sequenced_assembly.output"),
                SequencedAssemblyControllerMenu.OUTPUT_X,
                SequencedAssemblyControllerMenu.OUTPUT_Y - 10,
                PANEL_TEXT,
                false);
        graphics.drawString(
                font,
                Component.translatable("container.inventory"),
                SequencedAssemblyControllerMenu.INV_X,
                SequencedAssemblyControllerMenu.INV_Y - 10,
                PANEL_TEXT,
                false);
    }

    private void drawStations(GuiGraphics graphics, int iconY) {
        int textY = iconY + 4;
        Component label = Component.translatable("gui.nep.sequenced_assembly.stations");
        graphics.drawString(font, label, 12, textY, READOUT_TEXT, false);
        int x = 12 + font.width(label) + 4;
        if (state.stations().isEmpty()) {
            graphics.drawString(
                    font,
                    Component.translatable("gui.nep.sequenced_assembly.stations.none"),
                    x,
                    textY,
                    READOUT_TEXT,
                    false);
            return;
        }
        for (SequencedAssemblyState.Station station : state.stations()) {
            if (x + 16 > READOUT_RIGHT) {
                graphics.drawString(font, "…", x, textY, READOUT_TEXT, false);
                break;
            }
            ItemStack icon = Stations.icon(station.kind());
            if (icon.isEmpty()) {
                graphics.drawString(font, "?", x + 4, textY, READOUT_FAULT, false);
            } else {
                graphics.renderItem(icon, x, iconY);
            }
            int barColor = 0xFF000000 | (station.healthy() ? READOUT_OK : READOUT_FAULT);
            graphics.fill(x, iconY + 16, x + 16, iconY + 18, barColor);
            addHotspot(x, iconY, 16, 18, () -> stationTooltip(station));
            x += 20;
        }
    }

    private List<Component> stationTooltip(SequencedAssemblyState.Station station) {
        List<Component> lines = new ArrayList<>();
        BlockPos p = station.pos();
        String coords = Component.translatable("gui.nep.coords", p.getX(), p.getY(), p.getZ())
                .getString();
        switch (station.issue()) {
            case UNRECOGNIZED -> {
                lines.add(Component.translatable("gui.nep.sequenced_assembly.station.missing", coords)
                        .withStyle(ChatFormatting.RED));
                if (station.expected() != StationKind.UNKNOWN) {
                    lines.add(Component.translatable(
                                    "gui.nep.sequenced_assembly.station.expected",
                                    Stations.icon(station.expected()).getHoverName())
                            .withStyle(ChatFormatting.YELLOW));
                }
            }
            case WRONG_FACING -> {
                lines.add(stationTitle(station, coords).withStyle(ChatFormatting.RED));
                lines.add(Component.translatable("gui.nep.sequenced_assembly.station.facing")
                        .withStyle(ChatFormatting.RED));
            }
            case UNPOWERED -> {
                lines.add(stationTitle(station, coords).withStyle(ChatFormatting.RED));
                lines.add(Component.translatable("gui.nep.sequenced_assembly.station.unpowered")
                        .withStyle(ChatFormatting.RED));
            }
            default -> lines.add(stationTitle(station, coords).withStyle(ChatFormatting.GREEN));
        }
        return lines;
    }

    private List<Component> statusTooltip(boolean halted, boolean blocked) {
        List<Component> lines = new ArrayList<>();
        if (blocked) {
            lines.add(Component.translatable("gui.nep.sequenced_assembly.status.output_blocked.hint")
                    .withStyle(ChatFormatting.GRAY));
        }
        if (halted) {
            lines.add(Component.translatable("gui.nep.sequenced_assembly.status.halted.missing")
                    .withStyle(ChatFormatting.GRAY));
            for (GenericStack stack : state.missing()) {
                lines.add(MissingStacks.describe(stack).withStyle(ChatFormatting.RED));
            }
        }
        return lines;
    }

    private MutableComponent stationTitle(SequencedAssemblyState.Station station, String coords) {
        return Component.translatable(
                "gui.nep.sequenced_assembly.station.at",
                Stations.icon(station.kind()).getHoverName(),
                coords);
    }
}
