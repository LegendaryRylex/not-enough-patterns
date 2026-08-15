package dev.rylex.nep.compat.actuallyadditions;

import dev.rylex.nep.menu.MachineMenu;
import dev.rylex.nep.menu.MachineSlot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

public class AtomicEmpoweringMatrixMenu extends MachineMenu {

    static final int BUTTON_CLEAR_PENDING = 1;
    static final int BUTTON_CLEAR_BUFFER = 3;

    static final int WIDTH = 176;
    static final int HEIGHT = 256;

    static final int INPUT_X = 8;
    static final int INPUT_Y = 72;
    static final int INPUT_COLUMNS = 7;
    static final int INPUT_ROWS = 3;
    static final int OUTPUT_X = 8;
    static final int OUTPUT_Y = 137;
    static final int INV_X = 8;
    static final int INV_Y = 172;

    @Nullable
    private final AtomicEmpoweringMatrixBlockEntity matrix;

    private final BlockPos pos;

    public AtomicEmpoweringMatrixMenu(int windowId, Inventory playerInv, BlockPos pos) {
        super(NepActuallyAdditionsContent.MATRIX_MENU.get(), windowId);
        this.pos = pos;
        this.matrix = playerInv.player.level().getBlockEntity(pos) instanceof AtomicEmpoweringMatrixBlockEntity be
                ? be
                : null;

        IItemHandler input = matrix != null
                ? matrix.getInputBuffer()
                : new ItemStackHandler(AtomicEmpoweringMatrixBlockEntity.INPUT_SLOTS);
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
                : new ItemStackHandler(AtomicEmpoweringMatrixBlockEntity.OUTPUT_SLOTS);
        for (int col = 0; col < AtomicEmpoweringMatrixBlockEntity.OUTPUT_SLOTS; col++) {
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
    AtomicEmpoweringMatrixBlockEntity matrix() {
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
        if (id == BUTTON_CLEAR_BUFFER) {
            matrix.clearBufferTo(player);
            return true;
        }
        return false;
    }

    @Override
    protected boolean movePlayerStackIntoMachine(ItemStack stack) {
        return matrix != null && moveItemStackTo(stack, 0, AtomicEmpoweringMatrixBlockEntity.INPUT_SLOTS, false);
    }

    @Override
    public boolean stillValid(Player player) {
        return withinReach(player, matrix);
    }
}
