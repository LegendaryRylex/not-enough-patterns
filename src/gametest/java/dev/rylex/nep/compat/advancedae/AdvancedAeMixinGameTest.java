package dev.rylex.nep.compat.advancedae;

import dev.rylex.nep.Nep;
import dev.rylex.nep.provider.ImportTrackerHost;
import dev.rylex.nep.provider.ImportUpgradeHost;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.pedroksl.advanced_ae.common.logic.AdvPatternProviderLogic;
import net.pedroksl.advanced_ae.gui.advpatternprovider.AdvPatternProviderMenu;
import net.pedroksl.advanced_ae.gui.advpatternprovider.SmallAdvPatternProviderMenu;

@GameTestHolder(Nep.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AdvancedAeMixinGameTest {

    private static final String TEMPLATE = "empty_5x5x5";
    private static final String BATCH = "nep_advancedae";

    private AdvancedAeMixinGameTest() {}

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void theLogicMixinAppliesItsInterfaces(GameTestHelper helper) {
        helper.assertTrue(
                ImportUpgradeHost.class.isAssignableFrom(AdvPatternProviderLogic.class),
                "the Advanced AE provider logic mixin did not apply its upgrade host interface");
        helper.assertTrue(
                ImportTrackerHost.class.isAssignableFrom(AdvPatternProviderLogic.class),
                "the Advanced AE provider logic mixin did not apply its tracker host interface");
        helper.succeed();
    }

    private static void assertInitialises(GameTestHelper helper, Class<?> type, String failure) {
        try {
            Class.forName(type.getName(), true, type.getClassLoader());
        } catch (ClassNotFoundException | Error e) {
            helper.fail(failure + ": " + e);
        }
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void theMenuClassesLoadWithTheSlotMixinApplied(GameTestHelper helper) {
        assertInitialises(
                helper,
                AdvPatternProviderMenu.class,
                "the Advanced AE provider menu failed to load with the import card slot mixin");
        assertInitialises(
                helper,
                SmallAdvPatternProviderMenu.class,
                "the small Advanced AE provider menu failed to load with the import card slot mixin");
        helper.succeed();
    }
}
