package dev.rylex.nep.decoder;

import dev.rylex.nep.menu.MachineMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;
import org.jetbrains.annotations.Nullable;

public class PatternDecoderMenu extends MachineMenu {

    public static final int COLUMNS = 9;
    public static final int ROWS = Math.ceilDiv(PatternDecoderBlockEntity.SLOTS, COLUMNS);
    public static final int WIDTH = 176;
    public static final int INVENTORY_GAP = 4;
    public static final int HEIGHT = 114 + INVENTORY_GAP + ROWS * 18;
    public static final int SLOT_X = 8;
    public static final int SLOT_Y = 18;
    public static final int PLAYER_INVENTORY_Y = SLOT_Y + ROWS * 18 + 13 + INVENTORY_GAP;

    @Nullable
    private final PatternDecoderBlockEntity decoder;

    private final DataSlot status = DataSlot.standalone();
    private final DataSlot networkModules = DataSlot.standalone();

    public PatternDecoderMenu(int windowId, Inventory playerInv, BlockPos pos) {
        super(DecoderContent.PATTERN_DECODER_MENU.get(), windowId);
        this.decoder = PatternDecoderBlockEntity.at(playerInv.player.level(), pos);
        ModuleSlots slots = decoder != null ? decoder.moduleSlots() : new ModuleSlots();
        for (int slot = 0; slot < PatternDecoderBlockEntity.SLOTS; slot++) {
            addSlot(new ResourceHandlerSlot(slots, slots::set, slot, slotX(slot), slotY(slot)));
        }
        addPlayerInventory(playerInv, SLOT_X, PLAYER_INVENTORY_Y);
        addDataSlot(status);
        addDataSlot(networkModules);
    }

    public static int slotX(int slot) {
        return SLOT_X + (slot % COLUMNS) * 18;
    }

    public static int slotY(int slot) {
        return SLOT_Y + (slot / COLUMNS) * 18;
    }

    public DecoderStatus status() {
        return DecoderStatus.byOrdinal(status.get());
    }

    public int networkModules() {
        return networkModules.get();
    }

    public ItemStack moduleIn(DecoderModule module) {
        return slots.get(module.ordinal()).getItem();
    }

    @Override
    public void broadcastChanges() {
        if (decoder != null && !decoder.isRemoved()) {
            status.set(decoder.status().ordinal());
            networkModules.set(decoder.networkModules());
        }
        super.broadcastChanges();
    }

    @Override
    protected boolean movePlayerStackIntoMachine(ItemStack stack) {
        return stack.getItem() instanceof EncodingModuleItem module
                && moveItemStackTo(
                        stack, module.module().ordinal(), module.module().ordinal() + 1, false);
    }

    @Override
    public boolean stillValid(Player player) {
        return withinReach(player, decoder);
    }
}
