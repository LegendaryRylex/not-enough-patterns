package dev.rylex.nep.compat.oritech;

import dev.rylex.nep.menu.MachineMenu;
import dev.rylex.nep.menu.MachineSlot;
import dev.rylex.nep.menu.ManualCraftMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

public class ParticleCollisionMatrixMenu extends MachineMenu implements ManualCraftMenu {

    static final int BUTTON_CLEAR_PENDING = 1;
    static final int BUTTON_CLEAR_BUFFER = 3;

    static final int WIDTH = 176;
    static final int HEIGHT = 282;

    static final int INPUT_X = 8;
    static final int INPUT_Y = 98;
    static final int INPUT_COLUMNS = 7;
    static final int INPUT_ROWS = 3;
    static final int OUTPUT_X = 8;
    static final int OUTPUT_Y = 163;
    static final int INV_X = 8;
    static final int INV_Y = 198;
    static final int CORE_X = 11;
    static final int CORE_Y = 65;

    @Nullable
    private final ParticleCollisionMatrixBlockEntity matrix;

    private final BlockPos pos;
    private int coreSlotIndex;

    public ParticleCollisionMatrixMenu(int windowId, Inventory playerInv, BlockPos pos) {
        super(NepOritechContent.MATRIX_MENU.get(), windowId);
        this.pos = pos;
        this.matrix = playerInv.player.level().getBlockEntity(pos) instanceof ParticleCollisionMatrixBlockEntity be
                ? be
                : null;

        IItemHandler input = matrix != null
                ? matrix.getInputBuffer()
                : new ItemStackHandler(ParticleCollisionMatrixBlockEntity.INPUT_SLOTS);
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
                : new ItemStackHandler(ParticleCollisionMatrixBlockEntity.OUTPUT_SLOTS);
        for (int col = 0; col < ParticleCollisionMatrixBlockEntity.OUTPUT_SLOTS; col++) {
            addSlot(new MachineSlot(output, col, OUTPUT_X + col * 18, OUTPUT_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
        }

        coreSlotIndex = slots.size();
        addSlot(new MachineSlot(matrix != null ? matrix.coreSlot() : new ItemStackHandler(1), 0, CORE_X, CORE_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return MatrixAddons.isCore(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        addPlayerInventory(playerInv, INV_X, INV_Y);
    }

    @Nullable
    ParticleCollisionMatrixBlockEntity matrix() {
        return matrix;
    }

    @Override
    public BlockPos machinePos() {
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
        if (matrix == null) {
            return false;
        }
        if (MatrixAddons.isCore(stack) && moveItemStackTo(stack, coreSlotIndex, coreSlotIndex + 1, false)) {
            return true;
        }
        return moveItemStackTo(stack, 0, ParticleCollisionMatrixBlockEntity.INPUT_SLOTS, false);
    }

    @Override
    public boolean stillValid(Player player) {
        return withinReach(player, matrix);
    }
}
