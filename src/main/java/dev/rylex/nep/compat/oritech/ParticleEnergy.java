package dev.rylex.nep.compat.oritech;

import rearth.oritech.init.OritechConfig;

final class ParticleEnergy {
    private ParticleEnergy() {}

    /** A particle that has just left its Accelerator Controller always moves at speed 1. */
    static final long SLOW_PARTICLE_SPEED = 1;

    /** Every motor pass costs the particle's current speed times this, and adds one to the speed. */
    static long perSpeed() {
        try {
            return OritechConfig.accelerationRFCost.get();
        } catch (IllegalStateException unloaded) {
            return OritechConfig.accelerationRFCost.getDefault();
        }
    }

    /** The speed the fast particle needs before the slow one is let go, since the impact sums both. */
    static long launchSpeedFor(long minimumSpeed) {
        return Math.max(SLOW_PARTICLE_SPEED, minimumSpeed - SLOW_PARTICLE_SPEED);
    }

    static long toReach(long speed) {
        long passes = Math.max(0L, speed - 1L);
        return perSpeed() * passes * (passes + 1L) / 2L;
    }
}
