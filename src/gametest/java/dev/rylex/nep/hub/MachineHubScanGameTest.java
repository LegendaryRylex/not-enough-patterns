package dev.rylex.nep.hub;

import static dev.rylex.nep.hub.MachineHubGameTest.FIRST;
import static dev.rylex.nep.hub.MachineHubGameTest.HUB;
import static dev.rylex.nep.hub.MachineHubGameTest.SECOND;
import static dev.rylex.nep.hub.MachineHubGameTest.WIDE_HUB;
import static dev.rylex.nep.hub.MachineHubGameTest.casingWall;
import static dev.rylex.nep.hub.MachineHubGameTest.chest;
import static dev.rylex.nep.hub.MachineHubGameTest.hatch;
import static dev.rylex.nep.hub.MachineHubGameTest.hub;
import static dev.rylex.nep.hub.MachineHubGameTest.proposes;

import appeng.core.definitions.AEBlocks;
import dev.rylex.nep.ConfigOverrides;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.NepGameTests;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;

public final class MachineHubScanGameTest {

    private MachineHubScanGameTest() {}

    public static void register(NepGameTests.Batch scan, NepGameTests.Batch wide, NepGameTests.Batch rules) {
        scan.add(
                        "a_scan_proposes_the_inventories_touching_it",
                        MachineHubScanGameTest::aScanProposesTheInventoriesTouchingIt)
                .add(
                        "a_scan_crosses_plain_casing_to_reach_a_hatch_behind_it",
                        MachineHubScanGameTest::aScanCrossesPlainCasingToReachAHatchBehindIt)
                .add(
                        "a_scan_will_not_cross_vanilla_building_blocks_into_whatever_is_behind_them",
                        MachineHubScanGameTest::aScanWillNotCrossVanillaBuildingBlocksIntoWhateverIsBehindThem)
                .add(
                        "a_scan_will_not_bridge_through_a_vanilla_block_entity_that_holds_nothing",
                        MachineHubScanGameTest::aScanWillNotBridgeThroughAVanillaBlockEntityThatHoldsNothing)
                .add(
                        "a_scan_will_not_walk_off_through_the_ground_it_is_standing_on",
                        MachineHubScanGameTest::aScanWillNotWalkOffThroughTheGroundItIsStandingOn)
                .add(
                        "a_scan_turns_back_at_the_provider_feeding_it",
                        MachineHubScanGameTest::aScanTurnsBackAtTheProviderFeedingIt)
                .add("a_scan_leaves_player_storage_alone", MachineHubScanGameTest::aScanLeavesPlayerStorageAlone);

        wide.add(
                        "a_scan_crosses_a_casing_wall_up_to_the_configured_depth",
                        MachineHubScanGameTest::aScanCrossesACasingWallUpToTheConfiguredDepth)
                .add(
                        "a_scan_stops_at_a_casing_wall_thicker_than_the_configured_depth",
                        MachineHubScanGameTest::aScanStopsAtACasingWallThickerThanTheConfiguredDepth);

        rules.add(
                        "a_whitelisted_block_carries_the_scan_like_the_hub_face_would",
                        MachineHubScanGameTest::aWhitelistedBlockCarriesTheScanLikeTheHubFaceWould)
                .add(
                        "a_link_blacklisted_block_is_refused_even_from_a_plan",
                        MachineHubScanGameTest::aLinkBlacklistedBlockIsRefusedEvenFromAPlan);
    }

    public static void aScanProposesTheInventoriesTouchingIt(GameTestHelper helper) {
        MachineHubBlockEntity hub = hub(helper);
        hatch(helper, FIRST);
        hatch(helper, SECOND);

        List<HubLink> proposed = hub.scan();

        helper.assertTrue(
                proposes(helper, proposed, FIRST) && proposes(helper, proposed, SECOND),
                "the scan missed an inventory touching the hub: " + proposed);
        helper.assertTrue(!proposes(helper, proposed, HUB), "the scan proposed the hub itself");
        helper.succeed();
    }

    public static void aScanCrossesPlainCasingToReachAHatchBehindIt(GameTestHelper helper) {
        MachineHubBlockEntity hub = hub(helper);
        BlockPos casing = HUB.south();
        BlockPos hatch = casing.south();
        helper.setBlock(casing, AEBlocks.QUARTZ_BLOCK.block());
        helper.setBlock(hatch, AEBlocks.SKY_STONE_TANK.block());

        List<HubLink> proposed = hub.scan();

        helper.assertTrue(
                proposes(helper, proposed, hatch),
                "the scan stopped at a plain casing block instead of crossing it to the hatch behind: " + proposed);
        helper.succeed();
    }

    public static void aScanWillNotCrossVanillaBuildingBlocksIntoWhateverIsBehindThem(GameTestHelper helper) {
        MachineHubBlockEntity hub = hub(helper);
        helper.setBlock(HUB.south(), Blocks.IRON_BLOCK);
        helper.setBlock(HUB.south().south(), AEBlocks.SKY_STONE_TANK.block());

        List<HubLink> proposed = hub.scan();

        helper.assertTrue(
                proposed.isEmpty(),
                "a vanilla block is indistinguishable from terrain, so crossing one would let the scan wander off the"
                        + " machine: " + proposed);
        helper.succeed();
    }

