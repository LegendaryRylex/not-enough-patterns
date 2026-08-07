package dev.rylex.nep.compat.draconic;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

final class DraconicClientCompat {
    private DraconicClientCompat() {}

    static void init(IEventBus modBus) {
        modBus.addListener(DraconicClientCompat::registerScreens);
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(NepDraconicContent.MATRIX_MENU.get(), FusionMatrixScreen::new);
    }
}
