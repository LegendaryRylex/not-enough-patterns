package dev.rylex.nep.compat.malum;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.Nullable;

final class ResolverSupport {
    private ResolverSupport() {}

    @Nullable
    static <T extends Recipe<?>> RecipeHolder<T> byId(List<RecipeHolder<T>> candidates, ResourceLocation recipe) {
        for (RecipeHolder<T> candidate : candidates) {
            if (candidate.id().equals(recipe)) {
                return candidate;
            }
        }
        return null;
    }
}
