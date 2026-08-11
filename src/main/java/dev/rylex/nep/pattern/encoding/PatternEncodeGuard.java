package dev.rylex.nep.pattern.encoding;

import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public interface PatternEncodeGuard {

    void nep$expectEncoded(@Nullable ItemStack pattern);
}
