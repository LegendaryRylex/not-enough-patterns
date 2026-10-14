package dev.rylex.nep.compat.malum;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.GenericStack;
import com.sammy.malum.common.recipe.RuneworkingRecipe;
import com.sammy.malum.common.recipe.SpiritFocusingRecipe;
import com.sammy.malum.common.recipe.SpiritInfusionRecipe;
import dev.rylex.nep.GameTestFixtures;
import dev.rylex.nep.machine.MatrixGridNode;
import dev.rylex.nep.pattern.RuneworkingPattern;
import dev.rylex.nep.pattern.SpiritFocusingPattern;
import dev.rylex.nep.pattern.SpiritInfusionPattern;
import dev.rylex.nep.pattern.encoding.EncodedIngredients;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;

final class MalumFixtures {
    static final int UMBRAL_SLOT = SpiritBank.JOKER_SLOT;

    private MalumFixtures() {}

    @FunctionalInterface
    private interface PatternEncoder {
        ItemStack encode(ResourceLocation id, List<GenericStack> inputs, GenericStack output);
    }

    static FocusedSpiritMatrixBlockEntity placeMatrix(
            GameTestHelper helper, BlockPos controller, BlockPos energyCell, BlockPos matrixPos) {
        GameTestFixtures.powerUp(helper, controller, energyCell);
        return placeUnpoweredMatrix(helper, matrixPos);
    }

    static FocusedSpiritMatrixBlockEntity placeUnpoweredMatrix(GameTestHelper helper, BlockPos matrixPos) {
        SpiritInfusionResolver.clearCache();
        SpiritFocusingResolver.clearCache();
        RuneworkingResolver.clearCache();
        helper.setBlock(matrixPos, NepMalumContent.MATRIX.get().defaultBlockState());
        FocusedSpiritMatrixBlockEntity matrix =
                helper.getBlockEntity(matrixPos) instanceof FocusedSpiritMatrixBlockEntity be ? be : null;
        helper.assertTrue(matrix != null, "the Focused Spirit Matrix has no block entity");
        return matrix;
    }

    static FocusedSpiritMatrixBlockEntity freshMatrix(GameTestHelper helper, BlockPos matrixPos) {
        return new FocusedSpiritMatrixBlockEntity(
                NepMalumContent.MATRIX_BLOCK_ENTITY.get(),
                helper.absolutePos(matrixPos),
                NepMalumContent.MATRIX.get().defaultBlockState());
    }

    static MatrixGridNode node(GameTestHelper helper, FocusedSpiritMatrixBlockEntity matrix) {
        helper.assertTrue(
                matrix.gridNodeHost() instanceof MatrixGridNode, "the Matrix is not backed by a matrix grid node");
        return (MatrixGridNode) matrix.gridNodeHost();
    }

    static boolean outputHolds(FocusedSpiritMatrixBlockEntity matrix, ItemStack expected, int count) {
        return GameTestFixtures.holds(matrix.getOutputBuffer(), expected, count);
    }

    static <R extends Recipe<?>> Comparator<RecipeHolder<R>> byId() {
        return (first, second) -> first.id().compareTo(second.id());
    }

    static <R extends Recipe<?>> RecipeHolder<R> pickBest(
            GameTestHelper helper,
            Iterable<RecipeHolder<R>> candidates,
            Predicate<RecipeHolder<R>> keep,
            Comparator<RecipeHolder<R>> order,
            String failure) {
        RecipeHolder<R> best = null;
        for (RecipeHolder<R> holder : candidates) {
            if (!keep.test(holder)) {
                continue;
            }
            if (best == null || order.compare(holder, best) < 0) {
                best = holder;
            }
        }
        helper.assertTrue(best != null, failure);
        return best;
    }

    static RecipeHolder<SpiritInfusionRecipe> recipeWithExtras(GameTestHelper helper, int extras) {
        return pickBest(
                helper,
                SpiritInfusionResolver.candidates(helper.getLevel()),
                holder -> {
                    SpiritInfusionRecipe recipe = holder.value();
                    return holder.id().getNamespace().equals("malum")
                            && !recipe.carryOverComponentData
                            && recipe.extraInputs.size() == extras
                            && MalumRecipeIngredients.spiritInfusion(recipe) != null;
                },
                byId(),
                "no spirit infusion recipe with " + extras + " extra ingredient(s) was loaded");
    }

    static IPatternDetails spiritInfusionPattern(GameTestHelper helper, ResourceLocation recipe) {
        RecipeHolder<SpiritInfusionRecipe> holder = SpiritInfusionResolver.byId(helper.getLevel(), recipe);
        helper.assertTrue(holder != null, "the test recipe " + recipe + " did not load");
        return spiritInfusionPattern(helper, holder);
    }

    static IPatternDetails spiritInfusionPattern(GameTestHelper helper, RecipeHolder<SpiritInfusionRecipe> holder) {
        EncodedIngredients expected = MalumRecipeIngredients.spiritInfusion(holder.value());
        helper.assertTrue(expected != null, "the recipe produced no ingredient list");
        return decoded(helper, holder.id(), expected, SpiritInfusionPattern::encode, "spirit infusion");
    }

    static IPatternDetails spiritFocusingPattern(GameTestHelper helper, RecipeHolder<SpiritFocusingRecipe> holder) {
        EncodedIngredients expected = MalumRecipeIngredients.spiritFocusing(holder.value());
        helper.assertTrue(expected != null, "the focusing recipe produced no ingredient list");
        return decoded(helper, holder.id(), expected, SpiritFocusingPattern::encode, "spirit focusing");
    }

    static IPatternDetails runeworkingPattern(GameTestHelper helper, RecipeHolder<RuneworkingRecipe> holder) {
        EncodedIngredients expected = MalumRecipeIngredients.runeworking(holder.value());
        helper.assertTrue(expected != null, "the runeworking recipe produced no ingredient list");
        return decoded(helper, holder.id(), expected, RuneworkingPattern::encode, "runeworking");
    }

    private static IPatternDetails decoded(
            GameTestHelper helper,
            ResourceLocation id,
            EncodedIngredients expected,
            PatternEncoder encoder,
            String kind) {
        List<GenericStack> inputs = new ArrayList<>();
        for (List<GenericStack> options : expected.inputs()) {
            inputs.add(options.get(0));
        }
        ItemStack encoded = encoder.encode(id, inputs, expected.outputs().get(0));
        IPatternDetails details = PatternDetailsHelper.decodePattern(encoded, helper.getLevel());
        helper.assertTrue(details != null, "the " + kind + " pattern did not decode");
        return details;
    }
}
