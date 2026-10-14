package dev.rylex.nep.compat.draconic;

import static dev.rylex.nep.GameTestFixtures.countIn;
import static dev.rylex.nep.GameTestFixtures.firstStack;
import static dev.rylex.nep.GameTestFixtures.inputsOf;
import static dev.rylex.nep.compat.draconic.DraconicFixtures.CHARGE_SLACK_TICKS;
import static dev.rylex.nep.compat.draconic.DraconicFixtures.MATRIX;
import static dev.rylex.nep.compat.draconic.DraconicFixtures.allFusionRecipes;
import static dev.rylex.nep.compat.draconic.DraconicFixtures.anyFusionRecipe;
import static dev.rylex.nep.compat.draconic.DraconicFixtures.assertCompletesWithin;
import static dev.rylex.nep.compat.draconic.DraconicFixtures.cheapestFusionRecipe;
import static dev.rylex.nep.compat.draconic.DraconicFixtures.keepCharged;
import static dev.rylex.nep.compat.draconic.DraconicFixtures.patternFor;
import static dev.rylex.nep.compat.draconic.DraconicFixtures.placeMatrix;
import static dev.rylex.nep.compat.draconic.DraconicFixtures.powerUp;

import appeng.api.AECapabilities;
import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import com.brandon3055.draconicevolution.api.crafting.IFusionRecipe;
import com.brandon3055.draconicevolution.init.ItemData;
import dev.rylex.nep.ConfigOverrides;
import dev.rylex.nep.Nep;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.machine.CraftedOutputs;
import dev.rylex.nep.pattern.FusionCraftingPattern;
import dev.rylex.nep.pattern.encoding.EncodedIngredients;
import dev.rylex.nep.pattern.encoding.PatternConverters;
import dev.rylex.nep.pattern.encoding.PatternOrigin;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
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
    private static final String ME_CHARGE_FIELD = "DRACONIC_FUSION_MATRIX_ME_CHARGE";

    private FusionMatrixGameTest() {}

    private static KeyCounter[] inputsWithEnchantedCatalyst(
            GameTestHelper helper, IPatternDetails details, RecipeHolder<IFusionRecipe> holder) {
        KeyCounter[] inputs = new KeyCounter[details.getInputs().length];
        boolean swapped = false;
        for (int slot = 0; slot < inputs.length; slot++) {
            inputs[slot] = new KeyCounter();
            IPatternDetails.IInput input = details.getInputs()[slot];
            AEKey key = input.getPossibleInputs()[0].what();
            if (!swapped
                    && key instanceof AEItemKey item
                    && holder.value().getCatalyst().test(item.toStack())) {
                key = FusionModifiedIngredientGameTest.enchant(helper.getLevel(), item);
                swapped = true;
            }
            inputs[slot].add(key, input.getMultiplier());
        }
        helper.assertTrue(swapped, "the pattern has no slot the recipe would take as a catalyst");
        return inputs;
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 400)
    public static void anEnchantedCatalystIsUpgradedInPlace(GameTestHelper helper) {
        RecipeHolder<IFusionRecipe> holder = cheapestFusionRecipe(
                helper,
                candidate -> DraconicRecipeIngredients.carriesIngredientData(
                        FusionResults.expectedResult(candidate.value(), helper.getLevel())));
        helper.assertTrue(holder != null, "no fusion recipe carries catalyst data onto its result");
        assertCompletesWithin(helper, holder, 300);

        FusionMatrixBlockEntity matrix = placeMatrix(helper);
        powerUp(helper);
        IPatternDetails details = patternFor(helper, holder);
        AEItemKey declared = (AEItemKey) details.getOutputs().get(0).what();

        helper.assertTrue(
                matrix.pushMatrixPattern(details, inputsWithEnchantedCatalyst(helper, details, holder), Direction.UP),
                "the matrix refused a pattern carrying an enchanted catalyst");
        keepCharged(helper, matrix, 320);

        helper.runAfterDelay(300, () -> {
            ItemStack produced = firstStack(matrix.getOutputBuffer());
            helper.assertTrue(!produced.isEmpty(), "the matrix never finished the enchanted catalyst's upgrade");
            helper.assertTrue(produced.isEnchanted(), "the matrix stripped the catalyst's enchantment: " + produced);
            helper.assertTrue(
                    declared.equals(CraftedOutputs.declaredFor(AEItemKey.of(produced))),
                    "a job waiting for " + declared + " would never settle on " + produced);
            helper.succeed();
        });
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

        FusionMatrixBlockEntity matrix = placeMatrix(helper);
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

    private static List<RecipeHolder<IFusionRecipe>> gearUpgrades(GameTestHelper helper) {
        List<RecipeHolder<IFusionRecipe>> found = new ArrayList<>();
        for (RecipeHolder<IFusionRecipe> holder : allFusionRecipes(helper)) {
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

    private static ItemStack rawAssemble(GameTestHelper helper, RecipeHolder<IFusionRecipe> holder) {
        return holder.value()
                .assemble(
                        new FusionResults.CatalystOnly(
                                holder.value().getCatalyst().getItems()[0].copy()),
                        helper.getLevel().registryAccess());
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void withoutNormalizationTheSameUpgradeYieldsTwoDifferentItems(GameTestHelper helper) {
        RecipeHolder<IFusionRecipe> holder = null;
        for (RecipeHolder<IFusionRecipe> candidate : gearUpgrades(helper)) {
            ItemStack raw = rawAssemble(helper, candidate);
            if (raw.has(ItemData.PROVIDER_IDENTITY.get())) {
                holder = candidate;
                break;
            }
        }
        helper.assertTrue(
                holder != null,
                "no gear upgrade stamps a provider identity any more, so normalization is doing nothing and these"
                        + " tests no longer prove the fix works");

        ItemStack first = rawAssemble(helper, holder);
        ItemStack second = rawAssemble(helper, holder);
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

        FusionMatrixBlockEntity matrix = placeMatrix(helper);
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

        FusionMatrixBlockEntity matrix = placeMatrix(helper);
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
    public static void aSecondRecipeForTheSameOutputIsRefused(GameTestHelper helper) {
        FusionMatrixBlockEntity matrix = placeMatrix(helper);

        RecipeHolder<IFusionRecipe> first = FusionRecipeResolver.resolveById(
                helper.getLevel(), ResourceLocation.fromNamespaceAndPath("test", "fusion_mixed_output_a"));
        RecipeHolder<IFusionRecipe> second = FusionRecipeResolver.resolveById(
                helper.getLevel(), ResourceLocation.fromNamespaceAndPath("test", "fusion_mixed_output_b"));
        helper.assertTrue(first != null && second != null, "the shared-output fusion test recipes did not load");

        IPatternDetails queued = patternFor(helper, first);
        helper.assertTrue(
                matrix.pushMatrixPattern(queued, inputsOf(queued), Direction.UP),
                "the first shared-output pattern was rejected");

        IPatternDetails rival = patternFor(helper, second);
        helper.assertTrue(
                !matrix.pushMatrixPattern(rival, inputsOf(rival), Direction.UP),
                "the matrix accepted a second recipe for an output it already owes from another recipe; the rival"
                        + " overwrote the stored template, so both crafts would run under one recipe and consume the"
                        + " wrong ingredients");
        helper.assertTrue(
                matrix.refusal() == FusionMatrixBlockEntity.Refusal.MIXED_RECIPES,
                "the matrix refused the rival pattern as " + matrix.refusal() + " instead of MIXED_RECIPES");

        helper.assertTrue(
                matrix.pushMatrixPattern(queued, inputsOf(queued), Direction.UP),
                "refusing the rival recipe also blocked the recipe already queued");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void exposesItsMachineCapabilities(GameTestHelper helper) {
        placeMatrix(helper);
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
        FusionMatrixBlockEntity matrix = placeMatrix(helper);
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
        FusionMatrixBlockEntity matrix = placeMatrix(helper);
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
        FusionMatrixBlockEntity matrix = placeMatrix(helper);
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
        FusionMatrixBlockEntity matrix = placeMatrix(helper);
        ItemStack encoded = PatternDetailsHelper.encodeProcessingPattern(
                List.of(new GenericStack(AEItemKey.of(Items.DIAMOND), 1)),
                List.of(new GenericStack(AEItemKey.of(Items.NETHERITE_BLOCK), 1)));
        IPatternDetails details = PatternDetailsHelper.decodePattern(encoded, helper.getLevel());
        helper.assertTrue(details != null, "the test pattern did not decode");

        helper.assertTrue(
                !matrix.pushMatrixPattern(details, inputsOf(details), Direction.UP),
                "the matrix accepted a pattern that is not a fusion pattern");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 400)
    public static void aChargedMatrixOnANetworkCompletesTheCraft(GameTestHelper helper) {
        FusionMatrixBlockEntity matrix = placeMatrix(helper);
        powerUp(helper);

        RecipeHolder<IFusionRecipe> holder = anyFusionRecipe(helper);
        assertCompletesWithin(helper, holder, 300);
        IPatternDetails details = patternFor(helper, holder);
        ItemStack wanted = holder.value().getResultItem(helper.getLevel().registryAccess());

        helper.assertTrue(
                matrix.pushMatrixPattern(details, inputsOf(details), Direction.UP), "a fusion pattern was rejected");
        keepCharged(helper, matrix, 320);

        helper.runAfterDelay(300, () -> {
            ItemStack produced = firstStack(matrix.getOutputBuffer());
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
    public static void aMatrixWithNoFeSourceChargesItselfFromTheNetwork(GameTestHelper helper) {
        FusionMatrixBlockEntity matrix = placeMatrix(helper);
        powerUp(helper);

        RecipeHolder<IFusionRecipe> holder = anyFusionRecipe(helper);
        assertCompletesWithin(helper, holder, 300);
        IPatternDetails details = patternFor(helper, holder);
        ItemStack wanted = holder.value().getResultItem(helper.getLevel().registryAccess());

        helper.assertTrue(
                matrix.pushMatrixPattern(details, inputsOf(details), Direction.UP), "a fusion pattern was rejected");

        helper.runAfterDelay(300, () -> {
            ItemStack produced = firstStack(matrix.getOutputBuffer());
            helper.assertTrue(
                    !produced.isEmpty(),
                    "a matrix with nothing feeding it FE never charged off its own network (charged "
                            + matrix.chargedEnergy() + " of " + matrix.chargeCost() + " FE)");
            helper.assertTrue(
                    ItemStack.isSameItem(produced, wanted),
                    "the matrix produced " + produced + " instead of " + wanted);
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = "nep_fusion_me_charge_off", timeoutTicks = 200)
    public static void aMatrixWithNetworkChargingOffBanksNothing(GameTestHelper helper) {
        ConfigOverrides.Restore restore = ConfigOverrides.override(ME_CHARGE_FIELD, false);
        FusionMatrixBlockEntity matrix = placeMatrix(helper);
        powerUp(helper);

        IPatternDetails details = patternFor(helper, anyFusionRecipe(helper));
        helper.assertTrue(
                matrix.pushMatrixPattern(details, inputsOf(details), Direction.UP), "a fusion pattern was rejected");

        helper.runAfterDelay(100, () -> {
            long charged = matrix.chargedEnergy();
            long stored = matrix.storedEnergy();
            restore.undo();
            helper.assertTrue(
                    charged == 0 && stored == 0,
                    "the matrix took " + stored + " FE into its buffer and banked " + charged
                            + " of a craft with network charging turned off");
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

        FusionMatrixBlockEntity matrix = placeMatrix(helper);
        powerUp(helper);
        IPatternDetails details = patternFor(helper, holder);
        ItemStack wanted = FusionResults.expectedResult(holder.value(), helper.getLevel());

        helper.assertTrue(
                matrix.pushMatrixPattern(details, inputsOf(details), Direction.UP),
                "the matrix refused a gear upgrade pattern");
        keepCharged(helper, matrix, 320);

        helper.runAfterDelay(300, () -> {
            ItemStack produced = firstStack(matrix.getOutputBuffer());
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
    public static void everyFusionRecipeIsFoundBackFromTheItemTheMatrixOwes(GameTestHelper helper) {
        List<RecipeHolder<IFusionRecipe>> loaded = allFusionRecipes(helper);
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

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 1200)
    public static void aRecipeCostingFarMoreThanTheBufferStillCompletesOnASteadySupply(GameTestHelper helper) {
        FusionMatrixBlockEntity matrix = placeMatrix(helper);
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
}
