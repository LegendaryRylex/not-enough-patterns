package dev.rylex.nep.compat.create;

import appeng.api.AECapabilities;
import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.stacks.AEItemKey;
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

    private static IPatternDetails polishingPattern(GameTestHelper helper) {
        ItemStack encoded = AndesiteCraftingPattern.encode(
                POLISHING,
                List.of(new GenericStack(AEItemKey.of(AllItems.ROSE_QUARTZ.get()), 1)),
                List.of(new GenericStack(AEItemKey.of(AllItems.SAND_PAPER.get()), 1)),
                new GenericStack(AEItemKey.of(AllItems.POLISHED_ROSE_QUARTZ.get()), 1));
        IPatternDetails details = PatternDetailsHelper.decodePattern(encoded, helper.getLevel());
        helper.assertTrue(details != null, "the polishing test pattern did not decode");
        return details;
    }

    private static KeyCounter[] inputsOf(IPatternDetails details) {
        KeyCounter[] inputs = new KeyCounter[details.getInputs().length];
        for (int slot = 0; slot < inputs.length; slot++) {
            inputs[slot] = new KeyCounter();
            IPatternDetails.IInput input = details.getInputs()[slot];
            inputs[slot].add(input.getPossibleInputs()[0].what(), input.getMultiplier());
        }
        return inputs;
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void polishingReadsAsADeployerRecipeThatKeepsItsPaper(GameTestHelper helper) {
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
                recipe.shouldKeepHeldItem(),
                "the polishing recipe consumes the sand paper outright, so a whole sheet would go per polish");
        helper.assertTrue(
                recipe.getRollableResults().size() == 1
                        && recipe.getRollableResults().get(0).getStack().is(AllItems.POLISHED_ROSE_QUARTZ.get()),
                "the polishing recipe did not produce polished rose quartz");
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
                    "the push did not load the retained sand paper, so the polish would never run");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void aWornSheetStaysInTheDeployerAndTheSpareGoesBack(GameTestHelper helper) {
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
                    machine.pushPattern(details, inputsOf(details), PROVIDER_SIDE),
                    "a deployer holding a part-used sheet refused a polishing pattern it can still run");

            ItemStack held = heldByDeployer(helper, deployer);
            helper.assertTrue(
                    held.is(AllItems.SAND_PAPER.get()) && held.getDamageValue() == 1 && held.getCount() == 1,
                    "the push disturbed the part-used sheet the deployer was already sanding with");
            helper.assertTrue(
                    countIn(provider, AllItems.SAND_PAPER.get()) == 1,
                    "the spare sheet was neither used nor handed back, so the network lost it");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void aSheetWornByTheCraftIsNotHandedBack(GameTestHelper helper) {
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

            IItemHandler handler = deployerHandler(helper, deployer);
            int heldSlot = handler.getSlots() - 1;
            ItemStack lent = handler.extractItem(heldSlot, 1, false);
            lent.setDamageValue(1);
            handler.insertItem(heldSlot, lent, false);

            DeployerReclaimer.onCrafted(deployer);

            helper.assertTrue(
                    heldByDeployer(helper, deployer).getDamageValue() == 1,
                    "the sheet the polish wore down was pulled out of the deployer, so the next craft would draw a"
                            + " fresh one and this one would be lost");
            helper.assertTrue(
                    countIn(provider, AllItems.SAND_PAPER.get()) == 0,
                    "a part-used sheet was handed back to the network, where no pattern can ask for it again");
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
