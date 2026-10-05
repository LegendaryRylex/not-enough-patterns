package dev.rylex.nep.compat.ars;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import com.hollingsworth.arsnouveau.common.crafting.recipes.ApparatusRecipeInput;
import com.hollingsworth.arsnouveau.common.crafting.recipes.EnchantingApparatusRecipe;
import dev.rylex.nep.pattern.InputSubstitution;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

final class ArmorUpgradeSubstitution {
    private ArmorUpgradeSubstitution() {}

    @Nullable
    static InputSubstitution of(ResourceLocation recipe, Level level) {
        RecipeHolder<EnchantingApparatusRecipe> holder = ArsRecipeResolver.apparatusById(level, recipe);
        if (holder == null || !ArsRecipeResolver.upgradesArmor(holder.value().getType())) {
            return null;
        }
        EnchantingApparatusRecipe upgrade = holder.value();
        return (template, candidate, ignored) -> accepts(upgrade, template, candidate, level);
    }

    private static boolean accepts(EnchantingApparatusRecipe upgrade, AEKey template, AEKey candidate, Level level) {
        return template instanceof AEItemKey wanted
                && candidate instanceof AEItemKey offered
                && wanted.getItem() == offered.getItem()
                && upgrades(upgrade, wanted, level)
                && upgrades(upgrade, offered, level);
    }

    private static boolean upgrades(EnchantingApparatusRecipe upgrade, AEItemKey armor, Level level) {
        return upgrade.doesReagentMatch(new ApparatusRecipeInput(armor.toStack(), List.of(), null), level, null);
    }
}
