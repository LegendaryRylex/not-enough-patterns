package dev.rylex.nep.compat.ars;

import dev.rylex.nep.menu.MachineMenu;
import dev.rylex.nep.menu.MachineSlot;
import dev.rylex.nep.menu.ManualCraftMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

public class ArcaneEnchantingMatrixMenu extends MachineMenu implements ManualCraftMenu {

    static final int BUTTON_CLEAR_PENDING = 1;
    static final int BUTTON_CLEAR_BUFFER = 3;

    static final int WIDTH = 204;
    static final int HEIGHT = 289;
    static final int SHEET_HEIGHT = 512;

    static final int INPUT_X = 8;
    static final int INPUT_Y = 72;
    static final int INPUT_COLUMNS = 8;
    static final int INPUT_ROWS = 3;
    static final int ACCELERATE_X = 158;
    static final int ACCELERATE_Y = 81;
    static final int DAMPEN_X = 158;
    static final int DAMPEN_Y = 99;
    static final int GAUGE_X = 180;
    static final int GAUGE_Y = 73;
    static final int CATALYST_X = 21;
    static final int CATALYST_Y = 140;
    static final int CATALYST_COLUMNS = 9;
    static final int OUTPUT_X = 21;
    static final int OUTPUT_Y = 170;
    static final int INV_X = 21;
    static final int INV_Y = 205;

    @Nullable
    private final ArcaneEnchantingMatrixBlockEntity matrix;

    private final BlockPos pos;

    private final Slot accelerateSlot;
    private final Slot dampenSlot;

    public ArcaneEnchantingMatrixMenu(int windowId, Inventory playerInv, BlockPos pos) {
        super(NepArsContent.MATRIX_MENU.get(), windowId);
        this.pos = pos;
        this.matrix = playerInv.player.level().getBlockEntity(pos) instanceof ArcaneEnchantingMatrixBlockEntity be
                ? be
                : null;

        IItemHandler input = matrix != null
                ? matrix.getInputBuffer()
                : new ItemStackHandler(ArcaneEnchantingMatrixBlockEntity.INPUT_SLOTS);
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
                : new ItemStackHandler(ArcaneEnchantingMatrixBlockEntity.OUTPUT_SLOTS);
        for (int col = 0; col < ArcaneEnchantingMatrixBlockEntity.OUTPUT_SLOTS; col++) {
            addSlot(new MachineSlot(output, col, OUTPUT_X + col * 18, OUTPUT_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
        }

        accelerateSlot = addSlot(new MachineSlot(
                matrix != null ? matrix.getAccelerateSlot() : new ItemStackHandler(1), 0, ACCELERATE_X, ACCELERATE_Y));
        dampenSlot = addSlot(new MachineSlot(
                matrix != null ? matrix.getDampenSlot() : new ItemStackHandler(1), 0, DAMPEN_X, DAMPEN_Y));

        IItemHandler shelf = matrix != null ? matrix.getCatalystShelf() : new ItemStackHandler(CatalystShelf.SLOTS);
        for (int slot = 0; slot < CatalystShelf.SLOTS; slot++) {
            addSlot(
                    new MachineSlot(
                            shelf,
                            slot,
                            CATALYST_X + (slot % CATALYST_COLUMNS) * 18,
                            CATALYST_Y + (slot / CATALYST_COLUMNS) * 18) {
                        @Override
                        public boolean mayPlace(ItemStack stack) {
                            return false;
                        }

                        @Override
                        public boolean mayPickup(Player player) {
                            return false;
                        }
                    });
        }

        addPlayerInventory(playerInv, INV_X, INV_Y);
    }

    @Nullable
    ArcaneEnchantingMatrixBlockEntity matrix() {
        return matrix;
    }

    @Override
    public BlockPos machinePos() {
        return pos;
    }

    boolean accelerateSlotEmpty() {
        return !accelerateSlot.hasItem();
    }

    boolean dampenSlotEmpty() {
        return !dampenSlot.hasItem();
    }

    boolean isAccelerateSlot(@Nullable Slot slot) {
        return slot == accelerateSlot;
    }

    boolean isDampenSlot(@Nullable Slot slot) {
        return slot == dampenSlot;
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
        if (matrix == null) {
            return false;
        }
        if (accelerateSlot.mayPlace(stack)) {
            return moveItemStackTo(stack, accelerateSlot.index, accelerateSlot.index + 1, false);
        }
        if (dampenSlot.mayPlace(stack)) {
            return moveItemStackTo(stack, dampenSlot.index, dampenSlot.index + 1, false);
        }
        return moveItemStackTo(stack, 0, ArcaneEnchantingMatrixBlockEntity.INPUT_SLOTS, false);
    }

    @Override
    public boolean stillValid(Player player) {
        return withinReach(player, matrix);
    }
}
