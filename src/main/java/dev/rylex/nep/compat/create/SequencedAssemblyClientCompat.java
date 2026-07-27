package dev.rylex.nep.compat.create;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;

final class SequencedAssemblyClientCompat {
    private SequencedAssemblyClientCompat() {}

    static void init(IEventBus modBus) {
        modBus.addListener(SequencedAssemblyClientCompat::registerScreens);
        NeoForge.EVENT_BUS.addListener(MatrixStressTooltip::onTooltip);
        AssemblyLinkVisualizer.init();
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(NepCreateContent.CONTROLLER_MENU.get(), SequencedAssemblyControllerScreen::new);
        event.register(NepCreateContent.MATRIX_MENU.get(), SequencedAssemblyMatrixScreen::new);
    }
}
