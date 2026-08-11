package dev.rylex.nep.pattern;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.core.definitions.AEItems;
import appeng.helpers.IPatternTerminalLogicHost;
import appeng.parts.encoding.PatternEncodingLogic;
import dev.rylex.nep.NepComponents;
import dev.rylex.nep.NepGameTests;
import dev.rylex.nep.NepItems;
import dev.rylex.nep.pattern.encoding.PatternContents;
import dev.rylex.nep.pattern.encoding.PatternRecipeHolder;
import dev.rylex.nep.pattern.encoding.ProcessingPatternConversionRecipe;
import dev.rylex.nep.util.Recipes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public final class ProcessingPatternConversionGameTest {

    private static final Identifier SOURCE = Identifier.parse("test:conversion");

    private static ItemStack keeping(List<GenericStack> inputs, List<GenericStack> retained, GenericStack result) {
        ItemStack stack = new ItemStack(NepItems.INFUSION_PATTERN.get());
        stack.set(
                NepComponents.ENCODED_INFUSION_PATTERN.get(),
                new EncodedRecipePattern(SOURCE, inputs, retained, result));
        return stack;
    }

    private ProcessingPatternConversionGameTest() {}

    public static void register(NepGameTests.Batch batch) {
        batch.add(
                        "the_conversion_recipe_is_loaded_from_the_datapack",
                        ProcessingPatternConversionGameTest::theConversionRecipeIsLoadedFromTheDatapack)
                .add(
                        "a_consuming_pattern_converts_with_its_ingredients_intact",
                        ProcessingPatternConversionGameTest::aConsumingPatternConvertsWithItsIngredientsIntact)
                .add(
                        "a_pattern_that_keeps_an_ingredient_does_not_convert",
                        ProcessingPatternConversionGameTest::aPatternThatKeepsAnIngredientDoesNotConvert)
                .add(
                        "two_patterns_at_once_do_not_convert",
                        ProcessingPatternConversionGameTest::twoPatternsAtOnceDoNotConvert)
                .add(
                        "only_a_pattern_that_can_convert_advertises_the_recipe",
                        ProcessingPatternConversionGameTest::onlyAPatternThatCanConvertAdvertisesTheRecipe)
                .add(
                        "a_converted_pattern_remembers_the_recipe_it_came_from",
                        ProcessingPatternConversionGameTest::aConvertedPatternRemembersTheRecipeItCameFrom)
                .add(
                        "a_converted_pattern_put_back_into_a_terminal_can_be_encoded_again",
                        ProcessingPatternConversionGameTest::aConvertedPatternPutBackIntoATerminalCanBeEncodedAgain)
                .add(
                        "an_ordinary_processing_pattern_teaches_a_terminal_nothing",
                        ProcessingPatternConversionGameTest::anOrdinaryProcessingPatternTeachesATerminalNothing)
                .add("an_ae2_pattern_is_left_alone", ProcessingPatternConversionGameTest::anAe2PatternIsLeftAlone);
    }

    private static GenericStack stack(Item item, long amount) {
        return new GenericStack(AEItemKey.of(item), amount);
    }

    private static CraftingInput grid(ItemStack... contents) {
        List<ItemStack> items = new ArrayList<>(Collections.nCopies(9, ItemStack.EMPTY));
        for (int slot = 0; slot < contents.length; slot++) {
            items.set(slot, contents[slot]);
        }
        return CraftingInput.of(3, 3, items);
    }

    private static Optional<RecipeHolder<CraftingRecipe>> lookup(GameTestHelper helper, CraftingInput input) {
        ServerLevel level = helper.getLevel();
        return level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, level);
    }

    public static void theConversionRecipeIsLoadedFromTheDatapack(GameTestHelper helper) {
        boolean present = Recipes.of(helper.getLevel()).values().stream()
                .anyMatch(holder -> holder.value() instanceof ProcessingPatternConversionRecipe);
        helper.assertTrue(
                present,
                "no pattern conversion recipe was loaded; a NEP pattern for a machine the pack has moved could not be"
                        + " turned back into a processing pattern");
        helper.succeed();
    }

    public static void aConsumingPatternConvertsWithItsIngredientsIntact(GameTestHelper helper) {
        ItemStack pattern = InfusionPattern.encode(
                SOURCE, List.of(stack(Items.OBSIDIAN, 2), stack(Items.REDSTONE, 8)), stack(Items.DIAMOND, 1));

        CraftingInput input = grid(pattern);
        RecipeHolder<CraftingRecipe> holder =
                lookup(helper, input).orElseThrow(() -> new AssertionError("no crafting recipe matched the pattern"));

        ItemStack converted = holder.value().assemble(input);
        helper.assertTrue(
                converted.is(AEItems.PROCESSING_PATTERN.asItem()),
                "converting a NEP pattern produced " + converted + " rather than an AE2 processing pattern");

        IPatternDetails details = PatternDetailsHelper.decodePattern(converted, helper.getLevel());
        helper.assertTrue(details != null, "the converted pattern did not decode as a pattern at all");
        helper.assertValueEqual(
                PatternContents.condenseInputs(details),
                List.of(stack(Items.OBSIDIAN, 2), stack(Items.REDSTONE, 8)),
                "converted pattern ingredients");
        helper.assertValueEqual(details.getOutputs(), List.of(stack(Items.DIAMOND, 1)), "converted pattern result");
        helper.succeed();
    }

    public static void aPatternThatKeepsAnIngredientDoesNotConvert(GameTestHelper helper) {
        ItemStack pattern =
                keeping(List.of(stack(Items.OBSIDIAN, 1)), List.of(stack(Items.IRON_AXE, 1)), stack(Items.DIAMOND, 1));

        helper.assertTrue(
                lookup(helper, grid(pattern)).isEmpty(),
                "a deploying or filling pattern converted; the processing pattern it produced would ask the network"
                        + " for a fresh tool on every craft");
        helper.succeed();
    }

    public static void twoPatternsAtOnceDoNotConvert(GameTestHelper helper) {
        ItemStack first = InfusionPattern.encode(SOURCE, List.of(stack(Items.OBSIDIAN, 1)), stack(Items.DIAMOND, 1));
        ItemStack second = InfusionPattern.encode(SOURCE, List.of(stack(Items.REDSTONE, 1)), stack(Items.EMERALD, 1));

        helper.assertTrue(
                lookup(helper, grid(first, second)).isEmpty(),
                "two patterns in one grid converted, so one of them was consumed without producing anything");
        helper.succeed();
    }

    public static void onlyAPatternThatCanConvertAdvertisesTheRecipe(GameTestHelper helper) {
        ItemStack convertible =
                InfusionPattern.encode(SOURCE, List.of(stack(Items.OBSIDIAN, 2)), stack(Items.DIAMOND, 1));
        ItemStack keepsATool =
                keeping(List.of(stack(Items.OBSIDIAN, 1)), List.of(stack(Items.IRON_AXE, 1)), stack(Items.DIAMOND, 1));

        helper.assertTrue(
                ProcessingPatternConversionRecipe.converts(convertible),
                "a pattern that converts did not advertise the recipe, so its tooltip stays silent about the only way"
                        + " back to a processing pattern");
        helper.assertTrue(
                !ProcessingPatternConversionRecipe.converts(keepsATool),
                "a pattern that keeps an ingredient advertised the recipe, so its tooltip promises a conversion the"
                        + " crafting grid refuses");
        helper.succeed();
    }

    public static void aConvertedPatternRemembersTheRecipeItCameFrom(GameTestHelper helper) {
        ItemStack pattern = InfusionPattern.encode(SOURCE, List.of(stack(Items.OBSIDIAN, 2)), stack(Items.DIAMOND, 1));

        ItemStack converted = ProcessingPatternConversionRecipe.convert(pattern);
        helper.assertTrue(!converted.isEmpty(), "the pattern did not convert at all");
        helper.assertValueEqual(
                converted.get(NepComponents.SOURCE_RECIPE.get()),
                SOURCE,
                "the recipe a converted processing pattern came from");
        helper.succeed();
    }

    public static void aConvertedPatternPutBackIntoATerminalCanBeEncodedAgain(GameTestHelper helper) {
        ItemStack pattern = InfusionPattern.encode(SOURCE, List.of(stack(Items.OBSIDIAN, 2)), stack(Items.DIAMOND, 1));
        ItemStack converted = ProcessingPatternConversionRecipe.convert(pattern);
        helper.assertTrue(!converted.isEmpty(), "the pattern did not convert at all");

        PatternEncodingLogic logic = new Terminal(helper.getLevel()).getLogic();
        logic.getEncodedPatternInv().setItemDirect(0, converted);

        helper.assertValueEqual(
                ((PatternRecipeHolder) logic).nep$recipeId(),
                SOURCE,
                "the recipe a terminal recovered from a converted pattern; without it the terminal only re-encodes"
                        + " plain processing patterns until the recipe is pulled from the recipe list again");
        helper.succeed();
    }

    public static void anOrdinaryProcessingPatternTeachesATerminalNothing(GameTestHelper helper) {
        ItemStack processing = PatternDetailsHelper.encodeProcessingPattern(
                List.of(stack(Items.OBSIDIAN, 1)), List.of(stack(Items.DIAMOND, 1)));

        PatternEncodingLogic logic = new Terminal(helper.getLevel()).getLogic();
        logic.getEncodedPatternInv().setItemDirect(0, processing);

        helper.assertTrue(
                ((PatternRecipeHolder) logic).nep$recipeId() == null,
                "a hand-made processing pattern claimed a recipe, so encoding it would produce a NEP pattern for a"
                        + " recipe nobody chose");
        helper.succeed();
    }

    public static void anAe2PatternIsLeftAlone(GameTestHelper helper) {
        ItemStack processing = PatternDetailsHelper.encodeProcessingPattern(
                List.of(stack(Items.OBSIDIAN, 1)), List.of(stack(Items.DIAMOND, 1)));

        helper.assertTrue(
                ProcessingPatternConversionRecipe.convert(processing).isEmpty(),
                "an AE2 processing pattern was accepted by the conversion recipe, which would re-encode it for no"
                        + " reason");
        helper.succeed();
    }

    private static final class Terminal implements IPatternTerminalLogicHost {

        private final Level level;
        private final PatternEncodingLogic logic;

        private Terminal(Level level) {
            this.level = level;
            this.logic = new PatternEncodingLogic(this);
        }

        @Override
        public PatternEncodingLogic getLogic() {
            return logic;
        }

        @Override
        public Level getLevel() {
            return level;
        }

        @Override
        public void markForSave() {}
    }
}
