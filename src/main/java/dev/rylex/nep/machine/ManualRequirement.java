package dev.rylex.nep.machine;

import net.minecraft.world.item.crafting.Ingredient;

/**
 * @param carriesData whether the exact stack supplied for this requirement decides what the craft produces, which
 *     makes the machine prefer the item the player is holding over the first one it finds.
 */
public record ManualRequirement(Ingredient ingredient, int count, boolean consume, boolean carriesData) {

    public ManualRequirement(Ingredient ingredient, int count, boolean consume) {
        this(ingredient, count, consume, false);
    }
}
