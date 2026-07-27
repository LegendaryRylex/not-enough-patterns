package dev.rylex.nep.compat.create;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class StationLayoutsTest {

    private static final StationKind D = StationKind.DEPLOYER;
    private static final StationKind P = StationKind.PRESS;
    private static final StationKind S = StationKind.SPOUT;
    private static final StationKind W = StationKind.SAW;
    private static final StationKind GONE = StationKind.UNKNOWN;

    @Test
    void theSurvivingStationsPinDownWhatBelongsInTheGap() {
        List<StationKind> hints = StationLayouts.expected(
                List.of(List.of(D, P, D), List.of(D, S, D), List.of(S, S, D)), List.of(D, GONE, D));

        assertEquals(
                List.of(GONE, GONE, GONE), hints, "two layouts fit the surviving Deployers and disagree on the gap");
    }

    @Test
    void oneFittingLayoutNamesTheGap() {
        List<StationKind> hints =
                StationLayouts.expected(List.of(List.of(D, P, D), List.of(S, S, D)), List.of(D, GONE, D));

        assertEquals(List.of(GONE, P, GONE), hints);
    }

    @Test
    void everyGapIsAnsweredOnItsOwn() {
        List<StationKind> hints = StationLayouts.expected(List.of(List.of(D, P, S, W)), List.of(GONE, P, GONE, W));

        assertEquals(List.of(D, GONE, S, GONE), hints, "each broken slot names the station that belongs there");
    }

    @Test
    void layoutsOfAnotherLengthAreNotCandidates() {
        List<StationKind> hints =
                StationLayouts.expected(List.of(List.of(D, P), List.of(D, P, D, P)), List.of(D, GONE, D));

        assertEquals(List.of(GONE, GONE, GONE), hints, "a line only runs a recipe whose station count it matches");
    }

    @Test
    void aRecognizedStationNeverGetsAHint() {
        List<StationKind> hints = StationLayouts.expected(List.of(List.of(D, P)), List.of(S, GONE));

        assertEquals(List.of(GONE, GONE), hints, "the Spout contradicts the only layout, so nothing may be claimed");
    }
}
