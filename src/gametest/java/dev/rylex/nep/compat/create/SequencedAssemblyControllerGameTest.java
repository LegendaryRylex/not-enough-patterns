package dev.rylex.nep.compat.create;

import static dev.rylex.nep.compat.create.CreateFixtures.itemHandler;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.parts.IPartHost;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.util.AEColor;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEParts;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import dev.rylex.nep.ConfigOverrides;
import dev.rylex.nep.GameTestFixtures;
import dev.rylex.nep.Nep;
import dev.rylex.nep.pattern.SequencedAssemblyPattern;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

@GameTestHolder(Nep.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SequencedAssemblyControllerGameTest {

    private static final String TEMPLATE = "empty_5x5x5";
    private static final String BATCH = "nep_controller";
    private static final BlockPos CONTROLLER = new BlockPos(2, 1, 2);
    private static final BlockPos DEPLOYER = new BlockPos(1, 1, 1);
    private static final BlockPos SECOND_DEPLOYER = new BlockPos(1, 1, 3);
    private static final BlockPos PRESS = new BlockPos(3, 1, 3);
    private static final BlockPos DEPOT = new BlockPos(4, 1, 2);
    private static final String TRACK_RECIPE = "create:sequenced_assembly/track";
    private static final String RECLAIM_GRACE_FIELD = "CREATE_SEQUENCED_ASSEMBLY_RECLAIM_GRACE";

    private SequencedAssemblyControllerGameTest() {}

    private static SequencedAssemblyControllerBlockEntity place(GameTestHelper helper) {
        helper.setBlock(CONTROLLER, NepCreateContent.CONTROLLER.get().defaultBlockState());
        BlockEntity be = helper.getBlockEntity(CONTROLLER);
        helper.assertTrue(
                be instanceof SequencedAssemblyControllerBlockEntity, "the controller did not create its block entity");
        return (SequencedAssemblyControllerBlockEntity) be;
    }

    private static int comparator(GameTestHelper helper) {
        return CreateFixtures.comparator(helper, CONTROLLER);
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void anUnlinkedControllerTakesNoPatterns(GameTestHelper helper) {
        SequencedAssemblyControllerBlockEntity controller = place(helper);

        helper.assertTrue(!controller.readyForPatterns(), "a controller with no input or output accepted patterns");
        helper.assertTrue(!controller.isHalted(), "a controller with nothing to do reported itself halted");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void hoppersAndPipesCanFeedTheStagingBuffer(GameTestHelper helper) {
        SequencedAssemblyControllerBlockEntity controller = place(helper);
        IItemHandler handler = itemHandler(helper, CONTROLLER, "the controller exposed no item handler");

        ItemStack leftover = handler.insertItem(0, new ItemStack(Items.COBBLESTONE, 6), false);

        helper.assertTrue(leftover.isEmpty(), "the controller refused a machine insert into its staging buffer");
        helper.assertTrue(
                controller.getBuffer().getStackInSlot(0).getCount() == 6, "the insert did not land in the buffer");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void finishedResultsStayOutOfReachOfNeighbours(GameTestHelper helper) {
        SequencedAssemblyControllerBlockEntity controller = place(helper);
        controller.getBuffer().insertItem(0, new ItemStack(Items.COBBLESTONE, 6), false);
        controller.getOutputBuffer().insertItem(0, new ItemStack(Items.DIAMOND, 8), false);

        IItemHandler handler = itemHandler(helper, CONTROLLER, "the controller exposed no item handler");

        for (int slot = 0; slot < handler.getSlots(); slot++) {
            helper.assertTrue(
                    handler.extractItem(slot, 64, true).isEmpty(),
                    "slot " + slot + " gave items up to a neighbour; finished results owe themselves to the "
                            + "pattern provider and used to leak into adjacent inventories this way");
        }
        helper.assertTrue(
                controller.getOutputBuffer().getStackInSlot(0).getCount() == 8, "the staged result went missing");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void comparatorReadsTheOutputBufferAndNothingElse(GameTestHelper helper) {
        SequencedAssemblyControllerBlockEntity controller = place(helper);

        helper.assertTrue(comparator(helper) == 0, "an empty controller emitted a signal");

        controller.getBuffer().insertItem(0, new ItemStack(Items.COBBLESTONE, 64), false);
        helper.assertTrue(comparator(helper) == 0, "the staged input buffer reached the comparator");

        controller.getOutputBuffer().insertItem(0, new ItemStack(Items.DIAMOND, 64), false);
        helper.assertTrue(comparator(helper) > 0, "a stocked output buffer emitted no signal");
        helper.succeed();
    }

    private static SequencedAssemblyControllerBlockEntity linkedLine(GameTestHelper helper) {
        SequencedAssemblyControllerBlockEntity controller = place(helper);
        helper.setBlock(
                DEPLOYER,
                AllBlocks.DEPLOYER.getDefaultState().setValue(DirectionalKineticBlock.FACING, Direction.DOWN));
        helper.setBlock(PRESS, AllBlocks.MECHANICAL_PRESS.getDefaultState());
        controller.applyPlan(
                helper.absolutePos(new BlockPos(0, 1, 2)),
                helper.absolutePos(new BlockPos(4, 1, 2)),
                List.of(helper.absolutePos(DEPLOYER), helper.absolutePos(PRESS)));
        return controller;
    }

    private static SequencedAssemblyState.Station station(
            GameTestHelper helper, SequencedAssemblyState state, int slot) {
        helper.assertTrue(
                state.stations().size() == 2,
                "the controller reported " + state.stations().size() + " stations for a two station line");
        return state.stations().get(slot);
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void aBrokenStationNamesWhatStoodThere(GameTestHelper helper) {
        SequencedAssemblyControllerBlockEntity controller = linkedLine(helper);

        helper.setBlock(PRESS, Blocks.AIR);
        SequencedAssemblyState state = controller.buildState();

        SequencedAssemblyState.Station broken = station(helper, state, 1);
        helper.assertTrue(
                broken.issue() == SequencedAssemblyState.Issue.UNRECOGNIZED, "the emptied slot was not flagged");
        helper.assertTrue(
                broken.expected() == StationKind.PRESS,
                "the slot asked for " + broken.expected() + " where a Mechanical Press had stood");
        helper.assertTrue(
                station(helper, state, 0).expected() == StationKind.UNKNOWN,
                "an intact station was told to replace itself");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void everyBrokenStationAnswersForItsOwnSlot(GameTestHelper helper) {
        SequencedAssemblyControllerBlockEntity controller = linkedLine(helper);

        helper.setBlock(DEPLOYER, Blocks.AIR);
        helper.setBlock(PRESS, Blocks.AIR);
        SequencedAssemblyState state = controller.buildState();

        helper.assertTrue(
                station(helper, state, 0).expected() == StationKind.DEPLOYER,
                "the first slot lost track of its Deployer once a second station broke too");
        helper.assertTrue(
                station(helper, state, 1).expected() == StationKind.PRESS,
                "the second slot lost track of its Mechanical Press");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void relinkingForgetsTheOldLine(GameTestHelper helper) {
        SequencedAssemblyControllerBlockEntity controller = linkedLine(helper);
        helper.setBlock(PRESS, Blocks.AIR);

        controller.clearLinks();
        controller.applyPlan(null, null, List.of(helper.absolutePos(PRESS)));

        SequencedAssemblyState.Station only = controller.buildState().stations().get(0);
        helper.assertTrue(
                only.expected() == StationKind.UNKNOWN, "a slot from a cleared plan still claimed a Mechanical Press");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void repeatedStepsAskForOneStationEach(GameTestHelper helper) {
        placeTrackStations(helper);

        SequencedAssemblyRecipe track =
                SequencedAssemblyResolver.resolveById(helper.getLevel(), ResourceLocation.parse(TRACK_RECIPE));
        helper.assertTrue(track != null, "Create no longer ships " + TRACK_RECIPE);

        BlockPos input = helper.absolutePos(new BlockPos(0, 1, 2));
        List<BlockPos> line =
                List.of(helper.absolutePos(DEPLOYER), helper.absolutePos(SECOND_DEPLOYER), helper.absolutePos(PRESS));
        helper.assertTrue(
                SequencedAssemblyResolver.recipesForLine(helper.getLevel(), input, helper.absolutePos(DEPOT), line)
                        .contains(track),
                "a Deployer, Deployer, Mechanical Press line did not match Create's Track recipe; its two Deploying "
                        + "steps share one ingredient and used to collapse into a single required station");
        helper.assertTrue(
                !SequencedAssemblyResolver.recipesForLine(
                                helper.getLevel(),
                                input,
                                helper.absolutePos(DEPOT),
                                List.of(helper.absolutePos(DEPLOYER), helper.absolutePos(PRESS)))
                        .contains(track),
                "a line one Deployer short still claimed Create's Track recipe");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void aPoweredControllerReportsNoNetworkFault(GameTestHelper helper) {
        SequencedAssemblyControllerBlockEntity controller = place(helper);
        helper.setBlock(CONTROLLER.above(), AEBlocks.CREATIVE_ENERGY_CELL.block());
        helper.runAfterDelay(40, () -> {
            helper.assertValueEqual(
                    controller.buildState().network(),
                    SequencedAssemblyState.Network.OK,
                    "the network fault of a controller on its own powered cell");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void controllersSharingAnAdHocNetworkReportMissingChannels(GameTestHelper helper) {
        SequencedAssemblyControllerBlockEntity first = place(helper);
        helper.setBlock(CONTROLLER.south(), AEBlocks.CREATIVE_ENERGY_CELL.block());
        BlockPos secondPos = CONTROLLER.south(2);
        helper.setBlock(secondPos, NepCreateContent.CONTROLLER.get().defaultBlockState());
        SequencedAssemblyControllerBlockEntity second =
                (SequencedAssemblyControllerBlockEntity) helper.getBlockEntity(secondPos);
        helper.runAfterDelay(40, () -> {
            helper.assertValueEqual(
                    first.buildState().network(),
                    SequencedAssemblyState.Network.NO_CHANNELS,
                    "the network fault of two controllers sharing one ad-hoc network's channels");
            helper.assertValueEqual(
                    second.buildState().network(),
                    SequencedAssemblyState.Network.NO_CHANNELS,
                    "the network fault of the second controller on that network");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void aControllerOnAnUnpoweredCableReportsNoPower(GameTestHelper helper) {
        SequencedAssemblyControllerBlockEntity controller = place(helper);
        BlockPos cable = CONTROLLER.above();
        helper.setBlock(cable, AEBlocks.CABLE_BUS.block());
        BlockEntity host = helper.getBlockEntity(cable);
        helper.assertTrue(host instanceof IPartHost, "the cable bus placed no part host");
        ((IPartHost) host).addPart(AEParts.GLASS_CABLE.item(AEColor.TRANSPARENT), null, null);
        helper.runAfterDelay(40, () -> {
            helper.assertValueEqual(
                    controller.buildState().network(),
                    SequencedAssemblyState.Network.NO_POWER,
                    "the network fault of a controller on a cable with no energy source");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void aLoneControllerReportsNoNetworkFault(GameTestHelper helper) {
        SequencedAssemblyControllerBlockEntity controller = place(helper);
        helper.runAfterDelay(20, () -> {
            helper.assertValueEqual(
                    controller.buildState().network(),
                    SequencedAssemblyState.Network.OK,
                    "the network fault of a controller with nothing attached");
            helper.succeed();
        });
    }

    private static void placeTrackStations(GameTestHelper helper) {
        helper.setBlock(
                DEPLOYER,
                AllBlocks.DEPLOYER.getDefaultState().setValue(DirectionalKineticBlock.FACING, Direction.DOWN));
        helper.setBlock(
                SECOND_DEPLOYER,
                AllBlocks.DEPLOYER.getDefaultState().setValue(DirectionalKineticBlock.FACING, Direction.DOWN));
        helper.setBlock(PRESS, AllBlocks.MECHANICAL_PRESS.getDefaultState());
        helper.setBlock(DEPOT, AllBlocks.DEPOT.getDefaultState());
    }

    private static SequencedAssemblyControllerBlockEntity trackLine(GameTestHelper helper) {
        SequencedAssemblyControllerBlockEntity controller = place(helper);
        placeTrackStations(helper);
        controller.applyPlan(
                helper.absolutePos(new BlockPos(0, 1, 2)),
                helper.absolutePos(DEPOT),
                List.of(helper.absolutePos(DEPLOYER), helper.absolutePos(SECOND_DEPLOYER), helper.absolutePos(PRESS)));
        return controller;
    }

    private static void pushTrackPattern(GameTestHelper helper) {
        ResourceLocation id = ResourceLocation.parse(TRACK_RECIPE);
        SequencedAssemblyRecipe track = SequencedAssemblyResolver.resolveById(helper.getLevel(), id);
        helper.assertTrue(track != null, "Create no longer ships " + TRACK_RECIPE);

        ItemStack base = track.getIngredient().getItems()[0];
        ItemStack encoded = SequencedAssemblyPattern.encode(
                id,
                List.of(new GenericStack(AEItemKey.of(base), 1)),
                new GenericStack(
                        AEItemKey.of(track.getResultItem(helper.getLevel().registryAccess())), 1));
        IPatternDetails details = PatternDetailsHelper.decodePattern(encoded, helper.getLevel());
        helper.assertTrue(details != null, "the track pattern did not decode");

        ICraftingMachine machine = GameTestFixtures.craftingMachine(
                helper, CONTROLLER, "the controller exposed no crafting machine capability");

        helper.assertTrue(
                machine.pushPattern(details, GameTestFixtures.inputsOf(details), Direction.UP),
                "the controller refused the track pattern");
    }

    private static void cancelEverything(GameTestHelper helper, SequencedAssemblyControllerBlockEntity controller) {
        controller.clearPending();
        controller.clearBufferTo(helper.makeMockPlayer(GameType.SURVIVAL));
    }

    private static void putOnDepot(GameTestHelper helper, ItemStack stack) {
        IItemHandler depot = itemHandler(helper, DEPOT, "the output Depot exposed no item handler");
        helper.assertTrue(
                ItemHandlerHelper.insertItem(depot, stack, false).isEmpty(), "the output Depot refused the item");
    }

    private static boolean depotIsClear(GameTestHelper helper) {
        IItemHandler depot =
                helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(DEPOT), null);
        if (depot == null) {
            return false;
        }
        for (int slot = 0; slot < depot.getSlots(); slot++) {
            if (!depot.getStackInSlot(slot).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void aCancelledJobStillCollectsWhatIsLeftOnTheLine(GameTestHelper helper) {
        SequencedAssemblyControllerBlockEntity controller = trackLine(helper);
        pushTrackPattern(helper);

        cancelEverything(helper, controller);
        putOnDepot(helper, new ItemStack(Items.IRON_INGOT, 3));

        helper.runAfterDelay(40, () -> {
            helper.assertTrue(
                    depotIsClear(helper),
                    "a cancelled job left what the Controller had already sent down the line stranded on the Depot");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = "nep_controller_no_reclaim", timeoutTicks = 200)
    public static void aZeroReclaimGraceLeavesTheDepotAlone(GameTestHelper helper) {
        ConfigOverrides.Restore restore = ConfigOverrides.override(RECLAIM_GRACE_FIELD, 0);
        try {
            SequencedAssemblyControllerBlockEntity controller = trackLine(helper);
            pushTrackPattern(helper);

            cancelEverything(helper, controller);
            putOnDepot(helper, new ItemStack(Items.IRON_INGOT, 3));
        } catch (RuntimeException | Error e) {
            restore.undo();
            throw e;
        }

        helper.runAfterDelay(40, () -> {
            restore.undo();
            helper.assertTrue(
                    !depotIsClear(helper), "a zero-tick reclaim grace still had the Controller clearing its Depot");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = "nep_controller_short_reclaim", timeoutTicks = 200)
    public static void theReclaimWindowClosesOnceItRunsOut(GameTestHelper helper) {
        ConfigOverrides.Restore restore = ConfigOverrides.override(RECLAIM_GRACE_FIELD, 20);
        try {
            SequencedAssemblyControllerBlockEntity controller = trackLine(helper);
            pushTrackPattern(helper);

            cancelEverything(helper, controller);
        } catch (RuntimeException | Error e) {
            restore.undo();
            throw e;
        }

        helper.runAfterDelay(60, () -> {
            restore.undo();
            putOnDepot(helper, new ItemStack(Items.IRON_INGOT, 3));
        });
        helper.runAfterDelay(100, () -> {
            helper.assertTrue(
                    !depotIsClear(helper),
                    "the Controller was still clearing its Depot long after its reclaim window ran out");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void breakingTheControllerDropsItsStagedItems(GameTestHelper helper) {
        SequencedAssemblyControllerBlockEntity controller = place(helper);
        controller.getBuffer().insertItem(0, new ItemStack(Items.COBBLESTONE, 5), false);
        controller.getOutputBuffer().insertItem(0, new ItemStack(Items.DIAMOND, 2), false);

        helper.destroyBlock(CONTROLLER);

        helper.assertItemEntityPresent(Items.COBBLESTONE, CONTROLLER, 2.0);
        helper.assertItemEntityPresent(Items.DIAMOND, CONTROLLER, 2.0);
        helper.succeed();
    }
}
