package dev.rylex.nep.compat.malum;

import appeng.api.networking.IGrid;
import com.sammy.malum.common.entity.FloatingItemDestinationData;
import com.sammy.malum.common.entity.activator.EntityRiteEffectActivatorEntity;
import com.sammy.malum.common.entity.spirit.SpiritItemEntity;
import com.sammy.malum.core.systems.rite.effect.SpiritRiteEffectTag;
import com.sammy.malum.core.systems.rite.effect.SpiritRiteEntityEffect;
import com.sammy.malum.core.systems.spirit.type.SpiritArcanaType;
import com.sammy.malum.registry.common.MalumDamageTypes;
import com.sammy.malum.registry.common.MalumSoundEvents;
import com.sammy.malum.registry.common.magic.MalumSpiritTypes;
import dev.rylex.nep.NepConfig;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import team.lodestar.lodestone.helpers.DamageTypeHelper;
import team.lodestar.lodestone.helpers.RandomHelper;
import team.lodestar.lodestone.helpers.SoundHelper;

public class ReapingRiteEffect extends SpiritRiteEntityEffect<Monster> {

    public ReapingRiteEffect() {
        super(List.of(SpiritRiteEffectTag.GREATER_RITE));
    }

    @Override
    public Class<Monster> getTargetClass() {
        return Monster.class;
    }

    @Override
    public int getEffectRange() {
        return NepConfig.malumReapingreapingRiteKillRange();
    }

    @Override
    public int getCooldown() {
        return NepConfig.malumReapingSweepTicks();
    }

    @Override
    public void applyEffect(ServerLevel level, Monster target) {
        target.hurt(DamageTypeHelper.create(level, MalumDamageTypes.VOODOO_PLAYERLESS), target.getMaxHealth() * 2.0F);
        createEffect(level, target, MalumSpiritTypes.ELDRITCH_SPIRIT, NepSpirits.PURE);
    }

    @Override
    public boolean canApplyEffect(ServerLevel level, Monster target) {
        return !target.isInvulnerableTo(DamageTypeHelper.create(level, MalumDamageTypes.VOODOO_PLAYERLESS))
                && target.getHealth() <= target.getMaxHealth() * NepConfig.malumReapingExecutionThreshold();
    }

    @Override
    public boolean triggerRiteEffect(
            ServerLevel level, BlockPos pos, SpiritArcanaType definingSpirit, RiteParameters parameters) {
        List<Monster> targets = findNearbyTargets(level, pos);
        boolean struck = NepConfig.malumReapingSparks()
                ? launchSparks(level, pos, definingSpirit, parameters, targets)
                : strike(level, targets);
        return gatherSpirits(level, pos) || struck;
    }

    private boolean strike(ServerLevel level, List<Monster> targets) {
        RandomSource random = level.getRandom();
        for (Monster target : targets) {
            applyEffect(level, target);
            SoundHelper.playSound(
                    target, getImpactSound().value(), getImpactSoundVolume(target), Mth.nextFloat(random, 0.9F, 1.1F));
        }
        return !targets.isEmpty();
    }

    private boolean launchSparks(
            ServerLevel level,
            BlockPos pos,
            SpiritArcanaType definingSpirit,
            RiteParameters parameters,
            List<Monster> targets) {
        if (targets.isEmpty()) {
            return false;
        }
        Set<UUID> claimed = sparkTargets(level, pos);
        RandomSource random = level.getRandom();
        Vec3 origin = pos.getCenter().add(0.0, parameters.getTotemHeight(), 0.0);
        boolean launched = false;
        for (Monster target : targets) {
            if (!claimed.add(target.getUUID())) {
                continue;
            }
            Vec3 velocity = new Vec3(scatter(random), RandomHelper.randomBetween(random, 0.1F, 0.2F), scatter(random));
            ReapingSparkEntity spark = new ReapingSparkEntity(level, this, target.getUUID(), origin, velocity);
            spark.setSpirit(definingSpirit);
            level.addFreshEntity(spark);
            SoundHelper.playSound(spark, MalumSoundEvents.SPARK_FORMED.get(), 0.5F, Mth.nextFloat(random, 0.9F, 1.1F));
            launched = true;
        }
        return launched;
    }

    private Set<UUID> sparkTargets(ServerLevel level, BlockPos pos) {
        Set<UUID> claimed = new HashSet<>();
        AABB area = new AABB(pos).inflate(getEffectRange() * 2.0);
        for (EntityRiteEffectActivatorEntity spark :
                level.getEntitiesOfClass(EntityRiteEffectActivatorEntity.class, area)) {
            FloatingItemDestinationData destination = spark.getDestination();
            if (destination != null) {
                destination.getTargetLocation().ifLeft(claimed::add);
            }
        }
        return claimed;
    }

    private static double scatter(RandomSource random) {
        return RandomHelper.randomBetween(random, 0.3F, 0.6F) * (random.nextBoolean() ? 1 : -1);
    }

    private boolean gatherSpirits(ServerLevel level, BlockPos pos) {
        List<IGrid> grids = RiteNetwork.gridsNear(level, pos, NepConfig.malumReapingNetworkRange());
        if (grids.isEmpty()) {
            return false;
        }
        List<SpiritItemEntity> loose = level.getEntitiesOfClass(
                SpiritItemEntity.class, new AABB(pos).inflate(getEffectRange()), entity -> entity.isAlive());
        boolean gathered = false;
        for (SpiritItemEntity spirit : loose) {
            ItemStack stack = spirit.getItem();
            long moved = RiteNetwork.insertNearest(grids, stack);
            if (moved <= 0) {
                continue;
            }
            gathered = true;
            stack.shrink((int) moved);
            if (stack.isEmpty()) {
                spirit.discard();
            } else {
                spirit.setItem(stack);
            }
        }
        return gathered;
    }
}
