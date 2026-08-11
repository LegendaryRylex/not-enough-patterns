package dev.rylex.nep.compat.apothic;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import dev.shadowsoffire.apothic_enchanting.table.infusion.InfusionRecipe;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

final class InfusionPayments {
    private InfusionPayments() {}

    static List<GenericStack> options(InfusionRecipe recipe, InfusionCosts.Rates rates) {
        List<GenericStack> options = new ArrayList<>(2);
        long fluid = InfusionCosts.fluidCost(recipe, rates);
        if (rates.preferFluid() && fluid > 0) {
            for (AEFluidKey key : ExperienceFluids.all()) {
                options.add(new GenericStack(key, fluid));
            }
        }
        int bottles = InfusionCosts.bottleCost(recipe, rates);
        if (bottles > 0) {
            options.add(new GenericStack(AEItemKey.of(Items.EXPERIENCE_BOTTLE), bottles));
        }
        return options;
    }

    @Nullable
    static GenericStack preferred(InfusionRecipe recipe, InfusionCosts.Rates rates) {
        List<GenericStack> options = options(recipe, rates);
        return options.isEmpty() ? null : options.get(0);
    }
}
