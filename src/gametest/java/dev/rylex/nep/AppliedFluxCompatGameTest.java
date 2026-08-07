package dev.rylex.nep;

import appeng.api.upgrades.IUpgradeableObject;
import appeng.core.definitions.AEBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Nep.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AppliedFluxCompatGameTest {

    private static final String TEMPLATE = "empty_5x5x5";
    private static final String BATCH = "nep_appflux";

    private static final BlockPos PROVIDER = new BlockPos(2, 1, 2);

    private AppliedFluxCompatGameTest() {}

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void aNeighborsEnergyQueryOnTheProviderDoesNotKillTheTick(GameTestHelper helper) {
        helper.assertTrue(
                ModList.get().isLoaded("appflux"),
                "appflux is missing from the gametest classpath; the host interface clash this batch guards"
                        + " against cannot be exercised");

        helper.setBlock(PROVIDER, AEBlocks.PATTERN_PROVIDER.block());
        for (var side : Direction.values()) {
            helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, helper.absolutePos(PROVIDER), side);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void appliedFluxUpgradeQueriesResolveOnEveryProviderHost(GameTestHelper helper) {
        assertUpgradeQueriesResolve(helper, AEBlocks.PATTERN_PROVIDER.block().defaultBlockState());
        assertUpgradeQueriesResolve(helper, providerState("advanced_ae", "adv_pattern_provider"));
        assertUpgradeQueriesResolve(helper, providerState("advanced_ae", "small_adv_pattern_provider"));
        helper.succeed();
    }

    private static void assertUpgradeQueriesResolve(GameTestHelper helper, BlockState state) {
        helper.setBlock(PROVIDER, state);
        var be = helper.getBlockEntity(PROVIDER);
        helper.assertTrue(be != null, state.getBlock() + " placed no block entity");
        helper.assertTrue(
                be instanceof IUpgradeableObject,
                state.getBlock() + " is not an AppliedFlux upgrade host; its host mixin did not apply");

        var host = (IUpgradeableObject) be;
        helper.assertTrue(
                !host.isUpgradedWith(NepItems.IMPORT_CARD.get()),
                state.getBlock() + " reports an upgrade card that was never installed");
        helper.assertTrue(
                host.getInstalledUpgrades(NepItems.IMPORT_CARD.get()) == 0,
                state.getBlock() + " counts upgrade cards that were never installed");
        helper.setBlock(PROVIDER, Blocks.AIR.defaultBlockState());
    }

    private static BlockState providerState(String modId, String blockPath) {
        var id = ResourceLocation.fromNamespaceAndPath(modId, blockPath);
        return BuiltInRegistries.BLOCK
                .getOptional(id)
                .orElseThrow(() -> new AssertionError(id + " is not registered though the mod is on the classpath"))
                .defaultBlockState();
    }
}
