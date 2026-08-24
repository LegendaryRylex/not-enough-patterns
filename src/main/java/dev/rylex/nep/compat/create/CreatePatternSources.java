package dev.rylex.nep.compat.create;

import net.minecraft.resources.ResourceLocation;

final class CreatePatternSources {
    private CreatePatternSources() {}

    static final ResourceLocation MECHANICAL_CRAFTING = create("mechanical_crafting");
    static final ResourceLocation AUTOMATIC_SHAPED = create("automatic_shaped");
    static final ResourceLocation SEQUENCED_ASSEMBLY = create("sequenced_assembly");
    static final ResourceLocation DEPLOYING = create("deploying");
    static final ResourceLocation ITEM_APPLICATION = create("item_application");
    static final ResourceLocation SANDPAPER_POLISHING = create("sandpaper_polishing");
    static final ResourceLocation SPOUT_FILLING = create("spout_filling");

    private static ResourceLocation create(String path) {
        return ResourceLocation.fromNamespaceAndPath("create", path);
    }
}
