package dev.rylex.nep.pattern;

import appeng.menu.me.items.PatternEncodingTermMenu;
import appeng.parts.encoding.PatternEncodingLogic;
import dev.rylex.nep.Nep;
import dev.rylex.nep.pattern.encoding.PatternRecipeHolder;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
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
}
