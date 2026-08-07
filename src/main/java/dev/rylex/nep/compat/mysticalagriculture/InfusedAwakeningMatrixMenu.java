package dev.rylex.nep.compat.mysticalagriculture;

import dev.rylex.nep.menu.MachineMenu;
import dev.rylex.nep.menu.MachineSlot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

public class InfusedAwakeningMatrixMenu extends MachineMenu {

    static final int BUTTON_CLEAR_PENDING = 1;
    static final int BUTTON_REDSTONE_MODE = 2;
    static final int BUTTON_CLEAR_BUFFER = 3;

    static final int WIDTH = 176;
    static final int HEIGHT = 256;

    static final int INPUT_X = 8;
    static final int INPUT_Y = 72;
    static final int INPUT_COLUMNS = 6;
    static final int INPUT_ROWS = 3;
    static final int TANK_X = 122;
    static final int TANK_Y = 73;
    static final int TANK_WIDTH = 6;
    static final int TANK_HEIGHT = 51;
    static final int TANK_SPACING = 12;
    static final int OUTPUT_X = 8;
    static final int OUTPUT_Y = 137;
    static final int INV_X = 8;
    static final int INV_Y = 172;

    @Nullable
    private final InfusedAwakeningMatrixBlockEntity matrix;

    private final BlockPos pos;

    public InfusedAwakeningMatrixMenu(int windowId, Inventory playerInv, BlockPos pos) {
        super(NepMysticalContent.MATRIX_MENU.get(), windowId);
        this.pos = pos;
        this.matrix = playerInv.player.level().getBlockEntity(pos) instanceof InfusedAwakeningMatrixBlockEntity be
                ? be
                : null;

        IItemHandler input = matrix != null
                ? matrix.getInputBuffer()
                : new ItemStackHandler(InfusedAwakeningMatrixBlockEntity.INPUT_SLOTS);
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
                : new ItemStackHandler(InfusedAwakeningMatrixBlockEntity.OUTPUT_SLOTS);
        for (int col = 0; col < InfusedAwakeningMatrixBlockEntity.OUTPUT_SLOTS; col++) {
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
    InfusedAwakeningMatrixBlockEntity matrix() {
        return matrix;
    }

    BlockPos matrixPos() {
        return pos;
    }

    boolean isInputSlot(@Nullable Slot slot) {
        return slot != null && slot.index >= 0 && slot.index < InfusedAwakeningMatrixBlockEntity.INPUT_SLOTS;
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
        return matrix != null && moveItemStackTo(stack, 0, InfusedAwakeningMatrixBlockEntity.INPUT_SLOTS, false);
    }

    @Override
    public boolean stillValid(Player player) {
        return withinReach(player, matrix);
    }
}
