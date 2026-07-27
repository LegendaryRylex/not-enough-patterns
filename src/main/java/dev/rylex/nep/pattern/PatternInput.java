package dev.rylex.nep.pattern;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

final class PatternInput implements IPatternDetails.IInput {

    private final GenericStack[] template;
    private final long multiplier;

    PatternInput(AEKey what, long amount) {
        this.template = new GenericStack[] {new GenericStack(what, 1)};
        this.multiplier = amount;
    }

    @Override
    public GenericStack[] getPossibleInputs() {
        return template;
    }

    @Override
    public long getMultiplier() {
        return multiplier;
    }

    @Override
    public boolean isValid(AEKey input, Level level) {
        return input.matches(template[0]);
    }

    @Nullable
    @Override
    public AEKey getRemainingKey(AEKey template) {
        return null;
    }
}
