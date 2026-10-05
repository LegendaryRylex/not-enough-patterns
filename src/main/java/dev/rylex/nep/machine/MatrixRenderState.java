package dev.rylex.nep.machine;

import java.util.Arrays;
import net.minecraft.util.Mth;

public final class MatrixRenderState {

    /** Each phase wraps at its own period, so crossing a wrap lands exactly where the phase started. */
    public enum Motion {
        YAW(360.0F),
        PITCH(360.0F),
        SKIN_YAW(360.0F),
        SKIN_ROLL(360.0F),
        BROAD_U(1.0F),
        BROAD_V(1.0F),
        FINE_U(1.0F),
        FINE_V(1.0F),
        CROSS_U(1.0F),
        CROSS_V(1.0F),
        RING(360.0F),
        WANDER(360.0F);

        private final float period;

        Motion(float period) {
            this.period = period;
        }
    }

    private static final float WANDER_HOLD = 160.0F;
    private static final float WANDER_EASE = 0.06F;
    private static final float ALIGN_EASE = 0.014F;
    private static final float ALIGN_FLOOR = 0.002F;
    private static final float MAX_STEP = 20.0F;
    private static final float PROGRESS_EASE = 0.35F;
    private static final float RATE_EASE = 0.12F;
    private static final float FLASH_DECAY = 0.08F;
    private static final float COMPLETION_DROP = 0.4F;
    private static final double TAU = Math.PI * 2.0;

    private final float[] phases = new float[Motion.values().length];
    private final float[] rates = new float[Motion.values().length];

    private double clock;
    private double previous = Double.NaN;
    private float elapsed;
    private float progress;
    private float flash;
    private float alignment;
    private float hold;
    private boolean working;

    public MatrixRenderState() {
        Arrays.fill(rates, Float.NaN);
    }

    /** Mutates every accumulator, so exactly one caller per frame: the renderer, before it reads the getters. */
    public void advance(long gameTime, float partialTick, float target) {
        double now = gameTime + partialTick;
        if (Double.isNaN(previous)) {
            previous = now;
            progress = target;
            return;
        }
        double step = now - previous;
        previous = now;
        elapsed = step < 0.0 || step > MAX_STEP ? 0.0F : (float) step;
        clock += elapsed;
        if (target < progress - COMPLETION_DROP) {
            flash = 1.0F;
        }
        flash = Math.max(0.0F, flash - elapsed * FLASH_DECAY);
        progress += (target - progress) * Math.min(1.0F, elapsed * PROGRESS_EASE);
    }

    /** Alignment of the gimbal rings, zero on their fixed axes and one fully wandering. */
    public float gimbal(MatrixStatus status) {
        if (status == MatrixStatus.STALLED) {
            working = false;
            return alignment;
        }
        boolean busy = status == MatrixStatus.RUNNING;
        if (busy && !working) {
            hold = WANDER_HOLD;
        }
        working = busy;
        hold = Math.max(0.0F, hold - elapsed);
        float target = hold > 0.0F ? 1.0F : 0.0F;
        float ease = target > alignment ? WANDER_EASE : ALIGN_EASE;
        alignment += (target - alignment) * Math.min(1.0F, elapsed * ease);
        if (alignment < ALIGN_FLOOR) {
            alignment = 0.0F;
        }
        return alignment;
    }

    /**
     * Integrates the rate instead of scaling the clock by it, so a rate that changes mid-craft bends the
     * motion rather than teleporting the phase, and eases the rate so a status flip is not a snap.
     */
    public float motion(Motion motion, float ratePerTick) {
        int index = motion.ordinal();
        rates[index] = Float.isNaN(rates[index])
                ? ratePerTick
                : rates[index] + (ratePerTick - rates[index]) * Math.min(1.0F, elapsed * RATE_EASE);
        phases[index] = Mth.positiveModulo(phases[index] + rates[index] * elapsed, motion.period);
        return phases[index];
    }

    /** Wraps the argument in double, so the wave keeps full resolution however long the clock has run. */
    public float wave(float ratePerTick, float offset) {
        return Mth.sin((float) ((clock * ratePerTick + offset) % TAU));
    }

    public float progress() {
        return progress;
    }

    public float flash() {
        return flash;
    }
}
