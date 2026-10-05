package dev.rylex.nep.pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import appeng.api.stacks.AEItemKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

class ToolWearTest {

    private static final int SHOVEL_USES = new ItemStack(Items.WOODEN_SHOVEL).getMaxDamage();

    private static AEItemKey shovel(int damage) {
        ItemStack stack = new ItemStack(Items.WOODEN_SHOVEL);
        stack.setDamageValue(damage);
        return AEItemKey.of(stack);
    }

    @Test
    void oneUseCostsOnePoint() {
        assertEquals(shovel(1), ToolWear.worn(shovel(0)), "a fresh tool must come back one point worse");
        assertEquals(shovel(13), ToolWear.worn(shovel(12)), "a part-used tool must come back one point worse");
    }

    @Test
    void theLastPointLeavesNothingToHandBack() {
        assertNull(
                ToolWear.worn(shovel(SHOVEL_USES - 1)),
                "a tool on its last point breaks, so the pattern must declare no remainder");
    }

    @Test
    void somethingThatCannotWearHasNoWornForm() {
        assertNull(ToolWear.worn(AEItemKey.of(Items.DIAMOND)), "a diamond has no durability to spend");
    }

    @Test
    void onlyDamageMayDiffer() {
        assertTrue(
                ToolWear.differsOnlyByDamage(shovel(0), shovel(7)),
                "two shovels apart only in wear must count as the same tool");
        assertFalse(
                ToolWear.differsOnlyByDamage(shovel(0), AEItemKey.of(Items.WOODEN_PICKAXE)),
                "a different tool must not pass as a worn one");
        assertFalse(
                ToolWear.differsOnlyByDamage(AEItemKey.of(Items.DIAMOND), AEItemKey.of(Items.DIAMOND)),
                "an item with no durability never wears, so it must never take the worn path");
    }
}
