package dev.rylex.nep.compat.occultism;

import dev.rylex.nep.menu.MachineMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;
import org.jetbrains.annotations.Nullable;

public class RitualControllerMenu extends MachineMenu {

    static final int BUTTON_CLEAR_PENDING = 1;
    static final int BUTTON_CLEAR_BUFFER = 3;
    static final int BUTTON_TOGGLE_REPAIR = 5;

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
    static final int BOOK_X = 152;
    static final int BOOK_Y = 72;

    private static final int BOOK_SLOT =
            RitualControllerBlockEntity.INPUT_SLOTS + RitualControllerBlockEntity.OUTPUT_SLOTS;

    @Nullable
    private final RitualControllerBlockEntity controller;

    public RitualControllerMenu(int windowId, Inventory playerInv, BlockPos pos) {
        super(NepOccultismContent.CONTROLLER_MENU.get(), windowId);
        this.controller =
                playerInv.player.level().getBlockEntity(pos) instanceof RitualControllerBlockEntity be ? be : null;

        ItemStacksResourceHandler input = controller != null
                ? controller.getInputBuffer()
                : new ItemStacksResourceHandler(RitualControllerBlockEntity.INPUT_SLOTS);
        for (int row = 0; row < INPUT_ROWS; row++) {
            for (int col = 0; col < INPUT_COLUMNS; col++) {
                addSlot(
                        new ResourceHandlerSlot(
                                input, input::set, row * INPUT_COLUMNS + col, INPUT_X + col * 18, INPUT_Y + row * 18) {
                            @Override
                            public boolean mayPlace(ItemStack stack) {
                                return controller != null && controller.manualDemandFor(ItemResource.of(stack)) > 0;
                            }

                            @Override
                            public int getMaxStackSize(ItemStack stack) {
                                return controller == null
                                        ? super.getMaxStackSize(stack)
                                        : Math.min(
                                                super.getMaxStackSize(stack),
                                                getItem().getCount()
                                                        + controller.manualDemandFor(ItemResource.of(stack)));
                            }
                        });
            }
        }

        ItemStacksResourceHandler output = controller != null
                ? controller.getOutputBuffer()
                : new ItemStacksResourceHandler(RitualControllerBlockEntity.OUTPUT_SLOTS);
        for (int col = 0; col < RitualControllerBlockEntity.OUTPUT_SLOTS; col++) {
            addSlot(new ResourceHandlerSlot(output, output::set, col, OUTPUT_X + col * 18, OUTPUT_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
        }

        ItemStacksResourceHandler book = controller != null ? controller.book() : new ItemStacksResourceHandler(1);
        addSlot(new ResourceHandlerSlot(book, book::set, 0, BOOK_X, BOOK_Y));

        addPlayerInventory(playerInv, INV_X, INV_Y);
    }

    @Nullable
    RitualControllerBlockEntity controller() {
        return controller;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (controller == null) {
            return false;
        }
        if (id == BUTTON_CLEAR_PENDING) {
            controller.clearPending();
            return true;
        }
        if (id == BUTTON_CLEAR_BUFFER) {
            controller.clearBufferTo(player);
            return true;
        }
        if (id == BUTTON_TOGGLE_REPAIR) {
            controller.toggleRepair();
            return true;
        }
        return false;
    }

    @Override
    protected boolean movePlayerStackIntoMachine(ItemStack stack) {
        if (controller == null) {
            return false;
        }
        if (stack.is(NepOccultismContent.SPIRIT_INFUSED_BOOK.get())) {
            return moveItemStackTo(stack, BOOK_SLOT, BOOK_SLOT + 1, false);
        }
        return moveItemStackTo(stack, 0, RitualControllerBlockEntity.INPUT_SLOTS, false);
    }

    @Override
    public boolean stillValid(Player player) {
        return withinReach(player, controller);
    }
}
