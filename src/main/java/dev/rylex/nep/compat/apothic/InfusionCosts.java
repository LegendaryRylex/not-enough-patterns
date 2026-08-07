package dev.rylex.nep.compat.apothic;

import dev.rylex.nep.NepConfig;
import dev.shadowsoffire.apothic_enchanting.table.infusion.InfusionRecipe;
import dev.shadowsoffire.placebo.util.EnchantmentUtils;
import net.minecraft.util.Mth;

final class InfusionCosts {
    private InfusionCosts() {}

    static final int LAPIS = 3;

    private static final int INFUSION_SLOT = 2;
    private static final int MINIMUM_LEVEL = 3;

    record Rates(int experiencePerBottle, int millibucketsPerExperience, boolean preferFluid) {
        static Rates fromConfig() {
            return new Rates(
                    NepConfig.apothicInfusionExperiencePerBottle(),
                    NepConfig.apothicInfusionMillibucketsPerExperience(),
                    NepConfig.apothicInfusionPreferExperienceFluid());
        }
    }

    static int levelCost(InfusionRecipe recipe) {
        return Math.max(Mth.ceil(recipe.getRequirements().eterna()), MINIMUM_LEVEL);
    }

    static int experienceCost(InfusionRecipe recipe) {
        int level = levelCost(recipe);
        int cost = 0;
        for (int i = 0; i <= INFUSION_SLOT; i++) {
            cost += EnchantmentUtils.getExperienceForLevel(level - i);
        }
        return cost - 1;
    }

    static int bottleCost(InfusionRecipe recipe, int experiencePerBottle) {
        return Mth.positiveCeilDiv(experienceCost(recipe), experiencePerBottle);
    }

    static int bottleCost(InfusionRecipe recipe, Rates rates) {
        return bottleCost(recipe, rates.experiencePerBottle());
    }

    static long fluidCost(InfusionRecipe recipe, Rates rates) {
        return (long) experienceCost(recipe) * rates.millibucketsPerExperience();
    }
}
