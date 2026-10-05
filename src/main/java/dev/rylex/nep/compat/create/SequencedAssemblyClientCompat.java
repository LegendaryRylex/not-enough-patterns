package dev.rylex.nep.compat.create;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.rylex.nep.Nep;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;

final class SequencedAssemblyClientCompat {
    private SequencedAssemblyClientCompat() {}

    /** Created during mod construction, since Flywheel only loads partials that exist by model registration. */
    private static final PartialModel PORT_STUBS = PartialModel.of(Nep.id("block/matrix/port_stub"));

    private static final PartialModel COG = PartialModel.of(Nep.id("block/machine/assembly_cog"));

    static void init(IEventBus modBus) {
        modBus.addListener(SequencedAssemblyClientCompat::registerScreens);
        modBus.addListener(SequencedAssemblyClientCompat::registerRenderers);
        NeoForge.EVENT_BUS.addListener(MatrixStressTooltip::onTooltip);
        AssemblyLinkVisualizer.init();
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(NepCreateContent.CONTROLLER_MENU.get(), SequencedAssemblyControllerScreen::new);
        event.register(NepCreateContent.MATRIX_MENU.get(), SequencedAssemblyMatrixScreen::new);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                NepCreateContent.MATRIX_BLOCK_ENTITY.get(), context -> new SequencedAssemblyMatrixRenderer(PORT_STUBS));
        event.registerBlockEntityRenderer(
                NepCreateContent.CONTROLLER_BLOCK_ENTITY.get(),
                context -> new SequencedAssemblyControllerRenderer(COG));
    }
}
