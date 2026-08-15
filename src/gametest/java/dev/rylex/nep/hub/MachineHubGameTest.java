package dev.rylex.nep.hub;

import appeng.api.config.Actionable;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEItems;
import appeng.helpers.patternprovider.PatternProviderTarget;
import dev.rylex.nep.ConfigOverrides;
import dev.rylex.nep.Nep;
import dev.rylex.nep.NepConfig;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.Tags;
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
    private static final BlockPos ME_CONTROLLER = new BlockPos(2, 2, 2);
    private static final BlockPos ENERGY = ME_CONTROLLER.above();

    private static final String CHANNELS_FIELD = "MACHINE_HUB_CHANNELS";
    private static final String CHANNELS_PER_LINK_FIELD = "MACHINE_HUB_CHANNELS_PER_LINK";

    private static final int AD_HOC_CEILING = 8;

    private MachineHubGameTest() {}

    private static void powerUp(GameTestHelper helper) {
        helper.setBlock(ME_CONTROLLER, AEBlocks.CONTROLLER.block());
        helper.setBlock(ENERGY, AEBlocks.CREATIVE_ENERGY_CELL.block().defaultBlockState());
    }

    private static void whenOnline(GameTestHelper helper, MachineHubBlockEntity hub, Runnable body) {
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(
                        hub.routing(),
                        "the hub never came online on the creative energy cell's grid, so it offers no storage"))
                .thenExecute(body)
                .thenSucceed();
    }

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
        helper.setBlock(hatch, AEBlocks.SKY_STONE_TANK.block());
        return hatch;
    }

    private static IItemHandler chest(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, Blocks.CHEST);
        IItemHandler handler =
                helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(pos), null);
        helper.assertTrue(handler != null, "the test chest at " + pos + " exposed no item handler");
        return handler;
    }

    private static void hatch(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, AEBlocks.SKY_STONE_TANK.block());
        helper.assertTrue(
                helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(pos), null) != null,
                "the test hatch at " + pos + " exposed no fluid handler");
    }

    private static IItemHandler foreignMachine(GameTestHelper helper, BlockPos pos, String id) {
        helper.setBlock(
                pos, BuiltInRegistries.BLOCK.get(ResourceLocation.parse(id)).defaultBlockState());
        IItemHandler handler =
                helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(pos), null);
        helper.assertTrue(handler != null, id + " exposed no item handler, so this test would prove nothing");
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

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void ingredientsPushedAtTheHubLandInTheLinkedInventory(GameTestHelper helper) {
        MachineHubBlockEntity hub = hub(helper);
        IItemHandler linked = chest(helper, FIRST);
        link(helper, hub, FIRST, HubRole.INPUT);
        powerUp(helper);

        whenOnline(helper, hub, () -> {
            long inserted = targetOf(helper).insert(AEItemKey.of(Items.OBSIDIAN), 6, Actionable.MODULATE);

            helper.assertValueEqual(inserted, 6L, "items the hub accepted");
            helper.assertValueEqual(count(linked, Items.OBSIDIAN), 6L, "items that reached the linked inventory");
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void oneRecipeSpreadsOverTwoSeparateBlocks(GameTestHelper helper) {
        MachineHubBlockEntity hub = hub(helper);
        IItemHandler first = chest(helper, FIRST);
        IItemHandler second = chest(helper, SECOND);
        link(helper, hub, FIRST, HubRole.INPUT);
        link(helper, hub, SECOND, HubRole.INPUT);
        powerUp(helper);

        whenOnline(helper, hub, () -> {
            PatternProviderTarget target = targetOf(helper);
            target.insert(AEItemKey.of(Items.OBSIDIAN), 3, Actionable.MODULATE);
            target.insert(AEItemKey.of(Items.REDSTONE), 5, Actionable.MODULATE);

            helper.assertValueEqual(count(first, Items.OBSIDIAN), 3L, "obsidian in the first linked inventory");
            helper.assertValueEqual(count(first, Items.REDSTONE), 5L, "redstone in the first linked inventory");
            helper.assertTrue(
                    count(second, Items.OBSIDIAN) == 0 && count(second, Items.REDSTONE) == 0,
                    "the first inventory had room, so nothing should have spilled into the second");
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void anOutputInventoryIsNeverFedIngredients(GameTestHelper helper) {
        MachineHubBlockEntity hub = hub(helper);
        IItemHandler results = chest(helper, FIRST);
        link(helper, hub, FIRST, HubRole.OUTPUT);
        powerUp(helper);

        whenOnline(helper, hub, () -> {
            long inserted = targetOf(helper).insert(AEItemKey.of(Items.OBSIDIAN), 4, Actionable.MODULATE);

            helper.assertValueEqual(inserted, 0L, "items an output-only hub accepted");
            helper.assertValueEqual(count(results, Items.OBSIDIAN), 0L, "items that reached the output inventory");
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void resultsSittingInALinkedInventoryAreVisibleThroughTheHub(GameTestHelper helper) {
        MachineHubBlockEntity hub = hub(helper);
        IItemHandler results = chest(helper, FIRST);
        link(helper, hub, FIRST, HubRole.OUTPUT);
        results.insertItem(0, new ItemStack(Items.DIAMOND, 4), false);
        powerUp(helper);

        whenOnline(helper, hub, () -> {
            helper.assertTrue(
                    targetOf(helper).containsPatternInput(Set.of(AEItemKey.of(Items.DIAMOND))),
                    "blocking mode and the Import Card both read the hub's contents; a linked inventory that is not"
                            + " reported would let a second job be pushed on top of the first");
            helper.assertValueEqual(
                    hub.storage().extract(AEItemKey.of(Items.DIAMOND), 3, Actionable.MODULATE, IActionSource.empty()),
                    3L,
                    "results the hub gave up");
            helper.assertValueEqual(count(results, Items.DIAMOND), 1L, "results left behind");
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void aHubStandingAgainstAProviderJoinsItsNetwork(GameTestHelper helper) {
        MachineHubBlockEntity hub = hub(helper);
        helper.setBlock(FIRST, AEBlocks.PATTERN_PROVIDER.block());
        powerUp(helper);

        helper.succeedWhen(() -> {
            IGridNode node = hub.gridNodeHost().getGridNode(Direction.NORTH);
            helper.assertTrue(node != null, "the hub built no grid node of its own, so it can never claim a channel");
            helper.assertTrue(
                    !node.getConnections().isEmpty(),
                    "the hub's node never connected to the Provider it is standing against, so results have nowhere"
                            + " to go");
            helper.assertTrue(
                    node.isActive(), "the hub's node never came online, so it was refused either power or a channel");
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void aHubOnAControllerNetworkBillsItForEveryLink(GameTestHelper helper) {
        MachineHubBlockEntity hub = hub(helper);
        powerUp(helper);
        IItemHandler first = chest(helper, FIRST);
        chest(helper, SECOND);
        link(helper, hub, FIRST, HubRole.INPUT);
        link(helper, hub, SECOND, HubRole.INPUT);

        int expected = NepConfig.machineHubChannels() + 2 * NepConfig.machineHubChannelsPerLink();

        helper.succeedWhen(() -> {
            IGridNode node = hub.gridNodeHost().getGridNode(Direction.NORTH);
            helper.assertTrue(node != null && node.getGrid() != null, "the hub never joined the controller's grid");
            helper.assertTrue(
                    node.isActive(),
                    "the hub never came online against a controller that can supply " + expected + " channels");
            helper.assertValueEqual(
                    node.getGrid().getPathingService().getUsedChannels(),
                    expected,
                    "channels the controller network reports in use");
            helper.assertTrue(first != null, "the linked inventory vanished");
        });
    }

    @GameTest(template = TEMPLATE, batch = "nep_machine_hub_channels", timeoutTicks = 200)
    public static void aHubDemandingMoreChannelsThanADeviceMayCarryStaysOffline(GameTestHelper helper) {
        ConfigOverrides.Restore restore = ConfigOverrides.override(CHANNELS_PER_LINK_FIELD, 32);
        MachineHubBlockEntity hub = hub(helper);
        powerUp(helper);
        chest(helper, FIRST);
        link(helper, hub, FIRST, HubRole.INPUT);

        helper.startSequence()
                .thenIdle(60)
                .thenExecute(() -> {
                    restore.undo();
                    helper.assertTrue(
                            !hub.routing(),
                            "a hub demanding more channels than AE2 lets one device carry came online anyway, so the"
                                    + " channel cost is not being enforced");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, batch = "nep_machine_hub_adhoc", timeoutTicks = 200)
    public static void anUnlinkedHubClaimsItsWholeDemandFromAnAdHocNetwork(GameTestHelper helper) {
        ConfigOverrides.Restore restore = ConfigOverrides.override(CHANNELS_FIELD, AD_HOC_CEILING);
        MachineHubBlockEntity hub = hub(helper);
        helper.setBlock(HUB.above(), AEBlocks.CREATIVE_ENERGY_CELL.block().defaultBlockState());

        int expected = NepConfig.machineHubChannels();

        helper.startSequence()
                .thenIdle(60)
                .thenExecute(() -> {
                    restore.undo();
                    IGridNode node = hub.gridNodeHost().getGridNode(Direction.NORTH);
                    helper.assertTrue(
                            node != null && node.getGrid() != null, "the hub never joined the energy cell's grid");
                    helper.assertTrue(
                            node.isActive(),
                            "a hub wanting all " + expected + " of an ad-hoc network's channels never came online");
                    helper.assertValueEqual(
                            node.getGrid().getPathingService().getUsedChannels(),
                            expected,
                            "channels the ad-hoc network reports in use");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, batch = "nep_machine_hub_adhoc", timeoutTicks = 200)
    public static void aLinkedHubTakesAnAdHocNetworkPastItsCeilingAndGoesDark(GameTestHelper helper) {
        ConfigOverrides.Restore restore = ConfigOverrides.override(CHANNELS_FIELD, AD_HOC_CEILING);
        MachineHubBlockEntity hub = hub(helper);
        helper.setBlock(HUB.above(), AEBlocks.CREATIVE_ENERGY_CELL.block().defaultBlockState());
        chest(helper, FIRST);
        link(helper, hub, FIRST, HubRole.INPUT);

        helper.startSequence()
                .thenIdle(60)
                .thenExecute(() -> {
                    restore.undo();
                    helper.assertTrue(
                            !hub.routing(),
                            "a hub wanting more than the eight channels an ad-hoc network carries came online anyway,"
                                    + " so the ad-hoc ceiling is not being enforced");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void aHubWithNoNetworkSaysSoEvenWhenItOnlyFeedsAMachine(GameTestHelper helper) {
        MachineHubBlockEntity hub = hub(helper);
        chest(helper, FIRST);
        link(helper, hub, FIRST, HubRole.INPUT);

        hub.serverTick(helper.getLevel());

        helper.assertValueEqual(
                hub.status(),
                HubStatus.NO_NETWORK,
                "the hub now takes a channel of its own, so one with nothing networked around it is offline and the"
                        + " screen has to say so");
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
        hatch(helper, FIRST);
        hatch(helper, SECOND);

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
        helper.setBlock(hatch, AEBlocks.SKY_STONE_TANK.block());

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
    public static void aNetworkBlockCannotBeLinkedByHand(GameTestHelper helper) {
        MachineHubBlockEntity hub = hub(helper);
        helper.setBlock(FIRST, AEBlocks.PATTERN_PROVIDER.block());

        hub.applyPlan(List.of(new HubLink(helper.absolutePos(FIRST), HubRole.INPUT)));

        helper.assertTrue(
                hub.links().isEmpty(),
                "a hub linked to a pattern provider would push the provider's own contents straight back at it");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void aWrenchPicksTheHubUpWithItsLinks(GameTestHelper helper) {
        MachineHubBlockEntity hub = hub(helper);
        chest(helper, FIRST);
        link(helper, hub, FIRST, HubRole.OUTPUT);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack wrench = AEItems.CERTUS_QUARTZ_WRENCH.stack();
        helper.assertTrue(
                wrench.is(Tags.Items.TOOLS_WRENCH),
                "AE2's wrench left c:tools/wrench, which is the only tag a Hub answers a wrench through");

        HubWrench.dismantle(helper.getLevel(), helper.absolutePos(HUB), player, wrench);

        helper.assertBlockPresent(Blocks.AIR, HUB);
        ItemStack picked = pickedUp(helper, player);
        List<HubLink> kept = picked.getOrDefault(NepContent.HUB_PLAN.get(), List.of());
        helper.assertValueEqual(kept.size(), 1, "links carried by the wrenched Hub");
        helper.assertTrue(
                kept.get(0).role() == HubRole.OUTPUT,
                "the wrenched Hub kept its link but forgot the role configured for it");

        MachineHubBlockEntity replaced = hub(helper);
        replaced.applyComponentsFromItemStack(picked);

        helper.assertValueEqual(replaced.links().size(), 1, "links restored when the Hub was placed again");
        helper.succeed();
    }

    private static ItemStack pickedUp(GameTestHelper helper, Player player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(NepContent.MACHINE_HUB_ITEM.get())) {
                return stack;
            }
        }
        helper.fail("wrenching the Hub put no Machine Hub in the player's inventory");
        return ItemStack.EMPTY;
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

        helper.assertTrue(
                proposed.stream().anyMatch(link -> link.pos().equals(helper.absolutePos(FIRST))),
                "the scan missed the hatch touching the hub: " + proposed);
        helper.assertTrue(
                proposed.stream().noneMatch(link -> link.pos().equals(helper.absolutePos(foreign))),
                "the scan crossed from one mod's machine into another mod's, which is how a neighbouring machine"
                        + " gets its inventories hijacked: " + proposed);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = "nep_machine_hub_rules")
    public static void aWhitelistedBlockCarriesTheScanLikeTheHubFaceWould(GameTestHelper helper) {
        ConfigOverrides.Restore restore =
                ConfigOverrides.override("MACHINE_HUB_SCAN_WHITELIST", List.of("minecraft:iron_block"));
        try {
            MachineHubBlockEntity hub = hub(helper);
            helper.setBlock(HUB.south(), Blocks.IRON_BLOCK);
            BlockPos behind = HUB.south().south();
            hatch(helper, behind);

            List<HubLink> proposed = hub.scan();

            helper.assertTrue(
                    proposed.stream().anyMatch(link -> link.pos().equals(helper.absolutePos(behind))),
                    "a whitelisted vanilla casing block did not carry the scan to the hatch behind it: " + proposed);
        } finally {
            restore.undo();
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = "nep_machine_hub_rules")
    public static void aLinkBlacklistedBlockIsRefusedEvenFromAPlan(GameTestHelper helper) {
        ConfigOverrides.Restore restore =
                ConfigOverrides.override("MACHINE_HUB_LINK_BLACKLIST", List.of("minecraft:chest"));
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
                ConfigOverrides.override("MACHINE_HUB_LINK_BLACKLIST", List.of("create:depot"));
        try {
            MachineHubBlockEntity hub = hub(helper);
            foreignMachine(helper, FIRST, "create:depot");
            BlockPos beyond = FIRST.south();
            foreignMachine(helper, beyond, "create:basin");

            List<HubLink> proposed = hub.scan();

            helper.assertTrue(
                    proposed.stream().noneMatch(link -> link.pos().equals(helper.absolutePos(FIRST))),
                    "a link-blacklisted block was proposed by the scan: " + proposed);
            helper.assertTrue(
                    proposed.stream().anyMatch(link -> link.pos().equals(helper.absolutePos(beyond))),
                    "a link-blacklisted machine part must still carry the scan to the parts beyond it: " + proposed);
        } finally {
            restore.undo();
        }
        helper.succeed();
    }
}
