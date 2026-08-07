package dev.rylex.nep.compat.draconic;

import com.brandon3055.draconicevolution.init.DEContent;
import dev.rylex.nep.ConfigOverrides;
import dev.rylex.nep.Nep;
import dev.rylex.nep.NepConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

@GameTestHolder(Nep.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FusionMatrixConfigGameTest {

    private static final String TEMPLATE = "empty_5x5x5";
    private static final String SWAP_GRACE_FIELD = "DRACONIC_FUSION_MATRIX_CORE_SWAP_GRACE";

    private static final BlockPos MATRIX = new BlockPos(2, 1, 2);

    private FusionMatrixConfigGameTest() {}

    private static FusionMatrixBlockEntity place(GameTestHelper helper) {
        helper.setBlock(MATRIX, NepDraconicContent.MATRIX.get().defaultBlockState());
        BlockEntity be = helper.getBlockEntity(MATRIX);
        helper.assertTrue(be instanceof FusionMatrixBlockEntity, "the matrix did not create its block entity");
        return (FusionMatrixBlockEntity) be;
    }

    private static void setCores(FusionMatrixBlockEntity matrix, Item core, int count) {
        IItemHandler upgrades = matrix.getUpgradeSlot();
        upgrades.extractItem(0, Integer.MAX_VALUE, false);
        upgrades.insertItem(0, new ItemStack(core, count), false);
    }

    private static FusionMatrixBlockEntity fullOfWyvernCores(GameTestHelper helper) {
        FusionMatrixBlockEntity matrix = place(helper);
        setCores(matrix, DEContent.CORE_WYVERN.get(), NepConfig.draconicFusionMatrixMaxCores());
        matrix.energyStorage().modify(matrix.energyCapacity());
        return matrix;
    }

    @GameTest(template = TEMPLATE, batch = "nep_fusion_swap_grace_zero")
    public static void aZeroCoreSwapGraceVoidsTheExcessAsTheCoreComesOut(GameTestHelper helper) {
        ConfigOverrides.Restore restore = ConfigOverrides.override(SWAP_GRACE_FIELD, 0);
        try {
            FusionMatrixBlockEntity matrix = fullOfWyvernCores(helper);
            long upgraded = matrix.storedEnergy();
            long base = NepConfig.draconicFusionMatrixCapacity();
            helper.assertTrue(upgraded > base, "the cores did not grow the buffer, so there is no excess to void");

            matrix.getUpgradeSlot().extractItem(0, Integer.MAX_VALUE, false);

            helper.assertTrue(
                    !matrix.isHoldingSwappedEnergy(),
                    "a zero-tick grace still opened a swap window when the cores came out");
            helper.assertTrue(
                    matrix.energyCapacity() == base,
                    "the buffer stayed at " + matrix.energyCapacity() + " FE instead of dropping straight to " + base);
            helper.assertTrue(
                    matrix.storedEnergy() == base,
                    "a zero-tick grace left " + matrix.storedEnergy() + " FE stored in a " + base + " FE buffer");
        } finally {
            restore.undo();
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = "nep_fusion_swap_grace_long", timeoutTicks = 400)
    public static void aLongCoreSwapGraceHoldsTheEnergyUntilTheCoresGoBack(GameTestHelper helper) {
        int beyondTheDefault = NepConfig.draconicFusionMatrixCoreSwapGrace() + 50;
        ConfigOverrides.Restore restore = ConfigOverrides.override(SWAP_GRACE_FIELD, 24_000);
        FusionMatrixBlockEntity matrix = fullOfWyvernCores(helper);
        long filled = matrix.storedEnergy();
        matrix.getUpgradeSlot().extractItem(0, Integer.MAX_VALUE, false);

        long[] observed = new long[2];
        boolean[] holding = new boolean[1];
        helper.runAtTickTime(beyondTheDefault, () -> {
            holding[0] = matrix.isHoldingSwappedEnergy();
            observed[0] = matrix.storedEnergy();
            setCores(matrix, DEContent.CORE_AWAKENED.get(), NepConfig.draconicFusionMatrixMaxCores());
            observed[1] = matrix.storedEnergy();
            restore.undo();
        });

        helper.runAfterDelay(beyondTheDefault + 5, () -> {
            helper.assertTrue(
                    holding[0],
                    "a 24000 tick grace gave the energy up after only " + beyondTheDefault
                            + " ticks, so the setting is not being honoured");
            helper.assertTrue(
                    observed[0] == filled,
                    "the matrix held " + observed[0] + " FE of the " + filled + " it had when the cores came out");
            helper.assertTrue(
                    observed[1] == filled,
                    "putting cores back inside a long grace window still cost " + (filled - observed[1]) + " FE");
            helper.succeed();
        });
    }
}
