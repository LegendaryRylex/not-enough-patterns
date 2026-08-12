package dev.rylex.nep.pattern.encoding;

import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

public interface PatternRecipeHolder {

    PatternOrigin nep$origin();

    void nep$setOrigin(PatternOrigin origin);

    @Nullable
    default Identifier nep$recipeId() {
        return nep$origin().recipe();
    }

    default void nep$setRecipeId(@Nullable Identifier recipe) {
        nep$setOrigin(PatternOrigin.ofRecipe(recipe));
    }

    default int nep$encodingVersion() {
        return 0;
    }
}
