package dev.rylex.nep.util;

import dev.rylex.nep.compat.sable.SableSubLevels;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

public final class SubLevels {

    private static final boolean COMPANION_LOADED = ModList.get().isLoaded("sablecompanion");

    public record Transform(Vec3 position, Vec3 scale, Vec3 rotationPoint, Quaternionf orientation) {}

    private SubLevels() {}

    public static boolean sameSubLevel(@Nullable Level level, BlockPos a, BlockPos b) {
        return !COMPANION_LOADED || SableSubLevels.sameSubLevel(level, a, b);
    }

    public static double distanceSqr(Player player, BlockPos pos) {
        return COMPANION_LOADED ? SableSubLevels.distanceSqr(player, pos) : player.distanceToSqr(Vec3.atCenterOf(pos));
    }

    @Nullable
    public static Transform renderTransform(@Nullable Level level, BlockPos pos, float partialTick) {
        return COMPANION_LOADED ? SableSubLevels.renderTransform(level, pos, partialTick) : null;
    }
}
