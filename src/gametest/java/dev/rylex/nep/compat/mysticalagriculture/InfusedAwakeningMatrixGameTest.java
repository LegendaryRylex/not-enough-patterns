package dev.rylex.nep.compat.mysticalagriculture;

import appeng.api.AECapabilities;
import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.core.definitions.AEBlocks;
import com.blakebr0.mysticalagriculture.api.crafting.IAwakeningRecipe;
import com.blakebr0.mysticalagriculture.api.crafting.IInfusionRecipe;
import dev.rylex.nep.NepGameTests;
import dev.rylex.nep.machine.ManualCraftFixtures;
import dev.rylex.nep.machine.ManualCraftOutcome;
import dev.rylex.nep.machine.ManualCraftResult;
import dev.rylex.nep.machine.ManualRequirement;
import dev.rylex.nep.pattern.AwakeningPattern;
import dev.rylex.nep.pattern.InfusionPattern;
import dev.rylex.nep.pattern.encoding.EncodedIngredients;
import dev.rylex.nep.pattern.encoding.IngredientMatching;
import dev.rylex.nep.util.Recipes;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

public final class InfusedAwakeningMatrixGameTest {

    private static final BlockPos MATRIX = new BlockPos(2, 1, 2);
    private static final BlockPos ME_CONTROLLER = new BlockPos(2, 1, 3);
    private static final BlockPos ENERGY_CELL = ME_CONTROLLER.above();

    private InfusedAwakeningMatrixGameTest() {}

    public static void register(NepGameTests.Batch batch) {
        batch.add("exposes_its_machine_capabilities", InfusedAwakeningMatrixGameTest::exposesItsMachineCapabilities)
                .add(
                        "an_infusion_pattern_runs_to_completion",
                        400,
                        InfusedAwakeningMatrixGameTest::anInfusionPatternRunsToCompletion)
                .add(
                        "an_awakening_pattern_banks_its_essences",
                        400,
                        InfusedAwakeningMatrixGameTest::anAwakeningPatternBanksItsEssencesAndRunsToCompletion)
                .add(
                        "a_second_recipe_for_the_same_output_is_refused",
                        InfusedAwakeningMatrixGameTest::aSecondRecipeForTheSameOutputIsRefused)
                .add("a_non_mystical_pattern_is_refused", InfusedAwakeningMatrixGameTest::aNonMysticalPatternIsRefused)
                .add(
                        "breaking_the_matrix_mid_awakening_drops_the_claimed_essences",
                        400,
                        InfusedAwakeningMatrixGameTest::breakingTheMatrixMidAwakeningDropsTheClaimedEssences)
                .add(
                        "queues_an_infusion_craft_from_the_players_inventory",
                        InfusedAwakeningMatrixGameTest::queuesAnInfusionCraftFromThePlayersInventory)
                .add(
                        "a_multi_batch_manual_awakening_gives_every_essence_its_own_tank",
                        800,
                        InfusedAwakeningMatrixGameTest::aMultiBatchManualAwakeningGivesEveryEssenceItsOwnTank)
                .add(
                        "refuses_an_infusion_craft_the_inventory_cannot_pay_for",
                        InfusedAwakeningMatrixGameTest::refusesAnInfusionCraftTheInventoryCannotPayFor);
    }

    private static InfusedAwakeningMatrixBlockEntity placeMatrix(GameTestHelper helper) {
        MysticalRecipeResolver.clearCache();
        helper.setBlock(MATRIX, NepMysticalContent.MATRIX.get().defaultBlockState());
        return helper.getBlockEntity(MATRIX, InfusedAwakeningMatrixBlockEntity.class);
    }

    private static void powerUp(GameTestHelper helper) {
        helper.setBlock(ME_CONTROLLER, AEBlocks.CONTROLLER.block());
        helper.setBlock(ENERGY_CELL, AEBlocks.CREATIVE_ENERGY_CELL.block().defaultBlockState());
    }

    private static RecipeHolder<IInfusionRecipe> simplestInfusion(GameTestHelper helper) {
        RecipeHolder<IInfusionRecipe> best = null;
        for (RecipeHolder<IInfusionRecipe> holder : MysticalRecipeResolver.infusionCandidates(helper.getLevel())) {
            if (!holder.id().identifier().getNamespace().equals("mysticalagriculture")) {
                continue;
            }
            if (MysticalRecipeIngredients.infusion(holder.value(), helper.getLevel()) == null) {
                continue;
            }
            if (best == null || holder.id().identifier().compareTo(best.id().identifier()) < 0) {
                best = holder;
            }
        }
        helper.assertTrue(best != null, "no infusion recipe was loaded");
        return best;
    }

