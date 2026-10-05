package dev.rylex.nep.compat.malum;

import appeng.api.config.Actionable;
import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.core.definitions.AEBlocks;
import com.sammy.malum.common.recipe.SpiritFocusingRecipe;
import com.sammy.malum.common.recipe.SpiritInfusionRecipe;
import com.sammy.malum.core.systems.recipe.SpiritIngredient;
import com.sammy.malum.core.systems.spirit.type.SpiritArcanaType;
import com.sammy.malum.registry.common.block.MalumBlocks;
import com.sammy.malum.registry.common.item.MalumItems;
import com.sammy.malum.registry.common.magic.MalumSpiritTypes;
import dev.rylex.nep.Nep;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.pattern.SpiritFocusingPattern;
import dev.rylex.nep.pattern.SpiritInfusionPattern;
import dev.rylex.nep.pattern.encoding.EncodedIngredients;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

@GameTestHolder(Nep.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FocusedSpiritMatrixGameTest {

    private static final String TEMPLATE = "empty_5x5x5";
    private static final String BATCH = "nep_focused_spirit_matrix";

    private static final BlockPos MATRIX = new BlockPos(2, 1, 2);
    private static final BlockPos ME_CONTROLLER = new BlockPos(2, 1, 3);
    private static final BlockPos ENERGY_CELL = ME_CONTROLLER.above();

    private FocusedSpiritMatrixGameTest() {}

    private static FocusedSpiritMatrixBlockEntity placeMatrix(GameTestHelper helper) {
        SpiritInfusionResolver.clearCache();
        helper.setBlock(ME_CONTROLLER, AEBlocks.CONTROLLER.block());
        helper.setBlock(ENERGY_CELL, AEBlocks.CREATIVE_ENERGY_CELL.block().defaultBlockState());
        helper.setBlock(MATRIX, NepMalumContent.MATRIX.get().defaultBlockState());
        FocusedSpiritMatrixBlockEntity matrix =
                helper.getBlockEntity(MATRIX) instanceof FocusedSpiritMatrixBlockEntity be ? be : null;
        helper.assertTrue(matrix != null, "the Focused Spirit Matrix has no block entity");
        return matrix;
    }

    private static RecipeHolder<SpiritInfusionRecipe> recipeWithExtras(GameTestHelper helper, int extras) {
        RecipeHolder<SpiritInfusionRecipe> best = null;
        for (RecipeHolder<SpiritInfusionRecipe> holder : SpiritInfusionResolver.candidates(helper.getLevel())) {
            SpiritInfusionRecipe recipe = holder.value();
            if (!holder.id().getNamespace().equals("malum")
                    || recipe.carryOverComponentData
                    || recipe.extraInputs.size() != extras
                    || MalumRecipeIngredients.spiritInfusion(recipe) == null) {
                continue;
            }
            if (best == null || holder.id().compareTo(best.id()) < 0) {
                best = holder;
            }
        }
        helper.assertTrue(best != null, "no spirit infusion recipe with " + extras + " extra ingredient(s) was loaded");
        return best;
    }

    private static IPatternDetails patternFor(GameTestHelper helper, RecipeHolder<SpiritInfusionRecipe> holder) {
        EncodedIngredients expected = MalumRecipeIngredients.spiritInfusion(holder.value());
        helper.assertTrue(expected != null, "the recipe produced no ingredient list");

        List<GenericStack> inputs = new ArrayList<>();
        for (List<GenericStack> options : expected.inputs()) {
            inputs.add(options.get(0));
        }
        ItemStack encoded = SpiritInfusionPattern.encode(
                holder.id(), inputs, expected.outputs().get(0));
        IPatternDetails details = PatternDetailsHelper.decodePattern(encoded, helper.getLevel());
        helper.assertTrue(details != null, "the spirit infusion pattern did not decode");
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

    private static boolean outputHolds(FocusedSpiritMatrixBlockEntity matrix, ItemStack expected, int count) {
        int found = 0;
        for (int slot = 0; slot < matrix.getOutputBuffer().getSlots(); slot++) {
            ItemStack stack = matrix.getOutputBuffer().getStackInSlot(slot);
            if (ItemStack.isSameItem(stack, expected)) {
                found += stack.getCount();
            }
        }
        return found >= count;
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 400)
    public static void aSpiritInfusionPatternRunsToCompletion(GameTestHelper helper) {
        FocusedSpiritMatrixBlockEntity matrix = placeMatrix(helper);

        RecipeHolder<SpiritInfusionRecipe> holder = recipeWithExtras(helper, 0);
        IPatternDetails details = patternFor(helper, holder);
        helper.assertTrue(
                matrix.pushMatrixPattern(details, inputsOf(details), Direction.UP),
                "the Matrix refused a spirit infusion pattern");

        ItemStack result = holder.value().result;
        helper.succeedWhen(() ->
                helper.assertTrue(outputHolds(matrix, result, 1), "the Matrix never finished the spirit infusion"));
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 400)
    public static void aRecipeWantingPedestalsNeedsNoneInTheMatrix(GameTestHelper helper) {
        FocusedSpiritMatrixBlockEntity matrix = placeMatrix(helper);

        RecipeHolder<SpiritInfusionRecipe> holder = recipeWithExtras(helper, 1);
        IPatternDetails details = patternFor(helper, holder);
        helper.assertTrue(
                matrix.pushMatrixPattern(details, inputsOf(details), Direction.UP),
                "the Matrix refused a recipe that would need a pedestal on a real altar");

        ItemStack result = holder.value().result;
        helper.succeedWhen(() -> helper.assertTrue(
                outputHolds(matrix, result, 1), "the Matrix never finished a recipe with an extra ingredient"));
    }

    private static Map<AEItemKey, Long> spiritsOf(RecipeHolder<SpiritInfusionRecipe> holder) {
        Map<AEItemKey, Long> wanted = new LinkedHashMap<>();
        for (SpiritIngredient spirit : holder.value().spirits) {
            AEItemKey key = AEItemKey.of(spirit.asItemStack());
            wanted.merge(key, (long) spirit.count(), Long::sum);
        }
        return wanted;
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void theSpiritBankSortsSpiritsIntoTheirOwnSlots(GameTestHelper helper) {
        FocusedSpiritMatrixBlockEntity matrix = placeMatrix(helper);
        IItemHandler bank = matrix.getSpiritBank();

        helper.assertTrue(bank.getSlots() == SpiritBank.SLOTS, "the spirit bank is not a 3x3");
        helper.assertTrue(
                SpiritBank.spiritFor(4) == MalumItems.UMBRAL_SPIRIT.get(), "Umbral is not in the middle of the bank");

        for (int slot = 0; slot < SpiritBank.SLOTS; slot++) {
            ItemStack shard = new ItemStack(SpiritBank.spiritFor(slot), 8);
            helper.assertTrue(
                    ItemHandlerHelper.insertItem(bank, shard.copy(), false).isEmpty(),
                    "the bank would not take " + shard.getHoverName().getString());
            helper.assertTrue(
                    bank.getStackInSlot(slot).getCount() == 8,
                    shard.getHoverName().getString() + " did not land in its own slot");
        }
        helper.assertTrue(
                !ItemHandlerHelper.insertItem(bank, new ItemStack(Items.DIAMOND), false)
                        .isEmpty(),
                "the spirit bank accepted something that is not a spirit");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void restockAsksForSpiritsTheBankHasRunOutOf(GameTestHelper helper) {
        FocusedSpiritMatrixBlockEntity matrix = placeMatrix(helper);
        helper.assertTrue(matrix.spiritRestock(), "Restock Spirits was off on a freshly placed Matrix");
        FocusedSpiritMatrixBlockEntity.RestockPlan plan = matrix.restockPlan();
        helper.assertTrue(plan != null, "Restock Spirits over an empty bank asked the network for nothing");

        long stock = NepConfig.malumFocusedSpiritMatrixSpiritStock();
        for (int slot = 0; slot < SpiritBank.SLOTS; slot++) {
            AEItemKey key = AEItemKey.of(SpiritBank.spiritFor(slot));
            int at = plan.keys().indexOf(key);
            if (slot == SpiritBank.JOKER_SLOT) {
                helper.assertTrue(at < 0, "the empty joker slot was restocked with " + key);
                continue;
            }
            helper.assertTrue(at >= 0, "an empty bank slot left " + key + " out of the restock request");
            helper.assertTrue(
                    plan.targets().get(at) == stock,
                    key + " was requested up to " + plan.targets().get(at) + ", not the configured " + stock);
            helper.assertTrue(
                    !plan.needs().containsKey(key),
                    key + " was a restock top-up but would have read as a missing ingredient");
        }

        matrix.toggleSpiritRestock();
        helper.assertTrue(
                matrix.restockPlan() == null, "an idle Matrix with restock off still asked the network for something");
        matrix.toggleSpiritRestock();
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void everySpiritEitherHasASlotOrFallsBackToTheBuffer(GameTestHelper helper) {
        FocusedSpiritMatrixBlockEntity matrix = placeMatrix(helper);
        IItemHandler bank = matrix.getSpiritBank();

        int dedicated = 0;
        int jokers = 0;
        for (SpiritArcanaType type : MalumSpiritTypes.SPIRIT_TYPES_REGISTRY) {
            ItemStack shard = new ItemStack(type.getSpiritShard());
            boolean claimed = matrix.banksSpirit(AEItemKey.of(shard));
            boolean accepted =
                    ItemHandlerHelper.insertItem(bank, shard.copy(), true).isEmpty();
            helper.assertTrue(claimed == accepted, type.getName() + ": isBanked and what the bank takes disagree");
            helper.assertTrue(claimed, type.getName() + ": an empty bank turned a spirit away");
            helper.assertTrue(
                    matrix.getInputBuffer().isItemValid(0, shard),
                    type.getName() + ": the input buffer has no room for it either");
            if (SpiritBank.hasDedicatedSlot(shard)) {
                dedicated++;
            } else {
                jokers++;
            }
        }
        helper.assertTrue(
                dedicated == SpiritBank.SLOTS - 1,
                "the bank names " + dedicated + " spirits outright, not " + (SpiritBank.SLOTS - 1));
        helper.assertTrue(jokers >= 1, "nothing at all is left for the joker slot to hold");

        ItemStack claimant = new ItemStack(MalumItems.UMBRAL_SPIRIT.get());
        ItemHandlerHelper.insertItem(bank, claimant.copy(), false);
        for (SpiritArcanaType type : MalumSpiritTypes.SPIRIT_TYPES_REGISTRY) {
            ItemStack shard = new ItemStack(type.getSpiritShard());
            if (SpiritBank.hasDedicatedSlot(shard) || shard.is(claimant.getItem())) {
                continue;
            }
            helper.assertTrue(
                    !matrix.banksSpirit(AEItemKey.of(shard)),
                    type.getName() + ": the joker slot took a second spirit alongside Umbral");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void bankedSpiritsStopCountingOncePushedCraftsOweThem(GameTestHelper helper) {
        FocusedSpiritMatrixBlockEntity matrix = placeMatrix(helper);
        RecipeHolder<SpiritInfusionRecipe> holder = recipeWithExtras(helper, 0);
        Map<AEItemKey, Long> perCraft = spiritsOf(holder);

        long stocked = 64;
        for (AEItemKey key : perCraft.keySet()) {
            matrix.bankSpirits(key, stocked, Actionable.MODULATE);
            helper.assertTrue(
                    matrix.freeSpirits(key) == stocked,
                    "a freshly stocked spirit did not read as available to the network");
        }

        for (int craft = 1; craft <= 2; craft++) {
            IPatternDetails details = patternFor(helper, holder);
            for (Map.Entry<AEItemKey, Long> entry : perCraft.entrySet()) {
                helper.assertTrue(
                        matrix.takeSpirits(entry.getKey(), entry.getValue()) == entry.getValue(),
                        "the network could not draw the recipe's spirits out of the bank");
            }
            helper.assertTrue(
                    matrix.pushMatrixPattern(details, inputsOf(details), Direction.UP),
                    "the Matrix refused push " + craft);
            for (Map.Entry<AEItemKey, Long> entry : perCraft.entrySet()) {
                long expected = stocked - entry.getValue() * craft;
                helper.assertTrue(
                        matrix.freeSpirits(entry.getKey()) == expected,
                        "after " + craft + " queued craft(s) the bank offered the network "
                                + matrix.freeSpirits(entry.getKey()) + " spirits, not " + expected);
            }
        }
        matrix.clearPending();
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void obelisksCutTheCraftTime(GameTestHelper helper) {
        FocusedSpiritMatrixBlockEntity matrix = placeMatrix(helper);
        int bare = matrix.craftTicks();

        int max = FocusedSpiritMatrixUpgrades.maxObelisks();
        helper.assertTrue(max > 0, "the obelisk upgrade slot is disabled");
        helper.assertTrue(
                matrix.getUpgradeSlot()
                        .insertItem(0, new ItemStack(MalumBlocks.RUNEWOOD_OBELISK.get(), max), false)
                        .isEmpty(),
                "the upgrade slot would not take a full stack of Runewood Obelisks");
        int full = matrix.craftTicks();
        helper.assertTrue(full < bare, "Runewood Obelisks did not cut the craft time");
        int floor = NepConfig.malumFocusedSpiritMatrixObeliskMinimumCraftTicks();
        helper.assertTrue(
                full == floor, "a full slot reached " + full + " ticks rather than the configured floor of " + floor);

        matrix.getUpgradeSlot().extractItem(0, max - max / 2, false);
        int half = matrix.craftTicks();
        helper.assertTrue(
                half > full && half < bare,
                "a half slot gave " + half + " ticks, which is not between " + full + " and " + bare);

        matrix.getUpgradeSlot().extractItem(0, max, false);
        helper.assertTrue(matrix.craftTicks() == bare, "pulling the obelisks out did not restore the craft time");

        helper.assertTrue(
                !matrix.getUpgradeSlot()
                        .insertItem(0, new ItemStack(MalumBlocks.BRILLIANT_OBELISK.get()), true)
                        .isEmpty(),
                "the upgrade slot accepted a Brilliant Obelisk, which is not an altar accelerator");

        helper.assertTrue(
                !matrix.getUpgradeSlot()
                        .insertItem(0, new ItemStack(MalumBlocks.SPIRIT_ALTAR.get()), true)
                        .isEmpty(),
                "the upgrade slot accepted something that is not an obelisk");
        helper.succeed();
    }

    private static RecipeHolder<SpiritFocusingRecipe> quickestFocusingRecipe(GameTestHelper helper) {
        RecipeHolder<SpiritFocusingRecipe> best = null;
        for (RecipeHolder<SpiritFocusingRecipe> holder : SpiritFocusingResolver.candidates(helper.getLevel())) {
            if (!holder.id().getNamespace().equals("malum")
                    || MalumRecipeIngredients.spiritFocusing(holder.value()) == null) {
                continue;
            }
            if (best == null || holder.value().time < best.value().time) {
                best = holder;
            }
        }
        helper.assertTrue(best != null, "no spirit focusing recipe was loaded");
        return best;
    }

    private static IPatternDetails focusingPatternFor(
            GameTestHelper helper, RecipeHolder<SpiritFocusingRecipe> holder) {
        EncodedIngredients expected = MalumRecipeIngredients.spiritFocusing(holder.value());
        helper.assertTrue(expected != null, "the focusing recipe produced no ingredient list");

        List<GenericStack> inputs = new ArrayList<>();
        for (List<GenericStack> options : expected.inputs()) {
            inputs.add(options.get(0));
        }
        ItemStack encoded = SpiritFocusingPattern.encode(
                holder.id(), inputs, expected.outputs().get(0));
        IPatternDetails details = PatternDetailsHelper.decodePattern(encoded, helper.getLevel());
        helper.assertTrue(details != null, "the spirit focusing pattern did not decode");
        return details;
    }

    private static void slotImpetus(GameTestHelper helper, FocusedSpiritMatrixBlockEntity matrix) {
        helper.assertTrue(
                matrix.getImpetusSlot()
                        .insertItem(0, new ItemStack(NepMalumContent.MATRIX_IMPETUS.get()), false)
                        .isEmpty(),
                "the impetus slot would not take a Matrix Impetus");
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 1200)
    public static void aSpiritFocusingPatternRunsToCompletion(GameTestHelper helper) {
        SpiritFocusingResolver.clearCache();
        FocusedSpiritMatrixBlockEntity matrix = placeMatrix(helper);
        slotImpetus(helper, matrix);
        helper.assertTrue(
                matrix.getCatalyzerSlot()
                        .insertItem(
                                0,
                                new ItemStack(
                                        NepMalumContent.MATRIX_CATALYZER.get(),
                                        FocusedSpiritMatrixUpgrades.maxCatalyzers()),
                                false)
                        .isEmpty(),
                "the catalyzer slot would not take a full stack of Matrix Catalyzers");

        RecipeHolder<SpiritFocusingRecipe> holder = quickestFocusingRecipe(helper);
        IPatternDetails details = focusingPatternFor(helper, holder);
        helper.assertTrue(
                matrix.pushMatrixPattern(details, inputsOf(details), Direction.UP),
                "the Matrix refused a spirit focusing pattern");
        helper.assertTrue(
                matrix.getInputBuffer().getStackInSlot(0).isEmpty(),
                "focusing has no item input, but something was staged in the input buffer");

        ItemStack result = holder.value().output;
        helper.succeedWhen(() ->
                helper.assertTrue(outputHolds(matrix, result, 1), "the Matrix never finished the spirit focusing"));
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void focusingIsRefusedWithoutAMatrixImpetus(GameTestHelper helper) {
        SpiritFocusingResolver.clearCache();
        FocusedSpiritMatrixBlockEntity matrix = placeMatrix(helper);

        RecipeHolder<SpiritFocusingRecipe> holder = quickestFocusingRecipe(helper);
        IPatternDetails details = focusingPatternFor(helper, holder);
        helper.assertTrue(
                !matrix.pushMatrixPattern(details, inputsOf(details), Direction.UP),
                "a Matrix with an empty impetus slot accepted a focusing pattern");
        helper.assertTrue(
                matrix.refusal() == FocusedSpiritMatrixBlockEntity.Refusal.NO_IMPETUS,
                "the Matrix refused for " + matrix.refusal() + " rather than the missing impetus");
        helper.assertTrue(matrix.pendingJobs() == 0, "a refused focusing push still queued a job");

        slotImpetus(helper, matrix);
        helper.assertTrue(
                matrix.pushMatrixPattern(details, inputsOf(details), Direction.UP),
                "the Matrix still refused the pattern once an impetus was slotted");
        matrix.clearPending();
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void catalyzersCutTheFocusingTime(GameTestHelper helper) {
        FocusedSpiritMatrixBlockEntity matrix = placeMatrix(helper);
        int max = FocusedSpiritMatrixUpgrades.maxCatalyzers();
        helper.assertTrue(max > 0, "the catalyzer upgrade slot is disabled");

        int bare = FocusedSpiritMatrixUpgrades.focusingTicks(900, 0);
        helper.assertTrue(bare == 900, "a catalyzer-free Matrix did not focus at the recipe's own speed");
        int half = FocusedSpiritMatrixUpgrades.focusingTicks(900, max / 2);
        int full = FocusedSpiritMatrixUpgrades.focusingTicks(900, max);
        helper.assertTrue(full < half && half < bare, "catalyzers did not scale the focusing time down in a line");

        helper.assertTrue(
                matrix.getCatalyzerSlot()
                        .insertItem(0, new ItemStack(NepMalumContent.MATRIX_CATALYZER.get(), max), false)
                        .isEmpty(),
                "the catalyzer slot would not take a full stack");
        helper.assertTrue(matrix.catalyzerCount() == max, "the Matrix did not count a full slot of catalyzers");
        helper.assertTrue(
                !matrix.getCatalyzerSlot()
                        .insertItem(0, new ItemStack(MalumBlocks.RUNEWOOD_OBELISK.get()), true)
                        .isEmpty(),
                "the catalyzer slot accepted an obelisk");
        helper.assertTrue(
                !matrix.getImpetusSlot()
                        .insertItem(0, new ItemStack(MalumItems.IRON_IMPETUS.get()), true)
                        .isEmpty(),
                "the impetus slot accepted one of Malum's own impetuses");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 1200)
    public static void byDefaultFocusingSpendsTheImpetus(GameTestHelper helper) {
        SpiritFocusingResolver.clearCache();
        helper.assertTrue(
                NepConfig.malumFocusedSpiritMatrixConsumeImpetusDurability(),
                "impetus durability consumption is off by default, which this test assumes is on");

        FocusedSpiritMatrixBlockEntity matrix = placeMatrix(helper);
        slotImpetus(helper, matrix);

        RecipeHolder<SpiritFocusingRecipe> holder = quickestFocusingRecipe(helper);
        int cost = holder.value().durabilityCost;
        helper.assertTrue(cost > 0, "the focusing recipe under test charges no durability, so it proves nothing");
        IPatternDetails details = focusingPatternFor(helper, holder);
        helper.assertTrue(
                matrix.pushMatrixPattern(details, inputsOf(details), Direction.UP),
                "the Matrix refused a spirit focusing pattern");

        ItemStack result = holder.value().output;
        helper.succeedWhen(() -> {
            helper.assertTrue(outputHolds(matrix, result, 1), "the Matrix never finished the spirit focusing");
            ItemStack held = matrix.getImpetusSlot().getStackInSlot(0);
            helper.assertTrue(!held.isEmpty(), "the Matrix Impetus vanished rather than taking wear");
            helper.assertTrue(
                    held.getDamageValue() == cost,
                    "the Matrix Impetus took " + held.getDamageValue() + " damage rather than the recipe's " + cost);
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 1200)
    public static void anAlmostSpentImpetusFracturesOnItsLastCraft(GameTestHelper helper) {
        SpiritFocusingResolver.clearCache();
        FocusedSpiritMatrixBlockEntity matrix = placeMatrix(helper);
        ItemStack impetus = new ItemStack(NepMalumContent.MATRIX_IMPETUS.get());
        impetus.setDamageValue(impetus.getMaxDamage() - 1);
        helper.assertTrue(
                matrix.getImpetusSlot().insertItem(0, impetus, false).isEmpty(),
                "the impetus slot would not take an almost spent Matrix Impetus");

        RecipeHolder<SpiritFocusingRecipe> holder = quickestFocusingRecipe(helper);
        IPatternDetails details = focusingPatternFor(helper, holder);
        helper.assertTrue(
                matrix.pushMatrixPattern(details, inputsOf(details), Direction.UP),
                "the Matrix refused a craft its impetus still had the durability to start");

        ItemStack result = holder.value().output;
        helper.succeedWhen(() -> {
            helper.assertTrue(outputHolds(matrix, result, 1), "the craft that spent the impetus never finished");
            ItemStack held = matrix.getImpetusSlot().getStackInSlot(0);
            helper.assertTrue(
                    held.is(NepMalumContent.FRACTURED_MATRIX_IMPETUS.get()),
                    "the spent Matrix Impetus left " + held.getItem() + " in the slot rather than fracturing");
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void aSpentImpetusIsRefusedFocusingWork(GameTestHelper helper) {
        SpiritFocusingResolver.clearCache();
        FocusedSpiritMatrixBlockEntity matrix = placeMatrix(helper);
        helper.assertTrue(
                !matrix.getImpetusSlot()
                        .insertItem(0, new ItemStack(NepMalumContent.FRACTURED_MATRIX_IMPETUS.get()), true)
                        .isEmpty(),
                "the impetus slot took a Fractured Matrix Impetus, which only a Spirit Altar can put back together");

        ItemStack impetus = new ItemStack(NepMalumContent.MATRIX_IMPETUS.get());
        impetus.setDamageValue(impetus.getMaxDamage());
        helper.assertTrue(
                matrix.getImpetusSlot().insertItem(0, impetus, false).isEmpty(),
                "the impetus slot would not take a spent Matrix Impetus");

        RecipeHolder<SpiritFocusingRecipe> holder = quickestFocusingRecipe(helper);
        IPatternDetails details = focusingPatternFor(helper, holder);
        helper.assertTrue(
                !matrix.pushMatrixPattern(details, inputsOf(details), Direction.UP),
                "the Matrix took focusing work on an impetus with no durability left");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void anUnpoweredMatrixKeepsTheIngredientsItWasHanded(GameTestHelper helper) {
        SpiritInfusionResolver.clearCache();
        helper.setBlock(MATRIX, NepMalumContent.MATRIX.get().defaultBlockState());
        FocusedSpiritMatrixBlockEntity matrix =
                helper.getBlockEntity(MATRIX) instanceof FocusedSpiritMatrixBlockEntity be ? be : null;
        helper.assertTrue(matrix != null, "the Focused Spirit Matrix has no block entity");

        RecipeHolder<SpiritInfusionRecipe> holder = recipeWithExtras(helper, 0);
        IPatternDetails details = patternFor(helper, holder);
        helper.assertTrue(
                matrix.pushMatrixPattern(details, inputsOf(details), Direction.UP),
                "the Matrix refused a spirit infusion pattern");

        ItemStack result = holder.value().result;
        helper.runAfterDelay(120L, () -> {
            helper.assertTrue(!outputHolds(matrix, result, 1), "an unpowered Matrix produced a result anyway");
            helper.assertTrue(matrix.pendingJobs() == 1, "an unpowered Matrix dropped the job it was handed");
            helper.succeed();
        });
    }
}
