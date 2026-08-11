package dev.rylex.nep.pattern;

import appeng.api.crafting.IPatternDetails;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

public interface NepPattern extends IPatternDetails {

    @Nullable
    default Identifier nepRecipeId() {
        return null;
    }
}
