package dev.rylex.nep;

import appeng.menu.SlotSemantic;
import appeng.menu.SlotSemantics;

public final class NepSlotSemantics {

    private static final int QUICK_MOVE_PRIORITY = 100;

    public static final SlotSemantic IMPORT_UPGRADE =
            SlotSemantics.register(Nep.MOD_ID + ":import_upgrade", false, QUICK_MOVE_PRIORITY);

    private NepSlotSemantics() {}

    public static void init() {}
}
