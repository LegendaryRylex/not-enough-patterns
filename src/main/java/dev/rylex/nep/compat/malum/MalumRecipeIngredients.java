package dev.rylex.nep.compat.malum;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import com.sammy.malum.common.recipe.RuneworkingRecipe;
import com.sammy.malum.common.recipe.SpiritFocusingRecipe;
import com.sammy.malum.common.recipe.SpiritInfusionRecipe;
import com.sammy.malum.core.systems.recipe.SpiritIngredient;
import dev.rylex.nep.machine.ManualRequirement;
import dev.rylex.nep.pattern.encoding.EncodedIngredients;
import dev.rylex.nep.pattern.encoding.IngredientMatching;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import org.jetbrains.annotations.Nullable;

final class MalumRecipeIngredients {
    private MalumRecipeIngredients() {}

    @Nullable
    static EncodedIngredients spiritInfusion(SpiritInfusionRecipe recipe) {
        GenericStack result = IngredientMatching.resultOf(recipe.result);
        if (result == null || recipe.spirits.isEmpty()) {
            return null;
        }
        List<List<GenericStack>> inputs = new ArrayList<>();
        if (!addSized(inputs, recipe.input)) {
            return null;
        }
        for (SpiritIngredient spirit : recipe.spirits) {
            if (!addSpirit(inputs, spirit)) {
                return null;
            }
        }
        for (SizedIngredient extra : recipe.extraInputs) {
            if (!addSized(inputs, extra)) {
                return null;
            }
        }
        return new EncodedIngredients(List.copyOf(inputs), List.of(result));
    }

    /**
     * Focusing takes no item input: the impetus in the crucible picks the recipe and is damaged rather than consumed,
     * so a pattern's whole cost is spirits.
     */
    @Nullable
    static EncodedIngredients spiritFocusing(SpiritFocusingRecipe recipe) {
        GenericStack result = IngredientMatching.resultOf(recipe.output);
        if (result == null || recipe.spirits.isEmpty()) {
            return null;
        }
        List<List<GenericStack>> inputs = new ArrayList<>();
        for (SpiritIngredient spirit : recipe.spirits) {
            if (!addSpirit(inputs, spirit)) {
                return null;
            }
        }
        return new EncodedIngredients(List.copyOf(inputs), List.of(result));
    }

    @Nullable
    static EncodedIngredients runeworking(RuneworkingRecipe recipe) {
        GenericStack result = IngredientMatching.resultOf(recipe.output);
        if (result == null) {
            return null;
        }
        List<List<GenericStack>> inputs = new ArrayList<>();
        if (!addSized(inputs, recipe.input) || !addSized(inputs, recipe.secondaryInput)) {
            return null;
        }
        return new EncodedIngredients(List.copyOf(inputs), List.of(result));
    }

    @Nullable
    static List<ManualRequirement> manualSpiritInfusion(SpiritInfusionRecipe recipe) {
        if (recipe.spirits.isEmpty()) {
            return null;
        }
        List<ManualRequirement> requirements = new ArrayList<>();
        if (!addManualSized(requirements, recipe.input, recipe.carryOverComponentData)) {
            return null;
        }
        for (SpiritIngredient spirit : recipe.spirits) {
            if (!addManualSpirit(requirements, spirit)) {
                return null;
            }
        }
        for (SizedIngredient extra : recipe.extraInputs) {
            if (!addManualSized(requirements, extra, false)) {
                return null;
            }
        }
        return List.copyOf(requirements);
    }

    /** The impetus focusing runs through is the Matrix's own, so a manual focusing costs the player spirits alone. */
    @Nullable
    static List<ManualRequirement> manualSpiritFocusing(SpiritFocusingRecipe recipe) {
        if (recipe.spirits.isEmpty()) {
            return null;
        }
        List<ManualRequirement> requirements = new ArrayList<>();
        for (SpiritIngredient spirit : recipe.spirits) {
            if (!addManualSpirit(requirements, spirit)) {
                return null;
            }
        }
        return List.copyOf(requirements);
    }

    @Nullable
    static List<ManualRequirement> manualRuneworking(RuneworkingRecipe recipe) {
        List<ManualRequirement> requirements = new ArrayList<>();
        if (!addManualSized(requirements, recipe.input, false)
                || !addManualSized(requirements, recipe.secondaryInput, false)) {
            return null;
        }
        return List.copyOf(requirements);
    }

    private static boolean addManualSpirit(List<ManualRequirement> requirements, SpiritIngredient spirit) {
        if (spirit.count() <= 0) {
            return false;
        }
        requirements.add(new ManualRequirement(Ingredient.of(spirit.asItemStack()), spirit.count(), true));
        return true;
    }

    private static boolean addManualSized(
            List<ManualRequirement> requirements, SizedIngredient ingredient, boolean carriesData) {
        if (ingredient.count() <= 0
                || IngredientMatching.itemOptions(ingredient.ingredient()).isEmpty()) {
            return false;
        }
        requirements.add(new ManualRequirement(ingredient.ingredient(), ingredient.count(), true, carriesData));
        return true;
    }

    static int spiritCount(SpiritInfusionRecipe recipe) {
        return recipe.spirits.size();
    }

    private static boolean addSpirit(List<List<GenericStack>> inputs, SpiritIngredient spirit) {
        AEItemKey key = AEItemKey.of(spirit.asItemStack());
        if (key == null || spirit.count() <= 0) {
            return false;
        }
        inputs.add(List.of(new GenericStack(key, spirit.count())));
        return true;
    }

    /** A carrying recipe dresses its declared result in the components of whichever item the craft consumes. */
    static ItemStack infusionResult(SpiritInfusionRecipe recipe, Level level, Collection<? extends AEKey> inputs) {
        ItemStack declared = recipe.result.copy();
        if (!recipe.carryOverComponentData || !(level instanceof ServerLevel server)) {
            return declared;
        }
        for (AEKey input : inputs) {
            if (!(input instanceof AEItemKey key) || !recipe.input.ingredient().test(key.toStack())) {
                continue;
            }
            ItemStack carried = recipe.getOutput(server, key.toStack());
            if (!carried.isEmpty()) {
                return carried;
            }
        }
        return declared;
    }

    private static boolean addSized(List<List<GenericStack>> inputs, SizedIngredient ingredient) {
        List<AEItemKey> options = IngredientMatching.itemOptions(ingredient.ingredient());
        if (options.isEmpty() || ingredient.count() <= 0) {
            return false;
        }
        inputs.add(IngredientMatching.options(options, ingredient.count()));
        return true;
    }
}
