package dev.rylex.nep.machine;

import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

public enum RedstoneMode {
    OUTPUT("O", "output"),
    STATUS("S", "status"),
    INPUT("I", "input");

    private final String glyph;
    private final String key;

    RedstoneMode(String glyph, String keySuffix) {
        this.glyph = glyph;
        this.key = "gui.nep.redstone_mode." + keySuffix;
    }

    public String glyph() {
        return glyph;
    }

    public String key() {
        return key;
    }

    public RedstoneMode next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public static RedstoneMode byOrdinal(int ordinal) {
        RedstoneMode[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : OUTPUT;
    }

    public static RedstoneMode byName(String name) {
        for (RedstoneMode mode : values()) {
            if (mode.name().equals(name)) {
                return mode;
            }
        }
        return OUTPUT;
    }

    public static int fullness(IItemHandler handler) {
        int slots = handler.getSlots();
        if (slots == 0) {
            return 0;
        }
        float fill = itemFill(handler);
        fill /= slots;
        return scale(fill);
    }

    public static int inputFullness(IItemHandler items, IFluidHandler fluids) {
        int cells = items.getSlots() + fluids.getTanks();
        if (cells == 0) {
            return 0;
        }
        float fill = itemFill(items);
        for (int tank = 0; tank < fluids.getTanks(); tank++) {
            FluidStack held = fluids.getFluidInTank(tank);
            int capacity = fluids.getTankCapacity(tank);
            if (!held.isEmpty() && capacity > 0) {
                fill += Math.min(1.0F, held.getAmount() / (float) capacity);
            }
        }
        fill /= cells;
        return scale(fill);
    }

    private static float itemFill(IItemHandler handler) {
        float fill = 0.0F;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                fill += stack.getCount() / (float) Math.min(handler.getSlotLimit(slot), stack.getMaxStackSize());
            }
        }
        return fill;
    }

    private static int scale(float fill) {
        return Mth.floor(fill * 14.0F) + (fill > 0.0F ? 1 : 0);
    }
}
