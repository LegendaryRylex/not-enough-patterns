package dev.rylex.nep.compat.extendedae;

import com.glodblock.github.extendedae.container.pattern.PatternGuiHandler;
import dev.rylex.nep.pattern.AwakeningPattern;
import dev.rylex.nep.pattern.EnchantingPattern;
import dev.rylex.nep.pattern.InfusionPattern;
import dev.rylex.nep.pattern.NepPattern;
import java.util.List;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

public final class ExtendedAeCompat {
    private ExtendedAeCompat() {}

    private static final List<Class<? extends NepPattern>> VIEWABLE_PATTERNS =
            List.of(AwakeningPattern.class, EnchantingPattern.class, InfusionPattern.class);

    public static void init(IEventBus modBus, Dist dist) {
        NepExtendedAeContent.register(modBus);
        modBus.addListener(ExtendedAeCompat::commonSetup);
        if (dist.isClient()) {
            ExtendedAeClientCompat.init(modBus);
        }
    }

    /**
     * ExtendedAE fills its pattern view map from the parallel setup thread, so this has to wait for the main thread
     * rather than race it.
     */
    private static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            for (Class<? extends NepPattern> pattern : VIEWABLE_PATTERNS) {
                PatternGuiHandler.addPatternHandler(pattern, PatternViewMenu.ID);
            }
        });
    }
}
