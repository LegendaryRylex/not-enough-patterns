package dev.rylex.nep.compat.draconic;

import appeng.api.AECapabilities;
import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import com.brandon3055.draconicevolution.api.DraconicAPI;
import com.brandon3055.draconicevolution.api.crafting.IFusionInjector;
import com.brandon3055.draconicevolution.api.crafting.IFusionRecipe;
import com.brandon3055.draconicevolution.api.crafting.IFusionStateMachine;
import com.brandon3055.draconicevolution.blocks.machines.CraftingInjector;
import com.brandon3055.draconicevolution.blocks.tileentity.TileFusionCraftingCore;
import com.brandon3055.draconicevolution.init.DEContent;
import com.brandon3055.draconicevolution.init.ItemData;
import dev.rylex.nep.Nep;
import dev.rylex.nep.pattern.FusionCraftingPattern;
import dev.rylex.nep.pattern.encoding.EncodedIngredients;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

@GameTestHolder(Nep.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FusionCraftingMachineGameTest {

    private static final String TEMPLATE = "empty_9x6x9";
    private static final String BATCH = "nep_fusion";

    private static final BlockPos CORE = new BlockPos(4, 2, 4);

    private record InjectorSite(BlockPos pos, Direction facing) {}

    private static final List<InjectorSite> SITES = List.of(
            new InjectorSite(new BlockPos(1, 2, 4), Direction.EAST),
            new InjectorSite(new BlockPos(7, 2, 4), Direction.WEST),
            new InjectorSite(new BlockPos(4, 2, 1), Direction.SOUTH),
            new InjectorSite(new BlockPos(4, 2, 7), Direction.NORTH),
            new InjectorSite(new BlockPos(1, 3, 4), Direction.EAST),
            new InjectorSite(new BlockPos(1, 1, 4), Direction.EAST));

    private FusionCraftingMachineGameTest() {}

    private static TileFusionCraftingCore placeCore(GameTestHelper helper) {
        helper.setBlock(CORE, DEContent.CRAFTING_CORE.get().defaultBlockState());
        return (TileFusionCraftingCore) helper.getBlockEntity(CORE);
    }

    private static void placeInjectors(GameTestHelper helper, Block injector, int count) {
        for (int i = 0; i < count; i++) {
            InjectorSite site = SITES.get(i);
            helper.setBlock(site.pos(), injector.defaultBlockState().setValue(CraftingInjector.FACING, site.facing()));
        }
    }

    private static ICraftingMachine machine(GameTestHelper helper) {
        ICraftingMachine machine =
                helper.getLevel().getCapability(AECapabilities.CRAFTING_MACHINE, helper.absolutePos(CORE), null);
        helper.assertTrue(machine != null, "the fusion crafting core exposed no crafting machine capability");
        return machine;
    }

    @SuppressWarnings("unchecked")
    private static RecipeHolder<IFusionRecipe> sixIngredientRecipe(GameTestHelper helper) {
        RecipeType<IFusionRecipe> type = (RecipeType<IFusionRecipe>) DraconicAPI.FUSION_RECIPE_TYPE.get();
        for (RecipeHolder<IFusionRecipe> holder :
                helper.getLevel().getRecipeManager().getAllRecipesFor(type)) {
            EncodedIngredients expected = DraconicRecipeIngredients.fusion(holder, helper.getLevel());
            if (expected != null && expected.inputs().size() == SITES.size() + 1) {
                return holder;
            }
        }
        helper.fail("no fusion recipe with " + SITES.size() + " consumed ingredients was loaded");
        return null;
    }

    private static IPatternDetails patternFor(GameTestHelper helper, RecipeHolder<IFusionRecipe> holder) {
        return patternFor(helper, holder, true);
    }

    private static IPatternDetails patternFor(
            GameTestHelper helper, RecipeHolder<IFusionRecipe> holder, boolean includeRetained) {
        EncodedIngredients expected = DraconicRecipeIngredients.fusion(holder, helper.getLevel(), includeRetained);
        helper.assertTrue(expected != null, "the recipe produced no ingredient list");

        List<GenericStack> inputs = new ArrayList<>();
        List<GenericStack> retained = new ArrayList<>();
        for (int slot = 0; slot < expected.inputs().size(); slot++) {
            (expected.isRetained(slot) ? retained : inputs)
                    .add(expected.inputs().get(slot).get(0));
        }

        ItemStack encoded = FusionCraftingPattern.encode(
                holder.id(), inputs, retained, expected.outputs().get(0));
        IPatternDetails details = PatternDetailsHelper.decodePattern(encoded, helper.getLevel());
        helper.assertTrue(details != null, "the fusion pattern did not decode");
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
    public static void aFusionPatternLoadsEveryInjectorAndStartsTheCraft(GameTestHelper helper) {
        FusionCraftingMachine.clearCache();
        FusionRecipeResolver.clearCache();

        TileFusionCraftingCore core = placeCore(helper);
        placeInjectors(helper, DEContent.WYVERN_CRAFTING_INJECTOR.get(), SITES.size());
        ICraftingMachine machine = machine(helper);

        RecipeHolder<IFusionRecipe> holder = sixIngredientRecipe(helper);
        IPatternDetails details = patternFor(helper, holder);

        helper.assertTrue(
                machine.pushPattern(details, inputsOf(details), Direction.UP),
                "a fusion pattern with a complete injector array was rejected");

        helper.assertTrue(core.isCrafting(), "the core did not start crafting after the pattern was pushed");
        helper.assertTrue(!core.getCatalystStack().isEmpty(), "the catalyst slot was left empty");

        int loaded = 0;
        for (IFusionInjector injector : core.getInjectors()) {
            if (!injector.getInjectorStack().isEmpty()) {
                loaded++;
            }
        }
        helper.assertTrue(loaded == SITES.size(), "expected " + SITES.size() + " loaded injectors but found " + loaded);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void injectorsBelowTheRecipeTierAreRefused(GameTestHelper helper) {
        FusionCraftingMachine.clearCache();
        FusionRecipeResolver.clearCache();

        TileFusionCraftingCore core = placeCore(helper);
        placeInjectors(helper, DEContent.BASIC_CRAFTING_INJECTOR.get(), SITES.size());
        ICraftingMachine machine = machine(helper);

        RecipeHolder<IFusionRecipe> holder = sixIngredientRecipe(helper);
        IPatternDetails details = patternFor(helper, holder);

        helper.assertTrue(
                !machine.pushPattern(details, inputsOf(details), Direction.UP),
                "a wyvern-tier recipe was pushed into draconium-tier injectors");
        helper.assertTrue(!core.isCrafting(), "the core started a craft it cannot complete");
        helper.assertTrue(core.getCatalystStack().isEmpty(), "a refused push left the catalyst behind");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void anIncompleteInjectorArrayIsRefusedWithoutStrandingItems(GameTestHelper helper) {
        FusionCraftingMachine.clearCache();
        FusionRecipeResolver.clearCache();

        TileFusionCraftingCore core = placeCore(helper);
        placeInjectors(helper, DEContent.WYVERN_CRAFTING_INJECTOR.get(), SITES.size() - 1);
        ICraftingMachine machine = machine(helper);

        RecipeHolder<IFusionRecipe> holder = sixIngredientRecipe(helper);
        IPatternDetails details = patternFor(helper, holder);

        helper.assertTrue(
                !machine.pushPattern(details, inputsOf(details), Direction.UP),
                "a pattern was accepted with one injector missing");
        helper.assertTrue(core.getCatalystStack().isEmpty(), "a refused push left the catalyst behind");
        for (IFusionInjector injector : core.getInjectors()) {
            helper.assertTrue(injector.getInjectorStack().isEmpty(), "a refused push left an item in an injector");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void aSuppliedKeptIngredientIsLoadedIntoAnInjector(GameTestHelper helper) {
        TileFusionCraftingCore core = startKeptIngredientCraft(helper);
        helper.assertTrue(
                countInInjectors(core) == 1,
                "the network supplied the kept ingredient but it never reached an injector, so the craft cannot run");
        helper.assertTrue(countReturned(helper) == 0, "the kept ingredient was handed back before the craft finished");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void aCompletedCraftHandsTheKeptIngredientBack(GameTestHelper helper) {
        TileFusionCraftingCore core = startKeptIngredientCraft(helper);
        core.completeCraft();

        helper.assertTrue(
                countReturned(helper) == 1,
                "the core kept the supplied ingredient after the craft; the crafting CPU would stall waiting for it");
        helper.assertTrue(countInInjectors(core) == 0, "the kept ingredient was left behind in its injector");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void aCompletedCraftStripsTheIdentityDeStampedOnTheResult(GameTestHelper helper) {
        TileFusionCraftingCore core = startKeptIngredientCraft(helper);

        ItemStack result = new ItemStack(DEContent.PICKAXE_WYVERN.get());
        result.set(ItemData.PROVIDER_IDENTITY.get(), UUID.randomUUID());
        core.setOutputStack(result);
        helper.assertTrue(
                core.getOutputStack().has(ItemData.PROVIDER_IDENTITY.get()),
                "the probe result carries no identity, so this test proves nothing");

        core.completeCraft();

        helper.assertTrue(
                !core.getOutputStack().has(ItemData.PROVIDER_IDENTITY.get()),
                "the core handed back an item still carrying the identity DE stamped on it, so its key would not"
                        + " match the pattern output and the crafting job would wait forever");
        helper.assertTrue(
                core.getOutputStack().is(DEContent.PICKAXE_WYVERN.get()),
                "normalizing the result replaced the item instead of just clearing its identity");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void aHandCraftKeepsTheIdentityDeStampedOnTheResult(GameTestHelper helper) {
        FusionCraftingMachine.clearCache();
        FusionRecipeResolver.clearCache();
        TileFusionCraftingCore core = placeCore(helper);

        ItemStack result = new ItemStack(DEContent.PICKAXE_WYVERN.get());
        UUID identity = UUID.randomUUID();
        result.set(ItemData.PROVIDER_IDENTITY.get(), identity);
        core.setOutputStack(result);

        core.completeCraft();

        helper.assertTrue(
                identity.equals(core.getOutputStack().get(ItemData.PROVIDER_IDENTITY.get())),
                "a craft nep never started had its result rewritten; nep must not touch items from a hand craft");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void anAbortedCraftHandsTheKeptIngredientBack(GameTestHelper helper) {
        TileFusionCraftingCore core = startKeptIngredientCraft(helper);
        core.cancelCraft();

        helper.assertTrue(
                countReturned(helper) == 1,
                "a cancelled craft swallowed the supplied ingredient, stalling the job that asked for it");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void anAbortedCraftHandsBackEveryLoadedIngredient(GameTestHelper helper) {
        TileFusionCraftingCore core = startKeptIngredientCraft(helper);
        core.cancelCraft();

        helper.assertTrue(
                countReturned(helper, Items.COPPER_INGOT) == 1,
                "a cancelled craft left the consumed ingredient in an injector; nothing consumes it now, and it blocks"
                        + " every later push");
        helper.assertTrue(
                countReturned(helper, Items.IRON_INGOT) == 1,
                "a cancelled craft left the catalyst in the core, where it blocks every later push");
        helper.assertTrue(countReturned(helper) == 1, "a cancelled craft swallowed the kept ingredient");
        helper.assertTrue(core.getCatalystStack().isEmpty(), "the catalyst was handed back and left in the core");
        for (IFusionInjector injector : core.getInjectors()) {
            helper.assertTrue(
                    injector.getInjectorStack().isEmpty(),
                    "an ingredient was handed back to the network and left in its injector as well");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void aStrandedIngredientIsHandedBackOnTheNextPush(GameTestHelper helper) {
        FusionCraftingMachine.clearCache();
        FusionRecipeResolver.clearCache();

        TileFusionCraftingCore core = placeCore(helper);
        placeInjectors(helper, DEContent.BASIC_CRAFTING_INJECTOR.get(), 2);
        helper.setBlock(CORE.above(), Blocks.BARREL.defaultBlockState());
        ICraftingMachine machine = machine(helper);

        IFusionInjector stranded =
                (IFusionInjector) helper.getBlockEntity(SITES.get(0).pos());
        stranded.setInjectorStack(new ItemStack(Items.COPPER_INGOT));

        RecipeHolder<IFusionRecipe> holder = FusionRecipeResolver.resolveById(
                helper.getLevel(), ResourceLocation.fromNamespaceAndPath("test", "kept_ingredient"));
        helper.assertTrue(holder != null, "the kept-ingredient test recipe did not load");

        IPatternDetails details = patternFor(helper, holder);
        helper.assertTrue(
                machine.pushPattern(details, inputsOf(details), Direction.UP),
                "a push was refused over an ingredient an earlier craft had left in an injector, which no later push"
                        + " can ever clear on its own");
        helper.assertTrue(
                countReturned(helper, Items.COPPER_INGOT) == 1,
                "the stranded ingredient was not handed back to the pattern provider");
        helper.assertTrue(core.isCrafting(), "the core did not start the craft after clearing the stranded injector");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void aCoreLeftReadyToCraftByAnEarlierJobIsClearedAndRestarted(GameTestHelper helper) {
        FusionCraftingMachine.clearCache();
        FusionRecipeResolver.clearCache();

        TileFusionCraftingCore core = placeCore(helper);
        placeInjectors(helper, DEContent.BASIC_CRAFTING_INJECTOR.get(), 2);
        helper.setBlock(CORE.above(), Blocks.BARREL.defaultBlockState());
        ICraftingMachine machine = machine(helper);

        core.setCatalystStack(new ItemStack(Items.IRON_INGOT));
        ((IFusionInjector) helper.getBlockEntity(SITES.get(0).pos()))
                .setInjectorStack(new ItemStack(Items.COPPER_INGOT));
        ((IFusionInjector) helper.getBlockEntity(SITES.get(1).pos())).setInjectorStack(new ItemStack(Items.DIAMOND));

        RecipeHolder<IFusionRecipe> holder = FusionRecipeResolver.resolveById(
                helper.getLevel(), ResourceLocation.fromNamespaceAndPath("test", "kept_ingredient"));
        helper.assertTrue(holder != null, "the kept-ingredient test recipe did not load");

        IPatternDetails details = patternFor(helper, holder);
        helper.assertTrue(
                machine.pushPattern(details, inputsOf(details), Direction.UP),
                "a core sitting loaded and ready to craft, which is where a cancelled craft leaves it, refused every"
                        + " push instead of clearing itself");
        helper.assertTrue(
                countReturned(helper, Items.IRON_INGOT) == 1,
                "the catalyst an earlier craft left behind was not handed back to the pattern provider");
        helper.assertTrue(
                countReturned(helper, Items.COPPER_INGOT) == 1,
                "the ingredient an earlier craft left behind was not handed back to the pattern provider");
        helper.assertTrue(core.isCrafting(), "the core did not start the craft after clearing itself");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void aStrandedIngredientWithNowhereToGoLeavesTheCoreUntouched(GameTestHelper helper) {
        FusionCraftingMachine.clearCache();
        FusionRecipeResolver.clearCache();

        TileFusionCraftingCore core = placeCore(helper);
        placeInjectors(helper, DEContent.BASIC_CRAFTING_INJECTOR.get(), 2);
        ICraftingMachine machine = machine(helper);

        IFusionInjector stranded =
                (IFusionInjector) helper.getBlockEntity(SITES.get(0).pos());
        stranded.setInjectorStack(new ItemStack(Items.COPPER_INGOT));

        RecipeHolder<IFusionRecipe> holder = FusionRecipeResolver.resolveById(
                helper.getLevel(), ResourceLocation.fromNamespaceAndPath("test", "kept_ingredient"));
        helper.assertTrue(holder != null, "the kept-ingredient test recipe did not load");

        IPatternDetails details = patternFor(helper, holder);
        helper.assertTrue(
                !machine.pushPattern(details, inputsOf(details), Direction.UP),
                "a push was accepted with no room to clear the injector it had to empty first");
        helper.assertTrue(
                stranded.getInjectorStack().is(Items.COPPER_INGOT),
                "a refused push voided the ingredient it could not hand back");
        helper.assertTrue(core.getCatalystStack().isEmpty(), "a refused push left the catalyst behind");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void aCraftDrivenByTheCoreTickHandsTheKeptIngredientBack(GameTestHelper helper) {
        TileFusionCraftingCore core = startKeptIngredientCraft(helper);

        core.setFusionState(IFusionStateMachine.FusionState.CRAFTING);
        core.setCounter(Integer.MAX_VALUE - 1);
        core.tick();

        helper.assertTrue(!core.isCrafting(), "the core never finished the craft");
        helper.assertTrue(
                countReturned(helper) == 1,
                "a craft that ran through the core's own tick swallowed the kept ingredient (returned "
                        + countReturned(helper) + ")");
        helper.assertTrue(countInInjectors(core) == 0, "the kept ingredient was left behind in its injector");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void theKeptIngredientIsOnlyHandedBackOnce(GameTestHelper helper) {
        TileFusionCraftingCore core = startKeptIngredientCraft(helper);
        core.completeCraft();
        core.completeCraft();
        core.cancelCraft();

        helper.assertTrue(
                countReturned(helper) == 1,
                "the reclaim fired more than once and duplicated the kept ingredient (returned " + countReturned(helper)
                        + ")");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void aHandLoadedKeptIngredientIsLeftAlone(GameTestHelper helper) {
        FusionCraftingMachine.clearCache();
        FusionRecipeResolver.clearCache();

        TileFusionCraftingCore core = placeCore(helper);
        placeInjectors(helper, DEContent.BASIC_CRAFTING_INJECTOR.get(), 2);
        helper.setBlock(CORE.above(), Blocks.BARREL.defaultBlockState());
        ICraftingMachine machine = machine(helper);

        RecipeHolder<IFusionRecipe> holder = FusionRecipeResolver.resolveById(
                helper.getLevel(), ResourceLocation.fromNamespaceAndPath("test", "kept_ingredient"));
        helper.assertTrue(holder != null, "the kept-ingredient test recipe did not load");

        IFusionInjector preloaded =
                (IFusionInjector) helper.getBlockEntity(SITES.get(1).pos());
        preloaded.setInjectorStack(new ItemStack(Items.DIAMOND));

        IPatternDetails details = patternFor(helper, holder, false);
        helper.assertTrue(
                machine.pushPattern(details, inputsOf(details), Direction.UP),
                "a pattern that omits its kept ingredient was rejected despite a hand-loaded copy");
        core.completeCraft();

        helper.assertTrue(
                countInInjectors(core) == 1,
                "a hand-loaded kept ingredient was taken; the pattern never asked the network for it");
        helper.assertTrue(
                countReturned(helper) == 0, "a kept ingredient the network never supplied was handed back to it");
        helper.succeed();
    }

    private static TileFusionCraftingCore startKeptIngredientCraft(GameTestHelper helper) {
        FusionCraftingMachine.clearCache();
        FusionRecipeResolver.clearCache();

        TileFusionCraftingCore core = placeCore(helper);
        placeInjectors(helper, DEContent.BASIC_CRAFTING_INJECTOR.get(), 2);
        helper.setBlock(CORE.above(), Blocks.BARREL.defaultBlockState());
        ICraftingMachine machine = machine(helper);

        RecipeHolder<IFusionRecipe> holder = FusionRecipeResolver.resolveById(
                helper.getLevel(), ResourceLocation.fromNamespaceAndPath("test", "kept_ingredient"));
        helper.assertTrue(holder != null, "the kept-ingredient test recipe did not load");

        IPatternDetails details = patternFor(helper, holder);
        helper.assertTrue(
                machine.pushPattern(details, inputsOf(details), Direction.UP),
                "a pattern supplying its own kept ingredient was rejected by empty injectors");
        helper.assertTrue(core.isCrafting(), "the core did not start the kept-ingredient craft");
        return core;
    }

    private static int countReturned(GameTestHelper helper) {
        return countReturned(helper, Items.DIAMOND);
    }

    private static int countReturned(GameTestHelper helper, Item item) {
        IItemHandler provider = helper.getLevel()
                .getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(CORE.above()), Direction.DOWN);
        helper.assertTrue(provider != null, "the return inventory exposed no item handler");
        int returned = 0;
        for (int slot = 0; slot < provider.getSlots(); slot++) {
            ItemStack held = provider.getStackInSlot(slot);
            if (held.is(item)) {
                returned += held.getCount();
            }
        }
        return returned;
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void aRecipeThatConsumesAndKeepsTheSameItemHandsBackOnlyTheKeptCopy(GameTestHelper helper) {
        FusionCraftingMachine.clearCache();
        FusionRecipeResolver.clearCache();

        TileFusionCraftingCore core = placeCore(helper);
        placeInjectors(helper, DEContent.BASIC_CRAFTING_INJECTOR.get(), 1);
        helper.setBlock(CORE.above(), Blocks.BARREL.defaultBlockState());
        ICraftingMachine machine = machine(helper);

        RecipeHolder<IFusionRecipe> holder = FusionRecipeResolver.resolveById(
                helper.getLevel(), ResourceLocation.fromNamespaceAndPath("test", "kept_ingredient_matrix"));
        helper.assertTrue(holder != null, "the consumed-and-kept test recipe did not load");
        helper.assertTrue(
                holder.value().getCatalyst().test(new ItemStack(Items.IRON_INGOT)),
                "the test recipe no longer consumes an iron ingot as its catalyst, so it does not cover this case");

        IPatternDetails details = patternFor(helper, holder);
        helper.assertTrue(
                machine.pushPattern(details, inputsOf(details), Direction.UP),
                "a recipe that both consumes and keeps the same item was refused");
        core.completeCraft();

        int returned = countReturned(helper, Items.IRON_INGOT);
        helper.assertTrue(
                returned == 1,
                "the recipe consumes one iron ingot as its catalyst and keeps another, so exactly one comes back;"
                        + " the core handed back " + returned);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void breakingTheCoreMidCraftHandsTheKeptIngredientBack(GameTestHelper helper) {
        TileFusionCraftingCore core = startKeptIngredientCraft(helper);
        helper.assertTrue(countInInjectors(core) == 1, "the kept ingredient never reached an injector");

        BlockPos broken = helper.absolutePos(CORE);
        NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(
                helper.getLevel(),
                broken,
                helper.getLevel().getBlockState(broken),
                helper.makeMockPlayer(GameType.SURVIVAL)));
        helper.setBlock(CORE, Blocks.AIR);

        helper.assertTrue(
                countReturned(helper) == 1,
                "breaking the core mid-craft stranded the kept ingredient, so the job that lent it waits forever");
        helper.assertTrue(
                countInInjectors(core) == 0,
                "the kept ingredient was handed back to the network and left in its injector as well");
        helper.succeed();
    }

    private static int countInInjectors(TileFusionCraftingCore core) {
        int held = 0;
        for (IFusionInjector injector : core.getInjectors()) {
            if (injector.getInjectorStack().is(Items.DIAMOND)) {
                held += injector.getInjectorStack().getCount();
            }
        }
        return held;
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void aPatternNoFusionRecipeCanSatisfyIsRefused(GameTestHelper helper) {
        FusionCraftingMachine.clearCache();
        FusionRecipeResolver.clearCache();

        placeCore(helper);
        placeInjectors(helper, DEContent.WYVERN_CRAFTING_INJECTOR.get(), SITES.size());
        ICraftingMachine machine = machine(helper);

        ItemStack encoded = PatternDetailsHelper.encodeProcessingPattern(
                List.of(new GenericStack(AEItemKey.of(Items.DIAMOND), 1)),
                List.of(new GenericStack(AEItemKey.of(Items.NETHERITE_BLOCK), 1)));
        IPatternDetails details = PatternDetailsHelper.decodePattern(encoded, helper.getLevel());
        helper.assertTrue(details != null, "the test pattern did not decode");

        helper.assertTrue(
                !machine.pushPattern(details, inputsOf(details), Direction.UP), "an unmatchable pattern was accepted");
        helper.succeed();
    }
}
