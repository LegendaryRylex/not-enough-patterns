package dev.rylex.nep.compat.mysticalagriculture;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

final class MysticalClientCompat {
    private MysticalClientCompat() {}

    static void init(IEventBus modBus) {
        modBus.addListener(MysticalClientCompat::registerScreens);
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(NepMysticalContent.MATRIX_MENU.get(), InfusedAwakeningMatrixScreen::new);
    }
}
