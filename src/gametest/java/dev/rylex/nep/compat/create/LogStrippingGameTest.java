package dev.rylex.nep.compat.create;

import appeng.api.AECapabilities;
import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.deployer.DeployerApplicationRecipe;
import com.simibubi.create.content.kinetics.deployer.DeployerBlockEntity;
import com.simibubi.create.content.kinetics.deployer.ItemApplicationRecipe;
import com.simibubi.create.content.kinetics.deployer.ManualApplicationRecipe;
import com.simibubi.create.content.logistics.depot.DepotBlockEntity;
import dev.rylex.nep.Nep;
import dev.rylex.nep.pattern.AndesiteCraftingPattern;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;
import org.jetbrains.annotations.Nullable;

@GameTestHolder(Nep.MOD_ID)
@PrefixGameTestTemplate(false)
public final class LogStrippingGameTest {

    private static final String TEMPLATE = "empty_5x5x5";
    private static final String BATCH = "nep_log_stripping";

    private static final BlockPos DEPOT = new BlockPos(2, 1, 2);
    private static final BlockPos DEPLOYER = DEPOT.above(2);
    private static final Direction PROVIDER_SIDE = Direction.NORTH;
    private static final BlockPos PROVIDER = DEPOT.relative(PROVIDER_SIDE);

    private static final int SETUP_TICKS = 5;

    private LogStrippingGameTest() {}

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

    private static IItemHandler deployerHandler(GameTestHelper helper, DeployerBlockEntity deployer) {
        IItemHandler handler = DepotMachines.itemHandler(deployer);
        helper.assertTrue(handler != null, "the deployer exposed no item handler");
        return handler;
    }

    private static void holdAxe(GameTestHelper helper, DeployerBlockEntity deployer) {
        IItemHandler handler = deployerHandler(helper, deployer);
        ItemStack rejected = handler.insertItem(handler.getSlots() - 1, new ItemStack(Items.IRON_AXE), false);
        helper.assertTrue(rejected.isEmpty(), "the deployer would not take the axe it is meant to strip with");
    }

    private static ItemStack heldByDeployer(GameTestHelper helper, DeployerBlockEntity deployer) {
        IItemHandler handler = deployerHandler(helper, deployer);
        return handler.getStackInSlot(handler.getSlots() - 1);
    }

    private static ItemStack stagedOnDepot(GameTestHelper helper) {
        BlockEntity be = helper.getBlockEntity(DEPOT);
        helper.assertTrue(be instanceof DepotBlockEntity, "the depot did not create its block entity");
        return ((DepotBlockEntity) be).getHeldItem();
    }

    private static IPatternDetails strippingPattern(GameTestHelper helper) {
        ItemStack encoded = PatternDetailsHelper.encodeProcessingPattern(
                List.of(new GenericStack(AEItemKey.of(Items.OAK_LOG), 1)),
                List.of(new GenericStack(AEItemKey.of(Items.STRIPPED_OAK_LOG), 1)));
        IPatternDetails details = PatternDetailsHelper.decodePattern(encoded, helper.getLevel());
        helper.assertTrue(details != null, "the stripping test pattern did not decode");
        return details;
    }

    private static ResourceLocation strippingRecipeId() {
        return Nep.id("create/log_stripping/oak_log");
    }

    private static IPatternDetails andesiteStrippingPattern(GameTestHelper helper, boolean carriesTheAxe) {
        ItemStack encoded = AndesiteCraftingPattern.encode(
                strippingRecipeId(),
                List.of(new GenericStack(AEItemKey.of(Items.OAK_LOG), 1)),
                carriesTheAxe ? List.of(new GenericStack(AEItemKey.of(Items.IRON_AXE), 1)) : List.of(),
                new GenericStack(AEItemKey.of(Items.STRIPPED_OAK_LOG), 1));
        IPatternDetails details = PatternDetailsHelper.decodePattern(encoded, helper.getLevel());
        helper.assertTrue(
                details != null,
                "an andesite pattern naming a log stripping recipe did not decode; nep supplies that recipe itself, "
                        + "so it is absent from the recipe manager and has to be vouched for");
        return details;
    }

