package dev.rylex.nep.compat.advancedae;

import dev.rylex.nep.NepGameTests;
import dev.rylex.nep.provider.ImportTrackerHost;
import dev.rylex.nep.provider.ImportUpgradeHost;
import net.minecraft.gametest.framework.GameTestHelper;
import net.pedroksl.advanced_ae.common.logic.AdvPatternProviderLogic;
import net.pedroksl.advanced_ae.gui.advpatternprovider.AdvPatternProviderMenu;
import net.pedroksl.advanced_ae.gui.advpatternprovider.SmallAdvPatternProviderMenu;

public final class AdvancedAeMixinGameTest {

    private AdvancedAeMixinGameTest() {}

    public static void register(NepGameTests.Batch batch) {
        batch.add("the_logic_mixin_applies_its_interfaces", AdvancedAeMixinGameTest::theLogicMixinAppliesItsInterfaces)
                .add(
                        "the_menu_classes_load_with_the_slot_mixin_applied",
                        AdvancedAeMixinGameTest::theMenuClassesLoadWithTheSlotMixinApplied);
    }

    public static void theLogicMixinAppliesItsInterfaces(GameTestHelper helper) {
        helper.assertTrue(
                ImportUpgradeHost.class.isAssignableFrom(AdvPatternProviderLogic.class),
                "the Advanced AE provider logic mixin did not apply its upgrade host interface");
        helper.assertTrue(
                ImportTrackerHost.class.isAssignableFrom(AdvPatternProviderLogic.class),
                "the Advanced AE provider logic mixin did not apply its tracker host interface");
        helper.succeed();
    }

    public static void theMenuClassesLoadWithTheSlotMixinApplied(GameTestHelper helper) {
        helper.assertTrue(
                AdvPatternProviderMenu.class.getName() != null,
                "the Advanced AE provider menu failed to load with the import card slot mixin");
        helper.assertTrue(
                SmallAdvPatternProviderMenu.class.getName() != null,
                "the small Advanced AE provider menu failed to load with the import card slot mixin");
        helper.succeed();
    }
}
