package dev.rylex.nep.pattern.encoding;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import dev.rylex.nep.MinecraftBootstrap;
import java.util.List;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MinecraftBootstrap.class)
class IngredientMatchingTest {

    private static GenericStack stack(Item item, long amount) {
        return new GenericStack(AEItemKey.of(item), amount);
    }

    private static List<GenericStack> slot(GenericStack... options) {
        return List.of(options);
    }

    @Test
    void assignReportsTheChosenOptionInOriginalSlotOrder() {
        List<List<GenericStack>> slots = List.of(
                slot(stack(Items.NETHER_STAR, 1)),
                slot(stack(Items.IRON_INGOT, 1), stack(Items.GOLD_INGOT, 1)),
                slot(stack(Items.GOLD_INGOT, 1)));

        GenericStack[] chosen = IngredientMatching.assign(
                slots, List.of(stack(Items.NETHER_STAR, 1), stack(Items.IRON_INGOT, 1), stack(Items.GOLD_INGOT, 1)));

        assertNotNull(chosen);
        assertArrayEquals(
                new GenericStack[] {stack(Items.NETHER_STAR, 1), stack(Items.IRON_INGOT, 1), stack(Items.GOLD_INGOT, 1)
                },
                chosen);
    }

    @Test
    void assignBacktracksSoAGreedyFirstPickDoesNotStrandALaterSlot() {
        List<List<GenericStack>> slots =
                List.of(slot(stack(Items.IRON_INGOT, 1), stack(Items.GOLD_INGOT, 1)), slot(stack(Items.GOLD_INGOT, 1)));

        GenericStack[] chosen =
                IngredientMatching.assign(slots, List.of(stack(Items.IRON_INGOT, 1), stack(Items.GOLD_INGOT, 1)));

        assertNotNull(chosen);
        assertArrayEquals(new GenericStack[] {stack(Items.IRON_INGOT, 1), stack(Items.GOLD_INGOT, 1)}, chosen);
    }

    @Test
    void assignHonoursASlotThatNeedsSeveralOfOneItem() {
        List<List<GenericStack>> slots = List.of(slot(stack(Items.DIAMOND, 4)), slot(stack(Items.IRON_INGOT, 1)));

        GenericStack[] chosen =
                IngredientMatching.assign(slots, List.of(stack(Items.DIAMOND, 4), stack(Items.IRON_INGOT, 1)));

        assertNotNull(chosen);
        assertArrayEquals(new GenericStack[] {stack(Items.DIAMOND, 4), stack(Items.IRON_INGOT, 1)}, chosen);
    }

    @Test
    void assignRejectsLeftoverInputs() {
        List<List<GenericStack>> slots = List.of(slot(stack(Items.IRON_INGOT, 1)));

        assertNull(IngredientMatching.assign(slots, List.of(stack(Items.IRON_INGOT, 1), stack(Items.GOLD_INGOT, 1))));
    }

    @Test
    void assignRejectsAShortfall() {
        List<List<GenericStack>> slots = List.of(slot(stack(Items.IRON_INGOT, 4)));

        assertNull(IngredientMatching.assign(slots, List.of(stack(Items.IRON_INGOT, 3))));
    }
}