    private static RecipeHolder<IAwakeningRecipe> simplestAwakening(GameTestHelper helper) {
        RecipeHolder<IAwakeningRecipe> best = null;
        for (RecipeHolder<IAwakeningRecipe> holder : MysticalRecipeResolver.awakeningCandidates(helper.getLevel())) {
            if (MysticalRecipeIngredients.awakening(holder.value(), helper.getLevel()) == null) {
                continue;
            }
            if (best == null || holder.id().identifier().compareTo(best.id().identifier()) < 0) {
                best = holder;
            }
        }
        helper.assertTrue(best != null, "no awakening recipe was loaded");
        return best;
    }

    private static IPatternDetails pattern(
            GameTestHelper helper, EncodedIngredients expected, Identifier recipe, boolean awakening) {
        List<GenericStack> inputs = new ArrayList<>();
        for (List<GenericStack> options : expected.inputs()) {
            inputs.add(options.get(0));
        }
        GenericStack result = expected.outputs().get(0);

        ItemStack encoded = awakening
                ? AwakeningPattern.encode(recipe, inputs, result)
                : InfusionPattern.encode(recipe, inputs, result);
        IPatternDetails details = PatternDetailsHelper.decodePattern(encoded, helper.getLevel());
        helper.assertTrue(details != null, "the pattern did not decode");
        return details;
    }

    private static IPatternDetails infusionPattern(GameTestHelper helper, RecipeHolder<IInfusionRecipe> holder) {
        EncodedIngredients expected = MysticalRecipeIngredients.infusion(holder.value(), helper.getLevel());
        helper.assertTrue(expected != null, "the recipe produced no ingredient list");
        return pattern(helper, expected, holder.id().identifier(), false);
    }

