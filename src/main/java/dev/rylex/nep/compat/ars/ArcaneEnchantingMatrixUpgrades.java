package dev.rylex.nep.compat.ars;

import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAccelerate;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentDampen;
import dev.rylex.nep.NepConfig;
import net.minecraft.world.item.ItemStack;

final class ArcaneEnchantingMatrixUpgrades {
    private ArcaneEnchantingMatrixUpgrades() {}

    static boolean isAccelerate(ItemStack stack) {
        return !stack.isEmpty() && stack.is(AugmentAccelerate.INSTANCE.getGlyph());
    }

    static boolean isDampen(ItemStack stack) {
        return !stack.isEmpty() && stack.is(AugmentDampen.INSTANCE.getGlyph());
    }

    static ItemStack accelerateIcon() {
        return new ItemStack(AugmentAccelerate.INSTANCE.getGlyph());
    }

    static ItemStack dampenIcon() {
        return new ItemStack(AugmentDampen.INSTANCE.getGlyph());
    }

    static int maxAccelerate() {
        return Math.max(1, NepConfig.arsMatrixMaxAccelerate());
    }

    static int maxDampen() {
        return Math.max(1, NepConfig.arsMatrixMaxDampen());
    }

    static int craftTicks(int base, int accelerate) {
        if (accelerate <= 0) {
            return Math.max(1, base);
        }
        int floor = Math.min(base, NepConfig.arsMatrixAccelerateMinimumCraftTicks());
        return Math.max(1, (int) Math.round(base - (base - floor) * fill(accelerate, maxAccelerate())));
    }

    static int craftTimeReductionPercent(int base, int accelerate) {
        return base <= 0 ? 0 : (int) Math.round((base - craftTicks(base, accelerate)) * 100.0 / base);
    }

    static int sourceCost(int recipeCost, int dampen) {
        if (recipeCost <= 0 || dampen <= 0) {
            return Math.max(0, recipeCost);
        }
        double discount = NepConfig.arsMatrixDampenSourceDiscountPercent() / 100.0 * fill(dampen, maxDampen());
        return Math.max(0, (int) Math.round(recipeCost * (1.0 - discount)));
    }

    static int sourceDiscountPercent(int dampen) {
        return dampen <= 0
                ? 0
                : (int) Math.round(NepConfig.arsMatrixDampenSourceDiscountPercent() * fill(dampen, maxDampen()));
    }

    private static double fill(int count, int max) {
        return Math.min(1.0, count / (double) max);
    }
}
