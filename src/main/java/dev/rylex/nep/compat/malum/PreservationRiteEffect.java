package dev.rylex.nep.compat.malum;

import com.sammy.malum.common.block.curiosities.spirit_crucible.SpiritCrucibleCoreBlockEntity;
import com.sammy.malum.core.systems.rite.effect.SpiritRiteEffect;
import com.sammy.malum.core.systems.rite.effect.SpiritRiteEffectTag;
import com.sammy.malum.core.systems.spirit.type.SpiritArcanaType;
import com.sammy.malum.registry.common.MalumParticleEffectTypes;
import com.sammy.malum.visual_effects.networked.MalumNetworkedParticleEffectColorData;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public class PreservationRiteEffect extends SpiritRiteEffect {

    static final int RANGE = 8;

    private static final int WARD_TICKS = 160;

    public PreservationRiteEffect() {
        super(SpiritRiteEffectTag.RADIAL_EFFECT, SpiritRiteEffectTag.GREATER_RITE);
    }

    @Override
    public boolean triggerRiteEffect(
            ServerLevel level, BlockPos pos, SpiritArcanaType definingSpirit, RiteParameters parameters) {
        boolean[] warded = new boolean[1];
        RiteNetwork.forEachBlockEntity(level, pos, RANGE, blockEntity -> {
            if (blockEntity instanceof FocusedSpiritMatrixBlockEntity matrix) {
                matrix.preserveImpetus(WARD_TICKS);
            } else if (blockEntity instanceof SpiritCrucibleCoreBlockEntity crucible) {
                ImpetusWard.ward(crucible, WARD_TICKS);
            } else {
                return;
            }
            warded[0] = true;
            MalumParticleEffectTypes.BLOCK_RITE_EFFECT
                    .createEffect(blockEntity.getBlockPos())
                    .color(MalumNetworkedParticleEffectColorData.fromSpirits(List.of(NepSpirits.PURE)))
                    .spawn(level);
        });
        return warded[0];
    }
}
