package dev.rylex.nep.compat.occultism;

import dev.rylex.nep.Nep;
import dev.rylex.nep.client.MatrixCoreProfile;
import dev.rylex.nep.client.MatrixCoreRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

final class OccultismClientCompat {
    private OccultismClientCompat() {}

    static void init(IEventBus modBus) {
        modBus.addListener(OccultismClientCompat::registerScreens);
        modBus.addListener(OccultismClientCompat::registerRenderers);
        modBus.addListener(OccultismClientCompat::clientSetup);
    }

    private static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemProperties.register(
                NepOccultismContent.SPIRIT_INFUSED_BOOK.get(), Nep.id("bound_spirit"), (stack, level, entity, seed) -> {
                    BoundSpirit spirit = stack.get(NepOccultismContent.BOUND_SPIRIT.get());
                    return spirit == null
                            ? 0
                            : spirit.tier().ordinal() * 2 + spirit.work().ordinal() + 1;
                }));
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
