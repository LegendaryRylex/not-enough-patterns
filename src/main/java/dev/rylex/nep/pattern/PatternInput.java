package dev.rylex.nep.pattern;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

final class PatternInput implements IPatternDetails.IInput {

    private final GenericStack[] template;
    private final long multiplier;
    private final boolean retained;
    private final boolean wears;

    @Nullable
    private final InputSubstitution substitution;

    PatternInput(AEKey what, long amount, boolean retained, boolean wears, @Nullable InputSubstitution substitution) {
        this.template = new GenericStack[] {new GenericStack(what, 1)};
        this.multiplier = amount;
        this.retained = retained;
        this.wears = wears;
        this.substitution = substitution;
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
        return input.matches(template[0])
                || (wears && ToolWear.differsOnlyByDamage(template[0].what(), input))
                || (substitution != null && substitution.accepts(template[0].what(), input, level));
    }

    @Nullable
    @Override
    public AEKey getRemainingKey(AEKey template) {
        if (!retained) {
            return null;
        }
        return wears ? ToolWear.worn(template) : template;
    }
}
