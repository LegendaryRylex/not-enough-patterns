package dev.rylex.nep.hub;

import static dev.rylex.nep.hub.MachineHubGameTest.BATCH;
import static dev.rylex.nep.hub.MachineHubGameTest.FIRST;
import static dev.rylex.nep.hub.MachineHubGameTest.HUB;
import static dev.rylex.nep.hub.MachineHubGameTest.SECOND;
import static dev.rylex.nep.hub.MachineHubGameTest.TEMPLATE;
import static dev.rylex.nep.hub.MachineHubGameTest.WIDE_HUB;
import static dev.rylex.nep.hub.MachineHubGameTest.WIDE_TEMPLATE;
import static dev.rylex.nep.hub.MachineHubGameTest.casingWall;
import static dev.rylex.nep.hub.MachineHubGameTest.chest;
import static dev.rylex.nep.hub.MachineHubGameTest.foreignMachine;
import static dev.rylex.nep.hub.MachineHubGameTest.hatch;
import static dev.rylex.nep.hub.MachineHubGameTest.hub;
import static dev.rylex.nep.hub.MachineHubGameTest.proposes;

import appeng.core.definitions.AEBlocks;
import dev.rylex.nep.ConfigOverrides;
import dev.rylex.nep.Nep;
import dev.rylex.nep.NepConfig;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Nep.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MachineHubScanGameTest {

    private MachineHubScanGameTest() {}

    @GameTest(template = TEMPLATE, batch = BATCH)
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

    @GameTest(template = TEMPLATE, batch = BATCH)
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

    @GameTest(template = TEMPLATE, batch = BATCH)
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

    @GameTest(template = WIDE_TEMPLATE, batch = BATCH)
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

    @GameTest(template = WIDE_TEMPLATE, batch = BATCH)
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

    @GameTest(template = TEMPLATE, batch = BATCH)
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

    @GameTest(template = TEMPLATE, batch = BATCH)
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

    @GameTest(template = TEMPLATE, batch = BATCH)
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

    @GameTest(template = TEMPLATE, batch = BATCH)
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

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void aScanStaysOffAMachineWhoseModNeverTouchesTheHub(GameTestHelper helper) {
        MachineHubBlockEntity hub = hub(helper);
        hatch(helper, FIRST);
        BlockPos foreign = FIRST.south();
        foreignMachine(helper, foreign, "create:depot");

        List<HubLink> proposed = hub.scan();

        helper.assertTrue(proposes(helper, proposed, FIRST), "the scan missed the hatch touching the hub: " + proposed);
        helper.assertTrue(
                !proposes(helper, proposed, foreign),
                "the scan crossed from one mod's machine into another mod's, which is how a neighbouring machine"
                        + " gets its inventories hijacked: " + proposed);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = "nep_machine_hub_rules")
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

    @GameTest(template = TEMPLATE, batch = "nep_machine_hub_rules")
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

    @GameTest(template = TEMPLATE, batch = "nep_machine_hub_rules_walk")
    public static void aLinkBlacklistedPartStillCarriesTheScanPastItself(GameTestHelper helper) {
        ConfigOverrides.Restore restore =
                ConfigOverrides.override(HubConfigFields.LINK_BLACKLIST, List.of("create:depot"));
        try {
            MachineHubBlockEntity hub = hub(helper);
            foreignMachine(helper, FIRST, "create:depot");
            BlockPos beyond = FIRST.south();
            foreignMachine(helper, beyond, "create:basin");

            List<HubLink> proposed = hub.scan();

            helper.assertTrue(
                    !proposes(helper, proposed, FIRST),
                    "a link-blacklisted block was proposed by the scan: " + proposed);
            helper.assertTrue(
                    proposes(helper, proposed, beyond),
                    "a link-blacklisted machine part must still carry the scan to the parts beyond it: " + proposed);
        } finally {
            restore.undo();
        }
        helper.succeed();
    }
}
