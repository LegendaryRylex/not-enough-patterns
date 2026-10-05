package dev.rylex.nep.compat.malum;

import com.sammy.malum.core.systems.registry.DeferredSpiritTypes;
import com.sammy.malum.core.systems.registry.SpiritHolder;
import com.sammy.malum.core.systems.spirit.type.SpiritArcanaType;
import com.sammy.malum.core.systems.spirit.type.SpiritColorProperties;
import dev.rylex.nep.Nep;
import java.awt.Color;
import net.neoforged.bus.api.IEventBus;
import team.lodestar.lodestone.systems.easing.Easing;

final class NepSpirits {
    private NepSpirits() {}

    private static final DeferredSpiritTypes SPIRIT_TYPES = DeferredSpiritTypes.create(Nep.MOD_ID);

    static final SpiritHolder<SpiritArcanaType> RADIANT = SPIRIT_TYPES.register(
            "radiant", () -> new SpiritArcanaType(radiantColors(), NepMalumContent.RADIANT_SPIRIT));

    static final SpiritHolder<SpiritArcanaType> PURE =
            SPIRIT_TYPES.register("pure", () -> new SpiritArcanaType(pureColors(), NepMalumContent.PURE_SPIRIT));

    static void register(IEventBus modBus) {
        SPIRIT_TYPES.register(modBus);
    }

    private static SpiritColorProperties radiantColors() {
        return SpiritColorProperties.create(new Color(255, 96, 160), new Color(96, 224, 255))
                .setColorCoefficient(1.2F)
                .setColorEasing(Easing.SINE_IN_OUT)
                .setItemColor(Color.WHITE)
                .build();
    }

    private static SpiritColorProperties pureColors() {
        return SpiritColorProperties.create(new Color(245, 246, 250), new Color(168, 176, 196))
                .setColorCoefficient(0.8F)
                .setColorEasing(Easing.QUAD_IN)
                .setItemColor(new Color(248, 249, 255))
                .build();
    }
}
