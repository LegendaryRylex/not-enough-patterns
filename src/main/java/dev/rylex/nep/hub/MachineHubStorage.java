package dev.rylex.nep.hub;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import net.minecraft.network.chat.Component;

public final class MachineHubStorage implements MEStorage {

    private final MachineHubBlockEntity hub;

    MachineHubStorage(MachineHubBlockEntity hub) {
        this.hub = hub;
    }

    @Override
    public long insert(AEKey what, long amount, Actionable mode, IActionSource source) {
        return HubRouting.insert(hub.targets(), what, amount, mode);
    }

    @Override
    public long extract(AEKey what, long amount, Actionable mode, IActionSource source) {
        return HubRouting.extract(hub.targets(), what, amount, mode);
    }

    @Override
    public void getAvailableStacks(KeyCounter out) {
        HubRouting.collect(hub.targets(), out);
    }

    @Override
    public Component getDescription() {
        return Component.translatable("block.nep.machine_hub");
    }
}
