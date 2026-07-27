package dev.rylex.nep.pattern.encoding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import java.util.List;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

class PatternContentsTest {

    private record StubInput(GenericStack[] possible, long multiplier) implements IPatternDetails.IInput {
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
            return null;
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
}
