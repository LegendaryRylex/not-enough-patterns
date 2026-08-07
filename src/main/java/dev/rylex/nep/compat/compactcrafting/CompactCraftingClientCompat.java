package dev.rylex.nep.compat.compactcrafting;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

final class CompactCraftingClientCompat {
    private CompactCraftingClientCompat() {}

    static void init(IEventBus modBus) {
        modBus.addListener(CompactCraftingClientCompat::registerScreens);
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(NepCompactCraftingContent.MATRIX_MENU.get(), MiniaturizationMatrixScreen::new);
        event.register(NepCompactCraftingContent.CONTROLLER_MENU.get(), MiniaturizationControllerScreen::new);
    }
}