    private static IPatternDetails awakeningPattern(GameTestHelper helper, RecipeHolder<IAwakeningRecipe> holder) {
        EncodedIngredients expected = MysticalRecipeIngredients.awakening(holder.value(), helper.getLevel());
        helper.assertTrue(expected != null, "the recipe produced no ingredient list");
        return pattern(helper, expected, holder.id().identifier(), true);
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

    private static boolean outputHolds(InfusedAwakeningMatrixBlockEntity matrix, ItemStack expected, int count) {
        int found = 0;
        for (int slot = 0; slot < matrix.getOutputBuffer().size(); slot++) {
            ItemStack stack = matrix.getOutputBuffer().stackAt(slot);
            if (ItemStack.isSameItem(stack, expected)) {
                found += stack.getCount();
            }
        }
        return found >= count;
    }

    private static long tankTotal(InfusedAwakeningMatrixBlockEntity matrix) {
        long total = 0;
        for (int tank = 0; tank < InfusedAwakeningMatrixBlockEntity.TANKS; tank++) {
            total += matrix.tank(tank).amount();
        }
        return total;
    }

    private static int occupiedTanks(InfusedAwakeningMatrixBlockEntity matrix) {
        int used = 0;
        for (int tank = 0; tank < InfusedAwakeningMatrixBlockEntity.TANKS; tank++) {
            if (!matrix.tank(tank).isEmpty()) {
                used++;
            }
        }
        return used;
    }

    private static Map<Item, Integer> essenceTotals(RecipeHolder<IAwakeningRecipe> holder, Level level) {
        Map<Item, Integer> totals = new LinkedHashMap<>();
        for (SizedIngredient essence : holder.value().getEssenceIngredients()) {
            List<AEItemKey> options = IngredientMatching.itemOptions(essence.ingredient(), level);
            totals.merge(options.get(0).getItem(), essence.count(), Integer::sum);
        }
        return totals;
    }

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
                helper.getLevel().getCapability(Capabilities.Item.BLOCK, pos, null) != null,
                "the matrix exposed no item handler, so nothing can restock it by hand");
        helper.succeed();
    }

    public static void anInfusionPatternRunsToCompletion(GameTestHelper helper) {
        InfusedAwakeningMatrixBlockEntity matrix = placeMatrix(helper);
        powerUp(helper);

        RecipeHolder<IInfusionRecipe> holder = simplestInfusion(helper);
        IPatternDetails details = infusionPattern(helper, holder);

        helper.assertTrue(
                matrix.pushMatrixPattern(details, inputsOf(details), Direction.UP),
                "the Matrix refused an infusion pattern");

        ItemStack result = MysticalRecipeResolver.resultOf(holder.value());
        helper.succeedWhen(() -> helper.assertTrue(
                outputHolds(matrix, result, result.getCount()), "the Matrix never finished the infusion recipe"));
    }

    public static void anAwakeningPatternBanksItsEssencesAndRunsToCompletion(GameTestHelper helper) {
        InfusedAwakeningMatrixBlockEntity matrix = placeMatrix(helper);
        powerUp(helper);

        RecipeHolder<IAwakeningRecipe> holder = simplestAwakening(helper);
        IPatternDetails details = awakeningPattern(helper, holder);
        long essences = 0;
        for (int count : essenceTotals(holder, helper.getLevel()).values()) {
            essences += count;
        }

        helper.assertTrue(
                matrix.pushMatrixPattern(details, inputsOf(details), Direction.UP),
                "the Matrix refused an awakening pattern");
        helper.assertTrue(
                tankTotal(matrix) == essences,
                "the push banked " + tankTotal(matrix) + " essence into the tanks instead of the " + essences
                        + " the recipe needs");

        ItemStack result = MysticalRecipeResolver.resultOf(holder.value());
        helper.succeedWhen(() -> {
            helper.assertTrue(
                    outputHolds(matrix, result, result.getCount()), "the Matrix never finished the awakening recipe");
            helper.assertTrue(tankTotal(matrix) == 0, "the craft left essence behind in the tanks");
        });
    }

    public static void aSecondRecipeForTheSameOutputIsRefused(GameTestHelper helper) {
        InfusedAwakeningMatrixBlockEntity matrix = placeMatrix(helper);

        RecipeHolder<IInfusionRecipe> first = MysticalRecipeResolver.infusionById(
                helper.getLevel(), Identifier.fromNamespaceAndPath("test", "mixed_output_a"));
        RecipeHolder<IInfusionRecipe> second = MysticalRecipeResolver.infusionById(
                helper.getLevel(), Identifier.fromNamespaceAndPath("test", "mixed_output_b"));
        helper.assertTrue(first != null && second != null, "the shared-output test recipes did not load");

        IPatternDetails queued = infusionPattern(helper, first);
        helper.assertTrue(
                matrix.pushMatrixPattern(queued, inputsOf(queued), Direction.UP),
                "the first shared-output pattern was rejected");

        IPatternDetails rival = infusionPattern(helper, second);
        helper.assertTrue(
                !matrix.pushMatrixPattern(rival, inputsOf(rival), Direction.UP),
                "the Matrix accepted a second recipe for an output it already owes from another recipe; the two crafts"
                        + " would race for the same owed item");
        helper.assertTrue(
                matrix.refusal() == InfusedAwakeningMatrixBlockEntity.Refusal.MIXED_RECIPES,
                "the Matrix refused the rival pattern as " + matrix.refusal() + " instead of MIXED_RECIPES");

        helper.assertTrue(
                matrix.pushMatrixPattern(queued, inputsOf(queued), Direction.UP),
                "refusing the rival recipe also blocked the recipe already queued");
        helper.assertTrue(matrix.pendingJobs() == 2, "the repeated pattern did not queue a second craft");
        helper.succeed();
    }

    public static void aNonMysticalPatternIsRefused(GameTestHelper helper) {
        InfusedAwakeningMatrixBlockEntity matrix = placeMatrix(helper);
        ItemStack encoded = PatternDetailsHelper.encodeProcessingPattern(
                List.of(new GenericStack(AEItemKey.of(Items.DIAMOND), 1)),
                List.of(new GenericStack(AEItemKey.of(Items.NETHERITE_BLOCK), 1)));
        IPatternDetails details = PatternDetailsHelper.decodePattern(encoded, helper.getLevel());
        helper.assertTrue(details != null, "the test pattern did not decode");

        helper.assertTrue(
                !matrix.pushMatrixPattern(details, inputsOf(details), Direction.UP),
                "the matrix accepted a pattern that is not an infusion or awakening pattern");
        helper.assertTrue(
                matrix.refusal() == InfusedAwakeningMatrixBlockEntity.Refusal.NOT_A_MYSTICAL_PATTERN,
                "the matrix refused the pattern as " + matrix.refusal() + " instead of NOT_A_MYSTICAL_PATTERN");
        helper.succeed();
    }

    public static void breakingTheMatrixMidAwakeningDropsTheClaimedEssences(GameTestHelper helper) {
        InfusedAwakeningMatrixBlockEntity matrix = placeMatrix(helper);
        powerUp(helper);

        RecipeHolder<IAwakeningRecipe> holder = simplestAwakening(helper);
        IPatternDetails details = awakeningPattern(helper, holder);
        Map<Item, Integer> essences = essenceTotals(holder, helper.getLevel());

        helper.assertTrue(
                matrix.pushMatrixPattern(details, inputsOf(details), Direction.UP),
                "the Matrix refused an awakening pattern");

        AtomicBoolean broken = new AtomicBoolean();
        for (int tick = 1; tick <= 200; tick++) {
            helper.runAtTickTime(tick, () -> {
                if (!broken.get() && !matrix.activeResult().isEmpty()) {
                    broken.set(true);
                    helper.setBlock(MATRIX, Blocks.AIR);
                }
            });
        }

        helper.succeedWhen(() -> {
            helper.assertTrue(broken.get(), "the Matrix never claimed the awakening craft");
            for (Map.Entry<Item, Integer> essence : essences.entrySet()) {
                helper.assertItemEntityCountIs(essence.getKey(), MATRIX, 2.0, essence.getValue());
            }
        });
    }

    public static void queuesAnInfusionCraftFromThePlayersInventory(GameTestHelper helper) {
        InfusedAwakeningMatrixBlockEntity matrix = placeMatrix(helper);
        powerUp(helper);
        RecipeHolder<IInfusionRecipe> holder = simplestInfusion(helper);
        List<ManualRequirement> requirements = MysticalRecipeIngredients.manualInfusionRequirements(holder.value());
        Player player = ManualCraftFixtures.playerWith(helper, requirements);

        ManualCraftOutcome outcome = matrix.startManualCraft(player, Recipes.idOf(holder), 1);

        ManualCraftFixtures.assertQueued(helper, outcome, "the Infused Awakening Matrix");
        helper.assertTrue(
                ManualCraftFixtures.inventoryCount(player) == 0, "the Matrix left ingredients in the inventory");
        helper.assertTrue(
                ManualCraftFixtures.bufferedCount(matrix.getInputBuffer()) > 0,
                "the Matrix took the ingredients without staging them");
        helper.succeed();
    }

    public static void aMultiBatchManualAwakeningGivesEveryEssenceItsOwnTank(GameTestHelper helper) {
        InfusedAwakeningMatrixBlockEntity matrix = placeMatrix(helper);
        powerUp(helper);
        RecipeHolder<IAwakeningRecipe> holder = simplestAwakening(helper);
        List<ManualRequirement> requirements = MysticalRecipeIngredients.manualAwakeningRequirements(holder.value());
        int batches = 3;
        Player player = ManualCraftFixtures.playerWith(helper, requirements, batches);

        ManualCraftOutcome outcome = matrix.startManualCraft(player, Recipes.idOf(holder), batches);

        helper.assertTrue(
                outcome.status() == ManualCraftResult.STARTED,
                "the Matrix refused a multi-batch awakening it could pay for: " + outcome.status());
        helper.assertTrue(
                outcome.batches() == batches,
                "the Matrix queued " + outcome.batches() + " crafts instead of " + batches);

        int distinct = essenceTotals(holder, helper.getLevel()).size();
        ItemStack result = MysticalRecipeResolver.resultOf(holder.value());
        helper.runAtTickTime(
                5,
                () -> helper.assertTrue(
                        occupiedTanks(matrix) <= distinct,
                        "one essence spread over " + occupiedTanks(matrix) + " tanks for a recipe with " + distinct
                                + " essences"));
        helper.succeedWhen(() -> helper.assertTrue(
                outputHolds(matrix, result, result.getCount() * batches),
                "the Matrix never finished all " + batches + " hand-started awakening crafts"));
    }

    public static void refusesAnInfusionCraftTheInventoryCannotPayFor(GameTestHelper helper) {
        InfusedAwakeningMatrixBlockEntity matrix = placeMatrix(helper);
        powerUp(helper);
        RecipeHolder<IInfusionRecipe> holder = simplestInfusion(helper);

        ManualCraftOutcome outcome =
                matrix.startManualCraft(helper.makeMockPlayer(GameType.SURVIVAL), Recipes.idOf(holder), 1);

        helper.assertTrue(
                outcome.status() == ManualCraftResult.MISSING_ITEMS,
                "an empty inventory did not read as missing items: " + outcome.status());
        helper.succeed();
    }
}
