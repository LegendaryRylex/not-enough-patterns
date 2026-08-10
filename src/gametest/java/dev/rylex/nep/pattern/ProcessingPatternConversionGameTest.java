package dev.rylex.nep.pattern;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.core.definitions.AEItems;
import appeng.helpers.IPatternTerminalLogicHost;
import appeng.parts.encoding.PatternEncodingLogic;
import dev.rylex.nep.Nep;
import dev.rylex.nep.NepComponents;
import dev.rylex.nep.pattern.encoding.PatternContents;
import dev.rylex.nep.pattern.encoding.PatternRecipeHolder;
import dev.rylex.nep.pattern.encoding.ProcessingPatternConversionRecipe;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Nep.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ProcessingPatternConversionGameTest {

    private static final String TEMPLATE = "empty_5x5x5";
    private static final String BATCH = "nep_pattern_conversion";

    private static final ResourceLocation SOURCE = ResourceLocation.parse("test:conversion");

    private ProcessingPatternConversionGameTest() {}

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
        return level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level);
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void theConversionRecipeIsLoadedFromTheDatapack(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        boolean present = level.getRecipeManager().getRecipes().stream()
                .anyMatch(holder -> holder.value() instanceof ProcessingPatternConversionRecipe);
        helper.assertTrue(
                present,
                "no pattern conversion recipe was loaded; a NEP pattern for a machine the pack has moved could not be"
                        + " turned back into a processing pattern");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void aConsumingPatternConvertsWithItsIngredientsIntact(GameTestHelper helper) {
        ItemStack pattern = AndesiteCraftingPattern.encode(
                SOURCE, List.of(stack(Items.OBSIDIAN, 2), stack(Items.REDSTONE, 8)), stack(Items.DIAMOND, 1));

        CraftingInput input = grid(pattern);
        RecipeHolder<CraftingRecipe> holder =
                lookup(helper, input).orElseThrow(() -> new AssertionError("no crafting recipe matched the pattern"));

        ItemStack converted = holder.value().assemble(input, helper.getLevel().registryAccess());
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

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void aPatternThatKeepsAnIngredientDoesNotConvert(GameTestHelper helper) {
        ItemStack pattern = AndesiteCraftingPattern.encode(
                SOURCE, List.of(stack(Items.OBSIDIAN, 1)), List.of(stack(Items.IRON_AXE, 1)), stack(Items.DIAMOND, 1));

        helper.assertTrue(
                lookup(helper, grid(pattern)).isEmpty(),
                "a deploying or filling pattern converted; the processing pattern it produced would ask the network"
                        + " for a fresh tool on every craft");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void twoPatternsAtOnceDoNotConvert(GameTestHelper helper) {
        ItemStack first =
                AndesiteCraftingPattern.encode(SOURCE, List.of(stack(Items.OBSIDIAN, 1)), stack(Items.DIAMOND, 1));
        ItemStack second =
                AndesiteCraftingPattern.encode(SOURCE, List.of(stack(Items.REDSTONE, 1)), stack(Items.EMERALD, 1));

        helper.assertTrue(
                lookup(helper, grid(first, second)).isEmpty(),
                "two patterns in one grid converted, so one of them was consumed without producing anything");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void onlyAPatternThatCanConvertAdvertisesTheRecipe(GameTestHelper helper) {
        ItemStack convertible =
                AndesiteCraftingPattern.encode(SOURCE, List.of(stack(Items.OBSIDIAN, 2)), stack(Items.DIAMOND, 1));
        ItemStack keepsATool = AndesiteCraftingPattern.encode(
                SOURCE, List.of(stack(Items.OBSIDIAN, 1)), List.of(stack(Items.IRON_AXE, 1)), stack(Items.DIAMOND, 1));

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

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void aConvertedPatternRemembersTheRecipeItCameFrom(GameTestHelper helper) {
        ItemStack pattern =
                AndesiteCraftingPattern.encode(SOURCE, List.of(stack(Items.OBSIDIAN, 2)), stack(Items.DIAMOND, 1));

        ItemStack converted = ProcessingPatternConversionRecipe.convert(pattern);
        helper.assertTrue(!converted.isEmpty(), "the pattern did not convert at all");
        helper.assertValueEqual(
                converted.get(NepComponents.SOURCE_RECIPE.get()),
                SOURCE,
                "the recipe a converted processing pattern came from");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void aConvertedPatternPutBackIntoATerminalCanBeEncodedAgain(GameTestHelper helper) {
        ItemStack pattern =
                AndesiteCraftingPattern.encode(SOURCE, List.of(stack(Items.OBSIDIAN, 2)), stack(Items.DIAMOND, 1));
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

    @GameTest(template = TEMPLATE, batch = BATCH)
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

    @GameTest(template = TEMPLATE, batch = BATCH)
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
