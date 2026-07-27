package dev.rylex.nep.compat.create;

import net.minecraft.network.chat.Component;

enum StationKind {
    DEPLOYER,
    PRESS,
    SPOUT,
    SAW,
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
        return recognized()
                ? Stations.icon(this).getHoverName()
                : Component.translatable("chat.nep.sequenced_assembly.station.unknown");
    }

    boolean recognized() {
        return this != UNKNOWN;
    }

    boolean needsRotation() {
        return this == DEPLOYER || this == PRESS || this == SAW;
    }
}
