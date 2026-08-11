package dev.rylex.nep.pattern.encoding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.crafting.pattern.AEProcessingPattern;
import dev.rylex.nep.MinecraftBootstrap;
import dev.rylex.nep.pattern.EncodedRecipePattern;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MinecraftBootstrap.class)
class ProcessingPatternConversionTest {

    private static final Identifier RECIPE = Identifier.parse("nep:test_recipe");

    private static GenericStack stack(Item item, long amount) {
        return new GenericStack(AEItemKey.of(item), amount);
    }

    private static EncodedRecipePattern recipe(List<GenericStack> inputs, List<GenericStack> retained) {
        return new EncodedRecipePattern(RECIPE, inputs, retained, stack(Items.DIAMOND, 1));
    }

    @Test
    void aConsumingPatternKeepsItsIngredientsAndResult() {
        ProcessingPatternConversion.Conversion conversion =
                ProcessingPatternConversion.of(recipe(List.of(stack(Items.OBSIDIAN, 2), stack(Items.REDSTONE, 8))));

        assertNotNull(conversion);
        assertEquals(List.of(stack(Items.OBSIDIAN, 2), stack(Items.REDSTONE, 8)), conversion.inputs());
        assertEquals(List.of(stack(Items.DIAMOND, 1)), conversion.outputs());
    }

    @Test
    void aPatternThatKeepsAnIngredientIsRefusedRatherThanSilentlyConsumingIt() {
        assertNull(
                ProcessingPatternConversion.of(
                        recipe(List.of(stack(Items.OBSIDIAN, 1)), List.of(stack(Items.IRON_AXE, 1)))),
                "a deploying or filling pattern converted to a processing pattern would request a fresh tool on every"
                        + " craft, so it must not convert at all");
    }

    @Test
    void repeatedIngredientsAddUpIntoOneSlot() {
        ProcessingPatternConversion.Conversion conversion = ProcessingPatternConversion.of(
                recipe(List.of(stack(Items.IRON_INGOT, 1), stack(Items.IRON_INGOT, 1), stack(Items.GOLD_INGOT, 1))));

        assertNotNull(conversion);
        assertEquals(List.of(stack(Items.IRON_INGOT, 2), stack(Items.GOLD_INGOT, 1)), conversion.inputs());
    }

    @Test
    void aPatternWithNoIngredientsHasNothingToConvert() {
        assertNull(ProcessingPatternConversion.of(recipe(List.of())));
    }

    @Test
    void aPatternWiderThanAProcessingPatternCanHoldIsRefused() {
        List<GenericStack> inputs = BuiltInRegistries.ITEM.stream()
                .map(AEItemKey::of)
                .filter(java.util.Objects::nonNull)
                .limit(AEProcessingPattern.MAX_INPUT_SLOTS + 1L)
                .map(key -> new GenericStack(key, 1))
                .toList();
        assertEquals(
                AEProcessingPattern.MAX_INPUT_SLOTS + 1,
                inputs.size(),
                "the item registry is too small to build an over-wide pattern");

        assertNull(
                ProcessingPatternConversion.of(recipe(inputs)),
                "a pattern past the processing pattern ceiling would silently lose ingredients");
    }

    private static EncodedRecipePattern recipe(List<GenericStack> inputs) {
        return recipe(inputs, List.of());
    }
}
