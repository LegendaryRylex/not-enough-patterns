package dev.rylex.nep.compat.malum;

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

public class FocusedSpiritMatrixMenu extends MachineMenu implements ManualCraftMenu {

    static final int BUTTON_CLEAR_PENDING = 1;
    static final int BUTTON_CLEAR_BUFFER = 3;

    static final int WIDTH = 194;
    static final int HEIGHT = 256;

    static final int BUTTON_SPIRIT_RESTOCK = 4;

    static final int INPUT_X = 8;
    static final int INPUT_Y = 72;
    static final int INPUT_COLUMNS = 4;
    static final int INPUT_ROWS = 3;
    static final int UPGRADE_X = 98;
    static final int UPGRADE_Y = 82;
    static final int CATALYZER_X = 87;
    static final int CATALYZER_Y = 108;
    static final int IMPETUS_X = 109;
    static final int IMPETUS_Y = 108;
    static final int SPIRIT_X = 134;
    static final int SPIRIT_Y = 72;
    static final int SPIRIT_COLUMNS = SpiritBank.COLUMNS;
    static final int SPIRIT_ROWS = SpiritBank.ROWS;
    static final int OUTPUT_X = 17;
    static final int OUTPUT_Y = 137;
    static final int INV_X = 17;
    static final int INV_Y = 172;

    @Nullable
    private final FocusedSpiritMatrixBlockEntity matrix;

    private final BlockPos pos;

    private final Slot upgradeSlot;
    private final Slot catalyzerSlot;
    private final Slot impetusSlot;

    public FocusedSpiritMatrixMenu(int windowId, Inventory playerInv, BlockPos pos) {
        super(NepMalumContent.MATRIX_MENU.get(), windowId);
        this.pos = pos;
        this.matrix =
                playerInv.player.level().getBlockEntity(pos) instanceof FocusedSpiritMatrixBlockEntity be ? be : null;

        IItemHandler input = matrix != null
                ? matrix.getInputBuffer()
                : new ItemStackHandler(FocusedSpiritMatrixBlockEntity.INPUT_SLOTS);
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

        IItemHandler bank = matrix != null
                ? matrix.getSpiritBank()
                : new ItemStackHandler(FocusedSpiritMatrixBlockEntity.SPIRIT_SLOTS);
        for (int row = 0; row < SPIRIT_ROWS; row++) {
            for (int col = 0; col < SPIRIT_COLUMNS; col++) {
                addSlot(new MachineSlot(bank, row * SPIRIT_COLUMNS + col, SPIRIT_X + col * 18, SPIRIT_Y + row * 18));
            }
        }

        IItemHandler output = matrix != null
                ? matrix.getOutputBuffer()
                : new ItemStackHandler(FocusedSpiritMatrixBlockEntity.OUTPUT_SLOTS);
        for (int col = 0; col < FocusedSpiritMatrixBlockEntity.OUTPUT_SLOTS; col++) {
            addSlot(new MachineSlot(output, col, OUTPUT_X + col * 18, OUTPUT_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
        }

        IItemHandler upgrade = matrix != null
                ? matrix.getUpgradeSlot()
                : new ItemStackHandler(FocusedSpiritMatrixBlockEntity.UPGRADE_SLOTS);
        upgradeSlot = addSlot(new MachineSlot(upgrade, 0, UPGRADE_X, UPGRADE_Y));

        IItemHandler catalyzer = matrix != null
                ? matrix.getCatalyzerSlot()
                : new ItemStackHandler(FocusedSpiritMatrixBlockEntity.CATALYZER_SLOTS);
        catalyzerSlot = addSlot(new MachineSlot(catalyzer, 0, CATALYZER_X, CATALYZER_Y));

        IItemHandler impetus = matrix != null
                ? matrix.getImpetusSlot()
                : new ItemStackHandler(FocusedSpiritMatrixBlockEntity.IMPETUS_SLOTS);
        impetusSlot = addSlot(new MachineSlot(impetus, 0, IMPETUS_X, IMPETUS_Y));

        addPlayerInventory(playerInv, INV_X, INV_Y);
    }

    @Nullable
    FocusedSpiritMatrixBlockEntity matrix() {
        return matrix;
    }

    @Override
    public BlockPos machinePos() {
        return pos;
    }

    BlockPos matrixPos() {
        return pos;
    }

    boolean upgradeSlotEmpty() {
        return !upgradeSlot.hasItem();
    }

    boolean catalyzerSlotEmpty() {
        return !catalyzerSlot.hasItem();
    }

    boolean impetusSlotEmpty() {
        return !impetusSlot.hasItem();
    }

    boolean isCatalyzerSlot(@Nullable Slot slot) {
        return slot == catalyzerSlot;
    }

    boolean spiritSlotEmpty(int slot) {
        return !getSlot(FocusedSpiritMatrixBlockEntity.INPUT_SLOTS + slot).hasItem();
    }

    boolean isUpgradeSlot(@Nullable Slot slot) {
        return slot == upgradeSlot;
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
        if (id == BUTTON_SPIRIT_RESTOCK) {
            matrix.toggleSpiritRestock();
            return true;
        }
        return false;
    }

    @Override
    protected boolean movePlayerStackIntoMachine(ItemStack stack) {
        if (matrix == null) {
            return false;
        }
        if (upgradeSlot.mayPlace(stack)) {
            return moveItemStackTo(stack, upgradeSlot.index, upgradeSlot.index + 1, false);
        }
        if (catalyzerSlot.mayPlace(stack)) {
            return moveItemStackTo(stack, catalyzerSlot.index, catalyzerSlot.index + 1, false);
        }
        if (impetusSlot.mayPlace(stack)) {
            return moveItemStackTo(stack, impetusSlot.index, impetusSlot.index + 1, false);
        }
        int spiritStart = FocusedSpiritMatrixBlockEntity.INPUT_SLOTS;
        if (SpiritBank.isSpirit(stack)
                && moveItemStackTo(
                        stack, spiritStart, spiritStart + FocusedSpiritMatrixBlockEntity.SPIRIT_SLOTS, false)) {
            return true;
        }
        return moveItemStackTo(stack, 0, FocusedSpiritMatrixBlockEntity.INPUT_SLOTS, false);
    }

    @Override
    public boolean stillValid(Player player) {
        return withinReach(player, matrix);
    }
}
