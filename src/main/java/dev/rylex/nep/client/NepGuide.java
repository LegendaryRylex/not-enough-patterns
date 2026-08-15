package dev.rylex.nep.client;

import dev.rylex.nep.Nep;
import guideme.GuidesCommon;
import guideme.PageAnchor;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

public final class NepGuide {

    private static final ResourceLocation AE2_GUIDE = ResourceLocation.fromNamespaceAndPath("ae2", "guide");

    private NepGuide() {}

    public static void open(String page) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            GuidesCommon.openGuide(minecraft.player, AE2_GUIDE, PageAnchor.page(Nep.id(page)));
        }
    }
}
