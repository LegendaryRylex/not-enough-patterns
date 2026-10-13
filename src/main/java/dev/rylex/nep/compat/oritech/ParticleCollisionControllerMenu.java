package dev.rylex.nep.compat.oritech;

import dev.rylex.nep.menu.MachineMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;
import org.jetbrains.annotations.Nullable;

public class ParticleCollisionControllerMenu extends MachineMenu {

    static final int BUTTON_CLEAR_PENDING = 1;
    static final int BUTTON_CLEAR_BUFFER = 3;

    static final int WIDTH = 176;
    static final int HEIGHT = 256;

    static final int INPUT_X = 8;
    static final int INPUT_Y = 72;
    static final int INPUT_COLUMNS = 9;
    static final int INPUT_ROWS = 3;
    static final int OUTPUT_X = 8;
    static final int OUTPUT_Y = 137;
    static final int INV_X = 8;
    static final int INV_Y = 172;

    @Nullable
    private final ParticleCollisionControllerBlockEntity controller;

    public ParticleCollisionControllerMenu(int windowId, Inventory playerInv, BlockPos pos) {
        super(NepOritechContent.CONTROLLER_MENU.get(), windowId);
        this.controller =
                playerInv.player.level().getBlockEntity(pos) instanceof ParticleCollisionControllerBlockEntity be
                        ? be
                        : null;

        ItemStacksResourceHandler input = controller != null
                ? controller.getInputBuffer()
                : new ItemStacksResourceHandler(ParticleCollisionControllerBlockEntity.INPUT_SLOTS);
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
                : new ItemStacksResourceHandler(ParticleCollisionControllerBlockEntity.OUTPUT_SLOTS);
        for (int col = 0; col < ParticleCollisionControllerBlockEntity.OUTPUT_SLOTS; col++) {
            addSlot(new ResourceHandlerSlot(output, output::set, col, OUTPUT_X + col * 18, OUTPUT_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
        }

        addPlayerInventory(playerInv, INV_X, INV_Y);
    }

    @Nullable
    ParticleCollisionControllerBlockEntity controller() {
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
        return false;
    }

    @Override
    protected boolean movePlayerStackIntoMachine(ItemStack stack) {
        return controller != null
                && moveItemStackTo(stack, 0, ParticleCollisionControllerBlockEntity.INPUT_SLOTS, false);
    }

    @Override
    public boolean stillValid(Player player) {
        return withinReach(player, controller);
    }
}
