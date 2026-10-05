package dev.rylex.nep.compat.malum;

import com.sammy.malum.registry.common.block.MalumBlocks;
import dev.rylex.nep.NepConfig;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

final class FocusedSpiritMatrixUpgrades {
    private FocusedSpiritMatrixUpgrades() {}

    static boolean obelisksEnabled() {
        return NepConfig.malumFocusedSpiritMatrixObeliskSlot();
    }

    static boolean isObelisk(ItemStack stack) {
        return !stack.isEmpty() && Block.byItem(stack.getItem()) == MalumBlocks.RUNEWOOD_OBELISK.get();
    }

    static int maxObelisks() {
        return Math.max(1, NepConfig.malumFocusedSpiritMatrixMaxObelisks());
    }

    static int craftTicks(int obelisks) {
        int base = NepConfig.malumFocusedSpiritMatrixCraftTicks();
        if (obelisks <= 0) {
            return base;
        }
        int floor = NepConfig.malumFocusedSpiritMatrixObeliskMinimumCraftTicks();
        return Math.max(1, (int) Math.round(base - (base - floor) * fill(obelisks)));
    }

    static int craftTimeReductionPercent(int obelisks) {
        int base = NepConfig.malumFocusedSpiritMatrixCraftTicks();
        if (base <= 0) {
            return 0;
        }
        return (int) Math.round((base - craftTicks(obelisks)) * 100.0 / base);
    }

    static boolean focusingEnabled() {
        return NepConfig.malumFocusedSpiritMatrixFocusing();
    }

    static boolean catalyzersEnabled() {
        return focusingEnabled() && NepConfig.malumFocusedSpiritMatrixCatalyzerSlot();
    }

    static boolean isCatalyzer(ItemStack stack) {
        return !stack.isEmpty() && stack.is(NepMalumContent.MATRIX_CATALYZER.get());
    }

    static boolean isImpetus(ItemStack stack) {
        return !stack.isEmpty() && stack.is(NepMalumContent.MATRIX_IMPETUS.get());
    }

    static int maxCatalyzers() {
        return Math.max(1, NepConfig.malumFocusedSpiritMatrixMaxCatalyzers());
    }

    /**
     * Malum's focusing recipes run from 300 to 2700 ticks, so focusing keeps the recipe's own time as its base rather
     * than the Matrix's infusion time.
     */
    static int focusingTicks(int recipeTime, int catalyzers) {
        int base = Math.max(1, recipeTime);
        if (catalyzers <= 0) {
            return base;
        }
        double cut = NepConfig.malumFocusedSpiritMatrixCatalyzerCraftTimeReductionPercent()
                / 100.0
                * catalyzerFill(catalyzers);
        return Math.max(1, (int) Math.round(base * (1.0 - cut)));
    }

    static int focusingTimeReductionPercent(int recipeTime, int catalyzers) {
        int base = Math.max(1, recipeTime);
        return (int) Math.round((base - focusingTicks(base, catalyzers)) * 100.0 / base);
    }

    static double restorationChance(int catalyzers) {
        return chanceOf(NepConfig.malumFocusedSpiritMatrixCatalyzerRestorationPercent(), catalyzers);
    }

    static double chainFocusingChance(int catalyzers) {
        return chanceOf(NepConfig.malumFocusedSpiritMatrixCatalyzerChainFocusingPercent(), catalyzers);
    }

    static double fortuneChance(int catalyzers) {
        return chanceOf(NepConfig.malumFocusedSpiritMatrixCatalyzerFortunePercent(), catalyzers);
    }

    static int rollChance(double chance, RandomSource random) {
        int successes = 0;
        for (double left = chance; left > 0.0; left--) {
            if (left >= 1.0 || random.nextDouble() < left) {
                successes++;
            }
        }
        return successes;
    }

    private static double chanceOf(int percent, int catalyzers) {
        return catalyzers <= 0 ? 0.0 : percent / 100.0 * catalyzerFill(catalyzers);
    }

    private static double catalyzerFill(int catalyzers) {
        int max = maxCatalyzers();
        return max <= 0 ? 0.0 : Math.min(1.0, catalyzers / (double) max);
    }

    private static double fill(int obelisks) {
        int max = maxObelisks();
        return max <= 0 ? 0.0 : Math.min(1.0, obelisks / (double) max);
    }
}
