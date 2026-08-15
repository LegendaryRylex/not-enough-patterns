package dev.rylex.nep;

import appeng.api.config.Actionable;
import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.CalculationStrategy;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.networking.crafting.ICraftingSimulationRequester;
import appeng.api.networking.crafting.ICraftingSubmitResult;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.blockentity.crafting.PatternProviderBlockEntity;
import appeng.blockentity.storage.DriveBlockEntity;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEItems;
import dev.rylex.nep.provider.ImportTrackerHost;
import dev.rylex.nep.provider.ImportUpgradeHost;
import dev.rylex.nep.provider.OwedSource;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

@GameTestHolder(Nep.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ImportCardChainedCraftGameTest {

    private static final String TEMPLATE = "empty_9x6x9";
    private static final String BATCH = "nep_import_card_chain";

    private static final BlockPos CONTROLLER = new BlockPos(4, 1, 4);
    private static final BlockPos ENERGY = new BlockPos(4, 1, 3);
    private static final BlockPos CPU = new BlockPos(5, 1, 4);
    private static final BlockPos DRIVE = new BlockPos(3, 1, 4);
    private static final BlockPos PROVIDER = new BlockPos(4, 1, 5);
    private static final BlockPos MACHINE = new BlockPos(4, 1, 6);

    private static final BlockPos INSERT_PROOF_MACHINE = PROVIDER.above();

    private static final Item BASE = Items.IRON_INGOT;
    private static final Item INTERMEDIATE = Items.GOLD_INGOT;
    private static final Item SECONDARY = Items.DIAMOND;
    private static final Item FINAL = Items.NETHERITE_INGOT;

    private static final int FURNACE_RESULT_SLOT = 2;
    private static final int SPARES = 1;

    private ImportCardChainedCraftGameTest() {}

    private static IActionSource source() {
        return IActionSource.empty();
    }

    private static PatternProviderBlockEntity provider(GameTestHelper helper) {
        PatternProviderBlockEntity be = helper.getBlockEntity(PROVIDER);
        helper.assertTrue(be != null, "the pattern provider block entity was missing");
        return be;
    }

    private static IGrid grid(GameTestHelper helper) {
        IGrid grid = provider(helper).getMainNode().getGrid();
        helper.assertTrue(grid != null, "the pattern provider never joined a grid");
        return grid;
    }

    private static IItemHandler machine(GameTestHelper helper) {
        IItemHandler handler = helper.getLevel()
                .getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(MACHINE), Direction.UP);
        helper.assertTrue(handler != null, "the stand-in machine exposed no item handler");
        return handler;
    }

    private static FurnaceBlockEntity insertProofMachine(GameTestHelper helper) {
        FurnaceBlockEntity furnace = helper.getBlockEntity(INSERT_PROOF_MACHINE);
        helper.assertTrue(furnace != null, "the machine holding the spare result was missing");
        return furnace;
    }

    private static long countIn(IItemHandler handler, Item item) {
        long total = 0;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static long countInNetwork(GameTestHelper helper, Item item) {
        return grid(helper)
                .getStorageService()
                .getInventory()
                .extract(AEItemKey.of(item), Long.MAX_VALUE, Actionable.SIMULATE, source());
    }

    private static void build(GameTestHelper helper) {
        helper.setBlock(CONTROLLER, AEBlocks.CONTROLLER.block());
        helper.setBlock(ENERGY, AEBlocks.CREATIVE_ENERGY_CELL.block());
        helper.setBlock(CPU, AEBlocks.CRAFTING_STORAGE_1K.block());
        helper.setBlock(DRIVE, AEBlocks.DRIVE.block());
        helper.setBlock(PROVIDER, AEBlocks.PATTERN_PROVIDER.block());
        helper.setBlock(MACHINE, Blocks.CHEST);
    }

    private static ItemStack firstPattern() {
        return PatternDetailsHelper.encodeProcessingPattern(
                List.of(new GenericStack(AEItemKey.of(BASE), 1)),
                List.of(new GenericStack(AEItemKey.of(INTERMEDIATE), 1)));
    }

    private static ItemStack secondPattern() {
        return PatternDetailsHelper.encodeProcessingPattern(
                List.of(new GenericStack(AEItemKey.of(INTERMEDIATE), 1), new GenericStack(AEItemKey.of(SECONDARY), 1)),
                List.of(new GenericStack(AEItemKey.of(FINAL), 1)));
    }

    private static IPatternDetails decode(GameTestHelper helper, ItemStack encoded) {
        IPatternDetails details = PatternDetailsHelper.decodePattern(encoded, helper.getLevel());
        helper.assertTrue(details != null, "the test pattern did not decode");
        return details;
    }

    private static KeyCounter[] inputsOf(IPatternDetails details) {
        KeyCounter[] inputs = new KeyCounter[details.getInputs().length];
        for (int slot = 0; slot < inputs.length; slot++) {
            inputs[slot] = new KeyCounter();
            IPatternDetails.IInput input = details.getInputs()[slot];
            GenericStack primary = input.getPossibleInputs()[0];
            inputs[slot].add(primary.what(), primary.amount() * input.getMultiplier());
        }
        return inputs;
    }

    private static Direction machineSide(GameTestHelper helper) {
        BlockPos from = helper.absolutePos(PROVIDER);
        BlockPos to = helper.absolutePos(MACHINE);
        Direction side = Direction.fromDelta(to.getX() - from.getX(), to.getY() - from.getY(), to.getZ() - from.getZ());
        helper.assertTrue(side != null, "the stand-in machine was not next to the provider");
        return side;
    }

    private static void outfit(GameTestHelper helper) {
        DriveBlockEntity drive = helper.getBlockEntity(DRIVE);
        helper.assertTrue(drive != null, "the ME drive block entity was missing");
        helper.assertTrue(
                drive.getInternalInventory()
                        .addItems(AEItems.ITEM_CELL_1K.stack())
                        .isEmpty(),
                "the ME drive refused the storage cell");

        PatternProviderBlockEntity be = provider(helper);
        helper.assertTrue(be instanceof ImportUpgradeHost, "the pattern provider host mixin did not apply");
        helper.assertTrue(
                ((ImportUpgradeHost) be)
                        .nepImportUpgrades()
                        .addItems(new ItemStack(NepItems.IMPORT_CARD.get()))
                        .isEmpty(),
                "the pattern provider refused the import card");
    }

    private static void encodePatterns(GameTestHelper helper) {
        PatternProviderBlockEntity be = provider(helper);
        helper.assertTrue(
                be.getLogic().getPatternInv().addItems(firstPattern()).isEmpty(),
                "the provider refused the first pattern");
        helper.assertTrue(
                be.getLogic().getPatternInv().addItems(secondPattern()).isEmpty(),
                "the provider refused the second pattern");
    }

    private static void stock(GameTestHelper helper) {
        var inventory = grid(helper).getStorageService().getInventory();
        helper.assertValueEqual(
                inventory.insert(AEItemKey.of(BASE), 1, Actionable.MODULATE, source()),
                1L,
                "the base ingredient the network accepted");
        helper.assertValueEqual(
                inventory.insert(AEItemKey.of(SECONDARY), 1, Actionable.MODULATE, source()),
                1L,
                "the secondary ingredient the network accepted");
    }

    private static Future<ICraftingPlan> plan(GameTestHelper helper) {
        IGrid grid = grid(helper);
        IGridNode node = provider(helper).getMainNode().getNode();
        helper.assertTrue(node != null, "the pattern provider had no grid node to plan from");
        ICraftingSimulationRequester simulation = new ICraftingSimulationRequester() {
            @Override
            public IActionSource getActionSource() {
                return source();
            }

            @Override
            public IGridNode getGridNode() {
                return node;
            }
        };
        helper.assertTrue(
                grid.getCraftingService().isCraftable(AEItemKey.of(FINAL)),
                "the network never saw the final result as craftable, so the patterns never reached the crafting"
                        + " service");
        return grid.getCraftingService()
                .beginCraftingCalculation(
                        helper.getLevel(),
                        simulation,
                        AEItemKey.of(FINAL),
                        1,
                        CalculationStrategy.REPORT_MISSING_ITEMS);
    }

    private static void submit(GameTestHelper helper, Future<ICraftingPlan> future) {
        helper.assertTrue(future.isDone(), "the crafting calculation had not finished in time");
        ICraftingPlan computed;
        try {
            computed = future.get();
        } catch (Exception e) {
            throw new AssertionError("the crafting calculation failed: " + e, e);
        }
        helper.assertTrue(
                !computed.simulation(),
                "the plan came back as a simulation, so the network was missing ingredients it should have had");
        ICraftingSubmitResult result =
                grid(helper).getCraftingService().submitJob(computed, null, null, false, source());
        helper.assertTrue(result.successful(), "the crafting job was refused: " + result.errorCode());
    }

    private static void completeFirstStep(GameTestHelper helper) {
        IItemHandler handler = machine(helper);
        helper.assertValueEqual(
                countIn(handler, BASE), 1L, "the base ingredient the provider pushed into the machine for step one");
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            handler.extractItem(slot, Integer.MAX_VALUE, false);
        }
        helper.assertTrue(
                handler.insertItem(0, new ItemStack(INTERMEDIATE), false).isEmpty(),
                "the machine could not hold the intermediate it just crafted");
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 600)
    public static void anIntermediateCraftedAtAProviderStaysPutWhenItIsPushedBackAsAnInput(GameTestHelper helper) {
        AtomicReference<Future<ICraftingPlan>> pending = new AtomicReference<>();
        helper.startSequence()
                .thenExecute(() -> build(helper))
                .thenIdle(60)
                .thenExecute(() -> {
                    outfit(helper);
                    encodePatterns(helper);
                })
                .thenIdle(60)
                .thenExecute(() -> stock(helper))
                .thenIdle(40)
                .thenExecute(() -> pending.set(plan(helper)))
                .thenIdle(60)
                .thenExecute(() -> submit(helper, pending.get()))
                .thenIdle(40)
                .thenExecute(() -> completeFirstStep(helper))
                .thenIdle(80)
                .thenExecute(() -> {
                    IItemHandler handler = machine(helper);
                    long inMachine = countIn(handler, INTERMEDIATE);
                    long inNetwork = countInNetwork(helper, INTERMEDIATE);
                    helper.assertValueEqual(
                            countIn(handler, SECONDARY),
                            1L,
                            "the secondary ingredient the provider pushed for step two");
                    helper.assertTrue(
                            inMachine == 1,
                            "the intermediate the first step crafted was taken back out of the machine after the"
                                    + " provider pushed it there as an input for the second step (machine holds "
                                    + inMachine + ", network holds " + inNetwork
                                    + "), so the second step can never run");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 600)
    public static void aResultTheCpuIsStillHoldingIsCollectedOnlyOnce(GameTestHelper helper) {
        AtomicReference<Future<ICraftingPlan>> pending = new AtomicReference<>();
        helper.startSequence()
                .thenExecute(() -> build(helper))
                .thenIdle(60)
                .thenExecute(() -> {
                    outfit(helper);
                    encodePatterns(helper);
                })
                .thenIdle(60)
                .thenExecute(() -> stock(helper))
                .thenIdle(40)
                .thenExecute(() -> pending.set(plan(helper)))
                .thenIdle(60)
                .thenExecute(() -> submit(helper, pending.get()))
                .thenIdle(40)
                .thenExecute(() -> {
                    helper.assertValueEqual(
                            countIn(machine(helper), BASE),
                            1L,
                            "the base ingredient the provider pushed for step one, which is what leaves the crafting"
                                    + " CPU waiting for the intermediate");
                    helper.setBlock(INSERT_PROOF_MACHINE, Blocks.FURNACE);
                    insertProofMachine(helper).setItem(FURNACE_RESULT_SLOT, new ItemStack(INTERMEDIATE, 1 + SPARES));

                    ImportTrackerHost tracker =
                            (ImportTrackerHost) provider(helper).getLogic();
                    IPatternDetails producer = decode(helper, firstPattern());
                    tracker.nepRecordOwed(producer, inputsOf(producer), new OwedSource.Side(Direction.UP));
                })
                .thenIdle(80)
                .thenExecute(() -> {
                    int left = insertProofMachine(helper)
                            .getItem(FURNACE_RESULT_SLOT)
                            .getCount();
                    helper.assertTrue(
                            left == SPARES,
                            "the provider was owed one intermediate and took " + (1 + SPARES - left)
                                    + " out of the machine; the crafting CPU holds what it collects out of reach of"
                                    + " network extraction, so a collection measured by what network storage gained"
                                    + " reads zero and the debt never retires");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 400)
    public static void anotherDispatchesInputIsLeftAloneWhileItsOwnOutputIsStillOutstanding(GameTestHelper helper) {
        helper.startSequence()
                .thenExecute(() -> build(helper))
                .thenIdle(60)
                .thenExecute(() -> outfit(helper))
                .thenIdle(40)
                .thenExecute(() -> {
                    ImportTrackerHost tracker =
                            (ImportTrackerHost) provider(helper).getLogic();
                    Direction side = machineSide(helper);
                    IPatternDetails producer = decode(helper, firstPattern());
                    IPatternDetails consumer = decode(helper, secondPattern());
                    tracker.nepRecordOwed(producer, inputsOf(producer), new OwedSource.Side(side));
                    tracker.nepRecordOwed(consumer, inputsOf(consumer), new OwedSource.Side(side));

                    IItemHandler handler = machine(helper);
                    helper.assertTrue(
                            handler.insertItem(0, new ItemStack(INTERMEDIATE), false)
                                    .isEmpty(),
                            "the machine could not hold the parked intermediate");
                    helper.assertTrue(
                            handler.insertItem(1, new ItemStack(SECONDARY), false)
                                    .isEmpty(),
                            "the machine could not hold the parked secondary ingredient");
                })
                .thenIdle(80)
                .thenExecute(() -> {
                    IItemHandler handler = machine(helper);
                    helper.assertTrue(
                            countIn(handler, INTERMEDIATE) == 1,
                            "an outstanding dispatch that is owed the intermediate imported the copy a later dispatch"
                                    + " had already parked at the machine as an input (network holds "
                                    + countInNetwork(helper, INTERMEDIATE) + ")");
                    helper.assertTrue(
                            countIn(handler, SECONDARY) == 1,
                            "the parked secondary ingredient was taken out of the machine");
                })
                .thenSucceed();
    }
}
