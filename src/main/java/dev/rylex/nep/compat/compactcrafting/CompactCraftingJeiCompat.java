package dev.rylex.nep.compat.compactcrafting;

import dev.compactmods.crafting.compat.jei.JeiMiniaturizationCraftingCategory;
import dev.compactmods.crafting.recipes.MiniaturizationRecipe;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.compat.jei.JeiTransferSource;
import dev.rylex.nep.pattern.encoding.EncodedIngredients;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IIngredientAliasRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class CompactCraftingJeiCompat {
    private CompactCraftingJeiCompat() {}

    public static JeiTransferSource source() {
        return new JeiTransferSource() {
            @Override
            public void addTransfers(TransferCollector collector) {
                collector.addUnwrapped(
                        JeiMiniaturizationCraftingCategory.RECIPE_TYPE,
                        CompactCraftingJeiCompat::miniaturization,
                        CompactCraftingJeiCompat::idOf);
            }

            @Override
            public void addCatalysts(IRecipeCatalystRegistration registration) {
                if (NepConfig.compactCraftingMiniaturizationMatrix()) {
                    registration.addRecipeCatalysts(
                            JeiMiniaturizationCraftingCategory.RECIPE_TYPE,
                            NepCompactCraftingContent.MATRIX_ITEM.get());
                }
                if (NepConfig.compactCraftingMiniaturizationController()) {
                    registration.addRecipeCatalysts(
                            JeiMiniaturizationCraftingCategory.RECIPE_TYPE,
                            NepCompactCraftingContent.CONTROLLER_ITEM.get());
                }
            }

            @Override
            public void addAliases(IIngredientAliasRegistration registration) {
                alias(registration, NepCompactCraftingContent.MATRIX_ITEM.get());
                alias(registration, NepCompactCraftingContent.CONTROLLER_ITEM.get());
            }
        };
    }

    private static void alias(IIngredientAliasRegistration registration, Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        registration.addAlias(VanillaTypes.ITEM_STACK, new ItemStack(item), "nep.jei.alias." + id.getPath());
    }

    @Nullable
    private static EncodedIngredients miniaturization(MiniaturizationRecipe recipe, Level level) {
        if (!NepConfig.compactCraftingMiniaturizationMatrix()
                && !NepConfig.compactCraftingMiniaturizationController()) {
            return null;
        }
        return MiniaturizationRecipeIngredients.miniaturization(recipe, level);
    }

    @Nullable
    private static ResourceLocation idOf(MiniaturizationRecipe recipe, Level level) {
        for (RecipeHolder<MiniaturizationRecipe> candidate : MiniaturizationRecipeResolver.candidates(level)) {
            if (candidate.value() == recipe) {
                return candidate.id();
            }
        }
        return null;
    }
}
