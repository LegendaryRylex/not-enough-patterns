package dev.rylex.nep.compat.apothic;

import dev.rylex.nep.NepConfig;
import dev.rylex.nep.compat.jei.JeiTransferSource;
import dev.rylex.nep.pattern.encoding.EncodedIngredients;
import dev.shadowsoffire.apothic_enchanting.compat.InfusionRecipeCategory;
import dev.shadowsoffire.apothic_enchanting.table.infusion.InfusionRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class ApothicJeiCompat {
    private ApothicJeiCompat() {}

    public static JeiTransferSource source() {
        return new JeiTransferSource() {
            @Override
            public void addTransfers(TransferCollector collector) {
                collector.addUnwrapped(InfusionRecipeCategory.TYPE, ApothicJeiCompat::infusion, ApothicJeiCompat::idOf);
            }
        };
    }

    @Nullable
    private static EncodedIngredients infusion(InfusionRecipe recipe, Level level) {
        if (!NepConfig.apothicInfusion()) {
            return null;
        }
        return ApothicRecipeIngredients.infusion(recipe);
    }

    @Nullable
    private static ResourceLocation idOf(InfusionRecipe recipe, Level level) {
        for (RecipeHolder<InfusionRecipe> holder : InfusionRecipeResolver.candidates(level)) {
            if (holder.value() == recipe) {
                return holder.id();
            }
        }
        return null;
    }
}
