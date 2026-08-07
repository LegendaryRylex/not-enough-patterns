package dev.rylex.nep.compat.draconic;

import com.brandon3055.brandonscore.api.TechLevel;
import com.brandon3055.draconicevolution.init.DEContent;
import dev.rylex.nep.NepConfig;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

final class FusionMatrixUpgrades {
    private FusionMatrixUpgrades() {}

    @Nullable
    static TechLevel tierOf(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        Item item = stack.getItem();
        if (item == DEContent.CORE_WYVERN.get()) {
            return TechLevel.WYVERN;
        }
        if (item == DEContent.CORE_AWAKENED.get()) {
            return TechLevel.DRACONIC;
        }
        if (item == DEContent.CORE_CHAOTIC.get()) {
            return TechLevel.CHAOTIC;
        }
        return null;
    }

    static int maxCores() {
        return Math.max(0, NepConfig.draconicFusionMatrixMaxCores());
    }

    static long capacity(@Nullable TechLevel tier, int cores) {
        long base = NepConfig.draconicFusionMatrixCapacity();
        if (tier == null || cores <= 0) {
            return base;
        }
        return switch (tier) {
            case WYVERN -> additive(base, target(NepConfig.draconicFusionMatrixWyvernCapacity(), base), cores);
            case DRACONIC -> compound(base, target(NepConfig.draconicFusionMatrixDraconicCapacity(), base), cores);
            case CHAOTIC -> compound(base, target(NepConfig.draconicFusionMatrixChaoticCapacity(), base), cores);
            default -> base;
        };
    }

    static long capacityTarget(TechLevel tier) {
        long base = NepConfig.draconicFusionMatrixCapacity();
        return switch (tier) {
            case WYVERN -> target(NepConfig.draconicFusionMatrixWyvernCapacity(), base);
            case DRACONIC -> target(NepConfig.draconicFusionMatrixDraconicCapacity(), base);
            case CHAOTIC -> target(NepConfig.draconicFusionMatrixChaoticCapacity(), base);
            default -> base;
        };
    }

    private static long target(long configured, long base) {
        return Math.max(configured, base);
    }

    static int craftTicks(@Nullable TechLevel tier, int cores) {
        int base = NepConfig.draconicFusionMatrixCraftTicks();
        if (tier == null || cores <= 0) {
            return base;
        }
        if (tier == TechLevel.DRACONIC) {
            double cut = NepConfig.draconicFusionMatrixDraconicCraftTimeReductionPercent() / 100.0 * fill(cores);
            return Math.max(1, (int) Math.round(base * (1.0 - cut)));
        }
        if (tier == TechLevel.CHAOTIC) {
            int floor = NepConfig.draconicFusionMatrixChaoticMinimumCraftTicks();
            return Math.max(1, (int) Math.round(base - (base - floor) * fill(cores)));
        }
        return base;
    }

    static long chargeCost(long recipeCost, @Nullable TechLevel tier, int cores) {
        long cost = scale(Math.max(0, recipeCost), NepConfig.draconicFusionMatrixEnergyCostPercent());
        if (tier != TechLevel.CHAOTIC || cores <= 0) {
            return cost;
        }
        return scale(cost, 100 - energyCostReductionPercent(cores));
    }

    static int energyCostReductionPercent(int cores) {
        if (cores <= 0) {
            return 0;
        }
        return (int) Math.round(NepConfig.draconicFusionMatrixChaoticEnergyCostReductionPercent() * fill(cores));
    }

    static int craftTimeReductionPercent(int cores) {
        if (cores <= 0) {
            return 0;
        }
        return (int) Math.round(NepConfig.draconicFusionMatrixDraconicCraftTimeReductionPercent() * fill(cores));
    }

    private static double fill(int cores) {
        int max = maxCores();
        return max <= 0 ? 0.0 : Math.min(1.0, cores / (double) max);
    }

    private static long scale(long value, int percent) {
        if (percent == 100) {
            return value;
        }
        return (value / 100L) * percent + (value % 100L) * percent / 100L;
    }

    private static long additive(long base, long peak, int cores) {
        double filled = fill(cores);
        return filled >= 1.0 ? peak : clamp(base + (peak - base) * filled);
    }

    private static long compound(long base, long peak, int cores) {
        double filled = fill(cores);
        return filled >= 1.0 ? peak : clamp(base * Math.pow((double) peak / base, filled));
    }

    private static long clamp(double value) {
        if (!(value < (double) Long.MAX_VALUE)) {
            return Long.MAX_VALUE;
        }
        return Math.max(1L, (long) value);
    }
}
