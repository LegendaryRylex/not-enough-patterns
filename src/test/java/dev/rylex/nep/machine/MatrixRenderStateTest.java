package dev.rylex.nep.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.rylex.nep.machine.MatrixRenderState.Motion;
import net.minecraft.util.Mth;
import org.junit.jupiter.api.Test;

class MatrixRenderStateTest {

    private static final float STEP = 0.5F;
    private static final int FRAMES = 400_000;
    private static final int CRAFT_FRAMES = 200;
    private static final float TOLERANCE = 1.0E-3F;
    private static final int WITHIN_HOLD = 120;
    private static final int SHORT_FRAMES = 20;
    private static final int LONG_FRAMES = 2000;
    private static final float ALIGN_STEP = 0.1F;
    private static final float[] PERIODS = {
        360.0F, 360.0F, 360.0F, 360.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 360.0F, 360.0F
    };

    @Test
    void everyPhaseStaysContinuousAcrossItsWrapsAndEveryRateChange() {
        MatrixRenderState state = new MatrixRenderState();
        Motion[] motions = Motion.values();
        float[] previous = new float[motions.length];
        float[] travelled = new float[motions.length];
        float[] peak = new float[motions.length];
        float ceiling = 0.0F;

        for (int frame = 0; frame < FRAMES; frame++) {
            float now = frame * STEP;
            float progress = (frame % CRAFT_FRAMES) / (float) CRAFT_FRAMES;
            state.advance((long) now, now - (long) now, progress);
            float[] rates = rates(progress);
            for (int index = 0; index < motions.length; index++) {
                peak[index] = Math.max(peak[index], Math.abs(rates[index]));
                float phase = state.motion(motions[index], rates[index]);
                float moved = separation(previous[index], phase, PERIODS[index]);
                previous[index] = phase;
                if (frame == 0) {
                    continue;
                }
                travelled[index] += moved;
                ceiling = Math.max(ceiling, moved - peak[index] * STEP);
            }
        }

        assertTrue(ceiling < TOLERANCE, "phase jumped " + ceiling + " beyond a frame of travel");
        for (int index = 0; index < motions.length; index++) {
            assertTrue(travelled[index] > PERIODS[index] * 20.0F, motions[index] + " barely moved");
        }
    }

    @Test
    void waveStaysContinuousLongAfterFloatWouldHaveLostTheClock() {
        MatrixRenderState state = new MatrixRenderState();
        float rate = 0.15F;
        float previous = 0.0F;
        float ceiling = 0.0F;
        float swing = 0.0F;

        for (int frame = 0; frame < FRAMES; frame++) {
            float now = frame * STEP;
            state.advance((long) now, now - (long) now, 1.0F);
            float value = state.wave(rate, 0.0F);
            if (frame > 0) {
                ceiling = Math.max(ceiling, Math.abs(value - previous));
                swing = Math.max(swing, Math.abs(value));
            }
            previous = value;
        }

        assertTrue(ceiling < rate * STEP + TOLERANCE, "wave jumped " + ceiling);
        assertTrue(swing > 0.99F, "wave flattened out to " + swing);
    }

    private static float[] rates(float progress) {
        float spin = 6.0F + 14.0F * progress;
        float skin = spin * -0.35F;
        float broad = 0.0024F + 0.0060F * progress;
        float fine = -(0.0110F + 0.0180F * progress);
        float cross = 0.0074F + 0.0130F * progress;
        return new float[] {
            spin,
            spin * 0.37F,
            skin,
            skin * 0.6F,
            broad,
            broad * 0.4F,
            fine,
            fine * 0.7F,
            cross,
            cross * 0.55F,
            spin * 0.6F,
            0.4F
        };
    }

    @Test
    void theGimbalHoldsItsWanderThenRealignsAndAStallFreezesIt() {
        Clock clock = new Clock();
        float peak = clock.run(MatrixStatus.RUNNING, WITHIN_HOLD);
        assertTrue(peak > 0.9F, "wander never took hold, reached " + peak);

        float held = clock.run(MatrixStatus.IDLE, SHORT_FRAMES);
        assertTrue(held > 0.9F, "idle dropped the wander before its hold ran out, at " + held);

        float frozen = clock.run(MatrixStatus.STALLED, LONG_FRAMES);
        assertEquals(held, frozen, "a stall kept moving the gimbal");

        assertEquals(0.0F, clock.run(MatrixStatus.IDLE, LONG_FRAMES), "the gimbal never returned to its axes");

        float again = clock.run(MatrixStatus.RUNNING, WITHIN_HOLD);
        assertTrue(again > 0.9F, "a later craft did not wander again, reached " + again);
    }

    @Test
    void aRunningCraftRealignsOnceItsHoldExpires() {
        Clock clock = new Clock();
        clock.run(MatrixStatus.RUNNING, WITHIN_HOLD);
        assertEquals(0.0F, clock.run(MatrixStatus.RUNNING, LONG_FRAMES), "a long craft never settled");
    }

    private static final class Clock {

        private final MatrixRenderState state = new MatrixRenderState();
        private int frame;
        private float alignment;

        private Clock() {
            state.advance(0L, 0.0F, 1.0F);
        }

        private float run(MatrixStatus status, int frames) {
            for (int step = 0; step < frames; step++) {
                frame++;
                state.advance(frame, 0.0F, 1.0F);
                float next = state.gimbal(status);
                assertTrue(Math.abs(next - alignment) < ALIGN_STEP, "alignment jumped to " + next);
                alignment = next;
            }
            return alignment;
        }
    }

    private static float separation(float from, float to, float period) {
        float raw = Mth.positiveModulo(to - from, period);
        return Math.min(raw, period - raw);
    }
}
