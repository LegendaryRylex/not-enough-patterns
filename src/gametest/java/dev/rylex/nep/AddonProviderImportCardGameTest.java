package dev.rylex.nep;

import dev.rylex.nep.provider.ImportUpgradeHost;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

public final class AddonProviderImportCardGameTest {

    private static final BlockPos PROVIDER = new BlockPos(2, 1, 2);

    private AddonProviderImportCardGameTest() {}

    public static void register(NepGameTests.Batch batch) {
        batch.add(
                        "the_advanced_ae_providers_take_the_card",
                        AddonProviderImportCardGameTest::theAdvancedAeProvidersTakeTheCard)
                .add(
                        "the_extended_ae_provider_takes_the_card",
                        AddonProviderImportCardGameTest::theExtendedAeProviderTakesTheCard);
    }

    private static void assertProviderTakesTheCard(GameTestHelper helper, String modId, String blockPath) {
        var id = Identifier.fromNamespaceAndPath(modId, blockPath);
        var block = BuiltInRegistries.BLOCK
                .getOptional(id)
                .orElseThrow(() -> new AssertionError(id + " is not registered though the mod is on the classpath"));

        helper.setBlock(PROVIDER, block.defaultBlockState());
        var be = helper.getLevel().getBlockEntity(helper.absolutePos(PROVIDER));
        helper.assertTrue(be != null, id + " placed no block entity");
        helper.assertTrue(
                be instanceof ImportUpgradeHost,
                id + " is not an import upgrade host; the card cannot be installed in world");

        var host = (ImportUpgradeHost) be;
        var upgrades = host.nepImportUpgrades();
        helper.assertTrue(upgrades.size() == 1, id + " has no upgrade slot; its provider logic was not extended");

        var leftover = upgrades.addItems(new ItemStack(NepItems.IMPORT_CARD.get()));
        helper.assertTrue(leftover.isEmpty(), id + " refused the import card");
        helper.assertTrue(upgrades.isInstalled(NepItems.IMPORT_CARD.get()), id + " does not report the installed card");
        helper.setBlock(PROVIDER, Blocks.AIR.defaultBlockState());
    }

    public static void theAdvancedAeProvidersTakeTheCard(GameTestHelper helper) {
        assertProviderTakesTheCard(helper, "advanced_ae", "adv_pattern_provider");
        assertProviderTakesTheCard(helper, "advanced_ae", "small_adv_pattern_provider");
        helper.succeed();
    }

    public static void theExtendedAeProviderTakesTheCard(GameTestHelper helper) {
        assertProviderTakesTheCard(helper, "extendedae", "ex_pattern_provider");
        helper.succeed();
    }
}
