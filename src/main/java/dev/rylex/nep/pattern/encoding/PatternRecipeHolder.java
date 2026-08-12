package dev.rylex.nep.pattern.encoding;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public interface PatternRecipeHolder {

    PatternOrigin nep$origin();

    void nep$setOrigin(PatternOrigin origin);

    @Nullable
    default ResourceLocation nep$recipeId() {
        return nep$origin().recipe();
    }

    default void nep$setRecipeId(@Nullable ResourceLocation recipe) {
        nep$setOrigin(PatternOrigin.ofRecipe(recipe));
    }

    default int nep$encodingVersion() {
        return 0;
    }
}
