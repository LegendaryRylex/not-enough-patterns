package dev.rylex.nep.compat.create;

import appeng.api.AECapabilities;
import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.deployer.DeployerApplicationRecipe;
import com.simibubi.create.content.kinetics.deployer.DeployerBlockEntity;
import com.simibubi.create.content.logistics.depot.DepotBlockEntity;
import dev.rylex.nep.Nep;
import dev.rylex.nep.pattern.AndesiteCraftingPattern;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

@GameTestHolder(Nep.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SandPaperPolishingGameTest {

    private static final String TEMPLATE = "empty_5x5x5";
    private static final String BATCH = "nep_sandpaper_polishing";

    private static final BlockPos DEPOT = new BlockPos(2, 1, 2);
    private static final BlockPos DEPLOYER = DEPOT.above(2);
    private static final Direction PROVIDER_SIDE = Direction.NORTH;
    private static final BlockPos PROVIDER = DEPOT.relative(PROVIDER_SIDE);

    private static final ResourceLocation POLISHING =
            ResourceLocation.fromNamespaceAndPath("create", "sandpaper_polishing/rose_quartz");

    private static final int LAST_POINT = 7;

    private static final int SETUP_TICKS = 5;

    private SandPaperPolishingGameTest() {}

    private static ICraftingMachine placeDepot(GameTestHelper helper) {
        helper.setBlock(DEPOT, AllBlocks.DEPOT.getDefaultState());
        ICraftingMachine machine =
                helper.getLevel().getCapability(AECapabilities.CRAFTING_MACHINE, helper.absolutePos(DEPOT), null);
        helper.assertTrue(machine != null, "the depot exposed no crafting machine capability");
        return machine;
    }

    private static DeployerBlockEntity placeDeployer(GameTestHelper helper) {
        helper.setBlock(
                DEPLOYER,
                AllBlocks.DEPLOYER.getDefaultState().setValue(DirectionalKineticBlock.FACING, Direction.DOWN));
        BlockEntity be = helper.getBlockEntity(DEPLOYER);
        helper.assertTrue(be instanceof DeployerBlockEntity, "the deployer did not create its block entity");
        return (DeployerBlockEntity) be;
    }

    private static IItemHandler placeProvider(GameTestHelper helper) {
        helper.setBlock(PROVIDER, Blocks.CHEST);
        IItemHandler handler = helper.getLevel()
                .getCapability(
                        Capabilities.ItemHandler.BLOCK, helper.absolutePos(PROVIDER), PROVIDER_SIDE.getOpposite());
        helper.assertTrue(handler != null, "the stand-in pattern provider exposed no item handler");
        return handler;
    }

    private static IItemHandler deployerHandler(GameTestHelper helper, DeployerBlockEntity deployer) {
        IItemHandler handler = DepotMachines.itemHandler(deployer);
        helper.assertTrue(handler != null, "the deployer exposed no item handler");
        return handler;
    }

    private static ItemStack heldByDeployer(GameTestHelper helper, DeployerBlockEntity deployer) {
        IItemHandler handler = deployerHandler(helper, deployer);
        return handler.getStackInSlot(handler.getSlots() - 1);
    }

    private static void hold(GameTestHelper helper, DeployerBlockEntity deployer, ItemStack tool) {
        IItemHandler handler = deployerHandler(helper, deployer);
        ItemStack rejected = handler.insertItem(handler.getSlots() - 1, tool, false);
        helper.assertTrue(rejected.isEmpty(), "the deployer would not take " + tool);
    }

    private static void wearHeldSheet(GameTestHelper helper, DeployerBlockEntity deployer, int damage) {
        IItemHandler handler = deployerHandler(helper, deployer);
        int heldSlot = handler.getSlots() - 1;
        ItemStack lent = handler.extractItem(heldSlot, 1, false);
        helper.assertTrue(!lent.isEmpty(), "the deployer was holding nothing to wear down");
        lent.setDamageValue(damage);
        handler.insertItem(heldSlot, lent, false);
    }

    private static ItemStack stagedOnDepot(GameTestHelper helper) {
        BlockEntity be = helper.getBlockEntity(DEPOT);
        helper.assertTrue(be instanceof DepotBlockEntity, "the depot did not create its block entity");
        return ((DepotBlockEntity) be).getHeldItem();
    }

    private static int countIn(IItemHandler handler, net.minecraft.world.item.Item item) {
        int found = 0;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (stack.is(item)) {
                found += stack.getCount();
            }
        }
        return found;
    }

    private static ItemStack firstIn(IItemHandler handler, net.minecraft.world.item.Item item) {
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (stack.is(item)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static AEItemKey sheet(int damage) {
        ItemStack stack = AllItems.SAND_PAPER.asStack();
        stack.setDamageValue(damage);
        return AEItemKey.of(stack);
    }

    private static IPatternDetails polishingPattern(GameTestHelper helper) {
        ItemStack encoded = AndesiteCraftingPattern.encode(
                POLISHING,
                List.of(new GenericStack(AEItemKey.of(AllItems.ROSE_QUARTZ.get()), 1)),
                List.of(),
                List.of(new GenericStack(AEItemKey.of(AllItems.SAND_PAPER.get()), 1)),
                new GenericStack(AEItemKey.of(AllItems.POLISHED_ROSE_QUARTZ.get()), 1));
        IPatternDetails details = PatternDetailsHelper.decodePattern(encoded, helper.getLevel());
        helper.assertTrue(details != null, "the polishing test pattern did not decode");
        return details;
    }

    private static KeyCounter[] inputsOf(IPatternDetails details) {
        return inputsWith(details, null);
    }

    private static KeyCounter[] inputsWith(IPatternDetails details, AEKey tool) {
        KeyCounter[] inputs = new KeyCounter[details.getInputs().length];
        for (int slot = 0; slot < inputs.length; slot++) {
            inputs[slot] = new KeyCounter();
            IPatternDetails.IInput input = details.getInputs()[slot];
            AEKey template = input.getPossibleInputs()[0].what();
            boolean isTool = input.getRemainingKey(template) != null;
            inputs[slot].add(tool != null && isTool ? tool : template, input.getMultiplier());
        }
        return inputs;
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void polishingReadsAsADeployerRecipeThatWearsItsPaper(GameTestHelper helper) {
        SandPaperPolishing.clearCache();
        RecipeHolder<DeployerApplicationRecipe> holder = SandPaperPolishing.byId(POLISHING, helper.getLevel());
        helper.assertTrue(
                holder != null,
                "no deployer recipe was built for " + POLISHING + "; Create ships polishing as its own recipe type, so"
                        + " nep has to present it as an item application before a pattern can name it");

        DeployerApplicationRecipe recipe = holder.value();
        helper.assertTrue(
                recipe.getProcessedItem().test(AllItems.ROSE_QUARTZ.asStack()),
                "the polishing recipe does not take rose quartz on the depot");
        helper.assertTrue(
                recipe.getRequiredHeldItem().test(AllItems.SAND_PAPER.asStack())
                        && recipe.getRequiredHeldItem().test(AllItems.RED_SAND_PAPER.asStack()),
                "the polishing recipe does not accept both sand papers as the held item");
        helper.assertTrue(
                !recipe.getRequiredHeldItem().test(new ItemStack(Items.IRON_AXE)),
                "the polishing recipe accepts an axe as the held item");
        helper.assertTrue(
                recipe.getRollableResults().size() == 1
                        && recipe.getRollableResults().get(0).getStack().is(AllItems.POLISHED_ROSE_QUARTZ.get()),
                "the polishing recipe did not produce polished rose quartz");
        helper.assertTrue(
                DeployerToolFate.of(POLISHING, recipe, helper.getLevel()) == DeployerToolFate.WORN,
                "the synthesised polishing recipe claims keep_held_item, but Create only honours that for an item"
                        + " application recipe and sands the sheet down every polish");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void theSheetInputTakesAnyDamageValueAndComesBackOnePointWorse(GameTestHelper helper) {
        IPatternDetails details = polishingPattern(helper);
        IPatternDetails.IInput tool = details.getInputs()[details.getInputs().length - 1];

        helper.assertTrue(
                tool.isValid(sheet(3), helper.getLevel()),
                "a part-used sheet is not a valid input, so every sheet the network already sanded with is dead stock");
        helper.assertTrue(
                !tool.isValid(AEItemKey.of(Items.IRON_AXE), helper.getLevel()),
                "the sheet input accepted an unrelated item");
        helper.assertTrue(
                sheet(4).equals(tool.getRemainingKey(sheet(3))),
                "the pattern does not ask for the sheet back one point worse, so the crafting job waits on a sheet the"
                        + " deployer will never hand over");
        helper.assertTrue(
                tool.getRemainingKey(sheet(LAST_POINT)) == null,
                "a sheet on its last point still declares a remainder, so the job waits on a sheet that broke");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void aPolishingPatternStagesTheQuartzAndLoadsThePaper(GameTestHelper helper) {
        DepotCraftingMachine.clearCache();
        ICraftingMachine machine = placeDepot(helper);
        DeployerBlockEntity deployer = placeDeployer(helper);
        placeProvider(helper);

        helper.runAfterDelay(SETUP_TICKS, () -> {
            deployer.setSpeed(32);

            IPatternDetails details = polishingPattern(helper);
            helper.assertTrue(
                    machine.pushPattern(details, inputsOf(details), PROVIDER_SIDE),
                    "a depot under an empty-handed deployer refused a polishing pattern carrying its own sand paper");
            helper.assertTrue(
                    stagedOnDepot(helper).is(AllItems.ROSE_QUARTZ.get()),
                    "the polishing push did not stage the rose quartz on the depot");
            helper.assertTrue(
                    heldByDeployer(helper, deployer).is(AllItems.SAND_PAPER.get()),
                    "the push did not load the lent sand paper, so the polish would never run");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void aPartUsedSheetDrawnFromStockRunsThePolish(GameTestHelper helper) {
        DepotCraftingMachine.clearCache();
        ICraftingMachine machine = placeDepot(helper);
        DeployerBlockEntity deployer = placeDeployer(helper);
        placeProvider(helper);

        helper.runAfterDelay(SETUP_TICKS, () -> {
            deployer.setSpeed(32);

            IPatternDetails details = polishingPattern(helper);
            helper.assertTrue(
                    machine.pushPattern(details, inputsWith(details, sheet(5)), PROVIDER_SIDE),
                    "the depot refused a polishing push carrying a part-used sheet, so sanded sheets pile up in the"
                            + " network unusable");
            ItemStack held = heldByDeployer(helper, deployer);
            helper.assertTrue(
                    held.is(AllItems.SAND_PAPER.get()) && held.getDamageValue() == 5,
                    "the push loaded " + held + " rather than the part-used sheet it was handed");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void aDeployerStillHoldingASheetTurnsThePushAway(GameTestHelper helper) {
        DepotCraftingMachine.clearCache();
        ICraftingMachine machine = placeDepot(helper);
        DeployerBlockEntity deployer = placeDeployer(helper);
        IItemHandler provider = placeProvider(helper);

        helper.runAfterDelay(SETUP_TICKS, () -> {
            deployer.setSpeed(32);
            ItemStack worn = AllItems.SAND_PAPER.asStack();
            worn.setDamageValue(1);
            hold(helper, deployer, worn);

            IPatternDetails details = polishingPattern(helper);
            helper.assertTrue(
                    !machine.pushPattern(details, inputsOf(details), PROVIDER_SIDE),
                    "the depot ran a polish on a sheet it had not been lent; the network is owed the sheet the pattern"
                            + " handed over, not whatever was already in the deployer");
            helper.assertTrue(
                    countIn(provider, AllItems.SAND_PAPER.get()) == 0,
                    "a refused push still moved a sheet into the provider");
            helper.assertTrue(
                    stagedOnDepot(helper).isEmpty(), "a refused push still staged the base item on the depot");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void aSheetWornByTheCraftIsHandedBackOnePointWorse(GameTestHelper helper) {
        DepotCraftingMachine.clearCache();
        ICraftingMachine machine = placeDepot(helper);
        DeployerBlockEntity deployer = placeDeployer(helper);
        IItemHandler provider = placeProvider(helper);

        helper.runAfterDelay(SETUP_TICKS, () -> {
            deployer.setSpeed(32);

            IPatternDetails details = polishingPattern(helper);
            helper.assertTrue(
                    machine.pushPattern(details, inputsOf(details), PROVIDER_SIDE),
                    "a depot refused a polishing pattern carrying its own sand paper");

            wearHeldSheet(helper, deployer, 1);
            DeployerReclaimer.onCrafted(deployer);

            helper.assertTrue(
                    heldByDeployer(helper, deployer).isEmpty(),
                    "the worn sheet was left in the deployer, so the network never gets it back and the crafting job"
                            + " waits forever on the remainder the pattern declared");
            ItemStack returned = firstIn(provider, AllItems.SAND_PAPER.get());
            helper.assertTrue(
                    returned.getCount() == 1 && returned.getDamageValue() == 1,
                    "the provider was handed " + returned + " rather than the sheet one point worse");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void aSheetOnItsLastPointBreaksAndNothingComesBack(GameTestHelper helper) {
        DepotCraftingMachine.clearCache();
        ICraftingMachine machine = placeDepot(helper);
        DeployerBlockEntity deployer = placeDeployer(helper);
        IItemHandler provider = placeProvider(helper);

        helper.runAfterDelay(SETUP_TICKS, () -> {
            deployer.setSpeed(32);

            IPatternDetails details = polishingPattern(helper);
            helper.assertTrue(
                    machine.pushPattern(details, inputsWith(details, sheet(LAST_POINT)), PROVIDER_SIDE),
                    "the depot refused a polishing push carrying a sheet on its last point of use");

            IItemHandler handler = deployerHandler(helper, deployer);
            handler.extractItem(handler.getSlots() - 1, 1, false);
            DeployerReclaimer.onCrafted(deployer);

            helper.assertTrue(
                    countIn(provider, AllItems.SAND_PAPER.get()) == 0,
                    "a sheet that broke on its last polish was still handed back to the network");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void theAssemblyMatrixTurnsPolishingDown(GameTestHelper helper) {
        IPatternDetails details = polishingPattern(helper);
        MatrixJobs.Outcome outcome =
                MatrixJobs.resolve(details, helper.getLevel(), AEItemKey.of(AllItems.POLISHED_ROSE_QUARTZ.get()), 1);
        helper.assertTrue(
                outcome.job() == null,
                "the matrix took on a polishing pattern; it hands its retained inputs back untouched, so the sand"
                        + " paper would never wear down and polishing would cost nothing");
        helper.succeed();
    }
}
