package dev.rylex.nep.compat.draconic;

import appeng.api.AECapabilities;
import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.core.definitions.AEBlocks;
import com.brandon3055.draconicevolution.api.DraconicAPI;
import com.brandon3055.draconicevolution.api.crafting.IFusionRecipe;
import com.brandon3055.draconicevolution.init.DEContent;
import com.brandon3055.draconicevolution.init.ItemData;
import dev.rylex.nep.Nep;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.machine.RedstoneMode;
import dev.rylex.nep.pattern.FusionCraftingPattern;
import dev.rylex.nep.pattern.encoding.EncodedIngredients;
import dev.rylex.nep.pattern.encoding.PatternConverters;
import dev.rylex.nep.pattern.encoding.PatternOrigin;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

@GameTestHolder(Nep.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FusionMatrixGameTest {

    private static final String TEMPLATE = "empty_5x5x5";
    private static final String BATCH = "nep_fusion_matrix";

    private static final BlockPos MATRIX = new BlockPos(2, 1, 2);
    private static final BlockPos ENERGY_CELL = new BlockPos(2, 1, 3);

    private static final int CHARGE_SLACK_TICKS = 20;

    private FusionMatrixGameTest() {}

    private static FusionMatrixBlockEntity place(GameTestHelper helper) {
        helper.setBlock(MATRIX, NepDraconicContent.MATRIX.get().defaultBlockState());
        BlockEntity be = helper.getBlockEntity(MATRIX);
        helper.assertTrue(be instanceof FusionMatrixBlockEntity, "the matrix did not create its block entity");
        return (FusionMatrixBlockEntity) be;
    }

    private static void powerUp(GameTestHelper helper) {
        helper.setBlock(ENERGY_CELL, AEBlocks.CREATIVE_ENERGY_CELL.block().defaultBlockState());
    }

    private static void keepCharged(GameTestHelper helper, FusionMatrixBlockEntity matrix, int ticks) {
        for (int tick = 0; tick <= ticks; tick++) {
            helper.runAtTickTime(tick, () -> matrix.energyStorage().modify(matrix.energyCapacity()));
        }
    }

    private static RecipeHolder<IFusionRecipe> anyFusionRecipe(GameTestHelper helper) {
        RecipeHolder<IFusionRecipe> holder = cheapestFusionRecipe(helper, candidate -> true);
        if (holder == null) {
            helper.fail("no fusion recipe was loaded");
        }
        return holder;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private static RecipeHolder<IFusionRecipe> cheapestFusionRecipe(
            GameTestHelper helper, Predicate<RecipeHolder<IFusionRecipe>> filter) {
        RecipeType<IFusionRecipe> type = (RecipeType<IFusionRecipe>) DraconicAPI.FUSION_RECIPE_TYPE.get();
        RecipeHolder<IFusionRecipe> cheapest = null;
        for (RecipeHolder<IFusionRecipe> holder :
                helper.getLevel().getRecipeManager().getAllRecipesFor(type)) {
            if (!holder.id().getNamespace().equals("draconicevolution")
                    || !filter.test(holder)
                    || DraconicRecipeIngredients.fusion(holder, helper.getLevel()) == null) {
                continue;
            }
            if (cheapest == null || cheaperThan(holder, cheapest)) {
                cheapest = holder;
            }
        }
        return cheapest;
    }

    private static boolean cheaperThan(RecipeHolder<IFusionRecipe> holder, RecipeHolder<IFusionRecipe> than) {
        long cost = holder.value().getEnergyCost();
        long best = than.value().getEnergyCost();
        return cost != best
                ? cost < best
                : holder.id().toString().compareTo(than.id().toString()) < 0;
    }

    private static void assertCompletesWithin(GameTestHelper helper, RecipeHolder<IFusionRecipe> holder, int ticks) {
        long cost = FusionMatrixUpgrades.chargeCost(holder.value().getEnergyCost(), null, 0);
        long budget = NepConfig.draconicFusionMatrixChargeRate()
                * Math.max(0, ticks - NepConfig.draconicFusionMatrixCraftTicks() - CHARGE_SLACK_TICKS);
        helper.assertTrue(
                cost <= budget,
                "the cheapest fusion recipe available (" + holder.id() + ") costs " + cost
                        + " FE, which a bare Matrix cannot bank inside " + ticks + " ticks at "
                        + NepConfig.draconicFusionMatrixChargeRate()
                        + " FE/t; this test needs a cheaper recipe or upgrade cores, not a longer timeout");
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

    private static int countIn(IItemHandler handler, net.minecraft.world.item.Item item) {
        int total = 0;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack held = handler.getStackInSlot(slot);
            if (held.is(item)) {
                total += held.getCount();
            }
        }
        return total;
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
    public static void theMatrixIsBuiltByARealFusionCraft(GameTestHelper helper) {
        RecipeHolder<IFusionRecipe> holder =
                FusionRecipeResolver.resolveById(helper.getLevel(), Nep.id("fusion_matrix"));
        helper.assertTrue(
                holder != null,
                "nep:fusion_matrix did not load as a fusion recipe, so the Matrix cannot be crafted at all");
        helper.assertTrue(
                ItemStack.isSameItem(
                        holder.value().getResultItem(helper.getLevel().registryAccess()),
                        new ItemStack(NepDraconicContent.MATRIX_ITEM.get())),
                "the nep:fusion_matrix recipe does not produce an Injector Fusion Matrix");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 400)
    public static void aStackingResultConsumesOneRecipeWorthOfIngredients(GameTestHelper helper) {
        RecipeHolder<IFusionRecipe> holder = cheapestFusionRecipe(
                helper,
                candidate -> candidate
                                .value()
                                .getResultItem(helper.getLevel().registryAccess())
                                .getCount()
                        > 1);
        if (holder == null) {
            helper.succeed();
            return;
        }
        assertCompletesWithin(helper, holder, 300);

        FusionMatrixBlockEntity matrix = place(helper);
        powerUp(helper);
        IPatternDetails details = patternFor(helper, holder);
        long perCraft = 0;
        for (IPatternDetails.IInput input : details.getInputs()) {
            perCraft += input.getPossibleInputs()[0].amount() * input.getMultiplier();
        }

        helper.assertTrue(
                matrix.pushMatrixPattern(details, inputsOf(details), Direction.UP), "a fusion pattern was rejected");
        keepCharged(helper, matrix, 320);

        long staged = perCraft;
        helper.runAfterDelay(300, () -> {
            long left = 0;
            for (int slot = 0; slot < matrix.getInputBuffer().getSlots(); slot++) {
                left += matrix.getInputBuffer().getStackInSlot(slot).getCount();
            }
            helper.assertTrue(
                    left == 0,
                    "one pushed pattern is one recipe execution, but " + left + " of " + staged
                            + " staged items were left over; the matrix pulled more than it consumes");
            helper.assertTrue(!matrix.hasPending(), "the matrix still owes a craft it was only asked for once");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void aJeiTransferredKeptIngredientEncodesAsAFusionPattern(GameTestHelper helper) {
        RecipeHolder<IFusionRecipe> holder = FusionRecipeResolver.resolveById(
                helper.getLevel(), ResourceLocation.fromNamespaceAndPath("test", "kept_ingredient"));
        helper.assertTrue(holder != null, "the kept-ingredient test recipe did not load");
        EncodedIngredients expected = DraconicRecipeIngredients.fusion(holder, helper.getLevel());
        helper.assertTrue(expected != null, "the recipe produced no ingredient list");

        List<GenericStack> shown = new ArrayList<>();
        for (List<GenericStack> slot : expected.inputs()) {
            shown.add(slot.get(0));
        }
        ItemStack processing = PatternDetailsHelper.encodeProcessingPattern(
                shown, List.of(expected.outputs().get(0)));

        ItemStack converted = PatternConverters.convert(
                PatternOrigin.ofRecipe(holder.id()), processing, helper.makeMockPlayer(GameType.SURVIVAL));
        helper.assertTrue(
                converted != null && !converted.isEmpty(),
                "a pattern carrying the kept ingredient, as JEI transfers it, would not encode as a fusion pattern");

        IPatternDetails details = PatternDetailsHelper.decodePattern(converted, helper.getLevel());
        helper.assertTrue(details instanceof FusionCraftingPattern, "the converted pattern is not a fusion pattern");
        List<GenericStack> retained = ((FusionCraftingPattern) details).retained();
        helper.assertTrue(
                retained.size() == 1
                        && retained.get(0).what() instanceof AEItemKey key
                        && key.getItem() == Items.DIAMOND,
                "the kept ingredient was not encoded as retained, so the network would treat it as consumed");
        helper.succeed();
    }

    @SuppressWarnings("unchecked")
    private static List<RecipeHolder<IFusionRecipe>> gearUpgrades(GameTestHelper helper) {
        RecipeType<IFusionRecipe> type = (RecipeType<IFusionRecipe>) DraconicAPI.FUSION_RECIPE_TYPE.get();
        List<RecipeHolder<IFusionRecipe>> found = new ArrayList<>();
        for (RecipeHolder<IFusionRecipe> holder :
                helper.getLevel().getRecipeManager().getAllRecipesFor(type)) {
            if (DraconicRecipeIngredients.carriesIngredientData(
                    holder.value().getResultItem(helper.getLevel().registryAccess()))) {
                found.add(holder);
            }
        }
        return found;
    }

    @Nullable
    private static RecipeHolder<IFusionRecipe> aGearUpgrade(GameTestHelper helper) {
        List<RecipeHolder<IFusionRecipe>> found = gearUpgrades(helper);
        return found.isEmpty() ? null : found.get(0);
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void everyGearUpgradeEncodes(GameTestHelper helper) {
        List<RecipeHolder<IFusionRecipe>> upgrades = gearUpgrades(helper);
        helper.assertTrue(
                !upgrades.isEmpty(),
                "no fusion recipe produces modular gear, so this test proves nothing; check IFusionDataTransfer");
        for (RecipeHolder<IFusionRecipe> holder : upgrades) {
            helper.assertTrue(
                    DraconicRecipeIngredients.fusion(holder, helper.getLevel()) != null,
                    holder.id() + " was refused; stripping the provider identity should make every gear upgrade"
                            + " reproducible and so encodable");
        }
        helper.succeed();
    }

    @Nullable
    private static ItemStack asJeiTransfersIt(GameTestHelper helper, RecipeHolder<IFusionRecipe> holder) {
        ItemStack[] catalysts = holder.value().getCatalyst().getItems();
        if (catalysts.length == 0) {
            return null;
        }
        AEItemKey catalyst = AEItemKey.of(catalysts[0]);
        if (catalyst == null) {
            return null;
        }
        List<GenericStack> shown = new ArrayList<>();
        shown.add(new GenericStack(catalyst, 1));
        for (IFusionRecipe.IFusionIngredient ingredient : holder.value().fusionIngredients()) {
            ItemStack[] options = ingredient.get().getItems();
            if (options.length == 0) {
                return null;
            }
            AEItemKey key = AEItemKey.of(options[0]);
            if (key == null) {
                return null;
            }
            shown.add(new GenericStack(key, 1));
        }
        AEItemKey result =
                AEItemKey.of(holder.value().getResultItem(helper.getLevel().registryAccess()));
        return result == null
                ? null
                : PatternDetailsHelper.encodeProcessingPattern(shown, List.of(new GenericStack(result, 1)));
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void everyGearUpgradeEncodesFromTheStacksJeiTransfers(GameTestHelper helper) {
        List<RecipeHolder<IFusionRecipe>> upgrades = gearUpgrades(helper);
        helper.assertTrue(!upgrades.isEmpty(), "no fusion recipe produces modular gear, so this test proves nothing");

        List<String> refused = new ArrayList<>();
        for (RecipeHolder<IFusionRecipe> holder : upgrades) {
            ItemStack processing = asJeiTransfersIt(helper, holder);
            if (processing == null || processing.isEmpty()) {
                continue;
            }
            ItemStack converted = PatternConverters.convert(
                    PatternOrigin.ofRecipe(holder.id()), processing, helper.makeMockPlayer(GameType.SURVIVAL));
            IPatternDetails details = converted == null || converted.isEmpty()
                    ? null
                    : PatternDetailsHelper.decodePattern(converted, helper.getLevel());
            if (!(details instanceof FusionCraftingPattern)) {
                refused.add(holder.id().toString());
            }
        }
        helper.assertTrue(
                refused.isEmpty(),
                "JEI transfers a recipe's plain result, not the assembled one, so these fell back to a plain"
                        + " processing pattern the fusion crafter refuses: " + refused);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void anUpgradesEncodedOutputIsWhatTheMachineActuallyProduces(GameTestHelper helper) {
        List<RecipeHolder<IFusionRecipe>> upgrades = gearUpgrades(helper);
        helper.assertTrue(!upgrades.isEmpty(), "no fusion recipe produces modular gear, so this test proves nothing");
        for (RecipeHolder<IFusionRecipe> holder : upgrades) {
            EncodedIngredients encoded = DraconicRecipeIngredients.fusion(holder, helper.getLevel());
            helper.assertTrue(encoded != null, holder.id() + " did not encode");
            AEItemKey declared = (AEItemKey) encoded.outputs().get(0).what();
            for (int run = 0; run < 2; run++) {
                ItemStack produced = FusionResults.assemble(
                        holder.value(),
                        helper.getLevel(),
                        holder.value().getCatalyst().getItems()[0].copy());
                helper.assertTrue(
                        declared.equals(AEItemKey.of(produced)),
                        holder.id() + " produces an item the pattern does not declare, so the crafting job would"
                                + " wait forever; run " + run);
            }
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void withoutNormalizationTheSameUpgradeYieldsTwoDifferentItems(GameTestHelper helper) {
        RecipeHolder<IFusionRecipe> holder = null;
        for (RecipeHolder<IFusionRecipe> candidate : gearUpgrades(helper)) {
            ItemStack raw = candidate
                    .value()
                    .assemble(
                            new FusionResults.CatalystOnly(candidate
                                    .value()
                                    .getCatalyst()
                                    .getItems()[0]
                                    .copy()),
                            helper.getLevel().registryAccess());
            if (raw.has(ItemData.PROVIDER_IDENTITY.get())) {
                holder = candidate;
                break;
            }
        }
        helper.assertTrue(
                holder != null,
                "no gear upgrade stamps a provider identity any more, so normalization is doing nothing and these"
                        + " tests no longer prove the fix works");

        ItemStack first = holder.value()
                .assemble(
                        new FusionResults.CatalystOnly(
                                holder.value().getCatalyst().getItems()[0].copy()),
                        helper.getLevel().registryAccess());
        ItemStack second = holder.value()
                .assemble(
                        new FusionResults.CatalystOnly(
                                holder.value().getCatalyst().getItems()[0].copy()),
                        helper.getLevel().registryAccess());
        helper.assertTrue(
                !AEItemKey.of(first).equals(AEItemKey.of(second)),
                "two raw assembles matched, so the non-determinism this works around is gone and the extra"
                        + " normalization step is no longer justified");
        helper.assertTrue(
                AEItemKey.of(FusionResults.normalize(first)).equals(AEItemKey.of(FusionResults.normalize(second))),
                "normalization failed to make two assembles of the same recipe agree");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void anUpgradeCarriesTheCatalystsDataOntoTheResult(GameTestHelper helper) {
        RecipeHolder<IFusionRecipe> holder = aGearUpgrade(helper);
        helper.assertTrue(holder != null, "no data-carrying fusion recipe found, so this test proves nothing");

        ItemStack catalyst = holder.value().getCatalyst().getItems()[0].copy();
        catalyst.enchant(
                helper.getLevel()
                        .registryAccess()
                        .registryOrThrow(Registries.ENCHANTMENT)
                        .getHolderOrThrow(Enchantments.UNBREAKING),
                3);
        helper.assertTrue(catalyst.isEnchanted(), "the catalyst refused the enchantment, so this test proves nothing");

        ItemStack assembled = FusionResults.assemble(holder.value(), helper.getLevel(), catalyst);
        helper.assertTrue(
                assembled.isEnchanted(),
                "the matrix's assemble path dropped the catalyst's enchantments; it would hand the player back a"
                        + " stripped tool");
        helper.assertTrue(
                !holder.value()
                        .getResultItem(helper.getLevel().registryAccess())
                        .isEnchanted(),
                "the declared result is already enchanted, so this test cannot tell a transfer from a copy");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void theMatrixTakesAGearUpgradePattern(GameTestHelper helper) {
        RecipeHolder<IFusionRecipe> holder = aGearUpgrade(helper);
        helper.assertTrue(holder != null, "no fusion recipe produces modular gear, so this test proves nothing");

        FusionMatrixBlockEntity matrix = place(helper);
        IPatternDetails details = patternFor(helper, holder);
        helper.assertTrue(
                matrix.pushMatrixPattern(details, inputsOf(details), Direction.UP),
                "the matrix refused a gear upgrade pattern whose result is now reproducible");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void aHandBuiltPatternPromisingTheRawResultIsRefused(GameTestHelper helper) {
        RecipeHolder<IFusionRecipe> holder = null;
        for (RecipeHolder<IFusionRecipe> candidate : gearUpgrades(helper)) {
            if (!ItemStack.isSameItemSameComponents(
                    candidate.value().getResultItem(helper.getLevel().registryAccess()),
                    FusionResults.expectedResult(candidate.value(), helper.getLevel()))) {
                holder = candidate;
                break;
            }
        }
        helper.assertTrue(
                holder != null,
                "every gear upgrade assembles to exactly its declared result, so this test proves nothing");

        FusionMatrixBlockEntity matrix = place(helper);
        List<GenericStack> inputs = new ArrayList<>();
        inputs.add(new GenericStack(AEItemKey.of(holder.value().getCatalyst().getItems()[0]), 1));
        for (IFusionRecipe.IFusionIngredient ingredient : holder.value().fusionIngredients()) {
            inputs.add(new GenericStack(AEItemKey.of(ingredient.get().getItems()[0]), 1));
        }
        ItemStack encoded = PatternDetailsHelper.encodeProcessingPattern(
                inputs,
                List.of(new GenericStack(
                        AEItemKey.of(
                                holder.value().getResultItem(helper.getLevel().registryAccess())),
                        1)));
        IPatternDetails details = PatternDetailsHelper.decodePattern(encoded, helper.getLevel());
        helper.assertTrue(details != null, "the probe pattern did not decode");

        helper.assertTrue(
                !matrix.pushMatrixPattern(details, inputsOf(details), Direction.UP),
                "the matrix accepted a pattern promising the bare declared result; the craft would produce a"
                        + " different item and the job would wait forever");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void aKeptIngredientIsEncodedAsARemainingKey(GameTestHelper helper) {
        RecipeHolder<IFusionRecipe> holder = FusionRecipeResolver.resolveById(
                helper.getLevel(), ResourceLocation.fromNamespaceAndPath("test", "kept_ingredient"));
        helper.assertTrue(holder != null, "the kept-ingredient test recipe did not load");

        IPatternDetails details = patternFor(helper, holder);
        IPatternDetails.IInput kept = null;
        for (IPatternDetails.IInput input : details.getInputs()) {
            if (input.getPossibleInputs()[0].what() instanceof AEItemKey key && key.getItem() == Items.DIAMOND) {
                kept = input;
            }
        }
        helper.assertTrue(kept != null, "the kept ingredient was left out of the pattern, so JEI cannot fill it in");
        helper.assertTrue(
                kept.getRemainingKey(kept.getPossibleInputs()[0].what()) != null,
                "the kept ingredient is not marked as remaining, so a job for N crafts would demand N of it");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 400)
    public static void aSuppliedKeptIngredientIsHandedBackNotConsumed(GameTestHelper helper) {
        RecipeHolder<IFusionRecipe> holder = FusionRecipeResolver.resolveById(
                helper.getLevel(), ResourceLocation.fromNamespaceAndPath("test", "kept_ingredient"));
        helper.assertTrue(holder != null, "the kept-ingredient test recipe did not load");
        assertCompletesWithin(helper, holder, 300);

        FusionMatrixBlockEntity matrix = place(helper);
        powerUp(helper);
        IPatternDetails details = patternFor(helper, holder);

        helper.assertTrue(
                matrix.pushMatrixPattern(details, inputsOf(details), Direction.UP), "a fusion pattern was rejected");
        helper.assertTrue(
                countIn(matrix.getInputBuffer(), Items.DIAMOND) == 1,
                "the pattern did not stage the kept ingredient, so nothing supplied it");
        keepCharged(helper, matrix, 320);

        helper.runAfterDelay(300, () -> {
            helper.assertTrue(
                    countIn(matrix.getOutputBuffer(), Items.GOLD_INGOT) > 0, "the kept-ingredient recipe never ran");
            helper.assertTrue(
                    countIn(matrix.getInputBuffer(), Items.DIAMOND) == 0,
                    "the kept ingredient was left stranded in the input buffer instead of going back to the network");
            helper.assertTrue(
                    countIn(matrix.getOutputBuffer(), Items.DIAMOND) == 1,
                    "the kept ingredient was consumed; the crafting CPU expects it handed back every craft");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 400)
    public static void aPatternWithoutTheKeptIngredientStillRunsFromAHandLoadedCopy(GameTestHelper helper) {
        RecipeHolder<IFusionRecipe> holder = FusionRecipeResolver.resolveById(
                helper.getLevel(), ResourceLocation.fromNamespaceAndPath("test", "kept_ingredient"));
        helper.assertTrue(holder != null, "the kept-ingredient test recipe did not load");
        assertCompletesWithin(helper, holder, 300);

        FusionMatrixBlockEntity matrix = place(helper);
        powerUp(helper);
        IPatternDetails details = patternFor(helper, holder, false);

        helper.assertTrue(
                matrix.pushMatrixPattern(details, inputsOf(details), Direction.UP),
                "a fusion pattern that omits its kept ingredient was rejected");
        ItemStack kept = new ItemStack(Items.DIAMOND);
        helper.assertTrue(
                matrix.manualDemandFor(kept) > 0,
                "the matrix would not let a kept ingredient be loaded by hand, so the recipe can never run");
        matrix.getInputBuffer().insertItem(matrix.getInputBuffer().getSlots() - 1, kept.copy(), false);
        keepCharged(helper, matrix, 320);

        helper.runAfterDelay(300, () -> {
            helper.assertTrue(
                    countIn(matrix.getOutputBuffer(), Items.GOLD_INGOT) > 0, "the kept-ingredient recipe never ran");
            helper.assertTrue(
                    countIn(matrix.getInputBuffer(), Items.DIAMOND) == 1,
                    "a hand-loaded kept ingredient must stay put; the pattern never asked the network for it");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void exposesItsMachineCapabilities(GameTestHelper helper) {
        place(helper);
        BlockPos pos = helper.absolutePos(MATRIX);

        helper.assertTrue(
                helper.getLevel().getCapability(AECapabilities.CRAFTING_MACHINE, pos, null) != null,
                "the matrix exposed no crafting machine capability");
        helper.assertTrue(
                helper.getLevel().getCapability(AECapabilities.IN_WORLD_GRID_NODE_HOST, pos, null) != null,
                "the matrix exposed no in-world grid node host, so it can never join a network");
        helper.assertTrue(
                helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, pos, null) != null,
                "the matrix exposed no energy storage, so nothing can charge it");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void hoppersCannotStageIngredientsForNoJob(GameTestHelper helper) {
        FusionMatrixBlockEntity matrix = place(helper);
        IItemHandler handler =
                helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(MATRIX), null);
        helper.assertTrue(handler != null, "the matrix exposed no item handler");

        for (int slot = 0; slot < matrix.getInputBuffer().getSlots(); slot++) {
            helper.assertTrue(
                    handler.insertItem(slot, new ItemStack(Items.DIAMOND, 4), false)
                                    .getCount()
                            == 4,
                    "a hopper staged ingredients in slot " + slot + " for a matrix that owes nothing");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void anUnnetworkedMatrixNeverConsumesItsInput(GameTestHelper helper) {
        FusionMatrixBlockEntity matrix = place(helper);
        RecipeHolder<IFusionRecipe> holder = anyFusionRecipe(helper);
        IPatternDetails details = patternFor(helper, holder);

        helper.assertTrue(
                matrix.pushMatrixPattern(details, inputsOf(details), Direction.UP),
                "a fusion pattern was rejected by an idle matrix");
        matrix.energyStorage().modify(Long.MAX_VALUE / 4);

        helper.runAfterDelay(40, () -> {
            helper.assertTrue(
                    matrix.activeResult().isEmpty(),
                    "a matrix with no ME network started a craft; it must stay idle until it is on a powered grid");
            helper.assertTrue(
                    matrix.getOutputBuffer().getStackInSlot(0).isEmpty(),
                    "a matrix with no ME network produced an output");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void aPushedPatternStagesItsIngredients(GameTestHelper helper) {
        FusionMatrixBlockEntity matrix = place(helper);
        RecipeHolder<IFusionRecipe> holder = anyFusionRecipe(helper);
        IPatternDetails details = patternFor(helper, holder);

        helper.assertTrue(
                matrix.pushMatrixPattern(details, inputsOf(details), Direction.UP), "a fusion pattern was rejected");

        long staged = 0;
        for (int slot = 0; slot < matrix.getInputBuffer().getSlots(); slot++) {
            staged += matrix.getInputBuffer().getStackInSlot(slot).getCount();
        }
        long expected = 0;
        for (IPatternDetails.IInput input : details.getInputs()) {
            expected += input.getPossibleInputs()[0].amount() * input.getMultiplier();
        }
        helper.assertTrue(staged == expected, "staged " + staged + " items but the pattern carried " + expected);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void aNonFusionPatternIsRefused(GameTestHelper helper) {
        FusionMatrixBlockEntity matrix = place(helper);
        ItemStack encoded = PatternDetailsHelper.encodeProcessingPattern(
                List.of(new GenericStack(appeng.api.stacks.AEItemKey.of(Items.DIAMOND), 1)),
                List.of(new GenericStack(appeng.api.stacks.AEItemKey.of(Items.NETHERITE_BLOCK), 1)));
        IPatternDetails details = PatternDetailsHelper.decodePattern(encoded, helper.getLevel());
        helper.assertTrue(details != null, "the test pattern did not decode");

        helper.assertTrue(
                !matrix.pushMatrixPattern(details, inputsOf(details), Direction.UP),
                "the matrix accepted a pattern that is not a fusion pattern");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 400)
    public static void aChargedMatrixOnANetworkCompletesTheCraft(GameTestHelper helper) {
        FusionMatrixBlockEntity matrix = place(helper);
        powerUp(helper);

        RecipeHolder<IFusionRecipe> holder = anyFusionRecipe(helper);
        assertCompletesWithin(helper, holder, 300);
        IPatternDetails details = patternFor(helper, holder);
        ItemStack wanted = holder.value().getResultItem(helper.getLevel().registryAccess());

        helper.assertTrue(
                matrix.pushMatrixPattern(details, inputsOf(details), Direction.UP), "a fusion pattern was rejected");
        keepCharged(helper, matrix, 320);

        helper.runAfterDelay(300, () -> {
            ItemStack produced = ItemStack.EMPTY;
            for (int slot = 0; slot < matrix.getOutputBuffer().getSlots(); slot++) {
                ItemStack held = matrix.getOutputBuffer().getStackInSlot(slot);
                if (!held.isEmpty()) {
                    produced = held;
                    break;
                }
            }
            helper.assertTrue(
                    !produced.isEmpty(),
                    "a fully charged matrix on a powered network produced nothing (charged " + matrix.chargedEnergy()
                            + " of " + matrix.chargeCost() + " FE)");
            helper.assertTrue(
                    ItemStack.isSameItem(produced, wanted),
                    "the matrix produced " + produced + " instead of " + wanted);
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 400)
    public static void aChargedMatrixCompletesAGearUpgrade(GameTestHelper helper) {
        RecipeHolder<IFusionRecipe> holder = cheapestFusionRecipe(
                helper,
                candidate -> !ItemStack.isSameItemSameComponents(
                        FusionResults.expectedResult(candidate.value(), helper.getLevel()),
                        new ItemStack(FusionResults.expectedResult(candidate.value(), helper.getLevel())
                                .getItem())));
        helper.assertTrue(
                holder != null,
                "no fusion recipe stamps data onto its result any more, so this test no longer covers the upgrades"
                        + " whose result is more than a bare item");
        assertCompletesWithin(helper, holder, 300);

        FusionMatrixBlockEntity matrix = place(helper);
        powerUp(helper);
        IPatternDetails details = patternFor(helper, holder);
        ItemStack wanted = FusionResults.expectedResult(holder.value(), helper.getLevel());

        helper.assertTrue(
                matrix.pushMatrixPattern(details, inputsOf(details), Direction.UP),
                "the matrix refused a gear upgrade pattern");
        keepCharged(helper, matrix, 320);

        helper.runAfterDelay(300, () -> {
            ItemStack produced = ItemStack.EMPTY;
            for (int slot = 0; slot < matrix.getOutputBuffer().getSlots(); slot++) {
                ItemStack held = matrix.getOutputBuffer().getStackInSlot(slot);
                if (!held.isEmpty()) {
                    produced = held;
                    break;
                }
            }
            helper.assertTrue(
                    !produced.isEmpty(),
                    "the matrix staged a gear upgrade pattern for " + holder.id() + " and then never started it"
                            + " (charged " + matrix.chargedEnergy() + " of " + matrix.chargeCost() + " FE)");
            helper.assertTrue(
                    ItemStack.isSameItem(produced, wanted),
                    "the matrix produced " + produced + " instead of " + wanted);
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    @SuppressWarnings("unchecked")
    public static void everyFusionRecipeIsFoundBackFromTheItemTheMatrixOwes(GameTestHelper helper) {
        RecipeType<IFusionRecipe> type = (RecipeType<IFusionRecipe>) DraconicAPI.FUSION_RECIPE_TYPE.get();
        List<RecipeHolder<IFusionRecipe>> loaded =
                helper.getLevel().getRecipeManager().getAllRecipesFor(type);
        StringBuilder unfound = new StringBuilder();
        for (RecipeHolder<IFusionRecipe> holder : loaded) {
            if (DraconicRecipeIngredients.fusion(holder, helper.getLevel()) == null) {
                continue;
            }
            Item owed = FusionResults.expectedResult(holder.value(), helper.getLevel())
                    .getItem();
            if (FusionRecipeResolver.resolveByOutputItem(helper.getLevel(), owed) != null
                    || sharesOutputItem(helper, loaded, owed)) {
                continue;
            }
            unfound.append("\n  ").append(holder.id());
        }
        helper.assertTrue(
                unfound.isEmpty(),
                "the matrix owes an item and cannot find the recipe that makes it, so a job for it would stage its"
                        + " ingredients and then never start:" + unfound);
        helper.succeed();
    }

    private static boolean sharesOutputItem(
            GameTestHelper helper, List<RecipeHolder<IFusionRecipe>> loaded, Item output) {
        int makers = 0;
        for (RecipeHolder<IFusionRecipe> holder : loaded) {
            if (FusionResults.expectedResult(holder.value(), helper.getLevel()).getItem() == output) {
                makers++;
            }
        }
        return makers > 1;
    }

    private static void setCores(FusionMatrixBlockEntity matrix, Item core, int count) {
        IItemHandler upgrades = matrix.getUpgradeSlot();
        upgrades.extractItem(0, Integer.MAX_VALUE, false);
        upgrades.insertItem(0, new ItemStack(core, count), false);
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void theUpgradeSlotTakesTieredCoresOnly(GameTestHelper helper) {
        FusionMatrixBlockEntity matrix = place(helper);
        IItemHandler upgrades = matrix.getUpgradeSlot();

        helper.assertTrue(
                upgrades.insertItem(0, new ItemStack(Items.DIAMOND), false).getCount() == 1,
                "the upgrade slot accepted an item that is not a core");
        helper.assertTrue(
                upgrades.insertItem(0, new ItemStack(DEContent.CORE_DRACONIUM.get()), false)
                                .getCount()
                        == 1,
                "the upgrade slot accepted a Draconium Core, which tunes nothing");

        int max = NepConfig.draconicFusionMatrixMaxCores();
        upgrades.insertItem(0, new ItemStack(DEContent.CORE_WYVERN.get(), 64), false);
        helper.assertTrue(
                upgrades.getStackInSlot(0).getCount() == max,
                "the upgrade slot held " + upgrades.getStackInSlot(0).getCount() + " cores instead of the configured "
                        + max);
        helper.succeed();
    }

    private static void assertCapacity(
            GameTestHelper helper, FusionMatrixBlockEntity matrix, long expected, String tier) {
        helper.assertTrue(
                matrix.energyCapacity() == expected,
                "a full slot of " + tier + " Cores left the buffer at " + matrix.energyCapacity()
                        + " FE instead of the tier's configured " + expected + " FE");
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void eachCoreTierRetunesTheMatrix(GameTestHelper helper) {
        FusionMatrixBlockEntity matrix = place(helper);
        long baseCapacity = matrix.energyCapacity();
        int baseTicks = matrix.craftTicks();
        int max = NepConfig.draconicFusionMatrixMaxCores();

        setCores(matrix, DEContent.CORE_WYVERN.get(), max);
        assertCapacity(helper, matrix, NepConfig.draconicFusionMatrixWyvernCapacity(), "Wyvern");
        helper.assertTrue(
                matrix.craftTicks() == baseTicks, "Wyvern Cores changed the craft time; they only buy buffer");

        setCores(matrix, DEContent.CORE_AWAKENED.get(), max);
        int expected = Math.max(1, (int) Math.round(
                baseTicks * (1.0 - NepConfig.draconicFusionMatrixDraconicCraftTimeReductionPercent() / 100.0)));
        assertCapacity(helper, matrix, NepConfig.draconicFusionMatrixDraconicCapacity(), "Draconic");
        helper.assertTrue(
                matrix.craftTicks() == expected,
                "a full slot of Draconic Cores left the craft at " + matrix.craftTicks() + " ticks instead of "
                        + expected);

        setCores(matrix, DEContent.CORE_CHAOTIC.get(), max);
        assertCapacity(helper, matrix, NepConfig.draconicFusionMatrixChaoticCapacity(), "Chaotic");
        helper.assertTrue(
                matrix.craftTicks() == NepConfig.draconicFusionMatrixChaoticMinimumCraftTicks(),
                "a full slot of Chaotic Cores left the craft at " + matrix.craftTicks() + " ticks instead of the "
                        + "configured floor");

        setCores(matrix, DEContent.CORE_CHAOTIC.get(), 1);
        helper.assertTrue(
                matrix.energyCapacity() > baseCapacity
                        && matrix.energyCapacity() < NepConfig.draconicFusionMatrixChaoticCapacity(),
                "a single core of " + max + " landed the buffer at " + matrix.energyCapacity()
                        + " FE, outside the range it should interpolate across");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void pullingCoresRestoresTheBaseTuning(GameTestHelper helper) {
        FusionMatrixBlockEntity matrix = place(helper);
        long baseCapacity = matrix.energyCapacity();
        int baseTicks = matrix.craftTicks();

        setCores(matrix, DEContent.CORE_CHAOTIC.get(), NepConfig.draconicFusionMatrixMaxCores());
        matrix.getUpgradeSlot().extractItem(0, Integer.MAX_VALUE, false);

        helper.assertTrue(matrix.energyCapacity() == baseCapacity, "the buffer stayed upgraded after the cores left");
        helper.assertTrue(matrix.craftTicks() == baseTicks, "the craft time stayed upgraded after the cores left");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void chaoticCoresDiscountTheCraft(GameTestHelper helper) {
        FusionMatrixBlockEntity matrix = place(helper);
        powerUp(helper);

        RecipeHolder<IFusionRecipe> holder = anyFusionRecipe(helper);
        long undiscounted = FusionMatrixUpgrades.chargeCost(holder.value().getEnergyCost(), null, 0);
        helper.assertTrue(undiscounted > 0, "the test recipe costs nothing, so no discount can be measured");
        setCores(matrix, DEContent.CORE_CHAOTIC.get(), NepConfig.draconicFusionMatrixMaxCores());

        IPatternDetails details = patternFor(helper, holder);
        helper.assertTrue(
                matrix.pushMatrixPattern(details, inputsOf(details), Direction.UP), "a fusion pattern was rejected");

        helper.runAfterDelay(20, () -> {
            helper.assertTrue(!matrix.activeResult().isEmpty(), "the matrix never claimed the craft");
            helper.assertTrue(
                    matrix.chargeCost() < undiscounted,
                    "a full slot of Chaotic Cores charged " + matrix.chargeCost() + " FE, no cheaper than the "
                            + undiscounted + " FE an unupgraded Matrix pays");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 400)
    public static void pullingCoresHoldsTheEnergyLongEnoughToSwapThem(GameTestHelper helper) {
        int grace = NepConfig.draconicFusionMatrixCoreSwapGrace();
        if (grace <= 0) {
            helper.succeed();
            return;
        }
        FusionMatrixBlockEntity matrix = place(helper);
        int max = NepConfig.draconicFusionMatrixMaxCores();
        setCores(matrix, DEContent.CORE_WYVERN.get(), max);
        long filled = matrix.energyCapacity();
        matrix.energyStorage().modify(filled);

        matrix.getUpgradeSlot().extractItem(0, max, false);
        helper.assertTrue(
                matrix.storedEnergy() == filled,
                "pulling the cores voided the buffer on the spot: " + matrix.storedEnergy() + " of " + filled
                        + " FE left");

        helper.runAfterDelay(Math.max(1, grace / 2), () -> {
            helper.assertTrue(matrix.isHoldingSwappedEnergy(), "the matrix stopped holding the energy early");
            setCores(matrix, DEContent.CORE_AWAKENED.get(), max);
            helper.assertTrue(
                    matrix.storedEnergy() == filled,
                    "re-tiering the slot inside the grace window still cost " + (filled - matrix.storedEnergy())
                            + " FE");
            helper.assertTrue(
                    matrix.energyCapacity() >= filled, "the buffer did not grow back to hold the energy it kept");
            helper.assertTrue(!matrix.isHoldingSwappedEnergy(), "the hold outlived the swap it was covering");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 400)
    public static void heldEnergyIsVoidedOnceTheSwapWindowCloses(GameTestHelper helper) {
        int grace = NepConfig.draconicFusionMatrixCoreSwapGrace();
        if (grace <= 0) {
            helper.succeed();
            return;
        }
        FusionMatrixBlockEntity matrix = place(helper);
        setCores(matrix, DEContent.CORE_WYVERN.get(), NepConfig.draconicFusionMatrixMaxCores());
        matrix.energyStorage().modify(matrix.energyCapacity());

        matrix.getUpgradeSlot().extractItem(0, NepConfig.draconicFusionMatrixMaxCores(), false);
        long base = NepConfig.draconicFusionMatrixCapacity();

        helper.runAfterDelay(grace + 5, () -> {
            helper.assertTrue(
                    matrix.energyCapacity() == base,
                    "the buffer stayed at " + matrix.energyCapacity() + " FE after the grace window, not the " + base
                            + " FE an unupgraded Matrix holds");
            helper.assertTrue(
                    matrix.storedEnergy() == base,
                    "the buffer should be trimmed to its new size, but holds " + matrix.storedEnergy() + " FE");
            helper.assertTrue(!matrix.isHoldingSwappedEnergy(), "the matrix is still holding energy it gave up on");
            helper.succeed();
        });
    }

    private static long bankableEachTick(FusionMatrixBlockEntity matrix) {
        return Math.min(NepConfig.draconicFusionMatrixChargeRate(), matrix.energyCapacity());
    }

    @Nullable
    private static RecipeHolder<IFusionRecipe> costliestFusionRecipeWithin(
            GameTestHelper helper, long perTick, long maxTicks) {
        RecipeHolder<IFusionRecipe> costliest = null;
        long best = 0;
        for (RecipeHolder<IFusionRecipe> holder : allFusionRecipes(helper)) {
            if (!holder.id().getNamespace().equals("draconicevolution")
                    || holder.value().getRecipeTier().index > FusionMatrixBlockEntity.maximumTier().index
                    || DraconicRecipeIngredients.fusion(holder, helper.getLevel()) == null) {
                continue;
            }
            long cost = FusionMatrixUpgrades.chargeCost(holder.value().getEnergyCost(), null, 0);
            if (cost <= best || (cost + perTick - 1) / perTick > maxTicks) {
                continue;
            }
            best = cost;
            costliest = holder;
        }
        return costliest;
    }

    @SuppressWarnings("unchecked")
    private static List<RecipeHolder<IFusionRecipe>> allFusionRecipes(GameTestHelper helper) {
        RecipeType<IFusionRecipe> type = (RecipeType<IFusionRecipe>) DraconicAPI.FUSION_RECIPE_TYPE.get();
        return helper.getLevel().getRecipeManager().getAllRecipesFor(type);
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 1200)
    public static void aRecipeCostingFarMoreThanTheBufferStillCompletesOnASteadySupply(GameTestHelper helper) {
        FusionMatrixBlockEntity matrix = place(helper);
        powerUp(helper);

        long perTick = bankableEachTick(matrix);
        RecipeHolder<IFusionRecipe> holder = costliestFusionRecipeWithin(helper, perTick, 900);
        helper.assertTrue(holder != null, "no fusion recipe is cheap enough to bank inside this test's tick budget");

        long cost = FusionMatrixUpgrades.chargeCost(holder.value().getEnergyCost(), null, 0);
        helper.assertTrue(
                cost > matrix.energyCapacity(),
                "the costliest recipe available (" + holder.id() + ") costs " + cost + " FE, which already fits in the "
                        + matrix.energyCapacity()
                        + " FE buffer; this test only proves anything against a recipe the buffer cannot hold");

        IPatternDetails details = patternFor(helper, holder);
        ItemStack wanted = FusionResults.expectedResult(holder.value(), helper.getLevel());
        helper.assertTrue(
                matrix.pushMatrixPattern(details, inputsOf(details), Direction.UP), "a fusion pattern was rejected");

        int ticks = (int) ((cost + perTick - 1) / perTick)
                + NepConfig.draconicFusionMatrixCraftTicks()
                + CHARGE_SLACK_TICKS * 2;
        keepCharged(helper, matrix, ticks + 20);

        helper.runAfterDelay(ticks, () -> {
            helper.assertTrue(
                    countIn(matrix.getOutputBuffer(), wanted.getItem()) > 0,
                    "a recipe costing " + cost + " FE never finished on a buffer of only " + matrix.energyCapacity()
                            + " FE refilled every tick, so the buffer is gating the craft rather than feeding it"
                            + " (banked " + matrix.chargedEnergy() + " of " + matrix.chargeCost() + " FE)");
            helper.succeed();
        });
    }

    private static void fillOutputBuffer(FusionMatrixBlockEntity matrix) {
        IItemHandler output = matrix.getOutputBuffer();
        for (int slot = 0; slot < output.getSlots(); slot++) {
            output.insertItem(slot, new ItemStack(Items.NETHERITE_BLOCK, 64), false);
        }
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 400)
    public static void aFullOutputBufferMakesTheMatrixWaitInsteadOfLosingTheKeptItem(GameTestHelper helper) {
        RecipeHolder<IFusionRecipe> holder = FusionRecipeResolver.resolveById(
                helper.getLevel(), ResourceLocation.fromNamespaceAndPath("test", "kept_ingredient"));
        helper.assertTrue(holder != null, "the kept-ingredient test recipe did not load");
        assertCompletesWithin(helper, holder, 150);

        FusionMatrixBlockEntity matrix = place(helper);
        powerUp(helper);
        IPatternDetails details = patternFor(helper, holder);
        helper.assertTrue(
                matrix.pushMatrixPattern(details, inputsOf(details), Direction.UP), "a fusion pattern was rejected");
        fillOutputBuffer(matrix);
        keepCharged(helper, matrix, 380);

        helper.runAfterDelay(60, () -> {
            helper.assertTrue(
                    matrix.activeResult().isEmpty(),
                    "the matrix claimed a craft it has nowhere to put; finishing it would strand the kept item");
            helper.assertTrue(
                    matrix.stall() == FusionMatrixBlockEntity.Stall.OUTPUT_FULL,
                    "a matrix blocked on a full output buffer reported " + matrix.stall() + " instead of OUTPUT_FULL");
            helper.assertTrue(
                    countIn(matrix.getInputBuffer(), Items.DIAMOND) == 1,
                    "the kept item left the input buffer while the craft was waiting on output room");

            matrix.getOutputBuffer().extractItem(0, 64, false);
            matrix.getOutputBuffer().extractItem(1, 64, false);
            helper.runAfterDelay(80, () -> {
                helper.assertTrue(
                        countIn(matrix.getOutputBuffer(), Items.GOLD_INGOT) > 0,
                        "the craft never resumed once the output buffer had room again");
                helper.assertTrue(
                        countIn(matrix.getOutputBuffer(), Items.DIAMOND) == 1,
                        "the kept item was lost while the craft waited for output room");
                helper.succeed();
            });
        });
    }

    private static int totalIn(IItemHandler handler) {
        int total = 0;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            total += handler.getStackInSlot(slot).getCount();
        }
        return total;
    }

    private static FusionMatrixBlockEntity matrixStagingAKeptItem(GameTestHelper helper, int pushes) {
        RecipeHolder<IFusionRecipe> holder = FusionRecipeResolver.resolveById(
                helper.getLevel(), ResourceLocation.fromNamespaceAndPath("test", "kept_ingredient"));
        helper.assertTrue(holder != null, "the kept-ingredient test recipe did not load");

        FusionMatrixBlockEntity matrix = place(helper);
        IPatternDetails details = patternFor(helper, holder);
        for (int push = 0; push < pushes; push++) {
            helper.assertTrue(
                    matrix.pushMatrixPattern(details, inputsOf(details), Direction.UP),
                    "a fusion pattern was rejected on push " + push);
        }
        helper.assertTrue(
                countIn(matrix.getInputBuffer(), Items.DIAMOND) > 0,
                "no push staged the kept item, so nothing is at risk");
        return matrix;
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void clearingPendingRecipesLeavesAStagedKeptItemAlone(GameTestHelper helper) {
        FusionMatrixBlockEntity matrix = matrixStagingAKeptItem(helper, 1);
        int staged = totalIn(matrix.getInputBuffer());
        int kept = countIn(matrix.getInputBuffer(), Items.DIAMOND);

        matrix.clearPending();

        helper.assertTrue(!matrix.hasPending(), "clearing left the matrix still owing a craft");
        helper.assertTrue(
                totalIn(matrix.getInputBuffer()) == staged,
                "clearing pending recipes changed the input buffer from " + staged + " to "
                        + totalIn(matrix.getInputBuffer()) + " items; it must eat nothing and duplicate nothing");
        helper.assertTrue(
                countIn(matrix.getInputBuffer(), Items.DIAMOND) == kept, "clearing pending recipes ate the kept item");
        helper.assertTrue(matrix.missingInputs().isEmpty(), "clearing left a stale missing-ingredients readout behind");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void emptyingTheBuffersHandsAStagedKeptItemBackWhole(GameTestHelper helper) {
        FusionMatrixBlockEntity matrix = matrixStagingAKeptItem(helper, 1);
        int staged = totalIn(matrix.getInputBuffer()) + totalIn(matrix.getOutputBuffer());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        matrix.clearBufferTo(player);

        helper.assertTrue(
                totalIn(matrix.getInputBuffer()) == 0 && totalIn(matrix.getOutputBuffer()) == 0,
                "emptying the buffers left items behind");
        int handed = 0;
        int keptBack = 0;
        for (ItemStack stack : player.getInventory().items) {
            handed += stack.getCount();
            if (stack.is(Items.DIAMOND)) {
                keptBack += stack.getCount();
            }
        }
        helper.assertTrue(
                handed == staged,
                "emptying the buffers handed the player " + handed + " items out of " + staged
                        + " staged; nothing may be eaten and nothing duplicated");
        helper.assertTrue(keptBack == 1, "the staged kept item was not handed back");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void theKeptItemIsAskedForOnceNoMatterHowManyCraftsAreQueued(GameTestHelper helper) {
        FusionMatrixBlockEntity matrix = matrixStagingAKeptItem(helper, 3);
        IItemHandler input = matrix.getInputBuffer();
        for (int slot = 0; slot < input.getSlots(); slot++) {
            input.extractItem(slot, Integer.MAX_VALUE, false);
        }

        helper.runAfterDelay(40, () -> {
            long asked = 0;
            for (GenericStack missing : matrix.missingInputs()) {
                if (missing.what() instanceof AEItemKey key && key.getItem() == Items.DIAMOND) {
                    asked = missing.amount();
                }
            }
            helper.assertTrue(
                    asked == 1,
                    "the readout asked the network for " + asked
                            + " copies of the kept item to cover 3 queued crafts; it is handed back after every"
                            + " execution, so one copy covers the whole job");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void anItemBothConsumedAndKeptIsNotHighlightedAsRetained(GameTestHelper helper) {
        RecipeHolder<IFusionRecipe> holder = FusionRecipeResolver.resolveById(
                helper.getLevel(), ResourceLocation.fromNamespaceAndPath("test", "kept_ingredient_matrix"));
        helper.assertTrue(holder != null, "the consumed-and-kept test recipe did not load");

        FusionMatrixBlockEntity matrix = place(helper);
        IPatternDetails details = patternFor(helper, holder);
        helper.assertTrue(
                matrix.pushMatrixPattern(details, inputsOf(details), Direction.UP), "a fusion pattern was rejected");

        for (AEItemKey key : matrix.retainedInputKeys()) {
            helper.assertTrue(
                    key.getItem() != Items.IRON_INGOT,
                    "an iron ingot is consumed as the catalyst and kept as an ingredient, so it is not fully"
                            + " retained and must not be outlined as an item the network gets back");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void clearingWithNothingPendingChangesNothing(GameTestHelper helper) {
        FusionMatrixBlockEntity matrix = place(helper);
        helper.assertTrue(!matrix.hasPending(), "a freshly placed matrix already owed a craft");

        matrix.clearPending();
        matrix.clearPending();

        helper.assertTrue(!matrix.hasPending(), "clearing an idle matrix left it owing something");
        helper.assertTrue(matrix.missingInputs().isEmpty(), "clearing an idle matrix invented a missing ingredient");
        helper.assertTrue(
                matrix.stall() == FusionMatrixBlockEntity.Stall.NONE,
                "clearing an idle matrix left it reporting " + matrix.stall());
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void anIdleMatrixOffTheNetworkReportsThePowerFault(GameTestHelper helper) {
        FusionMatrixBlockEntity matrix = place(helper);

        helper.runAfterDelay(40, () -> {
            helper.assertTrue(
                    matrix.hasPowerFault(), "a matrix with no powered ME network reported no fault while idle");
            helper.assertTrue(
                    matrix.stall() == FusionMatrixBlockEntity.Stall.NONE,
                    "an idle matrix with nothing owed invented the job stall " + matrix.stall());
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void aMatrixOnAPoweredNetworkClearsThePowerFault(GameTestHelper helper) {
        FusionMatrixBlockEntity matrix = place(helper);
        powerUp(helper);

        helper.runAfterDelay(40, () -> {
            helper.assertTrue(!matrix.hasPowerFault(), "a matrix on a powered ME network still reported a power fault");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void anIdleMatrixWithAnEmptyBufferReportsTheEnergyFault(GameTestHelper helper) {
        FusionMatrixBlockEntity matrix = place(helper);
        powerUp(helper);

        helper.runAfterDelay(80, () -> {
            helper.assertTrue(
                    matrix.hasEnergyFault(), "a matrix with an empty buffer and no supply reported no energy fault");
            helper.assertTrue(
                    !matrix.hasPowerFault(),
                    "an empty energy buffer was mistaken for a missing ME network, which is a different fix");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void aFedBufferNeverReportsTheEnergyFault(GameTestHelper helper) {
        FusionMatrixBlockEntity matrix = place(helper);
        powerUp(helper);
        keepCharged(helper, matrix, 80);

        helper.runAfterDelay(80, () -> {
            helper.assertTrue(!matrix.hasEnergyFault(), "a matrix with a supplied buffer reported an energy fault");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void aBufferDrainedEveryTickByChargingIsNotAnEnergyFault(GameTestHelper helper) {
        FusionMatrixBlockEntity matrix = place(helper);
        powerUp(helper);
        RecipeHolder<IFusionRecipe> holder = anyFusionRecipe(helper);
        IPatternDetails details = patternFor(helper, holder);
        helper.assertTrue(
                matrix.pushMatrixPattern(details, inputsOf(details), Direction.UP), "a fusion pattern was rejected");
        for (int tick = 0; tick <= 80; tick++) {
            helper.runAtTickTime(tick, () -> matrix.energyStorage()
                    .modify(NepConfig.draconicFusionMatrixChargeRate()
                            - matrix.energyStorage().stored()));
        }

        helper.runAfterDelay(80, () -> {
            helper.assertTrue(
                    !matrix.hasEnergyFault(),
                    "a buffer drained to empty every tick by a healthy supply was read as no energy");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void everyComparatorModeReadsItsOwnSource(GameTestHelper helper) {
        FusionMatrixBlockEntity matrix = place(helper);
        helper.assertTrue(
                matrix.redstoneMode() == RedstoneMode.OUTPUT,
                "a fresh matrix starts on " + matrix.redstoneMode() + " rather than the output buffer");
        helper.assertTrue(matrix.comparatorOutput() == 0, "an empty matrix emitted a signal on output mode");

        matrix.getOutputBuffer().insertItem(0, new ItemStack(Items.NETHERITE_BLOCK, 32), false);
        matrix.getInputBuffer().insertItem(0, new ItemStack(Items.DIAMOND, 64), false);
        helper.assertTrue(
                matrix.comparatorOutput() == RedstoneMode.fullness(matrix.getOutputBuffer()),
                "output mode emitted " + matrix.comparatorOutput() + " instead of the output buffer's fullness of "
                        + RedstoneMode.fullness(matrix.getOutputBuffer()));

        matrix.cycleRedstoneMode();
        helper.assertTrue(
                matrix.redstoneMode() == RedstoneMode.STATUS,
                "one click moved the mode to " + matrix.redstoneMode() + " rather than status");
        helper.assertTrue(
                matrix.comparatorOutput() == 0,
                "status mode emitted " + matrix.comparatorOutput() + " on a matrix that owes nothing and is idle");

        matrix.cycleRedstoneMode();
        helper.assertTrue(
                matrix.redstoneMode() == RedstoneMode.INPUT,
                "two clicks moved the mode to " + matrix.redstoneMode() + " rather than input");
        helper.assertTrue(
                matrix.comparatorOutput() == RedstoneMode.fullness(matrix.getInputBuffer()),
                "input mode emitted " + matrix.comparatorOutput() + " instead of the input buffer's fullness of "
                        + RedstoneMode.fullness(matrix.getInputBuffer()));

        matrix.cycleRedstoneMode();
        helper.assertTrue(
                matrix.redstoneMode() == RedstoneMode.OUTPUT, "the mode did not cycle back round to the output buffer");

        RecipeHolder<IFusionRecipe> holder = FusionRecipeResolver.resolveById(
                helper.getLevel(), ResourceLocation.fromNamespaceAndPath("test", "kept_ingredient"));
        helper.assertTrue(holder != null, "the kept-ingredient test recipe did not load");
        matrix.getInputBuffer().extractItem(0, Integer.MAX_VALUE, false);
        matrix.getOutputBuffer().extractItem(0, Integer.MAX_VALUE, false);
        IPatternDetails details = patternFor(helper, holder);
        helper.assertTrue(
                matrix.pushMatrixPattern(details, inputsOf(details), Direction.UP), "a fusion pattern was rejected");
        matrix.cycleRedstoneMode();
        helper.assertTrue(
                matrix.comparatorOutput() > 0,
                "status mode emitted nothing on a matrix that owes a craft; the block is not idle");
        helper.succeed();
    }
}
