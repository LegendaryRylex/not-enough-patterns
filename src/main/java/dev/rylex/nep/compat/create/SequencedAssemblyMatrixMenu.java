package dev.rylex.nep.compat.create;

import dev.rylex.nep.machine.MachineFluidInput;
import dev.rylex.nep.menu.MachineMenu;
import dev.rylex.nep.menu.MachineSlot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

public class SequencedAssemblyMatrixMenu extends MachineMenu {

    static final int BUTTON_CLEAR_PENDING = 1;
    static final int BUTTON_FILL_TANK = 2;
    static final int BUTTON_REDSTONE_MODE = 3;
    static final int BUTTON_CLEAR_BUFFER = 4;

    static final int WIDTH = 200;
    static final int HEIGHT = 256;
    static final int SEPARATOR_Y = 158;

    static final int READOUT_TOP = 18;
    static final int READOUT_BOTTOM = 58;
    static final int INPUT_X = 8;
    static final int INPUT_Y = 72;
    static final int INPUT_COLUMNS = 6;
    static final int INPUT_ROWS = 3;
    static final int TANK_X = 122;
    static final int TANK_Y = 72;
    static final int TANK_WIDTH = 16;
    static final int TANK_HEIGHT = 52;
    static final int TANK_STEP = 18;
    static final int OUTPUT_X = 19;
    static final int OUTPUT_Y = 137;
    static final int INV_X = 19;
    static final int INV_Y = 172;

    @Nullable
    private final SequencedAssemblyMatrixBlockEntity matrix;

    private final BlockPos pos;

    public SequencedAssemblyMatrixMenu(int windowId, Inventory playerInv, BlockPos pos) {
        super(NepCreateContent.MATRIX_MENU.get(), windowId);
        this.pos = pos;
        this.matrix = playerInv.player.level().getBlockEntity(pos) instanceof SequencedAssemblyMatrixBlockEntity be
                ? be
                : null;

        IItemHandler input = matrix != null
                ? matrix.getInputBuffer()
                : new ItemStackHandler(SequencedAssemblyMatrixBlockEntity.INPUT_SLOTS);
        for (int row = 0; row < INPUT_ROWS; row++) {
            for (int col = 0; col < INPUT_COLUMNS; col++) {
                addSlot(new MachineSlot(input, row * INPUT_COLUMNS + col, INPUT_X + col * 18, INPUT_Y + row * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return matrix != null && matrix.manualDemandFor(stack) > 0;
                    }

                    @Override
                    public int getMaxStackSize(ItemStack stack) {
                        return matrix == null
                                ? super.getMaxStackSize(stack)
                                : Math.min(
                                        super.getMaxStackSize(stack),
                                        getItem().getCount() + matrix.manualDemandFor(stack));
                    }
                });
            }
        }
        IItemHandler output = matrix != null
                ? matrix.getOutputBuffer()
                : new ItemStackHandler(SequencedAssemblyMatrixBlockEntity.OUTPUT_SLOTS);
        for (int col = 0; col < SequencedAssemblyMatrixBlockEntity.OUTPUT_SLOTS; col++) {
            addSlot(new MachineSlot(output, col, OUTPUT_X + col * 18, OUTPUT_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
        }

        addPlayerInventory(playerInv, INV_X, INV_Y);
    }

    @Nullable
    SequencedAssemblyMatrixBlockEntity matrix() {
        return matrix;
    }

    BlockPos matrixPos() {
        return pos;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (matrix == null) {
            return false;
        }
        if (id == BUTTON_CLEAR_PENDING) {
            matrix.clearPending();
            return true;
        }
        if (id == BUTTON_FILL_TANK) {
            return MachineFluidInput.fillFromCarried(this, player, matrix.fluidHandler());
        }
        if (id == BUTTON_REDSTONE_MODE) {
            matrix.cycleRedstoneMode();
            return true;
        }
        if (id == BUTTON_CLEAR_BUFFER) {
            matrix.clearBufferTo(player);
            return true;
        }
        return false;
    }

    @Override
    protected boolean movePlayerStackIntoMachine(ItemStack stack) {
        return matrix != null && moveItemStackTo(stack, 0, SequencedAssemblyMatrixBlockEntity.INPUT_SLOTS, false);
    }

    @Override
    public boolean stillValid(Player player) {
        return withinReach(player, matrix);
    }
}
