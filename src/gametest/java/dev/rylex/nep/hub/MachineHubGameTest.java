package dev.rylex.nep.hub;

import appeng.api.config.Actionable;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEItems;
import appeng.helpers.patternprovider.PatternProviderTarget;
import dev.rylex.nep.ConfigOverrides;
import dev.rylex.nep.GameTestFixtures;
import dev.rylex.nep.Nep;
import dev.rylex.nep.NepConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
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

    static final String TEMPLATE = "empty_5x5x5";
    static final String WIDE_TEMPLATE = "empty_9x6x9";
    static final String BATCH = "nep_machine_hub";

    static final BlockPos HUB = new BlockPos(2, 1, 2);
    static final BlockPos FIRST = new BlockPos(2, 1, 3);
    static final BlockPos SECOND = new BlockPos(3, 1, 2);
    static final BlockPos WIDE_HUB = new BlockPos(1, 1, 4);
    static final BlockPos ME_CONTROLLER = new BlockPos(2, 2, 2);
    static final BlockPos ENERGY = ME_CONTROLLER.above();

    private static final int AD_HOC_CEILING = 8;

    private MachineHubGameTest() {}

    static void powerUp(GameTestHelper helper) {
        GameTestFixtures.powerUp(helper, ME_CONTROLLER, ENERGY);
    }

    static void whenOnline(GameTestHelper helper, MachineHubBlockEntity hub, Runnable body) {
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(
                        hub.routing(),
                        "the hub never came online on the creative energy cell's grid, so it offers no storage"))
                .thenExecute(body)
                .thenSucceed();
    }

    static MachineHubBlockEntity hub(GameTestHelper helper) {
        return hub(helper, HUB);
    }

    static MachineHubBlockEntity hub(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, NepContent.MACHINE_HUB.get());
        MachineHubBlockEntity hub = MachineHubBlockEntity.at(helper.getLevel(), helper.absolutePos(pos));
        helper.assertTrue(hub != null, "the Machine Hub placed no block entity");
        return hub;
    }

    static BlockPos casingWall(GameTestHelper helper, BlockPos from, int thickness) {
        BlockPos at = from;
        for (int step = 0; step < thickness; step++) {
            at = at.east();
            helper.setBlock(at, AEBlocks.QUARTZ_BLOCK.block());
        }
        BlockPos hatch = at.east();
        helper.setBlock(hatch, AEBlocks.SKY_STONE_TANK.block());
        return hatch;
    }

    static IItemHandler chest(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, Blocks.CHEST);
        IItemHandler handler =
                helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(pos), null);
        helper.assertTrue(handler != null, "the test chest at " + pos + " exposed no item handler");
        return handler;
    }

    static void hatch(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, AEBlocks.SKY_STONE_TANK.block());
        helper.assertTrue(
                helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(pos), null) != null,
                "the test hatch at " + pos + " exposed no fluid handler");
    }

    static IItemHandler foreignMachine(GameTestHelper helper, BlockPos pos, String id) {
        helper.setBlock(
                pos, BuiltInRegistries.BLOCK.get(ResourceLocation.parse(id)).defaultBlockState());
        IItemHandler handler =
                helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(pos), null);
        helper.assertTrue(handler != null, id + " exposed no item handler, so this test would prove nothing");
        return handler;
    }

    static void link(GameTestHelper helper, MachineHubBlockEntity hub, BlockPos pos, HubRole role) {
        List<HubLink> plan = new ArrayList<>(hub.links());
        plan.add(new HubLink(helper.absolutePos(pos), role));
        hub.applyPlan(plan);
    }

    static boolean proposes(GameTestHelper helper, List<HubLink> proposed, BlockPos pos) {
        return proposed.stream().anyMatch(link -> link.pos().equals(helper.absolutePos(pos)));
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

    private static long count(IItemHandler handler, Item item) {
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
    public static void aLinkTakesTheRoleItIsSetTo(GameTestHelper helper) {
        MachineHubBlockEntity hub = hub(helper);
        chest(helper, FIRST);
        link(helper, hub, FIRST, HubRole.INPUT);

        hub.setRole(0, HubRole.INPUT.previous());
        helper.assertValueEqual(hub.links().get(0).role(), HubRole.BOTH, "the role after stepping back from Input");
        hub.setRole(0, HubRole.BOTH.next());
        helper.assertValueEqual(hub.links().get(0).role(), HubRole.INPUT, "the role after stepping on from Both");
        hub.setRole(5, HubRole.OUTPUT);
        helper.assertValueEqual(
                hub.links().size(), 1, "the link count after setting the role of a link that is not there");
        helper.succeed();
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
        chest(helper, FIRST);
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
        });
    }

    @GameTest(template = TEMPLATE, batch = "nep_machine_hub_channels", timeoutTicks = 200)
    public static void aHubDemandingMoreChannelsThanADeviceMayCarryStaysOffline(GameTestHelper helper) {
        ConfigOverrides.Restore restore = ConfigOverrides.override(HubConfigFields.CHANNELS_PER_LINK, 32);
        MachineHubBlockEntity hub = HubConfigFields.guarded(restore, () -> {
            MachineHubBlockEntity placed = hub(helper);
            powerUp(helper);
            chest(helper, FIRST);
            link(helper, placed, FIRST, HubRole.INPUT);
            return placed;
        });

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
        ConfigOverrides.Restore restore = ConfigOverrides.override(HubConfigFields.CHANNELS, AD_HOC_CEILING);
        MachineHubBlockEntity hub = HubConfigFields.guarded(restore, () -> {
            MachineHubBlockEntity placed = hub(helper);
            helper.setBlock(HUB.above(), AEBlocks.CREATIVE_ENERGY_CELL.block().defaultBlockState());
            return placed;
        });

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
        ConfigOverrides.Restore restore = ConfigOverrides.override(HubConfigFields.CHANNELS, AD_HOC_CEILING);
        MachineHubBlockEntity hub = HubConfigFields.guarded(restore, () -> {
            MachineHubBlockEntity placed = hub(helper);
            helper.setBlock(HUB.above(), AEBlocks.CREATIVE_ENERGY_CELL.block().defaultBlockState());
            chest(helper, FIRST);
            link(helper, placed, FIRST, HubRole.INPUT);
            return placed;
        });

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

        CompoundTag saved = hub.saveWithFullMetadata(helper.getLevel().registryAccess());
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
}
