package dev.rylex.nep.pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import appeng.api.stacks.AEItemKey;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

class GridPlanTest {

    @Test
    void filledIndicesSkipEmptyCells() {
        AEItemKey iron = AEItemKey.of(Items.IRON_INGOT);
        GridPlan plan = new GridPlan(2, 2, Arrays.asList(iron, null, null, iron));

        assertEquals(List.of(0, 3), plan.filledIndices());
    }

    @Test
    void multisetCountsDuplicateKeys() {
        AEItemKey iron = AEItemKey.of(Items.IRON_INGOT);
        AEItemKey gold = AEItemKey.of(Items.GOLD_INGOT);
        GridPlan plan = new GridPlan(2, 2, Arrays.asList(iron, gold, null, iron));

        assertEquals(Map.of(iron, 2L, gold, 1L), plan.multiset());
    }

    @Test
    void anEmptyPlanHasNoFilledCells() {
        GridPlan plan = new GridPlan(1, 1, Arrays.asList((AEItemKey) null));
        assertTrue(plan.filledIndices().isEmpty());
        assertTrue(plan.multiset().isEmpty());
    }
}
