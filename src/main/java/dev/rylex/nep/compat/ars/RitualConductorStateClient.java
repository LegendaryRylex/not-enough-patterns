package dev.rylex.nep.compat.ars;

import net.minecraft.client.Minecraft;

final class RitualConductorStateClient {
    private RitualConductorStateClient() {}

    static void handle(RitualConductorState state) {
        if (Minecraft.getInstance().screen instanceof RitualConductorScreen screen) {
            screen.acceptState(state);
        }
    }
}
