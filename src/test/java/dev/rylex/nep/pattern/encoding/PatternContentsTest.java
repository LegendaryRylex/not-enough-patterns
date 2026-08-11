package dev.rylex.nep.pattern.encoding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import dev.rylex.nep.MinecraftBootstrap;
import java.util.List;
import java.util.Set;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MinecraftBootstrap.class)
class PatternContentsTest {

    private record StubInput(GenericStack[] possible, long multiplier, boolean retained)
            implements IPatternDetails.IInput {

        StubInput(GenericStack[] possible, long multiplier) {
            this(possible, multiplier, false);
        }

        @Override
        public GenericStack[] getPossibleInputs() {
            return possible;
        }

        @Override
        public long getMultiplier() {
            return multiplier;
        }

        @Override
        public boolean isValid(AEKey input, Level level) {
            return true;
        }

        @Override
        public AEKey getRemainingKey(AEKey template) {
            return retained ? template : null;
        }
    }

    private static IPatternDetails details(IPatternDetails.IInput... inputs) {
        return new IPatternDetails() {
            @Override
            public AEItemKey getDefinition() {
                return AEItemKey.of(Items.PAPER);
            }

            @Override
            public IInput[] getInputs() {
                return inputs;
            }

            @Override
            public List<GenericStack> getOutputs() {
                return List.of(new GenericStack(AEItemKey.of(Items.DIAMOND), 1));
            }
        };
    }

    @Test
    void condenseMergesDuplicateKeysAndMultipliesAmounts() {
        AEItemKey iron = AEItemKey.of(Items.IRON_INGOT);
        List<GenericStack> condensed = PatternContents.condenseInputs(details(
                new StubInput(new GenericStack[] {new GenericStack(iron, 2)}, 3),
                new StubInput(new GenericStack[] {new GenericStack(iron, 1)}, 4)));

        assertEquals(List.of(new GenericStack(iron, 10L)), condensed);
    }

    @Test
    void condenseSkipsInputsWithoutOptions() {
        AEItemKey iron = AEItemKey.of(Items.IRON_INGOT);
        List<GenericStack> condensed = PatternContents.condenseInputs(details(
                new StubInput(new GenericStack[0], 1),
                new StubInput(new GenericStack[] {new GenericStack(iron, 1)}, 1)));

        assertEquals(List.of(new GenericStack(iron, 1L)), condensed);
    }

    @Test
    void condenseOfNoInputsIsEmpty() {
        assertTrue(PatternContents.condenseInputs(details()).isEmpty());
    }

    @Test
    void condenseKeepsARetainedInputApartFromTheSameConsumedKey() {
        AEItemKey iron = AEItemKey.of(Items.IRON_INGOT);
        List<GenericStack> condensed = PatternContents.condenseInputs(details(
                new StubInput(new GenericStack[] {new GenericStack(iron, 1)}, 2),
                new StubInput(new GenericStack[] {new GenericStack(iron, 1)}, 1, true)));

        assertEquals(List.of(new GenericStack(iron, 2L), new GenericStack(iron, 1L)), condensed);
    }

    @Test
    void condenseSlotsMergesWithinEachGroupOnly() {
        AEItemKey iron = AEItemKey.of(Items.IRON_INGOT);
        AEItemKey gold = AEItemKey.of(Items.GOLD_INGOT);
        List<GenericStack> condensed = PatternContents.condenseSlots(
                java.util.Arrays.asList(
                        new GenericStack(iron, 1),
                        null,
                        new GenericStack(gold, 1),
                        new GenericStack(iron, 2),
                        new GenericStack(iron, 3)),
                Set.of(3));

        assertEquals(
                List.of(new GenericStack(iron, 4L), new GenericStack(gold, 1L), new GenericStack(iron, 2L)), condensed);
    }

    @Test
    void condenseListMergesRepeatedKeysInFirstSeenOrder() {
        AEItemKey iron = AEItemKey.of(Items.IRON_INGOT);
        AEItemKey gold = AEItemKey.of(Items.GOLD_INGOT);
        List<GenericStack> condensed = PatternContents.condense(List.of(
                new GenericStack(iron, 1),
                new GenericStack(gold, 4),
                new GenericStack(iron, 2),
                new GenericStack(iron, 1)));

        assertEquals(List.of(new GenericStack(iron, 4L), new GenericStack(gold, 4L)), condensed);
    }

    @Test
    void condenseOfEmptyListIsEmpty() {
        assertTrue(PatternContents.condense(List.of()).isEmpty());
    }
}
