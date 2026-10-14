package dev.rylex.nep.decoder;

import net.neoforged.fml.ModList;

public enum DecoderModule {
    APOTHIC_ENCHANTING(0, "apothic_enchanting", "apothic_enchanting"),
    MYSTICAL_AGRICULTURE(1, "mysticalagriculture", "mystical_agriculture"),
    ORITECH(2, "oritech", "oritech"),
    OCCULTISM(3, "occultism", "occultism");

    private final int slot;
    private final String modId;
    private final String itemPath;

    DecoderModule(int slot, String modId, String itemStem) {
        this.slot = slot;
        this.modId = modId;
        this.itemPath = itemStem + "_encoding_module";
    }

    public static DecoderModule bySlot(int slot) {
        for (DecoderModule module : values()) {
            if (module.slot == slot) {
                return module;
            }
        }
        throw new IndexOutOfBoundsException(slot);
    }

    public int slot() {
        return slot;
    }

    public String modId() {
        return modId;
    }

    public String itemPath() {
        return itemPath;
    }

    public int bit() {
        return 1 << slot;
    }

    public boolean installed() {
        return ModList.get().isLoaded(modId);
    }

    public String modName() {
        return ModList.get()
                .getModContainerById(modId)
                .map(container -> container.getModInfo().getDisplayName())
                .orElse(modId);
    }
}
