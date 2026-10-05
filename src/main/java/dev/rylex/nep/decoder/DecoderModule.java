package dev.rylex.nep.decoder;

import net.neoforged.fml.ModList;

public enum DecoderModule {
    APOTHIC_ENCHANTING("apothic_enchanting", "apothic_enchanting"),
    MYSTICAL_AGRICULTURE("mysticalagriculture", "mystical_agriculture");

    private final String modId;
    private final String itemPath;

    DecoderModule(String modId, String itemStem) {
        this.modId = modId;
        this.itemPath = itemStem + "_encoding_module";
    }

    public String modId() {
        return modId;
    }

    public String itemPath() {
        return itemPath;
    }

    public int bit() {
        return 1 << ordinal();
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
