package dev.rylex.nep.pattern;

import appeng.api.crafting.IPatternDetails;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public interface NepPattern extends IPatternDetails {

    @Nullable
    default ResourceLocation nepRecipeId() {
        return null;
    }
}
