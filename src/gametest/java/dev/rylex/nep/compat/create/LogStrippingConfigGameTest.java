package dev.rylex.nep.compat.create;

import dev.rylex.nep.ConfigOverrides;
import dev.rylex.nep.Nep;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Nep.MOD_ID)
@PrefixGameTestTemplate(false)
public final class LogStrippingConfigGameTest {

    private static final String TEMPLATE = "empty_5x5x5";

    private LogStrippingConfigGameTest() {}

    private static ResourceLocation strippingRecipeId() {
        return Nep.id("create/log_stripping/oak_log");
    }

    private static void assertStrippingIsOff(GameTestHelper helper, String why) {
        helper.assertTrue(
                LogStripping.recipes(helper.getLevel()).isEmpty(),
                why + " left " + LogStripping.recipes(helper.getLevel()).size() + " stripping recipes in place");
        helper.assertTrue(
                LogStripping.byId(strippingRecipeId(), helper.getLevel()) == null,
                why + " still resolved an already-encoded stripping pattern's recipe");
    }

    @GameTest(template = TEMPLATE, batch = "nep_log_stripping_off")
    public static void turningLogStrippingOffTakesTheRecipesAwayAndBringsThemBackWithoutARestart(
            GameTestHelper helper) {
        LogStripping.clearCache();
        helper.assertTrue(
                !LogStripping.recipes(helper.getLevel()).isEmpty(),
                "stripping was already off before the test started");

        ConfigOverrides.Restore restore = ConfigOverrides.override("CREATE_DEPLOYING_LOG_STRIPPING", false);
        try {
            assertStrippingIsOff(helper, "logStripping = false");
        } finally {
            restore.undo();
        }

        helper.assertTrue(
                !LogStripping.recipes(helper.getLevel()).isEmpty(),
                "turning logStripping back on did not bring the recipes back; it should not need a restart");
        helper.assertTrue(
                LogStripping.byId(strippingRecipeId(), helper.getLevel()) != null,
                "an already-encoded stripping pattern did not start resolving again when the setting came back on");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = "nep_deploying_off")
    public static void turningTheDeployingModuleOffAlsoKillsLogStripping(GameTestHelper helper) {
        LogStripping.clearCache();
        helper.assertTrue(
                !LogStripping.recipes(helper.getLevel()).isEmpty(),
                "stripping was already off before the test started");

        ConfigOverrides.Restore restore = ConfigOverrides.override("CREATE_DEPLOYING", false);
        try {
            assertStrippingIsOff(helper, "the deploying module being off");
        } finally {
            restore.undo();
        }

        helper.assertTrue(
                !LogStripping.recipes(helper.getLevel()).isEmpty(),
                "turning the deploying module back on did not restore stripping");
        helper.succeed();
    }
}
