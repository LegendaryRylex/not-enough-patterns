package dev.rylex.nep.compat.draconic;

import static dev.rylex.nep.GameTestFixtures.inputsOf;
import static dev.rylex.nep.compat.draconic.DraconicFixtures.anyFusionRecipe;
import static dev.rylex.nep.compat.draconic.DraconicFixtures.patternFor;
import static dev.rylex.nep.compat.draconic.DraconicFixtures.placeMatrix;
import static dev.rylex.nep.compat.draconic.DraconicFixtures.powerUp;

import appeng.api.crafting.IPatternDetails;
import com.brandon3055.draconicevolution.api.crafting.IFusionRecipe;
import com.brandon3055.draconicevolution.init.DEContent;
import dev.rylex.nep.Nep;
import dev.rylex.nep.NepConfig;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

@GameTestHolder(Nep.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FusionMatrixCoreTiersGameTest {

    private static final String TEMPLATE = "empty_5x5x5";
    private static final String BATCH = "nep_fusion_matrix";

    private FusionMatrixCoreTiersGameTest() {}

    private static void setCores(FusionMatrixBlockEntity matrix, Item core, int count) {
        IItemHandler upgrades = matrix.getUpgradeSlot();
        upgrades.extractItem(0, Integer.MAX_VALUE, false);
        upgrades.insertItem(0, new ItemStack(core, count), false);
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void theUpgradeSlotTakesTieredCoresOnly(GameTestHelper helper) {
        FusionMatrixBlockEntity matrix = placeMatrix(helper);
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
        FusionMatrixBlockEntity matrix = placeMatrix(helper);
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
        FusionMatrixBlockEntity matrix = placeMatrix(helper);
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
        FusionMatrixBlockEntity matrix = placeMatrix(helper);
        powerUp(helper);

        RecipeHolder<IFusionRecipe> holder = anyFusionRecipe(helper);
        long undiscounted = FusionMatrixUpgrades.chargeCost(holder.value().getEnergyCost(), null, 0);
        helper.assertTrue(undiscounted > 0, "the test recipe costs nothing, so no discount can be measured");
        setCores(matrix, DEContent.CORE_CHAOTIC.get(), NepConfig.draconicFusionMatrixMaxCores());

        IPatternDetails details = patternFor(helper, holder);
        helper.assertTrue(
                matrix.pushMatrixPattern(details, inputsOf(details), Direction.UP), "a fusion pattern was rejected");

        long[] claimed = new long[] {-1};
        for (int tick = 1; tick <= 20; tick++) {
            helper.runAtTickTime(tick, () -> {
                if (claimed[0] < 0 && !matrix.activeResult().isEmpty()) {
                    claimed[0] = matrix.chargeCost();
                }
            });
        }

        helper.runAfterDelay(20, () -> {
            helper.assertTrue(claimed[0] >= 0, "the matrix never claimed the craft");
            helper.assertTrue(
                    claimed[0] < undiscounted,
                    "a full slot of Chaotic Cores charged " + claimed[0] + " FE, no cheaper than the " + undiscounted
                            + " FE an unupgraded Matrix pays");
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
        FusionMatrixBlockEntity matrix = placeMatrix(helper);
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
        FusionMatrixBlockEntity matrix = placeMatrix(helper);
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
}
