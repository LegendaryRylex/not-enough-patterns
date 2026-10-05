package dev.rylex.nep.compat.ars;

import dev.rylex.nep.client.MatrixCoreProfile;
import dev.rylex.nep.client.MatrixCoreRenderer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

final class ArsClientCompat {
    private ArsClientCompat() {}

    static void init(IEventBus modBus) {
        modBus.addListener(ArsClientCompat::registerScreens);
        modBus.addListener(ArsClientCompat::registerRenderers);
        modBus.addListener(ArsClientCompat::registerModels);
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(NepArsContent.RITUAL_CONDUCTOR_MENU.get(), RitualConductorScreen::new);
        event.register(NepArsContent.MATRIX_MENU.get(), ArcaneEnchantingMatrixScreen::new);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(NepArsContent.ARCANE_LECTERN_BLOCK_ENTITY.get(), ArcaneLecternRenderer::new);
        event.registerBlockEntityRenderer(
                NepArsContent.RITUAL_CONDUCTOR_BLOCK_ENTITY.get(), RitualConductorRenderer::new);
        event.registerBlockEntityRenderer(
                NepArsContent.MATRIX_BLOCK_ENTITY.get(),
                context -> new MatrixCoreRenderer<>(MatrixCoreProfile.ARCANE_ENCHANTING));
    }

    private static void registerModels(ModelEvent.RegisterAdditional event) {
        event.register(RitualConductorRenderer.GEM);
    }
}
