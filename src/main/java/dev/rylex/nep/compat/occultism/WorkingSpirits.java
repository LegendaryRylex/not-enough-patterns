package dev.rylex.nep.compat.occultism;

import com.klikli_dev.occultism.common.entity.job.CrusherJob;
import com.klikli_dev.occultism.common.entity.job.CrystallizerJob;
import com.klikli_dev.occultism.common.entity.job.SmelterJob;
import com.klikli_dev.occultism.common.entity.job.SpiritJob;
import com.klikli_dev.occultism.common.entity.job.TraderJob;
import com.klikli_dev.occultism.common.entity.spirit.SpiritEntity;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

final class WorkingSpirits {
    private WorkingSpirits() {}

    static List<SpiritEntity> serving(Level level, BlockPos pos, int range) {
        Vec3 centre = Vec3.atCenterOf(pos);
        return level.getEntitiesOfClass(
                SpiritEntity.class,
                new AABB(pos).inflate(range),
                spirit -> spirit.isAlive()
                        && processes(spirit)
                        && workArea(spirit).contains(centre));
    }

    static boolean covers(SpiritEntity spirit, BlockPos pos) {
        return workArea(spirit).contains(Vec3.atCenterOf(pos));
    }

    static boolean processes(SpiritEntity spirit) {
        return spirit.getJob().filter(WorkingSpirits::isProcessing).isPresent();
    }

    static boolean accepts(SpiritEntity spirit, ItemStack stack) {
        return spirit.getJob().map(job -> accepts(job, stack)).orElse(false);
    }

    static boolean idle(SpiritEntity spirit) {
        return spirit.getItemInHand(InteractionHand.MAIN_HAND).isEmpty();
    }

    private static boolean accepts(SpiritJob job, ItemStack stack) {
        if (!isProcessing(job)) {
            return false;
        }
        for (Ingredient ingredient : job.getItemsToPickUp()) {
            if (ingredient.test(stack)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isProcessing(SpiritJob job) {
        return job instanceof CrusherJob
                || job instanceof CrystallizerJob
                || job instanceof SmelterJob
                || job instanceof TraderJob;
    }

    /** Mirrors the box Occultism's PickupItemsGoal searches: half height, centred on the work area centre. */
    private static AABB workArea(SpiritEntity spirit) {
        int size = spirit.getWorkAreaSize().getValue();
        return new AABB(-size, -size / 2.0, -size, size, size / 2.0, size).move(spirit.getWorkAreaCenter());
    }
}