    private static IItemHandler placeProvider(GameTestHelper helper) {
        helper.setBlock(PROVIDER, Blocks.CHEST);
        IItemHandler handler = helper.getLevel()
                .getCapability(
                        Capabilities.ItemHandler.BLOCK, helper.absolutePos(PROVIDER), PROVIDER_SIDE.getOpposite());
        helper.assertTrue(handler != null, "the stand-in pattern provider exposed no item handler");
        return handler;
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

    private static KeyCounter[] inputsOf(IPatternDetails details) {
        KeyCounter[] inputs = new KeyCounter[details.getInputs().length];
        for (int slot = 0; slot < inputs.length; slot++) {
            inputs[slot] = new KeyCounter();
            IPatternDetails.IInput input = details.getInputs()[slot];
            inputs[slot].add(input.getPossibleInputs()[0].what(), input.getMultiplier());
        }
        return inputs;
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void aDeployerHoldingAnAxeFindsAStrippingRecipeForALog(GameTestHelper helper) {
        DeployerBlockEntity deployer = placeDeployer(helper);

        helper.runAfterDelay(SETUP_TICKS, () -> {
            holdAxe(helper, deployer);

            RecipeHolder<? extends Recipe<? extends RecipeInput>> found =
                    deployer.getRecipe(new ItemStack(Items.OAK_LOG));
            helper.assertTrue(
                    found != null,
                    "a deployer holding an axe found no recipe for an oak log; Create ships log stripping as a "
                            + "display-only JEI entry, so nep has to supply the recipe through DeployerRecipeSearchEvent");
            helper.assertTrue(
                    found.value() instanceof ItemApplicationRecipe,
                    "the stripping recipe was not an item application recipe");

            ItemApplicationRecipe recipe = (ItemApplicationRecipe) found.value();
            helper.assertTrue(
                    recipe.getRollableResults().size() == 1
                            && recipe.getRollableResults().get(0).getStack().is(Items.STRIPPED_OAK_LOG),
                    "the stripping recipe did not produce a stripped oak log");
            helper.assertTrue(
                    recipe.shouldKeepHeldItem(),
                    "the stripping recipe consumes the axe, so a Deployer would eat one axe per log");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void aDeployerWithoutAnAxeFindsNoStrippingRecipe(GameTestHelper helper) {
        DeployerBlockEntity deployer = placeDeployer(helper);

        helper.runAfterDelay(SETUP_TICKS, () -> {
            IItemHandler handler = deployerHandler(helper, deployer);
            handler.insertItem(handler.getSlots() - 1, new ItemStack(Items.STICK), false);

            helper.assertTrue(
                    deployer.getRecipe(new ItemStack(Items.OAK_LOG)) == null,
                    "a deployer holding a stick still found a log stripping recipe");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void aStrippingPatternStagesTheLogAndLeavesTheAxeAlone(GameTestHelper helper) {
        DepotCraftingMachine.clearCache();
        ICraftingMachine machine = placeDepot(helper);
        DeployerBlockEntity deployer = placeDeployer(helper);

        helper.runAfterDelay(SETUP_TICKS, () -> {
            holdAxe(helper, deployer);
            deployer.setSpeed(32);

            IPatternDetails details = strippingPattern(helper);
            helper.assertTrue(
                    machine.pushPattern(details, inputsOf(details), Direction.NORTH),
                    "a depot under an axe-holding deployer refused a log stripping pattern");
            helper.assertTrue(
                    stagedOnDepot(helper).is(Items.OAK_LOG),
                    "the log stripping push did not stage the log on the depot");

            ItemStack held = heldByDeployer(helper, deployer);
            helper.assertTrue(
                    held.is(Items.IRON_AXE) && held.getCount() == 1,
                    "the push disturbed the deployer's axe; a kept tool is not a pattern input, so nothing should "
                            + "have been inserted into the deployer");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void aStrippingPatternCarryingTheAxeLoadsItAndHandsItBack(GameTestHelper helper) {
        DepotCraftingMachine.clearCache();
        ICraftingMachine machine = placeDepot(helper);
        DeployerBlockEntity deployer = placeDeployer(helper);
        IItemHandler provider = placeProvider(helper);

        helper.runAfterDelay(SETUP_TICKS, () -> {
            deployer.setSpeed(32);

            IPatternDetails details = andesiteStrippingPattern(helper, true);
            helper.assertTrue(
                    machine.pushPattern(details, inputsOf(details), PROVIDER_SIDE),
                    "a depot under an empty-handed deployer refused a stripping pattern that carries its own axe");
            helper.assertTrue(stagedOnDepot(helper).is(Items.OAK_LOG), "the push did not stage the log on the depot");

            ItemStack held = heldByDeployer(helper, deployer);
            helper.assertTrue(
                    held.is(Items.IRON_AXE) && held.getCount() == 1,
                    "the push did not load the retained axe into the deployer, so the craft would never run");

            DeployerReclaimer.onCrafted(deployer);
            helper.assertTrue(
                    heldByDeployer(helper, deployer).isEmpty(),
                    "the deployer kept the axe after the craft; a retained input AE2 never gets back strands the job");
            helper.assertTrue(
                    countIn(provider, Items.IRON_AXE) == 1,
                    "the reclaimed axe was not handed back to the pattern provider");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void aStrippingPatternReturnsTheSpareAxeWhenTheDeployerIsAlreadyLoaded(GameTestHelper helper) {
        DepotCraftingMachine.clearCache();
        ICraftingMachine machine = placeDepot(helper);
        DeployerBlockEntity deployer = placeDeployer(helper);
        IItemHandler provider = placeProvider(helper);

        helper.runAfterDelay(SETUP_TICKS, () -> {
            holdAxe(helper, deployer);
            deployer.setSpeed(32);

            IPatternDetails details = andesiteStrippingPattern(helper, true);
            helper.assertTrue(
                    machine.pushPattern(details, inputsOf(details), PROVIDER_SIDE),
                    "a depot under an axe-holding deployer refused a stripping pattern that carries its own axe");

            ItemStack held = heldByDeployer(helper, deployer);
            helper.assertTrue(
                    held.is(Items.IRON_AXE) && held.getCount() == 1,
                    "the push stacked a second axe onto the one the deployer already held");
            helper.assertTrue(
                    countIn(provider, Items.IRON_AXE) == 1,
                    "the spare axe was neither used nor returned, so the network silently lost it");
            helper.succeed();
        });
    }

    @Nullable
    private static Item strippedForm(Item log) {
        if (!(log instanceof BlockItem blockItem)) {
            return null;
        }
        BlockState stripped = AxeItem.getAxeStrippingState(blockItem.getBlock().defaultBlockState());
        if (stripped == null) {
            return null;
        }
        Item result = stripped.getBlock().asItem();
        return result == Items.AIR || result == log ? null : result;
    }

    private static boolean coveredByALoadedRecipe(GameTestHelper helper, Item log) {
        ItemStack staged = new ItemStack(log);
        List<RecipeHolder<? extends ItemApplicationRecipe>> loaded = new ArrayList<>();
        loaded.addAll(helper.getLevel()
                .getRecipeManager()
                .getAllRecipesFor(AllRecipeTypes.DEPLOYING.<RecipeWrapper, DeployerApplicationRecipe>getType()));
        loaded.addAll(helper.getLevel()
                .getRecipeManager()
                .getAllRecipesFor(AllRecipeTypes.ITEM_APPLICATION.<RecipeWrapper, ManualApplicationRecipe>getType()));
        for (RecipeHolder<? extends ItemApplicationRecipe> holder : loaded) {
            if (!holder.value().getProcessedItem().test(staged)) {
                continue;
            }
            for (Holder<Item> axe : BuiltInRegistries.ITEM.getTagOrEmpty(ItemTags.AXES)) {
                if (holder.value().getRequiredHeldItem().test(new ItemStack(axe.value()))) {
                    return true;
                }
            }
        }
        return false;
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void aLogAlreadyCoveredByALoadedAxeRecipeGetsNoSyntheticOne(GameTestHelper helper) {
        LogStripping.clearCache();

        helper.assertTrue(
                coveredByALoadedRecipe(helper, Items.JUNGLE_LOG),
                "the test-namespace datapack recipe applying an axe to a jungle log did not load, so this test proves"
                        + " nothing");
        helper.assertTrue(
                LogStripping.byId(Nep.id("create/log_stripping/jungle_log"), helper.getLevel()) == null,
                "a loaded recipe already applies an axe to a jungle log, so nep must not supply a second one; two"
                        + " recipes for the same log make the Deployer's choice arbitrary and leave a pack unable to"
                        + " override stripping");
        for (RecipeHolder<? extends ItemApplicationRecipe> recipe : LogStripping.recipes(helper.getLevel())) {
            helper.assertTrue(
                    !recipe.value().getProcessedItem().test(new ItemStack(Items.JUNGLE_LOG)),
                    recipe.id() + " still strips a jungle log despite a loaded recipe claiming it");
        }

        helper.assertTrue(
                !coveredByALoadedRecipe(helper, Items.OAK_LOG),
                "an oak log unexpectedly has a loaded axe recipe, so the control half of this test proves nothing");
        helper.assertTrue(
                LogStripping.byId(strippingRecipeId(), helper.getLevel()) != null,
                "an oak log has no loaded axe recipe and still lost its stripping recipe");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void everyLoggableWoodGetsAStrippingRecipeAndNoneProducesAir(GameTestHelper helper) {
        LogStripping.clearCache();
        List<RecipeHolder<? extends ItemApplicationRecipe>> recipes = LogStripping.recipes(helper.getLevel());
        helper.assertTrue(!recipes.isEmpty(), "log stripping built no recipes at all");

        int expected = 0;
        StringBuilder unstrippable = new StringBuilder();
        for (Holder<Item> log : BuiltInRegistries.ITEM.getTagOrEmpty(ItemTags.LOGS)) {
            Item item = log.value();
            if (strippedForm(item) == null || coveredByALoadedRecipe(helper, item)) {
                for (RecipeHolder<? extends ItemApplicationRecipe> recipe : recipes) {
                    if (recipe.value().getProcessedItem().test(new ItemStack(item))) {
                        unstrippable.append("\n  ").append(BuiltInRegistries.ITEM.getKey(item));
                    }
                }
                continue;
            }
            expected++;
            boolean found = false;
            for (RecipeHolder<? extends ItemApplicationRecipe> recipe : recipes) {
                if (recipe.value().getProcessedItem().test(new ItemStack(item))) {
                    found = true;
                    helper.assertTrue(
                            recipe.value()
                                    .getRollableResults()
                                    .get(0)
                                    .getStack()
                                    .is(strippedForm(item)),
                            BuiltInRegistries.ITEM.getKey(item) + " strips to the wrong item");
                }
            }
            helper.assertTrue(
                    found,
                    BuiltInRegistries.ITEM.getKey(item) + " is in #minecraft:logs, has a stripped"
                            + " form, and got no stripping recipe");
        }

        helper.assertTrue(
                unstrippable.isEmpty(),
                "a log that should have been left alone was given a recipe anyway, either because it has no stripped"
                        + " variant and a Deployer would strip it to air, or because a loaded recipe already covers it:"
                        + unstrippable);
        helper.assertTrue(
                recipes.size() == expected,
                "log stripping built " + recipes.size() + " recipes for " + expected + " strippable logs in the tag");
        Set<ResourceLocation> ids = new HashSet<>();
        for (RecipeHolder<? extends ItemApplicationRecipe> recipe : recipes) {
            helper.assertTrue(
                    !recipe.value().getRollableResults().get(0).getStack().isEmpty(),
                    recipe.id() + " produces nothing");
            helper.assertTrue(
                    ids.add(recipe.id()),
                    recipe.id() + " was minted for two different logs, so an encoded pattern naming it would resolve"
                            + " to whichever one happened to be built last");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void anyAxeStripsAndTheLentOneComesBackUndamaged(GameTestHelper helper) {
        DepotCraftingMachine.clearCache();
        placeDepot(helper);
        DeployerBlockEntity deployer = placeDeployer(helper);

        helper.runAfterDelay(SETUP_TICKS, () -> {
            IItemHandler handler = deployerHandler(helper, deployer);
            for (Item axe : List.of(
                    Items.WOODEN_AXE,
                    Items.STONE_AXE,
                    Items.IRON_AXE,
                    Items.GOLDEN_AXE,
                    Items.DIAMOND_AXE,
                    Items.NETHERITE_AXE)) {
                handler.extractItem(handler.getSlots() - 1, 64, false);
                handler.insertItem(handler.getSlots() - 1, new ItemStack(axe), false);
                helper.assertTrue(
                        deployer.getRecipe(new ItemStack(Items.OAK_LOG)) != null,
                        "a deployer holding a " + BuiltInRegistries.ITEM.getKey(axe) + " found no stripping recipe");
            }
            handler.extractItem(handler.getSlots() - 1, 64, false);
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void aDeployerHoldingTheWrongToolRefusesTheLentAxeWithoutEatingIt(GameTestHelper helper) {
        DepotCraftingMachine.clearCache();
        ICraftingMachine machine = placeDepot(helper);
        DeployerBlockEntity deployer = placeDeployer(helper);
        IItemHandler provider = placeProvider(helper);

        helper.runAfterDelay(SETUP_TICKS, () -> {
            IItemHandler handler = deployerHandler(helper, deployer);
            handler.insertItem(handler.getSlots() - 1, new ItemStack(Items.STICK), false);
            deployer.setSpeed(32);

            IPatternDetails details = andesiteStrippingPattern(helper, true);
            helper.assertTrue(
                    !machine.pushPattern(details, inputsOf(details), PROVIDER_SIDE),
                    "a deployer holding a stick accepted a stripping pattern it can never run");

            ItemStack held = heldByDeployer(helper, deployer);
            helper.assertTrue(
                    held.is(Items.STICK) && held.getCount() == 1,
                    "the refused push disturbed the item the deployer was already holding");
            helper.assertTrue(
                    countIn(provider, Items.IRON_AXE) == 0, "a refused push left the lent axe lying in the provider");
            helper.assertTrue(stagedOnDepot(helper).isEmpty(), "a refused push still staged the log on the depot");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void aLentToolWithNoRoomToGoBackIsDroppedRatherThanVoided(GameTestHelper helper) {
        DepotCraftingMachine.clearCache();
        ICraftingMachine machine = placeDepot(helper);
        DeployerBlockEntity deployer = placeDeployer(helper);
        IItemHandler provider = placeProvider(helper);

        helper.runAfterDelay(SETUP_TICKS, () -> {
            deployer.setSpeed(32);
            for (int slot = 0; slot < provider.getSlots(); slot++) {
                provider.insertItem(slot, new ItemStack(Items.NETHERITE_BLOCK, 64), false);
            }

            IPatternDetails details = andesiteStrippingPattern(helper, true);
            helper.assertTrue(
                    machine.pushPattern(details, inputsOf(details), PROVIDER_SIDE),
                    "a depot refused a stripping pattern that carries its own axe");
            helper.assertTrue(
                    heldByDeployer(helper, deployer).is(Items.IRON_AXE), "the push did not load the retained axe");

            DeployerReclaimer.onCrafted(deployer);

            helper.assertTrue(
                    heldByDeployer(helper, deployer).isEmpty(), "the deployer kept the axe when the provider was full");
            helper.assertTrue(
                    countIn(provider, Items.IRON_AXE) == 0, "the full provider somehow swallowed the axe as well");
            helper.assertItemEntityPresent(Items.IRON_AXE, DEPLOYER, 2.0);
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void breakingTheDeployerDropsALentTool(GameTestHelper helper) {
        DepotCraftingMachine.clearCache();
        ICraftingMachine machine = placeDepot(helper);
        DeployerBlockEntity deployer = placeDeployer(helper);
        placeProvider(helper);

        helper.runAfterDelay(SETUP_TICKS, () -> {
            deployer.setSpeed(32);
            IPatternDetails details = andesiteStrippingPattern(helper, true);
            helper.assertTrue(
                    machine.pushPattern(details, inputsOf(details), PROVIDER_SIDE),
                    "a depot refused a stripping pattern that carries its own axe");
            helper.assertTrue(
                    heldByDeployer(helper, deployer).is(Items.IRON_AXE), "the push did not load the retained axe");

            helper.destroyBlock(DEPLOYER);

            helper.assertItemEntityPresent(Items.IRON_AXE, DEPLOYER, 2.0);
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 400)
    public static void oneLentAxeCoversAWholeRunOfStrippingCrafts(GameTestHelper helper) {
        DepotCraftingMachine.clearCache();
        ICraftingMachine machine = placeDepot(helper);
        DeployerBlockEntity deployer = placeDeployer(helper);
        IItemHandler provider = placeProvider(helper);
        IItemHandler depot = helper.getLevel()
                .getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(DEPOT), Direction.UP);
        helper.assertTrue(depot != null, "the depot exposed no item handler");

        helper.runAfterDelay(SETUP_TICKS, () -> {
            deployer.setSpeed(32);
            for (int run = 0; run < 5; run++) {
                IPatternDetails details = andesiteStrippingPattern(helper, true);
                helper.assertTrue(
                        machine.pushPattern(details, inputsOf(details), PROVIDER_SIDE),
                        "the stripping pattern was refused on run " + run + " of a five craft job");
                helper.assertTrue(
                        heldByDeployer(helper, deployer).is(Items.IRON_AXE),
                        "the axe was not loaded on run " + run + "; one copy has to cover the whole job");

                DeployerReclaimer.onCrafted(deployer);
                helper.assertTrue(
                        countIn(provider, Items.IRON_AXE) == 1,
                        "run " + run + " left " + countIn(provider, Items.IRON_AXE)
                                + " axes in the provider; the one lent copy must come back exactly once per craft");
                ItemStack returned = provider.extractItem(0, 1, false);
                helper.assertTrue(
                        returned.is(Items.IRON_AXE) && returned.getDamageValue() == 0,
                        "the lent axe came back damaged, so a job of many would wear out a tool the recipe never"
                                + " consumes");
                depot.extractItem(0, 64, false);
            }
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void aStrippingPushWaitsForTheDeployerToHoldAnAxe(GameTestHelper helper) {
        DepotCraftingMachine.clearCache();
        ICraftingMachine machine = placeDepot(helper);
        DeployerBlockEntity deployer = placeDeployer(helper);

        helper.runAfterDelay(SETUP_TICKS, () -> {
            deployer.setSpeed(32);

            IPatternDetails details = strippingPattern(helper);
            helper.assertTrue(
                    !machine.pushPattern(details, inputsOf(details), Direction.NORTH),
                    "a depot under an empty-handed deployer accepted a log stripping pattern");
            helper.assertTrue(
                    stagedOnDepot(helper).isEmpty(), "a rejected stripping push still staged the log on the depot");
            helper.assertTrue(
                    DepotCraftingMachine.memoisedRejectCount() == 0,
                    "a missing axe was memoised as permanently unsatisfiable; the player can still load one");
            helper.succeed();
        });
    }
}
