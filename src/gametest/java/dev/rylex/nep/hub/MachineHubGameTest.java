package dev.rylex.nep.hub;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.core.definitions.AEBlocks;
import appeng.helpers.patternprovider.PatternProviderTarget;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.NepGameTests;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;

public final class MachineHubGameTest {

    private static final BlockPos HUB = new BlockPos(2, 1, 2);
    private static final BlockPos FIRST = new BlockPos(2, 1, 3);
    private static final BlockPos SECOND = new BlockPos(3, 1, 2);
    private static final BlockPos WIDE_HUB = new BlockPos(1, 1, 4);

    private MachineHubGameTest() {}

    public static void register(NepGameTests.Batch batch, NepGameTests.Batch wide) {
        batch.add("a_provider_sees_the_hub_as_something_it_can_push_into", MachineHubGameTest::aProviderSeesTheHub)
                .add(
                        "ingredients_pushed_at_the_hub_land_in_the_linked_inventory",
                        MachineHubGameTest::ingredientsPushedAtTheHubLandInTheLinkedInventory)
                .add("one_recipe_spreads_over_two_separate_blocks", MachineHubGameTest::oneRecipeSpreadsOverTwoBlocks)
                .add("an_output_inventory_is_never_fed_ingredients", MachineHubGameTest::anOutputIsNeverFedIngredients)
                .add(
                        "results_sitting_in_a_linked_inventory_are_visible_through_the_hub",
                        MachineHubGameTest::resultsAreVisibleThroughTheHub)
                .add(
                        "a_hub_standing_against_a_provider_borrows_its_network",
                        MachineHubGameTest::aHubBorrowsAnAdjacentNetwork)
                .add(
                        "a_hub_that_only_feeds_a_machine_never_complains_about_the_network",
                        MachineHubGameTest::aFeedOnlyHubNeverComplains)
                .add(
                        "a_hub_with_results_and_no_network_says_so_on_its_screen",
                        MachineHubGameTest::aHubWithResultsAndNoNetworkSaysSo)
                .add("a_hub_with_no_links_takes_nothing", MachineHubGameTest::aHubWithNoLinksTakesNothing)
                .add(
                        "a_hub_refuses_to_link_itself_or_another_hub",
                        MachineHubGameTest::aHubRefusesToLinkItselfOrAnotherHub)
                .add("a_link_is_kept_across_a_save_and_load", MachineHubGameTest::aLinkIsKeptAcrossASaveAndLoad)
                .add(
                        "a_scan_proposes_the_inventories_touching_it",
                        MachineHubGameTest::aScanProposesTheInventoriesTouchingIt)
                .add(
                        "a_scan_crosses_plain_casing_to_reach_a_hatch_behind_it",
                        MachineHubGameTest::aScanCrossesPlainCasing)
                .add(
                        "a_scan_will_not_cross_vanilla_building_blocks",
                        MachineHubGameTest::aScanWillNotCrossVanillaBuildingBlocks)
                .add(
                        "a_scan_will_not_bridge_through_a_vanilla_block_entity_that_holds_nothing",
                        MachineHubGameTest::aScanWillNotBridgeThroughAnEmptyVanillaBlockEntity)
                .add(
                        "a_scan_will_not_walk_off_through_the_ground_it_is_standing_on",
                        MachineHubGameTest::aScanWillNotWalkOffThroughTheGround)
                .add("a_scan_turns_back_at_the_provider_feeding_it", MachineHubGameTest::aScanTurnsBackAtTheProvider)
                .add("a_network_block_cannot_be_linked_by_hand", MachineHubGameTest::aNetworkBlockCannotBeLinkedByHand);

        wide.add("a_scan_crosses_a_casing_wall_up_to_the_configured_depth", MachineHubGameTest::aScanCrossesACasingWall)
                .add(
                        "a_scan_stops_at_a_casing_wall_thicker_than_the_configured_depth",
                        MachineHubGameTest::aScanStopsAtATooThickCasingWall);
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
        helper.setBlock(hatch, Blocks.BARREL);
        return hatch;
    }

