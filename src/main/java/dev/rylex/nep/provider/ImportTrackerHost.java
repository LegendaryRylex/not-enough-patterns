package dev.rylex.nep.provider;

import appeng.api.crafting.IPatternDetails;

public interface ImportTrackerHost {

    void nepRecordOwed(IPatternDetails pattern, OwedSource source);
}
