package dev.rylex.nep.pattern;

import appeng.api.stacks.AEKey;
import net.minecraft.world.level.Level;

@FunctionalInterface
public interface InputSubstitution {

    boolean accepts(AEKey template, AEKey candidate, Level level);
}
