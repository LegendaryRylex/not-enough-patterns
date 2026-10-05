package dev.rylex.nep.decoder;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import dev.rylex.nep.NepConfig;
import org.jetbrains.annotations.Nullable;

public final class PatternDecoding {
    private PatternDecoding() {}

    public static boolean required(DecoderModule module) {
        return NepConfig.decoderRequiredFor(module);
    }

    public static int modulesOn(@Nullable IGrid grid) {
        if (grid == null) {
            return 0;
        }
        int held = 0;
        for (DecoderGridNode decoder : grid.getActiveMachines(DecoderGridNode.class)) {
            held |= decoder.modules();
        }
        return held;
    }

    public static int unlocked(@Nullable IGridNode node) {
        if (!NepConfig.requireDecoder()) {
            return ~0;
        }
        int unlocked = modulesOn(node == null ? null : node.getGrid());
        for (DecoderModule module : DecoderModule.values()) {
            if (!required(module)) {
                unlocked |= module.bit();
            }
        }
        return unlocked;
    }

    public static boolean allows(int unlocked, DecoderModule module) {
        return (unlocked & module.bit()) != 0;
    }
}
