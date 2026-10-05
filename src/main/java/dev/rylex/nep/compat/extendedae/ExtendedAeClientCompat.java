package dev.rylex.nep.compat.extendedae;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

final class ExtendedAeClientCompat {
    private ExtendedAeClientCompat() {}

    static void init(IEventBus modBus) {
        modBus.addListener(ExtendedAeClientCompat::registerScreens);
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(NepExtendedAeContent.PATTERN_VIEW.get(), PatternViewScreen::new);
    }
}
