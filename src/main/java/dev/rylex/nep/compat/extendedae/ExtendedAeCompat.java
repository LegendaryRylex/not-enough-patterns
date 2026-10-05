package dev.rylex.nep.compat.extendedae;

import com.glodblock.github.extendedae.container.pattern.PatternGuiHandler;
import dev.rylex.nep.pattern.AndesiteCraftingPattern;
import dev.rylex.nep.pattern.ApparatusPattern;
import dev.rylex.nep.pattern.AtomicReconstructionPattern;
import dev.rylex.nep.pattern.AwakeningPattern;
import dev.rylex.nep.pattern.EmpoweringPattern;
import dev.rylex.nep.pattern.EnchantingPattern;
import dev.rylex.nep.pattern.FusionCraftingPattern;
import dev.rylex.nep.pattern.ImbuementPattern;
import dev.rylex.nep.pattern.InfusionPattern;
import dev.rylex.nep.pattern.MechanicalCraftingPattern;
import dev.rylex.nep.pattern.MiniaturizationPattern;
import dev.rylex.nep.pattern.NepPattern;
import dev.rylex.nep.pattern.RuneworkingPattern;
import dev.rylex.nep.pattern.SequencedAssemblyPattern;
import dev.rylex.nep.pattern.SpiritFocusingPattern;
import dev.rylex.nep.pattern.SpiritInfusionPattern;
import java.util.List;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

public final class ExtendedAeCompat {
    private ExtendedAeCompat() {}

    private static final List<Class<? extends NepPattern>> VIEWABLE_PATTERNS = List.of(
            AndesiteCraftingPattern.class,
            ApparatusPattern.class,
            AtomicReconstructionPattern.class,
            AwakeningPattern.class,
            EmpoweringPattern.class,
            EnchantingPattern.class,
            FusionCraftingPattern.class,
            ImbuementPattern.class,
            InfusionPattern.class,
            MechanicalCraftingPattern.class,
            MiniaturizationPattern.class,
            RuneworkingPattern.class,
            SequencedAssemblyPattern.class,
            SpiritFocusingPattern.class,
            SpiritInfusionPattern.class);

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
