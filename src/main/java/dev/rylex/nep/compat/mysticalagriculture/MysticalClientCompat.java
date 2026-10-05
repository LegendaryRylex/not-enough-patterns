package dev.rylex.nep.compat.mysticalagriculture;

import dev.rylex.nep.client.MatrixCoreProfile;
import dev.rylex.nep.client.MatrixCoreRenderer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

final class MysticalClientCompat {
    private MysticalClientCompat() {}

    static void init(IEventBus modBus) {
        modBus.addListener(MysticalClientCompat::registerScreens);
        modBus.addListener(MysticalClientCompat::registerRenderers);
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(NepMysticalContent.MATRIX_MENU.get(), InfusedAwakeningMatrixScreen::new);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                NepMysticalContent.MATRIX_BLOCK_ENTITY.get(),
                context -> new MatrixCoreRenderer<>(MatrixCoreProfile.INFUSED_AWAKENING));
    }
}
