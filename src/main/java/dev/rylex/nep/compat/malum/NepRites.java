package dev.rylex.nep.compat.malum;

import com.sammy.malum.core.systems.registry.rite.DeferredRiteEntityEffectTypes;
import com.sammy.malum.core.systems.registry.rite.DeferredRiteTypes;
import com.sammy.malum.core.systems.registry.rite.RiteEffectHolder;
import com.sammy.malum.core.systems.registry.rite.RiteHolder;
import com.sammy.malum.core.systems.rite.SpiritRiteType;
import com.sammy.malum.core.systems.rite.SpiritRiteTypeBuilder;
import com.sammy.malum.registry.common.magic.MalumSpiritTypes;
import dev.rylex.nep.Nep;
import net.neoforged.bus.api.IEventBus;

final class NepRites {
    private NepRites() {}

    private static final DeferredRiteEntityEffectTypes EFFECT_TYPES = DeferredRiteEntityEffectTypes.create(Nep.MOD_ID);
    private static final DeferredRiteTypes RITE_TYPES = DeferredRiteTypes.create(Nep.MOD_ID);

    static final RiteEffectHolder<PreservationRiteEffect> PRESERVATION_EFFECT =
            EFFECT_TYPES.register("preservation_effect", PreservationRiteEffect::new);

    static final RiteEffectHolder<ReapingRiteEffect> REAPING_EFFECT =
            EFFECT_TYPES.register("reaping_effect", ReapingRiteEffect::new);

    static final RiteHolder<SpiritRiteType> RITE_OF_PRESERVATION =
            RITE_TYPES.register("rite_of_preservation", () -> SpiritRiteTypeBuilder.create(
                            MalumSpiritTypes.ELDRITCH_SPIRIT,
                            MalumSpiritTypes.ARCANE_SPIRIT,
                            MalumSpiritTypes.SACRED_SPIRIT,
                            MalumSpiritTypes.SACRED_SPIRIT,
                            NepSpirits.PURE)
                    .build(PRESERVATION_EFFECT));

    static final RiteHolder<SpiritRiteType> RITE_OF_REAPING =
            RITE_TYPES.register("rite_of_reaping", () -> SpiritRiteTypeBuilder.create(
                            MalumSpiritTypes.ELDRITCH_SPIRIT,
                            MalumSpiritTypes.ARCANE_SPIRIT,
                            MalumSpiritTypes.WICKED_SPIRIT,
                            MalumSpiritTypes.WICKED_SPIRIT,
                            NepSpirits.PURE)
                    .setCorrupted()
                    .build(REAPING_EFFECT));

    static void register(IEventBus modBus) {
        EFFECT_TYPES.register(modBus);
        RITE_TYPES.register(modBus);
    }
}
