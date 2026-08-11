package dev.rylex.nep.pattern.encoding;

import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

public interface PatternRecipeHolder {

    @Nullable
    Identifier nep$recipeId();

    void nep$setRecipeId(@Nullable Identifier recipe);

    default int nep$encodingVersion() {
        return 0;
    }
}
