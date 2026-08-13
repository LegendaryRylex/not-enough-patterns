package dev.rylex.nep;

import dev.rylex.nep.provider.ImportUpgradeHost;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Nep.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AddonProviderImportCardGameTest {

    private static final String TEMPLATE = "empty_5x5x5";
    private static final String BATCH = "nep_addon_providers";

    private static final BlockPos PROVIDER = new BlockPos(2, 1, 2);

    private AddonProviderImportCardGameTest() {}

    private static void assertProviderTakesTheCard(GameTestHelper helper, String modId, String blockPath) {
        var id = ResourceLocation.fromNamespaceAndPath(modId, blockPath);
        var block = BuiltInRegistries.BLOCK
                .getOptional(id)
                .orElseThrow(() -> new AssertionError(id + " is not registered though the mod is on the classpath"));

        helper.setBlock(PROVIDER, block.defaultBlockState());
        var be = helper.getBlockEntity(PROVIDER);
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

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void theAdvancedAeProvidersTakeTheCard(GameTestHelper helper) {
        assertProviderTakesTheCard(helper, "advanced_ae", "adv_pattern_provider");
        assertProviderTakesTheCard(helper, "advanced_ae", "small_adv_pattern_provider");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void theExtendedAeProviderTakesTheCard(GameTestHelper helper) {
        assertProviderTakesTheCard(helper, "extendedae", "ex_pattern_provider");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void theExpandedAeProviderTakesTheCard(GameTestHelper helper) {
        assertProviderTakesTheCard(helper, "expandedae", "exp_pattern_provider");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void theMegaProviderTakesTheCard(GameTestHelper helper) {
        assertProviderTakesTheCard(helper, "megacells", "mega_pattern_provider");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void theAppliedCreateProvidersTakeTheCard(GameTestHelper helper) {
        assertProviderTakesTheCard(helper, "appliedcreate", "andesite_pattern_provider");
        assertProviderTakesTheCard(helper, "appliedcreate", "brass_pattern_provider");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void theDraconicFusionProviderTakesTheCard(GameTestHelper helper) {
        assertProviderTakesTheCard(helper, "ae2_draconic_fusion_autocrafter", "me_draconic_pattern_provider");
        helper.succeed();
    }
}
