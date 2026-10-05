package dev.rylex.nep.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.rylex.nep.machine.MatrixRenderHost;
import dev.rylex.nep.machine.MatrixRenderState;
import dev.rylex.nep.machine.MatrixRenderState.Motion;
import dev.rylex.nep.machine.MatrixStatus;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class MatrixCoreRenderer<T extends BlockEntity & MatrixRenderHost>
        implements BlockEntityRenderer<T, MatrixCoreRenderer.State> {

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

    public static final class State extends BlockEntityRenderState {
        private boolean ready;
        private boolean stalled;
        private float shudderX;
        private float shudderY;
        private float core;
        private float yaw;
        private float pitch;
        private float brightness;
        private final float[][] ringTurns = new float[MatrixRingMesh.RINGS.length][3];
        private float skinYaw;
        private float skinRoll;
        private float skin;
        private float fineStrength;
        private float fineTiling;
        private float fineU;
        private float fineV;
        private float crossStrength;
        private float crossU;
        private float crossV;
        private float halo;
        private float broadStrength;
        private float broadU;
        private float broadV;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    /** Advances the matrix's motion here rather than in {@link #submit}, which only ever sees the copied state. */
    @Override
    public void extractRenderState(
            T matrix,
            State state,
            float partialTick,
            Vec3 cameraPosition,
            @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(matrix, state, partialTick, cameraPosition, breakProgress);
        state.ready = matrix.getLevel() != null;
        if (!state.ready) {
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
        state.yaw = motion.motion(Motion.YAW, turn) + phase * Mth.RAD_TO_DEG;
        state.pitch = motion.motion(Motion.PITCH, turn * TILT_RATIO) + phase * Mth.RAD_TO_DEG;
        state.skinYaw = motion.motion(Motion.SKIN_YAW, turn * SKIN_SPIN) - phase * Mth.RAD_TO_DEG;
        state.skinRoll = motion.motion(Motion.SKIN_ROLL, turn * SKIN_SPIN * SKIN_TILT) + phase * Mth.RAD_TO_DEG;

        state.stalled = status == MatrixStatus.STALLED;
        if (state.stalled) {
            state.shudderX = shudder(motion, phase);
            state.shudderY = shudder(motion, phase + SHUDDER_OFFSET);
        }
        state.core = scale * CORE;
        state.brightness = brightness(status, progress, flash);

        float alignment = motion.gimbal(status);
        float wander = motion.motion(Motion.WANDER, state.stalled ? 0.0F : WANDER_RATE);
        float ring = motion.motion(Motion.RING, ringRate(status, progress));
        for (int index = 0; index < state.ringTurns.length; index++) {
            float[] turns = state.ringTurns[index];
            turns[0] = alignment * wanderAngle(wander, phase, index, PRECESS_HARMONICS);
            turns[1] = alignment * wanderAngle(wander, phase, index, WOBBLE_HARMONICS);
            turns[2] = DIRECTIONS[index] * ring + index * RING_OFFSET + phase * Mth.RAD_TO_DEG;
        }

        float strength = glow(status, progress, flash);
        float counter = -(FINE_DRIFT + FINE_GAIN * progress);
        float cross = CROSS_DRIFT + CROSS_GAIN * progress;
        float shimmer = SKIN_SHIMMER * motion.wave(SKIN_SHIMMER_RATE, phase);
        state.fineTiling = FINE_TILING * (1.0F + TILING_BREATH * motion.wave(BREATH_RATE * BREATH_RATIO, phase));
        state.skin = scale * SKIN;
        state.fineStrength = FINE_STRENGTH * strength * (1.0F + shimmer);
        state.fineU = motion.motion(Motion.FINE_U, counter);
        state.fineV = motion.motion(Motion.FINE_V, counter * COUNTER_SKEW);
        state.crossStrength = CROSS_STRENGTH * strength * (1.0F - shimmer);
        state.crossU = motion.motion(Motion.CROSS_U, cross);
        state.crossV = motion.motion(Motion.CROSS_V, cross * CROSS_SKEW);

        float drift = BROAD_DRIFT + DRIFT_GAIN * progress;
        state.halo = scale * (HALO + HALO_BREATH * motion.wave(BREATH_RATE, phase));
        state.broadStrength = BROAD_STRENGTH * strength;
        state.broadU = motion.motion(Motion.BROAD_U, drift);
        state.broadV = motion.motion(Motion.BROAD_V, drift * DRIFT_SKEW);
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.ready) {
            return;
        }
        int overlay = OverlayTexture.NO_OVERLAY;
        int fullBright = LightCoordsUtil.FULL_BRIGHT;

        pose.pushPose();
        pose.translate(0.5F, 0.5F, 0.5F);
        if (state.stalled) {
            pose.translate(state.shudderX, state.shudderY, 0.0F);
        }

        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(state.yaw));
        pose.mulPose(Axis.XP.rotationDegrees(state.pitch));
        pose.scale(state.core, state.core, state.core);
        float brightness = state.brightness;
        MatrixCoreMesh mesh = profile.mesh();
        collector.submitCustomGeometry(
                pose,
                RenderTypes.entityCutout(profile.texture()),
                (last, buffer) -> mesh.emit(last, buffer, brightness, 1.0F, fullBright, overlay));
        pose.popPose();

        RenderType bars = RenderTypes.entityCutout(profile.ring());
        int light = state.lightCoords;
        for (int index = 0; index < MatrixRingMesh.RINGS.length; index++) {
            float[] turns = state.ringTurns[index];
            pose.pushPose();
            if (turns[0] != 0.0F || turns[1] != 0.0F) {
                pose.mulPose(PIVOTS[(index + 1) % PIVOTS.length].rotationDegrees(turns[0]));
                pose.mulPose(PIVOTS[(index + 2) % PIVOTS.length].rotationDegrees(turns[1]));
            }
            pose.mulPose(PIVOTS[index].rotationDegrees(turns[2]));
            MatrixRingMesh ring = MatrixRingMesh.RINGS[index];
            collector.submitCustomGeometry(
                    pose, bars, (last, buffer) -> ring.emit(last, buffer, RING_BRIGHTNESS, light, overlay));
            pose.popPose();
        }

        RenderType plasma = MatrixPlasma.of(profile.plasma());
        int coreTint = profile.coreTint();
        float fineStrength = state.fineStrength;
        float fineTiling = state.fineTiling;
        float fineU = state.fineU;
        float fineV = state.fineV;
        float crossStrength = state.crossStrength;
        float crossU = state.crossU;
        float crossV = state.crossV;
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(state.skinYaw));
        pose.mulPose(Axis.ZP.rotationDegrees(state.skinRoll));
        pose.scale(state.skin, state.skin, state.skin);
        collector.submitCustomGeometry(pose, plasma, (last, buffer) -> {
            MatrixCoreShell.SPHERE.emit(
                    last,
                    buffer,
                    MatrixCoreShell.Layer.FINE,
                    coreTint,
                    fineStrength,
                    fineTiling,
                    fineU,
                    fineV,
                    fullBright,
                    overlay);
            MatrixCoreShell.SPHERE.emit(
                    last,
                    buffer,
                    MatrixCoreShell.Layer.BROAD,
                    coreTint,
                    crossStrength,
                    CROSS_TILING,
                    crossU,
                    crossV,
                    fullBright,
                    overlay);
        });
        pose.popPose();

        int bodyTint = profile.bodyTint();
        float broadStrength = state.broadStrength;
        float broadU = state.broadU;
        float broadV = state.broadV;
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(state.yaw));
        pose.mulPose(Axis.XP.rotationDegrees(state.pitch));
        pose.scale(state.halo, state.halo, state.halo);
        collector.submitCustomGeometry(
                pose,
                plasma,
                (last, buffer) -> MatrixCoreShell.SPHERE.emit(
                        last,
                        buffer,
                        MatrixCoreShell.Layer.BROAD,
                        bodyTint,
                        broadStrength,
                        BROAD_TILING,
                        broadU,
                        broadV,
                        fullBright,
                        overlay));
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
