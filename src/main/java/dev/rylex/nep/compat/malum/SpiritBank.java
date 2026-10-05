package dev.rylex.nep.compat.malum;

import com.sammy.malum.common.item.spirit.SpiritShardItem;
import com.sammy.malum.registry.common.item.MalumItems;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.machine.OverstackedItemHandler;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Laid out as Malum's own wheel reads: the four elemental spirits on the edges, the four arcane ones on the corners,
 * and a joker in the middle that holds Umbral or whichever addon spirit a craft asks for.
 */
class SpiritBank extends OverstackedItemHandler {

    static final int COLUMNS = 3;
    static final int ROWS = 3;
    static final int SLOTS = COLUMNS * ROWS;
    static final int JOKER_SLOT = 4;

    /** A pushed craft lands on top of a bank already restocked to spiritStock, so a slot has to hold both. */
    private static final int PUSH_HEADROOM = 1024;

    private static final List<Supplier<? extends Item>> LAYOUT = List.of(
            MalumItems.SACRED_SPIRIT,
            MalumItems.AERIAL_SPIRIT,
            MalumItems.WICKED_SPIRIT,
            MalumItems.AQUEOUS_SPIRIT,
            MalumItems.UMBRAL_SPIRIT,
            MalumItems.INFERNAL_SPIRIT,
            MalumItems.ARCANE_SPIRIT,
            MalumItems.EARTHEN_SPIRIT,
            MalumItems.ELDRITCH_SPIRIT);

    private final Runnable onChanged;

    SpiritBank(Runnable onChanged) {
        super(SLOTS);
        this.onChanged = onChanged;
    }

    static boolean isSpirit(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof SpiritShardItem;
    }

    static Item spiritFor(int slot) {
        return LAYOUT.get(slot).get();
    }

    static boolean hasDedicatedSlot(ItemStack stack) {
        return dedicatedSlotFor(stack) >= 0;
    }

    private static int dedicatedSlotFor(ItemStack stack) {
        if (!isSpirit(stack)) {
            return -1;
        }
        for (int slot = 0; slot < LAYOUT.size(); slot++) {
            if (slot != JOKER_SLOT && LAYOUT.get(slot).get() == stack.getItem()) {
                return slot;
            }
        }
        return -1;
    }

    /** The slot this spirit belongs in right now, or -1 while the joker is held by a different spirit. */
    int slotFor(ItemStack stack) {
        int dedicated = dedicatedSlotFor(stack);
        if (dedicated >= 0) {
            return dedicated;
        }
        if (!isSpirit(stack)) {
            return -1;
        }
        ItemStack joker = getStackInSlot(JOKER_SLOT);
        return joker.isEmpty() || joker.is(stack.getItem()) ? JOKER_SLOT : -1;
    }

    boolean isBanked(ItemStack stack) {
        return slotFor(stack) >= 0;
    }

    @Override
    public int getSlotLimit(int slot) {
        return (int) Math.min(Integer.MAX_VALUE, NepConfig.malumFocusedSpiritMatrixSpiritStock() + PUSH_HEADROOM);
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return slotFor(stack) == slot;
    }

    @Override
    protected void onContentsChanged(int slot) {
        onChanged.run();
    }

    long amountOf(ItemStack sample) {
        int slot = slotFor(sample);
        return slot < 0 ? 0 : getStackInSlot(slot).getCount();
    }
}
