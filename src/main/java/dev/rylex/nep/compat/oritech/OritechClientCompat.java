package dev.rylex.nep.compat.oritech;

import dev.rylex.nep.client.MatrixCoreProfile;
import dev.rylex.nep.client.MatrixCoreRenderer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

final class OritechClientCompat {
    private OritechClientCompat() {}

    static void init(IEventBus modBus) {
        modBus.addListener(OritechClientCompat::registerScreens);
        modBus.addListener(OritechClientCompat::registerRenderers);
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(NepOritechContent.CONTROLLER_MENU.get(), ParticleCollisionControllerScreen::new);
        event.register(NepOritechContent.MATRIX_MENU.get(), ParticleCollisionMatrixScreen::new);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                NepOritechContent.MATRIX_BLOCK_ENTITY.get(),
                context -> new MatrixCoreRenderer<>(MatrixCoreProfile.PARTICLE_COLLISION));
    }
}
