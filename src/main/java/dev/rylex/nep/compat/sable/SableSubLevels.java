package dev.rylex.nep.compat.sable;

import dev.ryanhcode.sable.companion.ClientSubLevelAccess;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.rylex.nep.util.SubLevels;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniondc;
import org.joml.Quaternionf;
import org.joml.Vector3dc;

public final class SableSubLevels {

    private SableSubLevels() {}

    @Nullable
    private static SubLevelAccess containing(@Nullable Level level, BlockPos pos) {
        return level == null ? null : SableCompanion.INSTANCE.getContaining(level, pos);
    }

    public static boolean sameSubLevel(@Nullable Level level, BlockPos a, BlockPos b) {
        return containing(level, a) == containing(level, b);
    }

    public static double distanceSqr(Player player, BlockPos pos) {
        Vec3 center = Vec3.atCenterOf(pos);
        return SableCompanion.INSTANCE.distanceSquaredWithSubLevels(
                player.level(), player.position(), center.x, center.y, center.z);
    }

    @Nullable
    public static SubLevels.Transform renderTransform(@Nullable Level level, BlockPos pos, float partialTick) {
        SubLevelAccess sub = containing(level, pos);
        if (sub == null) {
            return null;
        }
        Pose3dc pose = sub instanceof ClientSubLevelAccess client ? client.renderPose(partialTick) : sub.logicalPose();
        if (pose == null) {
            return null;
        }
        Vector3dc position = pose.position();
        Vector3dc scale = pose.scale();
        Vector3dc rotationPoint = pose.rotationPoint();
        Quaterniondc orientation = pose.orientation();
        return new SubLevels.Transform(
                new Vec3(position.x(), position.y(), position.z()),
                new Vec3(scale.x(), scale.y(), scale.z()),
                new Vec3(rotationPoint.x(), rotationPoint.y(), rotationPoint.z()),
                new Quaternionf((float) orientation.x(), (float) orientation.y(), (float) orientation.z(), (float)
                        orientation.w()));
    }
}