    public static void aScanCrossesACasingWallUpToTheConfiguredDepth(GameTestHelper helper) {
        MachineHubBlockEntity hub = hub(helper, WIDE_HUB);
        BlockPos hatch = casingWall(helper, WIDE_HUB, NepConfig.machineHubCasingDepth());

        List<HubLink> proposed = hub.scan();

        helper.assertTrue(
                proposes(helper, proposed, hatch),
                "a wall no thicker than the configured casing depth stopped the scan reaching the hatch behind it: "
                        + proposed);
        helper.succeed();
    }

    public static void aScanStopsAtACasingWallThickerThanTheConfiguredDepth(GameTestHelper helper) {
        MachineHubBlockEntity hub = hub(helper, WIDE_HUB);
        BlockPos hatch = casingWall(helper, WIDE_HUB, NepConfig.machineHubCasingDepth() + 1);

        List<HubLink> proposed = hub.scan();

        helper.assertTrue(
                !proposes(helper, proposed, hatch),
                "the scan ran through more plain blocks than the casing depth allows, which is how it would wander"
                        + " off a machine and into whatever a mod's decorative blocks lead to: " + proposed);
        helper.succeed();
    }

    public static void aScanWillNotBridgeThroughAVanillaBlockEntityThatHoldsNothing(GameTestHelper helper) {
        MachineHubBlockEntity hub = hub(helper);
        helper.setBlock(HUB.south(), Blocks.SCULK_SENSOR);
        helper.setBlock(HUB.south().south(), AEBlocks.SKY_STONE_TANK.block());

        List<HubLink> proposed = hub.scan();

        helper.assertTrue(
                proposed.isEmpty(),
                "a vanilla block entity with no inventory is decoration, not a machine part, so it must not carry the"
                        + " scan onward: " + proposed);
        helper.succeed();
    }

    public static void aScanWillNotWalkOffThroughTheGroundItIsStandingOn(GameTestHelper helper) {
        MachineHubBlockEntity hub = hub(helper);
        for (int x = 0; x < 5; x++) {
            for (int z = 0; z < 5; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
            }
        }
        helper.setBlock(new BlockPos(0, 0, 0), AEBlocks.SKY_STONE_TANK.block());

        List<HubLink> proposed = hub.scan();

        helper.assertTrue(
                proposed.isEmpty(),
                "the scan spread through a stone floor and picked up a chest sitting on the far side of it: "
                        + proposed);
        helper.succeed();
    }

    public static void aScanTurnsBackAtTheProviderFeedingIt(GameTestHelper helper) {
        MachineHubBlockEntity hub = hub(helper);
        helper.setBlock(FIRST, AEBlocks.PATTERN_PROVIDER.block());
        helper.setBlock(FIRST.north(), AEBlocks.SKY_STONE_TANK.block());

        List<HubLink> proposed = hub.scan();

        helper.assertTrue(
                proposed.isEmpty(),
                "the scan walked into an ME network block; a hub linked back through its own provider would loop: "
                        + proposed);
        helper.succeed();
    }

    public static void aScanLeavesPlayerStorageAlone(GameTestHelper helper) {
        MachineHubBlockEntity hub = hub(helper);
        chest(helper, FIRST);
        helper.setBlock(SECOND, Blocks.BARREL);

        List<HubLink> proposed = hub.scan();

        helper.assertTrue(
                proposed.isEmpty(),
                "chests and barrels ship in nep:machine_hub/blocked, so a scan proposing them would drag a player's"
                        + " storage into the machine: " + proposed);
        helper.succeed();
    }

    public static void aWhitelistedBlockCarriesTheScanLikeTheHubFaceWould(GameTestHelper helper) {
        ConfigOverrides.Restore restore =
                ConfigOverrides.override(HubConfigFields.SCAN_WHITELIST, List.of("minecraft:iron_block"));
        try {
            MachineHubBlockEntity hub = hub(helper);
            helper.setBlock(HUB.south(), Blocks.IRON_BLOCK);
            BlockPos behind = HUB.south().south();
            hatch(helper, behind);

            List<HubLink> proposed = hub.scan();

            helper.assertTrue(
                    proposes(helper, proposed, behind),
                    "a whitelisted vanilla casing block did not carry the scan to the hatch behind it: " + proposed);
        } finally {
            restore.undo();
        }
        helper.succeed();
    }

    public static void aLinkBlacklistedBlockIsRefusedEvenFromAPlan(GameTestHelper helper) {
        ConfigOverrides.Restore restore =
                ConfigOverrides.override(HubConfigFields.LINK_BLACKLIST, List.of("minecraft:chest"));
        try {
            MachineHubBlockEntity hub = hub(helper);
            chest(helper, FIRST);

            hub.applyPlan(List.of(new HubLink(helper.absolutePos(FIRST), HubRole.INPUT)));

            helper.assertTrue(
                    hub.links().isEmpty(),
                    "a link-blacklisted block was linked anyway, so the Hub Linker path around the scan is not"
                            + " gated");
        } finally {
            restore.undo();
        }
        helper.succeed();
    }
}
