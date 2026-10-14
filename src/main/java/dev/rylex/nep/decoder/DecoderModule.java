package dev.rylex.nep.decoder;

import net.neoforged.fml.ModList;

public enum DecoderModule {
    ACTUALLY_ADDITIONS(0, "actuallyadditions", "actually_additions"),
    APOTHIC_ENCHANTING(1, "apothic_enchanting", "apothic_enchanting"),
    ARS_NOUVEAU(2, "ars_nouveau", "ars_nouveau"),
    COMPACT_CRAFTING(3, "compactcrafting", "compact_crafting"),
    CREATE(4, "create", "create"),
    DRACONIC_EVOLUTION(5, "draconicevolution", "draconic_evolution"),
    MALUM(6, "malum", "malum"),
    MYSTICAL_AGRICULTURE(7, "mysticalagriculture", "mystical_agriculture"),
    ORITECH(8, "oritech", "oritech"),
    OCCULTISM(9, "occultism", "occultism");

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
