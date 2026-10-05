package dev.rylex.nep.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.rylex.nep.machine.MatrixRenderHost;
import dev.rylex.nep.machine.MatrixRenderState;
import dev.rylex.nep.machine.MatrixRenderState.Motion;
import dev.rylex.nep.machine.MatrixStatus;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class MatrixCoreRenderer<T extends BlockEntity & MatrixRenderHost> implements BlockEntityRenderer<T> {

    private static final int VIEW_DISTANCE = 48;
    private static final float BASE_SCALE = 0.215F;
    private static final float TILT_RATIO = 0.37F;
    private static final float PULSE_RATE = 0.15F;
    private static final float FLASH_SWELL = 0.06F;
    private static final float FLASH_GLOW = 0.5F;
    private static final float SHUDDER_RATE = 2.7F;
    private static final float SHUDDER_REACH = 0.012F;
    private static final float SHUDDER_OFFSET = 18.9F;
    private static final float PULSE_RUNNING = 0.08F;
    private static final float PULSE_STALLED = 0.02F;
    private static final float PULSE_IDLE = 0.03F;

    private static final float RING_SPIN = 0.6F;
    private static final float RING_OFFSET = 120.0F;
    private static final float RING_BRIGHTNESS = 1.0F;
    private static final float WANDER_RATE = 0.4F;
    private static final float WANDER_REACH = 55.0F;
    private static final float WANDER_SWAY = 25.0F;
    private static final float WANDER_STEP = 2.4F;

    /** Whole multiples of the wander phase, so every wander angle stays continuous across its wrap. */
    private static final int[] PRECESS_HARMONICS = {1, 2};

    private static final int[] WOBBLE_HARMONICS = {3, 1};
    private static final Axis[] PIVOTS = {Axis.YP, Axis.ZP, Axis.XP};
    private static final float[] DIRECTIONS = {1.0F, -1.0F, 1.0F};

    private static final float BROAD_TILING = 1.0F;
    private static final float BROAD_STRENGTH = 0.85F;
    private static final float BROAD_DRIFT = 0.0024F;
    private static final float DRIFT_GAIN = 0.0060F;
    private static final float DRIFT_SKEW = 0.4F;

    private static final float CORE = 0.95F;
    /** The glow shell is a coarser icosphere than the core, so its facets sink under the core unless stood off. */
    private static final float SKIN = 1.05F;

    private static final float SKIN_SPIN = -0.35F;
    private static final float SKIN_TILT = 0.6F;
    private static final float SKIN_SHIMMER = 0.22F;
    private static final float SKIN_SHIMMER_RATE = 0.21F;

    private static final float FINE_TILING = 2.4F;
    private static final float FINE_STRENGTH = 0.40F;
    private static final float FINE_DRIFT = 0.0110F;
    private static final float FINE_GAIN = 0.0180F;
    private static final float COUNTER_SKEW = 0.7F;

    /** The second plasma pass takes the shell's other projection, so its pole pinch never lands on the first's. */
    private static final float CROSS_TILING = 3.1F;

    private static final float CROSS_STRENGTH = 0.32F;
    private static final float CROSS_DRIFT = 0.0074F;
    private static final float CROSS_GAIN = 0.0130F;
    private static final float CROSS_SKEW = 0.55F;

    private static final float HALO = 1.70F;
    private static final float HALO_BREATH = 0.03F;
    private static final float TILING_BREATH = 0.06F;
    private static final float BREATH_RATE = 0.09F;
    private static final float BREATH_RATIO = 0.6F;
    private static final float FLASH_SURGE = 0.6F;

    /** The widest the solid core ever reaches, in blocks. */
    public static final float CORE_REACH = BASE_SCALE * CORE * (1.0F + PULSE_RUNNING) * (1.0F + FLASH_SWELL);

    private final MatrixCoreProfile profile;

    public MatrixCoreRenderer(MatrixCoreProfile profile) {
        this.profile = profile;
    }

    @Override
    public void render(
            T matrix,
            float partialTick,
            PoseStack pose,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay) {
        Level level = matrix.getLevel();
        if (level == null) {
            return;
        }
        matrix.advanceRender(partialTick);
        MatrixRenderState motion = matrix.renderState();
        MatrixStatus status = matrix.getBlockState().getValue(MatrixStatus.PROPERTY);
        float progress = motion.progress();
        float flash = motion.flash();
        float phase = phaseFor(matrix.getBlockPos());
        float turn = spinRate(status, progress);

        float scale = BASE_SCALE * pulse(motion, phase, status) * (1.0F + flash * FLASH_SWELL);
        float yaw = motion.motion(Motion.YAW, turn) + phase * Mth.RAD_TO_DEG;
        float pitch = motion.motion(Motion.PITCH, turn * TILT_RATIO) + phase * Mth.RAD_TO_DEG;
        float skinYaw = motion.motion(Motion.SKIN_YAW, turn * SKIN_SPIN) - phase * Mth.RAD_TO_DEG;
        float skinRoll = motion.motion(Motion.SKIN_ROLL, turn * SKIN_SPIN * SKIN_TILT) + phase * Mth.RAD_TO_DEG;

        pose.pushPose();
        pose.translate(0.5F, 0.5F, 0.5F);
        if (status == MatrixStatus.STALLED) {
            pose.translate(shudder(motion, phase), shudder(motion, phase + SHUDDER_OFFSET), 0.0F);
        }

        float core = scale * CORE;
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        pose.mulPose(Axis.XP.rotationDegrees(pitch));
        pose.scale(core, core, core);
        VertexConsumer facets = buffers.getBuffer(RenderType.entityCutoutNoCull(profile.texture()));
        profile.mesh()
                .emit(
                        pose.last(),
                        facets,
                        brightness(status, progress, flash),
                        1.0F,
                        LightTexture.FULL_BRIGHT,
                        packedOverlay);
        pose.popPose();

        float alignment = motion.gimbal(status);
        float wander = motion.motion(Motion.WANDER, status == MatrixStatus.STALLED ? 0.0F : WANDER_RATE);
        float ring = motion.motion(Motion.RING, ringRate(status, progress));
        VertexConsumer bars = buffers.getBuffer(RenderType.entityCutoutNoCull(profile.ring()));
        for (int index = 0; index < MatrixRingMesh.RINGS.length; index++) {
            pose.pushPose();
            if (alignment > 0.0F) {
                pose.mulPose(PIVOTS[(index + 1) % PIVOTS.length].rotationDegrees(
                        alignment * wanderAngle(wander, phase, index, PRECESS_HARMONICS)));
                pose.mulPose(PIVOTS[(index + 2) % PIVOTS.length].rotationDegrees(
                        alignment * wanderAngle(wander, phase, index, WOBBLE_HARMONICS)));
            }
            pose.mulPose(PIVOTS[index].rotationDegrees(
                    DIRECTIONS[index] * ring + index * RING_OFFSET + phase * Mth.RAD_TO_DEG));
            MatrixRingMesh.RINGS[index].emit(pose.last(), bars, RING_BRIGHTNESS, packedLight, packedOverlay);
            pose.popPose();
        }

        VertexConsumer plasma = buffers.getBuffer(RenderType.eyes(profile.plasma()));
        float strength = glow(status, progress, flash);
        float counter = -(FINE_DRIFT + FINE_GAIN * progress);
        float cross = CROSS_DRIFT + CROSS_GAIN * progress;
        float tiling = FINE_TILING * (1.0F + TILING_BREATH * motion.wave(BREATH_RATE * BREATH_RATIO, phase));
        float shimmer = SKIN_SHIMMER * motion.wave(SKIN_SHIMMER_RATE, phase);
        float skin = scale * SKIN;
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(skinYaw));
        pose.mulPose(Axis.ZP.rotationDegrees(skinRoll));
        pose.scale(skin, skin, skin);
        MatrixCoreShell.SPHERE.emit(
                pose.last(),
                plasma,
                MatrixCoreShell.Layer.FINE,
                profile.coreTint(),
                FINE_STRENGTH * strength * (1.0F + shimmer),
                tiling,
                motion.motion(Motion.FINE_U, counter),
                motion.motion(Motion.FINE_V, counter * COUNTER_SKEW),
                LightTexture.FULL_BRIGHT,
                packedOverlay);
        MatrixCoreShell.SPHERE.emit(
                pose.last(),
                plasma,
                MatrixCoreShell.Layer.BROAD,
                profile.coreTint(),
                CROSS_STRENGTH * strength * (1.0F - shimmer),
                CROSS_TILING,
                motion.motion(Motion.CROSS_U, cross),
                motion.motion(Motion.CROSS_V, cross * CROSS_SKEW),
                LightTexture.FULL_BRIGHT,
                packedOverlay);
        pose.popPose();

        float drift = BROAD_DRIFT + DRIFT_GAIN * progress;
        float halo = scale * (HALO + HALO_BREATH * motion.wave(BREATH_RATE, phase));
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        pose.mulPose(Axis.XP.rotationDegrees(pitch));
        pose.scale(halo, halo, halo);
        MatrixCoreShell.SPHERE.emit(
                pose.last(),
                plasma,
                MatrixCoreShell.Layer.BROAD,
                profile.bodyTint(),
                BROAD_STRENGTH * strength,
                BROAD_TILING,
                motion.motion(Motion.BROAD_U, drift),
                motion.motion(Motion.BROAD_V, drift * DRIFT_SKEW),
                LightTexture.FULL_BRIGHT,
                packedOverlay);
        pose.popPose();

        pose.popPose();
    }

    @Override
    public int getViewDistance() {
        return VIEW_DISTANCE;
    }

    private static float spinRate(MatrixStatus status, float progress) {
        return switch (status) {
            case RUNNING -> 6.0F + 14.0F * progress;
            case STALLED -> 1.0F;
            case IDLE -> 3.0F;
        };
    }

    private static float ringRate(MatrixStatus status, float progress) {
        return status == MatrixStatus.STALLED ? 0.0F : spinRate(status, progress) * RING_SPIN;
    }

    private static float wanderAngle(float wander, float phase, int ring, int[] harmonics) {
        float lead = Mth.sin(Mth.DEG_TO_RAD * harmonics[0] * wander + phase + ring * WANDER_STEP);
        float sway = Mth.sin(Mth.DEG_TO_RAD * harmonics[1] * wander + phase * 2.0F + ring * WANDER_STEP);
        return WANDER_REACH * lead + WANDER_SWAY * sway;
    }

    private static float pulse(MatrixRenderState motion, float phase, MatrixStatus status) {
        float amplitude =
                switch (status) {
                    case RUNNING -> PULSE_RUNNING;
                    case STALLED -> PULSE_STALLED;
                    case IDLE -> PULSE_IDLE;
                };
        return 1.0F + amplitude * motion.wave(PULSE_RATE, phase);
    }

    private static float brightness(MatrixStatus status, float progress, float flash) {
        float base =
                switch (status) {
                    case RUNNING -> 0.72F + 0.28F * progress;
                    case STALLED -> 0.45F;
                    case IDLE -> 0.60F;
                };
        return Math.min(1.0F, base + flash * FLASH_GLOW);
    }

    private static float glow(MatrixStatus status, float progress, float flash) {
        float base =
                switch (status) {
                    case RUNNING -> 0.55F + 0.45F * progress;
                    case STALLED -> 0.22F;
                    case IDLE -> 0.40F;
                };
        return Math.min(1.0F, base + flash * FLASH_SURGE);
    }

    private static float shudder(MatrixRenderState motion, float offset) {
        return motion.wave(SHUDDER_RATE, offset) * SHUDDER_REACH;
    }

    private static float phaseFor(BlockPos pos) {
        return (Mth.murmurHash3Mixer(pos.hashCode()) & 0xFFFF) / 65536.0F * Mth.TWO_PI;
    }
}
