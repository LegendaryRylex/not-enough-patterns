package dev.rylex.nep.compat.malum;

import com.sammy.malum.common.entity.activator.EntityRiteEffectActivatorEntity;
import com.sammy.malum.core.systems.rite.effect.SpiritRiteEntityEffect;
import java.util.UUID;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Shares Malum's spark entity type, so a spark that survives a chunk reload comes back as Malum's slower base spark.
 */
public class ReapingSparkEntity extends EntityRiteEffectActivatorEntity {

    private static final int WIND_UP_TICKS = 8;
    private static final float TOP_SPEED = 3.0F;
    private static final float APPROACH_SPEED = 0.4F;
    private static final float STEERING = 0.5F;

    public ReapingSparkEntity(
            Level level, SpiritRiteEntityEffect<?> effect, UUID target, Vec3 position, Vec3 velocity) {
        super(level, effect, target, position, velocity);
        this.maxAge = 200;
    }

    @Override
    public int getWindUpDuration() {
        return WIND_UP_TICKS;
    }

    /** The distance Malum hands over is squared, and the wind-up curve only ever releases three quarters of this. */
    @Override
    public float getMovementSpeed(float windUp, float distance) {
        return Mth.clamp(Mth.sqrt(distance) * 0.5F, APPROACH_SPEED, TOP_SPEED) / 0.75F;
    }

    @Override
    public float getMovementEasing(float windUp, float distance) {
        return STEERING;
    }
}
