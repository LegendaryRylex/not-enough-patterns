package dev.rylex.nep.compat.occultism;

import dev.rylex.nep.client.MatrixCoreProfile;
import dev.rylex.nep.client.MatrixCoreRenderer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

final class OccultismClientCompat {
    private OccultismClientCompat() {}

    static void init(IEventBus modBus) {
        modBus.addListener(OccultismClientCompat::registerScreens);
        modBus.addListener(OccultismClientCompat::registerRenderers);
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(NepOccultismContent.MATRIX_MENU.get(), BoundSpiritMatrixScreen::new);
        event.register(NepOccultismContent.CONTROLLER_MENU.get(), RitualControllerScreen::new);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                NepOccultismContent.MATRIX_BLOCK_ENTITY.get(),
                context -> new MatrixCoreRenderer<>(MatrixCoreProfile.BOUND_SPIRIT));
    }
}
