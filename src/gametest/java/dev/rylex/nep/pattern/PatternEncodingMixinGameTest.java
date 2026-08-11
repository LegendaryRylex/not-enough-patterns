package dev.rylex.nep.pattern;

import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.helpers.IPatternTerminalLogicHost;
import appeng.menu.me.items.PatternEncodingTermMenu;
import appeng.parts.encoding.PatternEncodingLogic;
import appeng.util.ConfigInventory;
import dev.rylex.nep.NepComponents;
import dev.rylex.nep.NepGameTests;
import dev.rylex.nep.pattern.encoding.PatternEncodeGuard;
import dev.rylex.nep.pattern.encoding.PatternGrid;
import dev.rylex.nep.pattern.encoding.PatternRecipeHolder;
import java.util.Arrays;
import java.util.List;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public final class PatternEncodingMixinGameTest {

    private PatternEncodingMixinGameTest() {}

    public static void register(NepGameTests.Batch batch) {
        batch.add(
                        "the_encoding_logic_carries_a_recipe_id",
                        PatternEncodingMixinGameTest::theEncodingLogicCarriesARecipeId)
                .add(
                        "the_encoding_menu_carries_a_recipe_id",
                        PatternEncodingMixinGameTest::theEncodingMenuCarriesARecipeId)
                .add(
                        "the_encoding_logic_can_be_told_what_it_just_encoded",
                        PatternEncodingMixinGameTest::theEncodingLogicCanBeToldWhatItJustEncoded)
                .add(
                        "the_pattern_a_terminal_just_encoded_leaves_the_grid_alone",
                        PatternEncodingMixinGameTest::thePatternATerminalJustEncodedLeavesTheGridAlone)
                .add(
                        "a_pattern_carrying_its_grid_restores_the_slots_it_was_encoded_from",
                        PatternEncodingMixinGameTest::aPatternCarryingItsGridRestoresTheSlotsItWasEncodedFrom)
                .add(
                        "a_pattern_without_a_grid_still_loads_the_way_ae2_loads_it",
                        PatternEncodingMixinGameTest::aPatternWithoutAGridStillLoadsTheWayAe2LoadsIt);
    }

    private static GenericStack stack(Item item, long amount) {
        return new GenericStack(AEItemKey.of(item), amount);
    }

    public static void theEncodingLogicCarriesARecipeId(GameTestHelper helper) {
        helper.assertTrue(
                PatternRecipeHolder.class.isAssignableFrom(PatternEncodingLogic.class),
                "the pattern encoding logic mixin did not apply; encoded patterns would lose their recipe");
        helper.succeed();
    }

    public static void theEncodingMenuCarriesARecipeId(GameTestHelper helper) {
        helper.assertTrue(
                PatternRecipeHolder.class.isAssignableFrom(PatternEncodingTermMenu.class),
                "the pattern encoding terminal mixin did not apply; nothing would convert encoded patterns");
        helper.succeed();
    }

    public static void theEncodingLogicCanBeToldWhatItJustEncoded(GameTestHelper helper) {
        helper.assertTrue(
                PatternEncodeGuard.class.isAssignableFrom(PatternEncodingLogic.class),
                "the encode guard did not apply; encoding would rewrite the grid from the pattern it just produced");
        helper.succeed();
    }

    public static void thePatternATerminalJustEncodedLeavesTheGridAlone(GameTestHelper helper) {
        PatternEncodingLogic logic = new Terminal(helper.getLevel()).getLogic();
        ConfigInventory inputs = logic.getEncodedInputInv();
        inputs.setStack(0, stack(Items.OBSIDIAN, 1));
        inputs.setStack(4, stack(Items.OBSIDIAN, 1));

        ItemStack encoded = PatternDetailsHelper.encodeProcessingPattern(
                List.of(stack(Items.OBSIDIAN, 2)), List.of(stack(Items.DIAMOND, 1)));
        ((PatternEncodeGuard) logic).nep$expectEncoded(encoded);
        logic.getEncodedPatternInv().setItemDirect(0, encoded);

        helper.assertValueEqual(
                inputs.getStack(0), stack(Items.OBSIDIAN, 1), "the first ingredient slot after encoding");
        helper.assertValueEqual(
                inputs.getStack(4), stack(Items.OBSIDIAN, 1), "the fifth ingredient slot after encoding");
        helper.succeed();
    }

    public static void aPatternCarryingItsGridRestoresTheSlotsItWasEncodedFrom(GameTestHelper helper) {
        PatternEncodingLogic logic = new Terminal(helper.getLevel()).getLogic();

        ItemStack encoded = PatternDetailsHelper.encodeProcessingPattern(
                List.of(stack(Items.OBSIDIAN, 2)), List.of(stack(Items.DIAMOND, 1)));
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

    public static void aPatternWithoutAGridStillLoadsTheWayAe2LoadsIt(GameTestHelper helper) {
        PatternEncodingLogic logic = new Terminal(helper.getLevel()).getLogic();

        ItemStack encoded = PatternDetailsHelper.encodeProcessingPattern(
                List.of(stack(Items.OBSIDIAN, 2)), List.of(stack(Items.DIAMOND, 1)));
        logic.getEncodedPatternInv().setItemDirect(0, encoded);

        helper.assertValueEqual(
                logic.getEncodedInputInv().getStack(0),
                stack(Items.OBSIDIAN, 2),
                "the first ingredient slot of a pattern encoded before grids were recorded");
        helper.succeed();
    }

    private static final class Terminal implements IPatternTerminalLogicHost {

        private final Level level;
        private final PatternEncodingLogic logic;

        private Terminal(Level level) {
            this.level = level;
            this.logic = new PatternEncodingLogic(this);
        }

        @Override
        public PatternEncodingLogic getLogic() {
            return logic;
        }

        @Override
        public Level getLevel() {
            return level;
        }

        @Override
        public void markForSave() {}
    }
}
