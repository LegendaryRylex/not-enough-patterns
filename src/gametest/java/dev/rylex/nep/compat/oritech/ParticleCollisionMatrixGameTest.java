package dev.rylex.nep.compat.oritech;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.core.definitions.AEBlocks;
import dev.rylex.nep.ConfigOverrides;
import dev.rylex.nep.Nep;
import dev.rylex.nep.machine.ManualCraftFixtures;
import dev.rylex.nep.machine.ManualCraftOutcome;
import dev.rylex.nep.pattern.ParticleCollisionPattern;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import rearth.oritech.init.recipes.OritechRecipe;

@GameTestHolder(Nep.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ParticleCollisionMatrixGameTest {

    private static final String TEMPLATE = "empty_5x5x5";
    private static final String BATCH = "nep_particle_collision_matrix";
    private static final String ME_CHARGE_FIELD = "ORITECH_MATRIX_ME_CHARGE";
    private static final ResourceLocation RECIPE =
            ResourceLocation.fromNamespaceAndPath("test", "particle_matrix_triple");

    private static final BlockPos MATRIX = new BlockPos(2, 1, 2);
    private static final BlockPos ME_CONTROLLER = new BlockPos(2, 1, 3);
    private static final BlockPos ENERGY_CELL = ME_CONTROLLER.above();

    private ParticleCollisionMatrixGameTest() {}

    private static ParticleCollisionMatrixBlockEntity placeMatrix(GameTestHelper helper) {
        helper.setBlock(ME_CONTROLLER, AEBlocks.CONTROLLER.block());
        helper.setBlock(ENERGY_CELL, AEBlocks.CREATIVE_ENERGY_CELL.block().defaultBlockState());
        helper.setBlock(MATRIX, NepOritechContent.MATRIX.get().defaultBlockState());
        ParticleCollisionMatrixBlockEntity matrix =
                helper.getBlockEntity(MATRIX) instanceof ParticleCollisionMatrixBlockEntity be ? be : null;
        helper.assertTrue(matrix != null, "the Particle Collision Matrix has no block entity");
        return matrix;
    }

    private static void keepCharged(GameTestHelper helper, ParticleCollisionMatrixBlockEntity matrix, int ticks) {
        for (int tick = 1; tick <= ticks; tick++) {
            helper.runAtTickTime(tick, () -> matrix.energyStorage().receiveEnergy(Integer.MAX_VALUE, false));
        }
    }

    private static RecipeHolder<OritechRecipe> recipe(GameTestHelper helper) {
        OritechRecipeResolver.clearCache();
        RecipeHolder<OritechRecipe> holder = OritechRecipeResolver.collisionById(helper.getLevel(), RECIPE);
        helper.assertTrue(holder != null, "the test collision recipe was not loaded");
        return holder;
    }

    private static boolean push(GameTestHelper helper, ParticleCollisionMatrixBlockEntity matrix, long batch) {
        ItemStack encoded = ParticleCollisionPattern.encode(
                RECIPE,
                List.of(
                        new GenericStack(AEItemKey.of(Items.SUGAR), batch),
                        new GenericStack(AEItemKey.of(Items.BONE_MEAL), batch)),
                new GenericStack(AEItemKey.of(Items.LAPIS_LAZULI), batch));
        IPatternDetails pattern = PatternDetailsHelper.decodePattern(encoded, helper.getLevel());
        helper.assertTrue(pattern instanceof ParticleCollisionPattern, "the test pattern did not decode");
        KeyCounter inputs = new KeyCounter();
        inputs.add(AEItemKey.of(Items.SUGAR), batch);
        inputs.add(AEItemKey.of(Items.BONE_MEAL), batch);
        return matrix.pushMatrixPattern(pattern, new KeyCounter[] {inputs}, Direction.UP);
    }

    private static int lapisIn(ParticleCollisionMatrixBlockEntity matrix) {
        int found = 0;
        for (int slot = 0; slot < matrix.getOutputBuffer().getSlots(); slot++) {
            ItemStack stack = matrix.getOutputBuffer().getStackInSlot(slot);
            if (stack.is(Items.LAPIS_LAZULI)) {
                found += stack.getCount();
            }
        }
        return found;
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void theCostIsWhatTheMotorsWouldSpendOnTheLaunch(GameTestHelper helper) {
        RecipeHolder<OritechRecipe> holder = recipe(helper);
        long perSpeed = ParticleEnergy.perSpeed();
        long expected = perSpeed * (1 + 2);
        helper.assertTrue(
                ParticleCollisionMatrixBlockEntity.energyCost(holder.value()) == expected,
                "a speed 4 collision should cost the speed 3 launch, " + expected + " FE, but cost "
                        + ParticleCollisionMatrixBlockEntity.energyCost(holder.value()));
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 400)
    public static void aCollisionPatternMakesTheRecipesFullCount(GameTestHelper helper) {
        ParticleCollisionMatrixBlockEntity matrix = placeMatrix(helper);
        keepCharged(helper, matrix, 300);
        recipe(helper);

        helper.assertTrue(push(helper, matrix, 1), "the Matrix refused a particle collision pattern");

        helper.succeedWhen(() -> helper.assertTrue(
                lapisIn(matrix) == 3, "the Matrix should make all three lapis, but holds " + lapisIn(matrix)));
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 600)
    public static void aMultipliedPatternQueuesEveryCollision(GameTestHelper helper) {
        ParticleCollisionMatrixBlockEntity matrix = placeMatrix(helper);
        keepCharged(helper, matrix, 500);
        recipe(helper);

        helper.assertTrue(push(helper, matrix, 3), "the Matrix refused a batched particle collision pattern");
        helper.assertTrue(matrix.pendingJobs() == 3, "a x3 pattern did not queue three collisions");

        helper.succeedWhen(() -> helper.assertTrue(
                lapisIn(matrix) == 9,
                "three collisions should make nine lapis, but the Matrix holds " + lapisIn(matrix)));
    }

    @GameTest(template = TEMPLATE, batch = "nep_particle_collision_matrix_unpowered", timeoutTicks = 200)
    public static void anUnpoweredMatrixKeepsTheIngredientsItWasHanded(GameTestHelper helper) {
        ConfigOverrides.Restore restore = ConfigOverrides.override(ME_CHARGE_FIELD, false);
        ParticleCollisionMatrixBlockEntity placed;
        try {
            placed = placeMatrix(helper);
            recipe(helper);
            helper.assertTrue(push(helper, placed, 1), "the Matrix refused a particle collision pattern");
        } catch (RuntimeException e) {
            restore.undo();
            throw e;
        }
        ParticleCollisionMatrixBlockEntity matrix = placed;

        helper.runAfterDelay(120L, () -> {
            restore.undo();
            helper.assertTrue(lapisIn(matrix) == 0, "an unpowered Matrix produced a result anyway");
            helper.assertTrue(matrix.pendingJobs() == 1, "an unpowered Matrix dropped the job it was handed");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void queuesACollisionFromThePlayersInventory(GameTestHelper helper) {
        ParticleCollisionMatrixBlockEntity matrix = placeMatrix(helper);
        RecipeHolder<OritechRecipe> holder = recipe(helper);
        Player player =
                ManualCraftFixtures.playerWith(helper, OritechRecipeIngredients.manualRequirements(holder.value()));

        ManualCraftOutcome outcome = matrix.startManualCraft(player, holder.id(), 1);

        ManualCraftFixtures.assertQueued(helper, outcome, "the Particle Collision Matrix");
        helper.assertTrue(
                ManualCraftFixtures.inventoryCount(player) == 0, "the Matrix left ingredients in the inventory");
        helper.assertTrue(
                ManualCraftFixtures.bufferedCount(matrix.getInputBuffer()) == 2,
                "the Matrix took the ingredients without staging them");
        helper.succeed();
    }
}
