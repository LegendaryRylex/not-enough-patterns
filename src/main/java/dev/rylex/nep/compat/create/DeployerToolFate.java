package dev.rylex.nep.compat.create;

import com.simibubi.create.content.kinetics.deployer.ItemApplicationRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;

/** Create only honours {@code keep_held_item} for an {@link ItemApplicationRecipe}, so a polishing recipe wears its sheet down however nep synthesises it. */
enum DeployerToolFate {
    KEPT,
    WORN,
    CONSUMED;

    static DeployerToolFate of(ResourceLocation id, ItemApplicationRecipe recipe, Level level) {
        if (recipe.getIngredients().size() < 2) {
            return CONSUMED;
        }
        if (SandPaperPolishing.byId(id, level) != null) {
            return WORN;
        }
        if (recipe.shouldKeepHeldItem()) {
            return KEPT;
        }
        return anyDamageable(recipe.getRequiredHeldItem()) ? WORN : CONSUMED;
    }

    boolean returnsTool() {
        return this != CONSUMED;
    }

    private static boolean anyDamageable(Ingredient tool) {
        for (ItemStack stack : tool.getItems()) {
            if (stack.isDamageableItem()) {
                return true;
            }
        }
        return false;
    }
}
