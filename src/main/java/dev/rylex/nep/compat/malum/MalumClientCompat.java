package dev.rylex.nep.compat.malum;

import com.sammy.malum.common.item.spirit.SpiritShardItem;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.client.MatrixCoreProfile;
import dev.rylex.nep.client.MatrixCoreRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RecipesUpdatedEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;
import team.lodestar.lodestone.handlers.screenparticle.ParticleEmitterHandler;
import team.lodestar.lodestone.helpers.ColorHelper;

final class MalumClientCompat {
    private MalumClientCompat() {}

    private static Runnable jeiRefresh = () -> {};

    static void onJeiRuntime(Runnable refresh) {
        jeiRefresh = refresh;
    }

    static void init(IEventBus modBus) {
        modBus.addListener(MalumClientCompat::registerScreens);
        modBus.addListener(MalumClientCompat::registerRenderers);
        modBus.addListener(MalumClientCompat::registerItemColors);
        modBus.addListener(MalumClientCompat::onConfigLoaded);
        modBus.addListener(MalumClientCompat::onConfigReloaded);
        NeoForge.EVENT_BUS.addListener(VoidSecretsTooltip::onTooltip);
        NeoForge.EVENT_BUS.addListener(MalumClientCompat::onRecipesUpdated);
        NeoForge.EVENT_BUS.addListener(MalumClientCompat::onLoggingIn);
    }

    private static void onRecipesUpdated(RecipesUpdatedEvent event) {
        Minecraft.getInstance().execute(() -> {
            VoidSecretsTooltip.warm();
            jeiRefresh.run();
        });
    }

    /**
     * Lodestone's own sweep runs at client setup, before some mod orderings have their items in hand, so the shards are
     * claimed again once a world is up and only when nothing already claimed them.
     */
    private static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        claimScreenParticles(NepMalumContent.PURE_SPIRIT.get());
        claimScreenParticles(NepMalumContent.RADIANT_SPIRIT.get());
    }

    private static void claimScreenParticles(SpiritShardItem shard) {
        if (!ParticleEmitterHandler.EMITTERS.containsKey(shard)) {
            ParticleEmitterHandler.registerItemParticleEmitter(
                    (Item) shard, (ParticleEmitterHandler.ItemParticleSupplier) shard);
        }
    }

    private static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register(
                (stack, tintIndex) -> ColorHelper.getColor(
                        ((SpiritShardItem) stack.getItem()).getSpiritHolder().getItemColor()),
                NepMalumContent.PURE_SPIRIT.get());
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(NepMalumContent.MATRIX_MENU.get(), FocusedSpiritMatrixScreen::new);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                NepMalumContent.MATRIX_BLOCK_ENTITY.get(),
                context -> new MatrixCoreRenderer<>(MatrixCoreProfile.FOCUSED_SPIRIT));
    }

    private static void onConfigLoaded(ModConfigEvent.Loading event) {
        refreshJei(event);
    }

    private static void onConfigReloaded(ModConfigEvent.Reloading event) {
        refreshJei(event);
    }

    private static void refreshJei(ModConfigEvent event) {
        if (event.getConfig().getSpec() == NepConfig.SPEC) {
            Minecraft.getInstance().execute(jeiRefresh);
        }
    }
}
