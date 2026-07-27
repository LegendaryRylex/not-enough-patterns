package dev.rylex.nep.compat.create;

import net.minecraft.network.chat.Component;

final class MatrixReadout {

    private MatrixReadout() {}

    static Component status(int flags, boolean idle) {
        if ((flags & SequencedAssemblyMatrixBlockEntity.FLAG_OVERSTRESSED) != 0) {
            return Component.translatable("gui.nep.sequenced_assembly_matrix.status.overstressed");
        }
        if ((flags & SequencedAssemblyMatrixBlockEntity.FLAG_ROTATING) == 0) {
            return Component.translatable("gui.nep.sequenced_assembly_matrix.status.no_rotation");
        }
        if ((flags & SequencedAssemblyMatrixBlockEntity.FLAG_FAST_ENOUGH) == 0) {
            return Component.translatable(
                    "gui.nep.sequenced_assembly_matrix.status.too_slow",
                    Math.round(SequencedAssemblyMatrixBlockEntity.minimumSpeed()));
        }
        if ((flags & SequencedAssemblyMatrixBlockEntity.FLAG_POWERED) == 0) {
            return Component.translatable("gui.nep.sequenced_assembly_matrix.status.no_power");
        }
        if ((flags & SequencedAssemblyMatrixBlockEntity.FLAG_OUTPUT_BLOCKED) != 0) {
            return Component.translatable("gui.nep.sequenced_assembly_matrix.status.output_blocked");
        }
        if ((flags & SequencedAssemblyMatrixBlockEntity.FLAG_STARVED) != 0) {
            return Component.translatable("gui.nep.sequenced_assembly_matrix.status.starved");
        }
        return idle
                ? Component.translatable("gui.nep.sequenced_assembly_matrix.status.idle")
                : Component.translatable("gui.nep.sequenced_assembly_matrix.status.running");
    }

    static boolean faulted(int flags) {
        return (flags & SequencedAssemblyMatrixBlockEntity.FLAG_POWERED) == 0
                || (flags & SequencedAssemblyMatrixBlockEntity.FLAG_FAST_ENOUGH) == 0
                || (flags & SequencedAssemblyMatrixBlockEntity.FLAG_OVERSTRESSED) != 0
                || (flags & SequencedAssemblyMatrixBlockEntity.FLAG_OUTPUT_BLOCKED) != 0
                || (flags & SequencedAssemblyMatrixBlockEntity.FLAG_STARVED) != 0;
    }

    static Component power(int draw) {
        return Component.translatable("gui.nep.sequenced_assembly_matrix.power", count(draw));
    }

    static Component speed(float rpm, float fraction) {
        return Component.translatable(
                "gui.nep.sequenced_assembly_matrix.speed", Math.round(Math.abs(rpm)), percent(fraction));
    }

    static Component stress(int units, float fraction) {
        return Component.translatable("gui.nep.sequenced_assembly_matrix.stress", count(units), percent(fraction));
    }

    static String count(int value) {
        return String.format("%,d", value);
    }

    static String percent(float fraction) {
        return String.format("%.0f", Math.min(1.0F, Math.max(0.0F, fraction)) * 100.0F);
    }
}
