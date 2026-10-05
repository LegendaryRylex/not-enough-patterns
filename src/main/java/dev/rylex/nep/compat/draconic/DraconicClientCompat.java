package dev.rylex.nep.compat.draconic;

import dev.rylex.nep.client.MatrixCoreProfile;
import dev.rylex.nep.client.MatrixCoreRenderer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

final class DraconicClientCompat {
    private DraconicClientCompat() {}

    static void init(IEventBus modBus) {
        modBus.addListener(DraconicClientCompat::registerScreens);
        modBus.addListener(DraconicClientCompat::registerRenderers);
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(NepDraconicContent.MATRIX_MENU.get(), FusionMatrixScreen::new);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                NepDraconicContent.MATRIX_BLOCK_ENTITY.get(),
                context -> new MatrixCoreRenderer<>(MatrixCoreProfile.FUSION));
    }
}
