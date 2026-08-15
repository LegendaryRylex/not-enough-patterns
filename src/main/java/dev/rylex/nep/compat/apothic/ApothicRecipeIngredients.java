package dev.rylex.nep.compat.apothic;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import dev.rylex.nep.pattern.encoding.EncodedIngredients;
import dev.shadowsoffire.apothic_enchanting.table.infusion.InfusionRecipe;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

final class ApothicRecipeIngredients {
    private ApothicRecipeIngredients() {}

    @Nullable
    static EncodedIngredients infusion(InfusionRecipe recipe) {
        if (InfusionRecipeResolver.statsFor(recipe) == null
                || recipe.getOutput().isEmpty()) {
            return null;
        }
        ItemStack[] accepted = recipe.getInput().getItems();
        List<GenericStack> alternatives = new ArrayList<>(accepted.length);
        for (ItemStack stack : accepted) {
            AEItemKey key = AEItemKey.of(stack);
            if (key != null) {
                alternatives.add(new GenericStack(key, 1));
            }
        }
        if (alternatives.isEmpty()) {
            return null;
        }
        List<List<GenericStack>> inputs = new ArrayList<>(3);
        inputs.add(List.copyOf(alternatives));
        inputs.add(List.of(new GenericStack(AEItemKey.of(Items.LAPIS_LAZULI), InfusionCosts.LAPIS)));
        List<GenericStack> experience = InfusionPayments.options(recipe, InfusionCosts.Rates.fromConfig());
        if (!experience.isEmpty()) {
            inputs.add(experience);
        }
        return new EncodedIngredients(List.copyOf(inputs), List.of(GenericStack.fromItemStack(recipe.getOutput())));
    }

    static List<GenericStack> chosen(InfusionRecipe recipe, AEItemKey input, @Nullable AEKey payment) {
        List<GenericStack> inputs = new ArrayList<>(3);
        inputs.add(new GenericStack(input, 1));
        inputs.add(new GenericStack(AEItemKey.of(Items.LAPIS_LAZULI), InfusionCosts.LAPIS));
        GenericStack experience = InfusionPayments.chosen(recipe, InfusionCosts.Rates.fromConfig(), payment);
        if (experience != null) {
            inputs.add(experience);
        }
        return inputs;
    }
}