    private static ResourceHandler<ItemResource> chest(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, Blocks.CHEST);
        ResourceHandler<ItemResource> handler =
                helper.getLevel().getCapability(Capabilities.Item.BLOCK, helper.absolutePos(pos), null);
        helper.assertTrue(handler != null, "the test chest at " + pos + " exposed no item handler");
        return handler;
    }

    private static void link(GameTestHelper helper, MachineHubBlockEntity hub, BlockPos pos, HubRole role) {
        List<HubLink> plan = new ArrayList<>(hub.links());
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

    private static long count(ResourceHandler<ItemResource> handler, Item item) {
        long total = 0;
        for (int slot = 0; slot < handler.size(); slot++) {
            if (handler.getResource(slot).is(item)) {
                total += handler.getAmountAsLong(slot);
            }
        }
        return total;
    }

    private static void fill(ResourceHandler<ItemResource> handler, ItemStack stack) {
        ResourceHandlerUtil.insertStacking(handler, ItemResource.of(stack), stack.getCount(), null);
    }

    public static void aProviderSeesTheHub(GameTestHelper helper) {
        MachineHubBlockEntity hub = hub(helper);
        chest(helper, FIRST);
        link(helper, hub, FIRST, HubRole.INPUT);

        targetOf(helper);
        helper.succeed();
    }

    public static void ingredientsPushedAtTheHubLandInTheLinkedInventory(GameTestHelper helper) {
        MachineHubBlockEntity hub = hub(helper);
        ResourceHandler<ItemResource> linked = chest(helper, FIRST);
        link(helper, hub, FIRST, HubRole.INPUT);

        long inserted = targetOf(helper).insert(AEItemKey.of(Items.OBSIDIAN), 6, Actionable.MODULATE);

        helper.assertValueEqual(inserted, 6L, "items the hub accepted");
        helper.assertValueEqual(count(linked, Items.OBSIDIAN), 6L, "items that reached the linked inventory");
        helper.succeed();
    }

    public static void oneRecipeSpreadsOverTwoBlocks(GameTestHelper helper) {
        MachineHubBlockEntity hub = hub(helper);
        ResourceHandler<ItemResource> first = chest(helper, FIRST);
        ResourceHandler<ItemResource> second = chest(helper, SECOND);
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

    public static void anOutputIsNeverFedIngredients(GameTestHelper helper) {
        MachineHubBlockEntity hub = hub(helper);
        ResourceHandler<ItemResource> results = chest(helper, FIRST);
        link(helper, hub, FIRST, HubRole.OUTPUT);

        long inserted = targetOf(helper).insert(AEItemKey.of(Items.OBSIDIAN), 4, Actionable.MODULATE);

        helper.assertValueEqual(inserted, 0L, "items an output-only hub accepted");
        helper.assertValueEqual(count(results, Items.OBSIDIAN), 0L, "items that reached the output inventory");
        helper.succeed();
    }

    public static void resultsAreVisibleThroughTheHub(GameTestHelper helper) {
        MachineHubBlockEntity hub = hub(helper);
        ResourceHandler<ItemResource> results = chest(helper, FIRST);
        link(helper, hub, FIRST, HubRole.OUTPUT);
        fill(results, new ItemStack(Items.DIAMOND, 4));

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

    public static void aHubBorrowsAnAdjacentNetwork(GameTestHelper helper) {
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

    public static void aFeedOnlyHubNeverComplains(GameTestHelper helper) {
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

    public static void aHubWithResultsAndNoNetworkSaysSo(GameTestHelper helper) {
        MachineHubBlockEntity hub = hub(helper);
        ResourceHandler<ItemResource> results = chest(helper, FIRST);
        fill(results, new ItemStack(Items.DIAMOND, 4));
        link(helper, hub, FIRST, HubRole.OUTPUT);

        hub.serverTick(helper.getLevel());

        helper.assertValueEqual(
                hub.status(),
                HubStatus.NO_NETWORK,
                "results piling up with no network to take them is exactly what the screen has to explain");
        helper.assertValueEqual(count(results, Items.DIAMOND), 4L, "results left where the machine put them");
        helper.succeed();
    }

    public static void aHubWithNoLinksTakesNothing(GameTestHelper helper) {
        hub(helper);

        long inserted = targetOf(helper).insert(AEItemKey.of(Items.OBSIDIAN), 4, Actionable.MODULATE);

        helper.assertValueEqual(inserted, 0L, "items an unlinked hub accepted");
        helper.succeed();
    }

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

    public static void aLinkIsKeptAcrossASaveAndLoad(GameTestHelper helper) {
        MachineHubBlockEntity hub = hub(helper);
        chest(helper, FIRST);
        link(helper, hub, FIRST, HubRole.OUTPUT);

        TagValueOutput output = TagValueOutput.createWithContext(
                ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
        hub.saveWithFullMetadata(output);
        CompoundTag saved = output.buildResult();

        MachineHubBlockEntity reloaded = new MachineHubBlockEntity(
                helper.absolutePos(HUB), NepContent.MACHINE_HUB.get().defaultBlockState());
        reloaded.loadWithComponents(TagValueInput.create(
                ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), saved));

        helper.assertValueEqual(
                reloaded.links(),
                List.of(new HubLink(helper.absolutePos(FIRST), HubRole.OUTPUT)),
                "links read back from disk");
        helper.succeed();
    }

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

    public static void aScanCrossesPlainCasing(GameTestHelper helper) {
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

    public static void aScanWillNotCrossVanillaBuildingBlocks(GameTestHelper helper) {
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

    public static void aScanCrossesACasingWall(GameTestHelper helper) {
        MachineHubBlockEntity hub = hub(helper, WIDE_HUB);
        BlockPos hatch = casingWall(helper, WIDE_HUB, NepConfig.machineHubCasingDepth());

        List<HubLink> proposed = hub.scan();

        helper.assertTrue(
                proposed.stream().anyMatch(link -> link.pos().equals(helper.absolutePos(hatch))),
                "a wall no thicker than the configured casing depth stopped the scan reaching the hatch behind it: "
                        + proposed);
        helper.succeed();
    }

    public static void aScanStopsAtATooThickCasingWall(GameTestHelper helper) {
        MachineHubBlockEntity hub = hub(helper, WIDE_HUB);
        BlockPos hatch = casingWall(helper, WIDE_HUB, NepConfig.machineHubCasingDepth() + 1);

        List<HubLink> proposed = hub.scan();

        helper.assertTrue(
                proposed.stream().noneMatch(link -> link.pos().equals(helper.absolutePos(hatch))),
                "the scan ran through more plain blocks than the casing depth allows, which is how it would wander"
                        + " off a machine and into whatever a mod's decorative blocks lead to: " + proposed);
        helper.succeed();
    }

    public static void aScanWillNotBridgeThroughAnEmptyVanillaBlockEntity(GameTestHelper helper) {
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

    public static void aScanWillNotWalkOffThroughTheGround(GameTestHelper helper) {
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

    public static void aScanTurnsBackAtTheProvider(GameTestHelper helper) {
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
