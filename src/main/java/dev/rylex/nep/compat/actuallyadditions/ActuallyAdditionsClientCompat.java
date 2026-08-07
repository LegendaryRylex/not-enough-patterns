package dev.rylex.nep.compat.actuallyadditions;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

final class ActuallyAdditionsClientCompat {
    private ActuallyAdditionsClientCompat() {}

    static void init(IEventBus modBus) {
        modBus.addListener(ActuallyAdditionsClientCompat::registerScreens);
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(NepActuallyAdditionsContent.MATRIX_MENU.get(), AtomicEmpoweringMatrixScreen::new);
    }
}
