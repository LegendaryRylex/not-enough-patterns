package dev.rylex.nep.compat.create;

import dev.rylex.nep.machine.MachineFluidInput;
import dev.rylex.nep.menu.MachineMenu;
import dev.rylex.nep.menu.MachineSlot;
import dev.rylex.nep.menu.ManualCraftMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.network.PacketDistributor;

public class SequencedAssemblyControllerMenu extends MachineMenu implements ManualCraftMenu {

    static final int BUTTON_CLEAR_BUFFER = 1;
    static final int BUTTON_CLEAR_PENDING = 2;
    static final int BUTTON_FILL_TANK = 3;

    static final int WIDTH = 200;
    static final int HEIGHT = 266;
    static final int TEXTURE_HEIGHT = 288;
    static final int INGREDIENT_X = 8;
    static final int INGREDIENT_Y = 82;
    static final int INGREDIENT_COLUMNS = 6;
    static final int INGREDIENT_ROWS = 3;
    private static final int INPUT_END = INGREDIENT_COLUMNS * INGREDIENT_ROWS;
    static final int OUTPUT_X = 19;
    static final int OUTPUT_Y = 147;
    static final int INV_X = 19;
    static final int INV_Y = 182;
    static final int TANK_X = 122;
    static final int TANK_Y = 82;
    static final int TANK_WIDTH = 16;
    static final int TANK_HEIGHT = 52;
    static final int TANK_STEP = 18;
    static final int SEPARATOR_Y = 168;
    private static final int STATE_INTERVAL = 2;

    private final SequencedAssemblyControllerBlockEntity controller;
    private final BlockPos pos;
    private final Player player;

    private SequencedAssemblyState lastState = SequencedAssemblyState.empty();
    private int stateCooldown;

    public SequencedAssemblyControllerMenu(int windowId, Inventory playerInv, BlockPos pos) {
        super(NepCreateContent.CONTROLLER_MENU.get(), windowId);
        this.pos = pos;
        this.player = playerInv.player;
        this.controller =
                playerInv.player.level().getBlockEntity(pos) instanceof SequencedAssemblyControllerBlockEntity be
                        ? be
                        : null;

        IItemHandler input = controller != null ? controller.getBuffer() : new ItemStackHandler(INPUT_END);
        IItemHandler output = controller != null ? controller.getOutputBuffer() : new ItemStackHandler(9);
        addInputGrid(input, INGREDIENT_COLUMNS, INGREDIENT_ROWS, INGREDIENT_X, INGREDIENT_Y);
        addTakeOnlyGrid(output, 9, 1, OUTPUT_X, OUTPUT_Y);

        addPlayerInventory(playerInv, INV_X, INV_Y);
    }

    private void addInputGrid(IItemHandler handler, int cols, int rows, int x, int y) {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                addSlot(new MachineSlot(handler, row * cols + col, x + col * 18, y + row * 18));
            }
        }
    }

    private void addTakeOnlyGrid(IItemHandler handler, int cols, int rows, int x, int y) {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                addSlot(new MachineSlot(handler, row * cols + col, x + col * 18, y + row * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false;
                    }
                });
            }
        }
    }

    @Override
    public BlockPos machinePos() {
        return pos;
    }

    BlockPos controllerPos() {
        return pos;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (controller == null || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (stateCooldown > 0) {
            stateCooldown--;
            return;
        }
        stateCooldown = STATE_INTERVAL - 1;
        SequencedAssemblyState current = controller.buildState();
        if (!current.matches(lastState)) {
            lastState = current;
            PacketDistributor.sendToPlayer(serverPlayer, current);
        }
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (controller == null) {
            return false;
        }
        if (id == BUTTON_CLEAR_BUFFER) {
            controller.clearBufferTo(player);
            return true;
        }
        if (id == BUTTON_CLEAR_PENDING) {
            controller.clearPending();
            return true;
        }
        if (id == BUTTON_FILL_TANK) {
            return MachineFluidInput.fillFromCarried(this, player, controller.fluidHandler());
        }
        return false;
    }

    @Override
    protected boolean movePlayerStackIntoMachine(ItemStack stack) {
        return controller != null && moveItemStackTo(stack, 0, INPUT_END, false);
    }

    @Override
    public boolean stillValid(Player player) {
        return withinReach(player, controller);
    }
}
