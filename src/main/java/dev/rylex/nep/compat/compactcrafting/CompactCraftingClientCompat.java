package dev.rylex.nep.compat.compactcrafting;

import dev.rylex.nep.client.MatrixCoreProfile;
import dev.rylex.nep.client.MatrixCoreRenderer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

final class CompactCraftingClientCompat {
    private CompactCraftingClientCompat() {}

    static void init(IEventBus modBus) {
        modBus.addListener(CompactCraftingClientCompat::registerScreens);
        modBus.addListener(CompactCraftingClientCompat::registerRenderers);
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(NepCompactCraftingContent.MATRIX_MENU.get(), MiniaturizationMatrixScreen::new);
        event.register(NepCompactCraftingContent.CONTROLLER_MENU.get(), MiniaturizationControllerScreen::new);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                NepCompactCraftingContent.MATRIX_BLOCK_ENTITY.get(),
                context -> new MatrixCoreRenderer<>(MatrixCoreProfile.MINIATURIZATION));
    }
}
