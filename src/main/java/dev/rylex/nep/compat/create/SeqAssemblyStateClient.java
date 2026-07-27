package dev.rylex.nep.compat.create;

import net.minecraft.client.Minecraft;

final class SeqAssemblyStateClient {
    private SeqAssemblyStateClient() {}

    static void handle(SequencedAssemblyState state) {
        if (Minecraft.getInstance().screen instanceof SequencedAssemblyControllerScreen screen) {
            screen.acceptState(state);
        }
    }
}
