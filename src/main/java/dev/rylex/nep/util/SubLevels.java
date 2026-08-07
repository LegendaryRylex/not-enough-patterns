package dev.rylex.nep.util;

import dev.ryanhcode.sable.companion.ClientSubLevelAccess;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class SubLevels {

    private SubLevels() {}

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
    public static Pose3dc renderPose(@Nullable Level level, BlockPos pos, float partialTick) {
        SubLevelAccess sub = containing(level, pos);
        if (sub == null) {
            return null;
        }
        return sub instanceof ClientSubLevelAccess client ? client.renderPose(partialTick) : sub.logicalPose();
    }
}
