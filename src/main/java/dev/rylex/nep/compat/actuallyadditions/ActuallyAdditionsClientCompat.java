package dev.rylex.nep.compat.actuallyadditions;

import dev.rylex.nep.client.MatrixCoreProfile;
import dev.rylex.nep.client.MatrixCoreRenderer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

final class ActuallyAdditionsClientCompat {
    private ActuallyAdditionsClientCompat() {}

    static void init(IEventBus modBus) {
        modBus.addListener(ActuallyAdditionsClientCompat::registerScreens);
        modBus.addListener(ActuallyAdditionsClientCompat::registerRenderers);
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(NepActuallyAdditionsContent.MATRIX_MENU.get(), AtomicEmpoweringMatrixScreen::new);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                NepActuallyAdditionsContent.MATRIX_BLOCK_ENTITY.get(),
                context -> new MatrixCoreRenderer<>(MatrixCoreProfile.EMPOWERING));
    }
}
