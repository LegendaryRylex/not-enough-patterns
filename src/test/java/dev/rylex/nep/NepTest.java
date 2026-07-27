package dev.rylex.nep;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class NepTest {
    @Test
    void modIdIsValid() {
        assertTrue(Nep.MOD_ID.matches("[a-z][a-z0-9_]{1,63}"));
    }
}
