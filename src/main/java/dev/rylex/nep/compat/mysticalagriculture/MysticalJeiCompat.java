package dev.rylex.nep.compat.mysticalagriculture;

import com.blakebr0.mysticalagriculture.api.crafting.IAwakeningRecipe;
import com.blakebr0.mysticalagriculture.api.crafting.IInfusionRecipe;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.compat.jei.JeiTransferSource;
import dev.rylex.nep.pattern.encoding.EncodedIngredients;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class MysticalJeiCompat {
    private MysticalJeiCompat() {}

    private static final RecipeType<IInfusionRecipe> INFUSION =
            RecipeType.create("mysticalagriculture", "infusion", IInfusionRecipe.class);

    private static final RecipeType<IAwakeningRecipe> AWAKENING =
            RecipeType.create("mysticalagriculture", "awakening", IAwakeningRecipe.class);

    public static JeiTransferSource source() {
        return new JeiTransferSource() {
            @Override
            public void addTransfers(TransferCollector collector) {
                collector.addUnwrapped(INFUSION, MysticalJeiCompat::infusion, MysticalJeiCompat::infusionId);
                collector.addUnwrapped(AWAKENING, MysticalJeiCompat::awakening, MysticalJeiCompat::awakeningId);
            }

            @Override
            public void addCatalysts(IRecipeCatalystRegistration registration) {
                if (!NepConfig.mysticalInfusedAwakeningMatrix()) {
                    return;
                }
                ItemStack matrix = new ItemStack(NepMysticalContent.MATRIX_ITEM.get());
                registration.addRecipeCatalyst(matrix, INFUSION);
                registration.addRecipeCatalyst(matrix, AWAKENING);
            }
        };
    }

    @Nullable
    private static EncodedIngredients infusion(IInfusionRecipe recipe, Level level) {
        return NepConfig.mysticalInfusion() ? MysticalRecipeIngredients.infusion(recipe, level) : null;
    }

    @Nullable
    private static EncodedIngredients awakening(IAwakeningRecipe recipe, Level level) {
        return NepConfig.mysticalAwakening() ? MysticalRecipeIngredients.awakening(recipe, level) : null;
    }

    @Nullable
    private static ResourceLocation infusionId(IInfusionRecipe recipe, Level level) {
        for (RecipeHolder<IInfusionRecipe> holder : MysticalRecipeResolver.infusionCandidates(level)) {
            if (holder.value() == recipe) {
                return holder.id();
            }
        }
        return null;
    }

    @Nullable
    private static ResourceLocation awakeningId(IAwakeningRecipe recipe, Level level) {
        for (RecipeHolder<IAwakeningRecipe> holder : MysticalRecipeResolver.awakeningCandidates(level)) {
            if (holder.value() == recipe) {
                return holder.id();
            }
        }
        return null;
    }
}
