package dev.rylex.nep.pattern.encoding;

import appeng.api.crafting.IPatternDetails;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface PatternConverter<R extends Recipe<?>> {

    @Nullable
    ItemStack convert(IPatternDetails encoded, RecipeHolder<R> recipe, Level level);
}
