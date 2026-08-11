package dev.rylex.nep.machine;

import net.minecraft.util.Mth;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

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

    public static int fullness(ResourceHandler<ItemResource> handler) {
        int slots = handler.size();
        if (slots == 0) {
            return 0;
        }
        float fill = itemFill(handler);
        fill /= slots;
        return scale(fill);
    }

    private static float itemFill(ResourceHandler<ItemResource> handler) {
        float fill = 0.0F;
        for (int slot = 0; slot < handler.size(); slot++) {
            ItemResource resource = handler.getResource(slot);
            long amount = handler.getAmountAsLong(slot);
            if (amount > 0) {
                long capacity = Math.min(handler.getCapacityAsLong(slot, resource), resource.getMaxStackSize());
                if (capacity > 0) {
                    fill += Math.min(1.0F, amount / (float) capacity);
                }
            }
        }
        return fill;
    }

    private static int scale(float fill) {
        return Mth.floor(fill * 14.0F) + (fill > 0.0F ? 1 : 0);
    }
}
