package dev.rylex.nep.hub;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.core.definitions.AEBlocks;
import appeng.helpers.patternprovider.PatternProviderTarget;
import dev.rylex.nep.Nep;
import dev.rylex.nep.NepConfig;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

@GameTestHolder(Nep.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MachineHubGameTest {

    private static final String TEMPLATE = "empty_5x5x5";
    private static final String WIDE_TEMPLATE = "empty_9x6x9";
    private static final String BATCH = "nep_machine_hub";

    private static final BlockPos HUB = new BlockPos(2, 1, 2);
    private static final BlockPos FIRST = new BlockPos(2, 1, 3);
    private static final BlockPos SECOND = new BlockPos(3, 1, 2);
    private static final BlockPos WIDE_HUB = new BlockPos(1, 1, 4);

    private MachineHubGameTest() {}

    private static MachineHubBlockEntity hub(GameTestHelper helper) {
        return hub(helper, HUB);
    }

    private static MachineHubBlockEntity hub(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, NepContent.MACHINE_HUB.get());
        MachineHubBlockEntity hub = MachineHubBlockEntity.at(helper.getLevel(), helper.absolutePos(pos));
        helper.assertTrue(hub != null, "the Machine Hub placed no block entity");
        return hub;
    }

    private static BlockPos casingWall(GameTestHelper helper, BlockPos from, int thickness) {
        BlockPos at = from;
        for (int step = 0; step < thickness; step++) {
            at = at.east();
            helper.setBlock(at, AEBlocks.QUARTZ_BLOCK.block());
        }
        BlockPos hatch = at.east();
        helper.setBlock(hatch, Blocks.BARREL);
        return hatch;
    }

    private static IItemHandler chest(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, Blocks.CHEST);
        IItemHandler handler =
                helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(pos), null);
        helper.assertTrue(handler != null, "the test chest at " + pos + " exposed no item handler");
        return handler;
    }

    private static void link(GameTestHelper helper, MachineHubBlockEntity hub, BlockPos pos, HubRole role) {
        List<HubLink> plan = new java.util.ArrayList<>(hub.links());
        plan.add(new HubLink(helper.absolutePos(pos), role));
        hub.applyPlan(plan);
    }

    private static PatternProviderTarget targetOf(GameTestHelper helper) {
        BlockPos absolute = helper.absolutePos(HUB);
        PatternProviderTarget target = PatternProviderTarget.get(
                helper.getLevel(),
                absolute,
                helper.getLevel().getBlockEntity(absolute),
                Direction.NORTH,
                IActionSource.empty());
        helper.assertTrue(
                target != null,
                "a pattern provider found nothing to push into on the Machine Hub; the ME storage capability is not"
                        + " reaching it");
        return target;
    }

    private static long count(IItemHandler handler, net.minecraft.world.item.Item item) {
        long total = 0;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void aProviderSeesTheHubAsSomethingItCanPushInto(GameTestHelper helper) {
        MachineHubBlockEntity hub = hub(helper);
        chest(helper, FIRST);
        link(helper, hub, FIRST, HubRole.INPUT);

        targetOf(helper);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void ingredientsPushedAtTheHubLandInTheLinkedInventory(GameTestHelper helper) {
        MachineHubBlockEntity hub = hub(helper);
        IItemHandler linked = chest(helper, FIRST);
        link(helper, hub, FIRST, HubRole.INPUT);

        long inserted = targetOf(helper).insert(AEItemKey.of(Items.OBSIDIAN), 6, Actionable.MODULATE);

        helper.assertValueEqual(inserted, 6L, "items the hub accepted");
        helper.assertValueEqual(count(linked, Items.OBSIDIAN), 6L, "items that reached the linked inventory");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void oneRecipeSpreadsOverTwoSeparateBlocks(GameTestHelper helper) {
        MachineHubBlockEntity hub = hub(helper);
        IItemHandler first = chest(helper, FIRST);
        IItemHandler second = chest(helper, SECOND);
        link(helper, hub, FIRST, HubRole.INPUT);
        link(helper, hub, SECOND, HubRole.INPUT);

        PatternProviderTarget target = targetOf(helper);
        target.insert(AEItemKey.of(Items.OBSIDIAN), 3, Actionable.MODULATE);
        target.insert(AEItemKey.of(Items.REDSTONE), 5, Actionable.MODULATE);

        helper.assertValueEqual(count(first, Items.OBSIDIAN), 3L, "obsidian in the first linked inventory");
        helper.assertValueEqual(count(first, Items.REDSTONE), 5L, "redstone in the first linked inventory");
        helper.assertTrue(
                count(second, Items.OBSIDIAN) == 0 && count(second, Items.REDSTONE) == 0,
                "the first inventory had room, so nothing should have spilled into the second");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void anOutputInventoryIsNeverFedIngredients(GameTestHelper helper) {
        MachineHubBlockEntity hub = hub(helper);
        IItemHandler results = chest(helper, FIRST);
        link(helper, hub, FIRST, HubRole.OUTPUT);

        long inserted = targetOf(helper).insert(AEItemKey.of(Items.OBSIDIAN), 4, Actionable.MODULATE);

        helper.assertValueEqual(inserted, 0L, "items an output-only hub accepted");
        helper.assertValueEqual(count(results, Items.OBSIDIAN), 0L, "items that reached the output inventory");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void resultsSittingInALinkedInventoryAreVisibleThroughTheHub(GameTestHelper helper) {
        MachineHubBlockEntity hub = hub(helper);
        IItemHandler results = chest(helper, FIRST);
        link(helper, hub, FIRST, HubRole.OUTPUT);
        results.insertItem(0, new ItemStack(Items.DIAMOND, 4), false);

        helper.assertTrue(
                targetOf(helper).containsPatternInput(Set.of(AEItemKey.of(Items.DIAMOND))),
                "blocking mode and the Import Card both read the hub's contents; a linked inventory that is not"
                        + " reported would let a second job be pushed on top of the first");
        helper.assertValueEqual(
                hub.storage().extract(AEItemKey.of(Items.DIAMOND), 3, Actionable.MODULATE, IActionSource.empty()),
                3L,
                "results the hub gave up");
        helper.assertValueEqual(count(results, Items.DIAMOND), 1L, "results left behind");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void aHubStandingAgainstAProviderBorrowsItsNetwork(GameTestHelper helper) {
        hub(helper);
        helper.setBlock(FIRST, AEBlocks.PATTERN_PROVIDER.block());

        helper.succeedWhen(() -> {
            helper.assertTrue(
                    HubNetwork.adjacentNode(helper.getLevel(), helper.absolutePos(HUB)) != null,
                    "the hub found no grid node on the Provider it is standing against, so results have nowhere to go");
            helper.assertTrue(
                    HubNetwork.adjacentNode(helper.getLevel(), helper.absolutePos(SECOND)) == null,
                    "a position with nothing networked around it reported a grid node");
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void aHubThatOnlyFeedsAMachineNeverComplainsAboutTheNetwork(GameTestHelper helper) {
        MachineHubBlockEntity hub = hub(helper);
        chest(helper, FIRST);
        link(helper, hub, FIRST, HubRole.INPUT);

        hub.serverTick(helper.getLevel());

        helper.assertValueEqual(
                hub.status(),
                HubStatus.OK,
                "a hub with nothing to send back has no use for a network, so the screen must stay clean");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void aHubWithResultsAndNoNetworkSaysSoOnItsScreen(GameTestHelper helper) {
        MachineHubBlockEntity hub = hub(helper);
        IItemHandler results = chest(helper, FIRST);
        results.insertItem(0, new ItemStack(Items.DIAMOND, 4), false);
        link(helper, hub, FIRST, HubRole.OUTPUT);

        hub.serverTick(helper.getLevel());

        helper.assertValueEqual(
                hub.status(),
                HubStatus.NO_NETWORK,
                "results piling up with no network to take them is exactly what the screen has to explain");
        helper.assertValueEqual(count(results, Items.DIAMOND), 4L, "results left where the machine put them");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void aHubWithNoLinksTakesNothing(GameTestHelper helper) {
        hub(helper);

        long inserted = targetOf(helper).insert(AEItemKey.of(Items.OBSIDIAN), 4, Actionable.MODULATE);

        helper.assertValueEqual(inserted, 0L, "items an unlinked hub accepted");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void aHubRefusesToLinkItselfOrAnotherHub(GameTestHelper helper) {
        MachineHubBlockEntity hub = hub(helper);
        helper.setBlock(FIRST, NepContent.MACHINE_HUB.get());

        hub.applyPlan(List.of(
                new HubLink(helper.absolutePos(HUB), HubRole.INPUT),
                new HubLink(helper.absolutePos(FIRST), HubRole.INPUT)));

        helper.assertTrue(
                hub.links().isEmpty(),
                "a hub linked to itself or to another hub would recurse the moment a provider read its contents");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void aLinkIsKeptAcrossASaveAndLoad(GameTestHelper helper) {
        MachineHubBlockEntity hub = hub(helper);
        chest(helper, FIRST);
        link(helper, hub, FIRST, HubRole.OUTPUT);

        net.minecraft.nbt.CompoundTag saved =
                hub.saveWithFullMetadata(helper.getLevel().registryAccess());
        MachineHubBlockEntity reloaded = new MachineHubBlockEntity(
                helper.absolutePos(HUB), NepContent.MACHINE_HUB.get().defaultBlockState());
        reloaded.loadWithComponents(saved, helper.getLevel().registryAccess());

        helper.assertValueEqual(
                reloaded.links(),
                List.of(new HubLink(helper.absolutePos(FIRST), HubRole.OUTPUT)),
                "links read back from disk");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void aScanProposesTheInventoriesTouchingIt(GameTestHelper helper) {
        MachineHubBlockEntity hub = hub(helper);
        chest(helper, FIRST);
        chest(helper, SECOND);

        List<HubLink> proposed = hub.scan();

        helper.assertTrue(
                proposed.stream().anyMatch(link -> link.pos().equals(helper.absolutePos(FIRST)))
                        && proposed.stream().anyMatch(link -> link.pos().equals(helper.absolutePos(SECOND))),
                "the scan missed an inventory touching the hub: " + proposed);
        helper.assertTrue(
                proposed.stream().noneMatch(link -> link.pos().equals(helper.absolutePos(HUB))),
                "the scan proposed the hub itself");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void aScanCrossesPlainCasingToReachAHatchBehindIt(GameTestHelper helper) {
        MachineHubBlockEntity hub = hub(helper);
        BlockPos casing = HUB.south();
        BlockPos hatch = casing.south();
        helper.setBlock(casing, AEBlocks.QUARTZ_BLOCK.block());
        helper.setBlock(hatch, Blocks.BARREL);

        List<HubLink> proposed = hub.scan();

        helper.assertTrue(
                proposed.stream().anyMatch(link -> link.pos().equals(helper.absolutePos(hatch))),
                "the scan stopped at a plain casing block instead of crossing it to the hatch behind: " + proposed);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void aScanWillNotCrossVanillaBuildingBlocksIntoWhateverIsBehindThem(GameTestHelper helper) {
        MachineHubBlockEntity hub = hub(helper);
        helper.setBlock(HUB.south(), Blocks.IRON_BLOCK);
        helper.setBlock(HUB.south().south(), Blocks.BARREL);

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
                proposed.stream().anyMatch(link -> link.pos().equals(helper.absolutePos(hatch))),
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
                proposed.stream().noneMatch(link -> link.pos().equals(helper.absolutePos(hatch))),
                "the scan ran through more plain blocks than the casing depth allows, which is how it would wander"
                        + " off a machine and into whatever a mod's decorative blocks lead to: " + proposed);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void aScanWillNotBridgeThroughAVanillaBlockEntityThatHoldsNothing(GameTestHelper helper) {
        MachineHubBlockEntity hub = hub(helper);
        helper.setBlock(HUB.south(), Blocks.SCULK_SENSOR);
        helper.setBlock(HUB.south().south(), Blocks.BARREL);

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
        helper.setBlock(new BlockPos(0, 0, 0), Blocks.CHEST);

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
        helper.setBlock(FIRST.north(), Blocks.CHEST);

        List<HubLink> proposed = hub.scan();

        helper.assertTrue(
                proposed.isEmpty(),
                "the scan walked into an ME network block; a hub linked back through its own provider would loop: "
                        + proposed);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void aNetworkBlockCannotBeLinkedByHand(GameTestHelper helper) {
        MachineHubBlockEntity hub = hub(helper);
        helper.setBlock(FIRST, AEBlocks.PATTERN_PROVIDER.block());

        hub.applyPlan(List.of(new HubLink(helper.absolutePos(FIRST), HubRole.INPUT)));

        helper.assertTrue(
                hub.links().isEmpty(),
                "a hub linked to a pattern provider would push the provider's own contents straight back at it");
        helper.succeed();
    }
}
