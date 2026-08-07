package dev.rylex.nep.compat.create;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

enum StationKind {
    DEPLOYER,
    PRESS,
    SPOUT,
    SAW,
    ENERGIZER,
    UNKNOWN;

    static StationKind byName(String name) {
        for (StationKind kind : values()) {
            if (kind.name().equals(name)) {
                return kind;
            }
        }
        return UNKNOWN;
    }

    Component displayName() {
        ItemStack icon = Stations.icon(this);
        return recognized() && !icon.isEmpty()
                ? icon.getHoverName()
                : Component.translatable("chat.nep.sequenced_assembly.station.unknown");
    }

    boolean recognized() {
        return this != UNKNOWN;
    }

    boolean needsRotation() {
        return this == DEPLOYER || this == PRESS || this == SAW || this == ENERGIZER;
    }
}
