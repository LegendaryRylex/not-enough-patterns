package dev.rylex.nep.compat.create;

final class MatrixStressCurve {
    private MatrixStressCurve() {}

    static float effectiveMinimumSpeed(int configuredMinimumSpeed, float peakSpeed) {
        return peakSpeed <= 0 ? configuredMinimumSpeed : Math.min(configuredMinimumSpeed, peakSpeed);
    }

    static int stressAt(
            float rpm, int configuredMinimumSpeed, float peakSpeed, int configuredMinimumStress, int maximumStress) {
        if (maximumStress <= 0) {
            return 0;
        }
        float speed = Math.abs(rpm);
        float minimumSpeed = effectiveMinimumSpeed(configuredMinimumSpeed, peakSpeed);
        if (speed < minimumSpeed) {
            return 0;
        }
        if (peakSpeed <= minimumSpeed) {
            return maximumStress;
        }
        int minimumStress = Math.max(0, Math.min(configuredMinimumStress, maximumStress));
        double ramp = Math.min(1.0, (speed - minimumSpeed) / (peakSpeed - minimumSpeed));
        return (int) Math.min(maximumStress, Math.round(minimumStress + (maximumStress - minimumStress) * ramp));
    }

    static long workPerCraft(int craftTicks, int maximumStress) {
        return Math.max(1L, (long) craftTicks * maximumStress);
    }
}
