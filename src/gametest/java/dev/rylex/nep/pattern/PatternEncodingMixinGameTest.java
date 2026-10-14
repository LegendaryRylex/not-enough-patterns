package dev.rylex.nep.pattern;

import static dev.rylex.nep.pattern.TerminalFixture.obsidianToDiamond;
import static dev.rylex.nep.pattern.TerminalFixture.stack;

import appeng.menu.me.items.PatternEncodingTermMenu;
import appeng.parts.encoding.PatternEncodingLogic;
import appeng.util.ConfigInventory;
import dev.rylex.nep.Nep;
import dev.rylex.nep.NepComponents;
import dev.rylex.nep.pattern.encoding.PatternEncodeGuard;
import dev.rylex.nep.pattern.encoding.PatternGrid;
import dev.rylex.nep.pattern.encoding.PatternRecipeHolder;
import java.util.Arrays;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Nep.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PatternEncodingMixinGameTest {

    private static final String TEMPLATE = "empty_5x5x5";
    private static final String BATCH = "nep_pattern_encoding";

    private PatternEncodingMixinGameTest() {}

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void theEncodingLogicCarriesARecipeId(GameTestHelper helper) {
        helper.assertTrue(
                PatternRecipeHolder.class.isAssignableFrom(PatternEncodingLogic.class),
                "the pattern encoding logic mixin did not apply; encoded patterns would lose their recipe");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void theEncodingMenuCarriesARecipeId(GameTestHelper helper) {
        helper.assertTrue(
                PatternRecipeHolder.class.isAssignableFrom(PatternEncodingTermMenu.class),
                "the pattern encoding terminal mixin did not apply; nothing would convert encoded patterns");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void theEncodingLogicCanBeToldWhatItJustEncoded(GameTestHelper helper) {
        helper.assertTrue(
                PatternEncodeGuard.class.isAssignableFrom(PatternEncodingLogic.class),
                "the encode guard did not apply; encoding would rewrite the grid from the pattern it just produced");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void thePatternATerminalJustEncodedLeavesTheGridAlone(GameTestHelper helper) {
        PatternEncodingLogic logic = new TerminalFixture(helper.getLevel()).getLogic();
        ConfigInventory inputs = logic.getEncodedInputInv();
        inputs.setStack(0, stack(Items.OBSIDIAN, 1));
        inputs.setStack(4, stack(Items.OBSIDIAN, 1));

        ItemStack encoded = obsidianToDiamond(2);
        ((PatternEncodeGuard) logic).nep$expectEncoded(encoded);
        logic.getEncodedPatternInv().setItemDirect(0, encoded);

        helper.assertValueEqual(
                inputs.getStack(0), stack(Items.OBSIDIAN, 1), "the first ingredient slot after encoding");
        helper.assertValueEqual(
                inputs.getStack(4), stack(Items.OBSIDIAN, 1), "the fifth ingredient slot after encoding");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void aPatternCarryingItsGridRestoresTheSlotsItWasEncodedFrom(GameTestHelper helper) {
        PatternEncodingLogic logic = new TerminalFixture(helper.getLevel()).getLogic();

        ItemStack encoded = obsidianToDiamond(2);
        encoded.set(
                NepComponents.PATTERN_GRID.get(),
                new PatternGrid(
                        Arrays.asList(stack(Items.OBSIDIAN, 1), null, null, null, stack(Items.OBSIDIAN, 1)),
                        List.of(stack(Items.DIAMOND, 1))));
        logic.getEncodedPatternInv().setItemDirect(0, encoded);

        ConfigInventory inputs = logic.getEncodedInputInv();
        helper.assertValueEqual(
                inputs.getStack(0), stack(Items.OBSIDIAN, 1), "the first ingredient slot after loading");
        helper.assertTrue(
                inputs.getStack(1) == null,
                "loading a pattern packed its ingredients to the front instead of restoring the slots the player used");
        helper.assertValueEqual(
                inputs.getStack(4), stack(Items.OBSIDIAN, 1), "the fifth ingredient slot after loading");
        helper.assertValueEqual(
                logic.getEncodedOutputInv().getStack(0), stack(Items.DIAMOND, 1), "the result slot after loading");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void aPatternWithoutAGridStillLoadsTheWayAe2LoadsIt(GameTestHelper helper) {
        PatternEncodingLogic logic = new TerminalFixture(helper.getLevel()).getLogic();

        ItemStack encoded = obsidianToDiamond(2);
        logic.getEncodedPatternInv().setItemDirect(0, encoded);

        helper.assertValueEqual(
                logic.getEncodedInputInv().getStack(0),
                stack(Items.OBSIDIAN, 2),
                "the first ingredient slot of a pattern encoded before grids were recorded");
        helper.succeed();
    }
}
