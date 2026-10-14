package dev.rylex.nep.compat.draconic;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.GenericStack;
import com.brandon3055.draconicevolution.api.DraconicAPI;
import com.brandon3055.draconicevolution.api.crafting.IFusionRecipe;
import com.brandon3055.draconicevolution.blocks.machines.CraftingInjector;
import dev.rylex.nep.GameTestFixtures;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.pattern.FusionCraftingPattern;
import dev.rylex.nep.pattern.encoding.EncodedIngredients;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

final class DraconicFixtures {
    private DraconicFixtures() {}

    record InjectorSite(BlockPos pos, Direction facing) {}

    static final List<InjectorSite> SITES = List.of(
            new InjectorSite(new BlockPos(1, 2, 4), Direction.EAST),
            new InjectorSite(new BlockPos(7, 2, 4), Direction.WEST),
            new InjectorSite(new BlockPos(4, 2, 1), Direction.SOUTH),
            new InjectorSite(new BlockPos(4, 2, 7), Direction.NORTH),
            new InjectorSite(new BlockPos(1, 3, 4), Direction.EAST),
            new InjectorSite(new BlockPos(1, 1, 4), Direction.EAST),
            new InjectorSite(new BlockPos(7, 3, 4), Direction.WEST),
            new InjectorSite(new BlockPos(7, 1, 4), Direction.WEST));

    static void clearCaches() {
        FusionCraftingMachine.clearCache();
        FusionRecipeResolver.clearCache();
    }

    static void placeInjectors(GameTestHelper helper, Block injector, int count) {
        for (int i = 0; i < count; i++) {
            InjectorSite site = SITES.get(i);
            helper.setBlock(site.pos(), injector.defaultBlockState().setValue(CraftingInjector.FACING, site.facing()));
        }
    }

    static final BlockPos MATRIX = new BlockPos(2, 1, 2);
    static final BlockPos ME_CONTROLLER = new BlockPos(2, 1, 3);
    static final BlockPos ENERGY_CELL = ME_CONTROLLER.above();

    static FusionMatrixBlockEntity placeMatrix(GameTestHelper helper) {
        helper.setBlock(MATRIX, NepDraconicContent.MATRIX.get().defaultBlockState());
        BlockEntity be = helper.getBlockEntity(MATRIX);
        helper.assertTrue(be instanceof FusionMatrixBlockEntity, "the matrix did not create its block entity");
        return (FusionMatrixBlockEntity) be;
    }

    static void powerUp(GameTestHelper helper) {
        GameTestFixtures.powerUp(helper, ME_CONTROLLER, ENERGY_CELL);
    }

    static void keepCharged(GameTestHelper helper, FusionMatrixBlockEntity matrix, int ticks) {
        for (int tick = 0; tick <= ticks; tick++) {
            helper.runAtTickTime(tick, () -> matrix.energyStorage().modify(matrix.energyCapacity()));
        }
    }

    @SuppressWarnings("unchecked")
    static List<RecipeHolder<IFusionRecipe>> allFusionRecipes(GameTestHelper helper) {
        RecipeType<IFusionRecipe> type = (RecipeType<IFusionRecipe>) DraconicAPI.FUSION_RECIPE_TYPE.get();
        return helper.getLevel().getRecipeManager().getAllRecipesFor(type);
    }

    static RecipeHolder<IFusionRecipe> keptIngredientRecipe(GameTestHelper helper) {
        RecipeHolder<IFusionRecipe> holder = FusionRecipeResolver.resolveById(
                helper.getLevel(), ResourceLocation.fromNamespaceAndPath("test", "kept_ingredient"));
        helper.assertTrue(holder != null, "the kept-ingredient test recipe did not load");
        return holder;
    }

    static ItemStack encodeFusion(RecipeHolder<IFusionRecipe> holder, EncodedIngredients encoded) {
        List<GenericStack> consumed = new ArrayList<>();
        List<GenericStack> kept = new ArrayList<>();
        for (int slot = 0; slot < encoded.inputs().size(); slot++) {
            (encoded.isRetained(slot) ? kept : consumed)
                    .add(encoded.inputs().get(slot).get(0));
        }
        return FusionCraftingPattern.encode(
                holder.id(), consumed, kept, encoded.outputs().get(0));
    }

    static final int CHARGE_SLACK_TICKS = 20;

    static RecipeHolder<IFusionRecipe> anyFusionRecipe(GameTestHelper helper) {
        RecipeHolder<IFusionRecipe> holder = cheapestFusionRecipe(helper, candidate -> true);
        if (holder == null) {
            helper.fail("no fusion recipe was loaded");
        }
        return holder;
    }

    @Nullable
    static RecipeHolder<IFusionRecipe> cheapestFusionRecipe(
            GameTestHelper helper, Predicate<RecipeHolder<IFusionRecipe>> filter) {
        RecipeHolder<IFusionRecipe> cheapest = null;
        for (RecipeHolder<IFusionRecipe> holder : allFusionRecipes(helper)) {
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

    static boolean cheaperThan(RecipeHolder<IFusionRecipe> holder, RecipeHolder<IFusionRecipe> than) {
        long cost = holder.value().getEnergyCost();
        long best = than.value().getEnergyCost();
        return cost != best
                ? cost < best
                : holder.id().toString().compareTo(than.id().toString()) < 0;
    }

    static void assertCompletesWithin(GameTestHelper helper, RecipeHolder<IFusionRecipe> holder, int ticks) {
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

    static IPatternDetails patternFor(GameTestHelper helper, RecipeHolder<IFusionRecipe> holder) {
        return patternFor(helper, holder, true);
    }

    static IPatternDetails patternFor(
            GameTestHelper helper, RecipeHolder<IFusionRecipe> holder, boolean includeRetained) {
        EncodedIngredients expected = DraconicRecipeIngredients.fusion(holder, helper.getLevel(), includeRetained);
        helper.assertTrue(expected != null, "the recipe produced no ingredient list");
        ItemStack encoded = DraconicFixtures.encodeFusion(holder, expected);
        IPatternDetails details = PatternDetailsHelper.decodePattern(encoded, helper.getLevel());
        helper.assertTrue(details != null, "the fusion pattern did not decode");
        return details;
    }
}
