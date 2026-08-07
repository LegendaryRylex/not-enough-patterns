package dev.rylex.nep.pattern.encoding;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public interface PatternRecipeHolder {

    @Nullable
    ResourceLocation nep$recipeId();

    void nep$setRecipeId(@Nullable ResourceLocation recipe);

    default int nep$encodingVersion() {
        return 0;
    }
}
