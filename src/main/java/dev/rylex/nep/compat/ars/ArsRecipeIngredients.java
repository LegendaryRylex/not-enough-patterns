package dev.rylex.nep.compat.ars;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import com.hollingsworth.arsnouveau.common.crafting.recipes.EnchantingApparatusRecipe;
import com.hollingsworth.arsnouveau.common.crafting.recipes.ImbuementRecipe;
import dev.rylex.nep.machine.ManualRequirement;
import dev.rylex.nep.pattern.encoding.EncodedIngredients;
import dev.rylex.nep.pattern.encoding.IngredientMatching;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

final class ArsRecipeIngredients {
    private ArsRecipeIngredients() {}

    static final int MAX_PEDESTALS = 16;

    private static final TagKey<Item> APPARATUS_PRESERVES =
            ItemTags.create(ResourceLocation.fromNamespaceAndPath("ars_nouveau", "apparatus_not_consumed"));

    @Nullable
    static EncodedIngredients apparatus(EnchantingApparatusRecipe recipe, Level level) {
        GenericStack result = IngredientMatching.resultOf(recipe.getResultItem(level.registryAccess()));
        if (result == null) {
            return null;
        }

        List<List<GenericStack>> pedestals = pedestals(recipe);
        if (pedestals == null) {
            return null;
        }

        List<List<GenericStack>> inputs = new ArrayList<>(pedestals.size() + 1);
        if (!addReagent(inputs, recipe.reagent())) {
            return null;
        }
        inputs.addAll(pedestals);
        return new EncodedIngredients(List.copyOf(inputs), List.of(result));
    }

    @Nullable
    static EncodedIngredients openReagent(EnchantingApparatusRecipe recipe, ItemStack reagent, Level level) {
        AEItemKey reagentKey = AEItemKey.of(reagent);
        List<List<GenericStack>> pedestals = pedestals(recipe);
        if (reagentKey == null || pedestals == null) {
            return null;
        }

        List<GenericStack> shownPedestals =
                pedestals.stream().map(options -> options.get(0)).toList();
        GenericStack result = IngredientMatching.resultOf(
                ArsRecipeResolver.produced(recipe, level, new GenericStack(reagentKey, 1), shownPedestals));
        if (result == null) {
            return null;
        }

        List<List<GenericStack>> inputs = new ArrayList<>(pedestals.size() + 1);
        inputs.add(List.of(new GenericStack(reagentKey, 1)));
        inputs.addAll(pedestals);
        return new EncodedIngredients(List.copyOf(inputs), List.of(result));
    }

    @Nullable
    static List<List<GenericStack>> pedestals(EnchantingApparatusRecipe recipe) {
        List<Ingredient> pedestals = recipe.pedestalItems();
        if (pedestals.isEmpty() || pedestals.size() > MAX_PEDESTALS) {
            return null;
        }
        List<List<GenericStack>> slots = new ArrayList<>(pedestals.size());
        for (Ingredient ingredient : pedestals) {
            if (!addPedestal(slots, ingredient)) {
                return null;
            }
        }
        return List.copyOf(slots);
    }

    @Nullable
    static EncodedIngredients imbuement(ImbuementRecipe recipe, Level level) {
        if (recipe.getClass() != ImbuementRecipe.class) {
            return null;
        }

        GenericStack result = IngredientMatching.resultOf(recipe.getResultItem(level.registryAccess()));
        if (result == null) {
            return null;
        }

        List<Ingredient> pedestals = recipe.getPedestalItems();
        if (pedestals.size() > MAX_PEDESTALS) {
            return null;
        }

        List<List<GenericStack>> inputs = new ArrayList<>(pedestals.size() + 1);
        if (!addReagent(inputs, recipe.getInput())) {
            return null;
        }
        for (Ingredient ingredient : pedestals) {
            if (!addPedestal(inputs, ingredient)) {
                return null;
            }
        }
        return new EncodedIngredients(List.copyOf(inputs), List.of(result));
    }

    @Nullable
    static List<ManualRequirement> apparatusRequirements(EnchantingApparatusRecipe recipe) {
        if (recipe.getType() != ArsRecipeResolver.apparatusType() || pedestals(recipe) == null) {
            return null;
        }
        List<ManualRequirement> requirements =
                new ArrayList<>(recipe.pedestalItems().size() + 1);
        requirements.add(new ManualRequirement(recipe.reagent(), 1, true));
        for (Ingredient pedestal : recipe.pedestalItems()) {
            requirements.add(new ManualRequirement(pedestal, 1, true));
        }
        return List.copyOf(requirements);
    }

    @Nullable
    static List<ManualRequirement> imbuementRequirements(ImbuementRecipe recipe) {
        if (recipe.getClass() != ImbuementRecipe.class
                || recipe.getPedestalItems().size() > MAX_PEDESTALS) {
            return null;
        }
        return List.of(new ManualRequirement(recipe.getInput(), 1, true));
    }

    private static boolean addReagent(List<List<GenericStack>> inputs, Ingredient ingredient) {
        List<AEItemKey> options = IngredientMatching.itemOptions(ingredient);
        if (options.isEmpty()) {
            return false;
        }
        inputs.add(IngredientMatching.options(options, 1));
        return true;
    }

    private static boolean addPedestal(List<List<GenericStack>> inputs, Ingredient ingredient) {
        List<AEItemKey> options = new ArrayList<>();
        for (AEItemKey key : IngredientMatching.itemOptions(ingredient)) {
            ItemStack stack = key.toStack();
            if (stack.is(APPARATUS_PRESERVES) || stack.hasCraftingRemainingItem()) {
                return false;
            }
            options.add(key);
        }
        if (options.isEmpty()) {
            return false;
        }
        inputs.add(IngredientMatching.options(options, 1));
        return true;
    }
}
