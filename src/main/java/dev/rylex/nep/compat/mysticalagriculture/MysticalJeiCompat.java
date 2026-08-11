package dev.rylex.nep.compat.mysticalagriculture;

import com.blakebr0.mysticalagriculture.api.crafting.IAwakeningRecipe;
import com.blakebr0.mysticalagriculture.api.crafting.IInfusionRecipe;
import com.blakebr0.mysticalagriculture.compat.jei.category.AwakeningCategory;
import com.blakebr0.mysticalagriculture.compat.jei.category.InfusionCategory;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.compat.jei.JeiTransferSource;
import dev.rylex.nep.pattern.encoding.EncodedIngredients;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class MysticalJeiCompat {
    private MysticalJeiCompat() {}

    public static JeiTransferSource source() {
        return new JeiTransferSource() {
            @Override
            public void addTransfers(TransferCollector collector) {
                collector.add(InfusionCategory.RECIPE_TYPE, MysticalJeiCompat::infusion);
                collector.add(AwakeningCategory.RECIPE_TYPE, MysticalJeiCompat::awakening);
            }

            @Override
            public void addCatalysts(IRecipeCatalystRegistration registration) {
                if (!NepConfig.mysticalInfusedAwakeningMatrix()) {
                    return;
                }
                ItemStack matrix = new ItemStack(NepMysticalContent.MATRIX_ITEM.get());
                registration.addCraftingStation(InfusionCategory.RECIPE_TYPE, matrix);
                registration.addCraftingStation(AwakeningCategory.RECIPE_TYPE, matrix);
            }
        };
    }

    @Nullable
    private static EncodedIngredients infusion(RecipeHolder<IInfusionRecipe> holder, Level level) {
        return NepConfig.mysticalInfusion() ? MysticalRecipeIngredients.infusion(holder.value(), level) : null;
    }

    @Nullable
    private static EncodedIngredients awakening(RecipeHolder<IAwakeningRecipe> holder, Level level) {
        return NepConfig.mysticalAwakening() ? MysticalRecipeIngredients.awakening(holder.value(), level) : null;
    }
}
