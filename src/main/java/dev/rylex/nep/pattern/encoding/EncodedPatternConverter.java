package dev.rylex.nep.pattern.encoding;

import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public interface EncodedPatternConverter {

    @Nullable
    ItemStack nep$convertEncoded(@Nullable ItemStack encoded);

    void nep$encodeFinished();
}
