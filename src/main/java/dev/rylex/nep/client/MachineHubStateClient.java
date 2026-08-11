package dev.rylex.nep.client;

import dev.rylex.nep.hub.MachineHubState;
import net.minecraft.client.Minecraft;

public final class MachineHubStateClient {
    private MachineHubStateClient() {}

    public static void handle(MachineHubState state) {
        if (Minecraft.getInstance().screen instanceof MachineHubScreen screen) {
            screen.acceptState(state);
        }
    }
}
