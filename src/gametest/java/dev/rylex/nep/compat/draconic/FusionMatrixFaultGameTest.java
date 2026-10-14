package dev.rylex.nep.compat.draconic;

import static dev.rylex.nep.GameTestFixtures.inputsOf;
import static dev.rylex.nep.compat.draconic.DraconicFixtures.anyFusionRecipe;
import static dev.rylex.nep.compat.draconic.DraconicFixtures.keepCharged;
import static dev.rylex.nep.compat.draconic.DraconicFixtures.keptIngredientRecipe;
import static dev.rylex.nep.compat.draconic.DraconicFixtures.patternFor;
import static dev.rylex.nep.compat.draconic.DraconicFixtures.placeMatrix;
import static dev.rylex.nep.compat.draconic.DraconicFixtures.powerUp;

import appeng.api.crafting.IPatternDetails;
import com.brandon3055.draconicevolution.api.crafting.IFusionRecipe;
import dev.rylex.nep.Nep;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.machine.ComparatorSignal;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Nep.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FusionMatrixFaultGameTest {

    private static final String TEMPLATE = "empty_5x5x5";
    private static final String BATCH = "nep_fusion_matrix";

    private FusionMatrixFaultGameTest() {}

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void anIdleMatrixOffTheNetworkReportsThePowerFault(GameTestHelper helper) {
        FusionMatrixBlockEntity matrix = placeMatrix(helper);

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
        FusionMatrixBlockEntity matrix = placeMatrix(helper);
        powerUp(helper);

        helper.runAfterDelay(40, () -> {
            helper.assertTrue(!matrix.hasPowerFault(), "a matrix on a powered ME network still reported a power fault");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void anIdleMatrixWithAnEmptyBufferReportsTheEnergyFault(GameTestHelper helper) {
        FusionMatrixBlockEntity matrix = placeMatrix(helper);
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
        FusionMatrixBlockEntity matrix = placeMatrix(helper);
        powerUp(helper);
        keepCharged(helper, matrix, 80);

        helper.runAfterDelay(80, () -> {
            helper.assertTrue(!matrix.hasEnergyFault(), "a matrix with a supplied buffer reported an energy fault");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void aBufferDrainedEveryTickByChargingIsNotAnEnergyFault(GameTestHelper helper) {
        FusionMatrixBlockEntity matrix = placeMatrix(helper);
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
    public static void theComparatorReadsTheOutputBufferAndNothingElse(GameTestHelper helper) {
        FusionMatrixBlockEntity matrix = placeMatrix(helper);
        helper.assertTrue(matrix.comparatorOutput() == 0, "an empty matrix emitted a signal");

        matrix.getInputBuffer().insertItem(0, new ItemStack(Items.DIAMOND, 64), false);
        helper.assertTrue(
                matrix.comparatorOutput() == 0,
                "a stocked input buffer emitted " + matrix.comparatorOutput() + " rather than nothing");

        matrix.getOutputBuffer().insertItem(0, new ItemStack(Items.NETHERITE_BLOCK, 32), false);
        helper.assertTrue(
                matrix.comparatorOutput() == ComparatorSignal.of(matrix.getOutputBuffer()),
                "the comparator emitted " + matrix.comparatorOutput() + " instead of the output buffer's fullness of "
                        + ComparatorSignal.of(matrix.getOutputBuffer()));

        RecipeHolder<IFusionRecipe> holder = keptIngredientRecipe(helper);
        matrix.getInputBuffer().extractItem(0, Integer.MAX_VALUE, false);
        matrix.getOutputBuffer().extractItem(0, Integer.MAX_VALUE, false);
        IPatternDetails details = patternFor(helper, holder);
        helper.assertTrue(
                matrix.pushMatrixPattern(details, inputsOf(details), Direction.UP), "a fusion pattern was rejected");
        helper.assertTrue(
                matrix.comparatorOutput() == 0,
                "an owed craft emitted " + matrix.comparatorOutput() + " before anything reached the output buffer");
        helper.succeed();
    }
}
