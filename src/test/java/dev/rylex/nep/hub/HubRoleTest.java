package dev.rylex.nep.hub;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.rylex.nep.MinecraftBootstrap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MinecraftBootstrap.class)
class HubRoleTest {

    @Test
    void previousUndoesNext() {
        for (HubRole role : HubRole.values()) {
            assertEquals(role, role.next().previous(), "previous() after next() from " + role);
            assertEquals(role, role.previous().next(), "next() after previous() from " + role);
        }
    }

    @Test
    void previousWrapsFromTheFirstRoleToTheLast() {
        assertEquals(HubRole.BOTH, HubRole.INPUT.previous());
    }
}
