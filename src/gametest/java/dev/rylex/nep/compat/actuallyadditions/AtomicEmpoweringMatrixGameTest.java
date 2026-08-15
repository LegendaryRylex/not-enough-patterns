package dev.rylex.nep.compat.actuallyadditions;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.core.definitions.AEBlocks;
import de.ellpeck.actuallyadditions.mod.crafting.EmpowererRecipe;
import de.ellpeck.actuallyadditions.mod.crafting.LaserRecipe;
import dev.rylex.nep.Nep;
import dev.rylex.nep.pattern.AtomicReconstructionPattern;
import dev.rylex.nep.pattern.EmpoweringPattern;
import dev.rylex.nep.pattern.encoding.EncodedIngredients;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Nep.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AtomicEmpoweringMatrixGameTest {

    private static final String TEMPLATE = "empty_5x5x5";
    private static final String BATCH = "nep_atomic_empowering_matrix";

    private static final BlockPos MATRIX = new BlockPos(2, 1, 2);
    private static final BlockPos ME_CONTROLLER = new BlockPos(2, 1, 3);
    private static final BlockPos ENERGY_CELL = ME_CONTROLLER.above();

    private AtomicEmpoweringMatrixGameTest() {}

    private static AtomicEmpoweringMatrixBlockEntity placeMatrix(GameTestHelper helper) {
        helper.setBlock(ME_CONTROLLER, AEBlocks.CONTROLLER.block());
        helper.setBlock(ENERGY_CELL, AEBlocks.CREATIVE_ENERGY_CELL.block().defaultBlockState());
        helper.setBlock(MATRIX, NepActuallyAdditionsContent.MATRIX.get().defaultBlockState());
        AtomicEmpoweringMatrixBlockEntity matrix =
                helper.getBlockEntity(MATRIX) instanceof AtomicEmpoweringMatrixBlockEntity be ? be : null;
        helper.assertTrue(matrix != null, "the Atomic Empowering Matrix has no block entity");
        return matrix;
    }

    private static void keepCharged(GameTestHelper helper, AtomicEmpoweringMatrixBlockEntity matrix, int ticks) {
        for (int tick = 1; tick <= ticks; tick++) {
            helper.runAtTickTime(tick, () -> {
                for (int i = 0; i < 200; i++) {
                    matrix.energyStorage().receiveEnergy(Integer.MAX_VALUE, false);
                }
            });
        }
    }

    private static RecipeHolder<EmpowererRecipe> cheapestEmpowering(GameTestHelper helper) {
        RecipeHolder<EmpowererRecipe> best = null;
        for (RecipeHolder<EmpowererRecipe> holder :
                ActuallyAdditionsRecipeResolver.empoweringCandidates(helper.getLevel())) {
            if (ActuallyAdditionsRecipeIngredients.empowering(holder.value()) == null) {
                continue;
            }
            if (best == null
                    || holder.value().getEnergyPerStand() < best.value().getEnergyPerStand()) {
                best = holder;
            }
        }
        helper.assertTrue(best != null, "no empowering recipe was loaded");
        return best;
    }

    private static RecipeHolder<LaserRecipe> cheapestLaser(GameTestHelper helper) {
        RecipeHolder<LaserRecipe> best = null;
        for (RecipeHolder<LaserRecipe> holder : ActuallyAdditionsRecipeResolver.laserCandidates(helper.getLevel())) {
            if (ActuallyAdditionsRecipeIngredients.laser(holder.value(), helper.getLevel()) == null) {
                continue;
            }
            if (best == null || holder.value().getEnergy() < best.value().getEnergy()) {
                best = holder;
            }
        }
        helper.assertTrue(best != null, "no atomic reconstruction recipe was loaded");
        return best;
    }

    private static IPatternDetails pattern(
            GameTestHelper helper,
            EncodedIngredients expected,
            net.minecraft.resources.ResourceLocation recipe,
            boolean empowering,
            long batch) {
        List<GenericStack> inputs = new ArrayList<>();
        for (List<GenericStack> options : expected.inputs()) {
            GenericStack option = options.get(0);
            inputs.add(new GenericStack(option.what(), option.amount() * batch));
        }
        GenericStack unit = expected.outputs().get(0);
        GenericStack result = new GenericStack(unit.what(), unit.amount() * batch);

        ItemStack encoded = empowering
                ? EmpoweringPattern.encode(recipe, inputs, result)
                : AtomicReconstructionPattern.encode(recipe, inputs, result);
        IPatternDetails details = PatternDetailsHelper.decodePattern(encoded, helper.getLevel());
        helper.assertTrue(details != null, "the pattern did not decode");
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

    private static boolean outputHolds(AtomicEmpoweringMatrixBlockEntity matrix, ItemStack expected, int count) {
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
    public static void anEmpoweringPatternRunsToCompletion(GameTestHelper helper) {
        ActuallyAdditionsRecipeResolver.clearCache();
        AtomicEmpoweringMatrixBlockEntity matrix = placeMatrix(helper);
        keepCharged(helper, matrix, 300);

        RecipeHolder<EmpowererRecipe> holder = cheapestEmpowering(helper);
        EncodedIngredients expected = ActuallyAdditionsRecipeIngredients.empowering(holder.value());
        helper.assertTrue(expected != null, "the recipe produced no ingredient list");
        IPatternDetails details = pattern(helper, expected, holder.id(), true, 1);

        helper.assertTrue(
                matrix.pushMatrixPattern(details, inputsOf(details), Direction.UP),
                "the Matrix refused an empowering pattern");

        ItemStack result = holder.value().getOutput();
        helper.succeedWhen(() ->
                helper.assertTrue(outputHolds(matrix, result, 1), "the Matrix never finished the empowering recipe"));
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 400)
    public static void aLaserPatternRunsToCompletionInTheSameBlock(GameTestHelper helper) {
        ActuallyAdditionsRecipeResolver.clearCache();
        AtomicEmpoweringMatrixBlockEntity matrix = placeMatrix(helper);
        keepCharged(helper, matrix, 300);

        RecipeHolder<LaserRecipe> holder = cheapestLaser(helper);
        EncodedIngredients expected = ActuallyAdditionsRecipeIngredients.laser(holder.value(), helper.getLevel());
        helper.assertTrue(expected != null, "the recipe produced no ingredient list");
        IPatternDetails details = pattern(helper, expected, holder.id(), false, 1);

        helper.assertTrue(
                matrix.pushMatrixPattern(details, inputsOf(details), Direction.UP),
                "the Matrix refused an atomic reconstruction pattern");

        ItemStack result = holder.value().getResultItem(helper.getLevel().registryAccess());
        helper.succeedWhen(() -> helper.assertTrue(
                outputHolds(matrix, result, 1), "the Matrix never finished the atomic reconstruction recipe"));
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 400)
    public static void aMultipliedLaserPatternQueuesEveryCraft(GameTestHelper helper) {
        ActuallyAdditionsRecipeResolver.clearCache();
        AtomicEmpoweringMatrixBlockEntity matrix = placeMatrix(helper);
        keepCharged(helper, matrix, 300);

        RecipeHolder<LaserRecipe> holder = cheapestLaser(helper);
        EncodedIngredients expected = ActuallyAdditionsRecipeIngredients.laser(holder.value(), helper.getLevel());
        helper.assertTrue(expected != null, "the recipe produced no ingredient list");
        IPatternDetails details = pattern(helper, expected, holder.id(), false, 4);

        helper.assertTrue(
                matrix.pushMatrixPattern(details, inputsOf(details), Direction.UP),
                "the Matrix refused a batched atomic reconstruction pattern");
        helper.assertTrue(matrix.pendingJobs() == 4, "a x4 pattern did not queue four crafts");

        ItemStack result = holder.value().getResultItem(helper.getLevel().registryAccess());
        helper.succeedWhen(
                () -> helper.assertTrue(outputHolds(matrix, result, 4), "the Matrix did not finish all four crafts"));
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void anUnpoweredMatrixKeepsTheIngredientsItWasHanded(GameTestHelper helper) {
        ActuallyAdditionsRecipeResolver.clearCache();
        AtomicEmpoweringMatrixBlockEntity matrix = placeMatrix(helper);

        RecipeHolder<LaserRecipe> holder = cheapestLaser(helper);
        EncodedIngredients expected = ActuallyAdditionsRecipeIngredients.laser(holder.value(), helper.getLevel());
        helper.assertTrue(expected != null, "the recipe produced no ingredient list");
        IPatternDetails details = pattern(helper, expected, holder.id(), false, 1);

        helper.assertTrue(
                matrix.pushMatrixPattern(details, inputsOf(details), Direction.UP),
                "the Matrix refused an atomic reconstruction pattern");

        ItemStack result = holder.value().getResultItem(helper.getLevel().registryAccess());
        helper.runAfterDelay(120L, () -> {
            helper.assertTrue(!outputHolds(matrix, result, 1), "an unpowered Matrix produced a result anyway");
            helper.assertTrue(matrix.pendingJobs() == 1, "an unpowered Matrix dropped the job it was handed");
            helper.succeed();
        });
    }
}
