package dev.rylex.nep.provider;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.KeyCounter;
import org.jetbrains.annotations.Nullable;

public interface ImportTrackerHost {

    void nepRecordOwed(IPatternDetails pattern, @Nullable KeyCounter[] inputs, OwedSource source);
}
