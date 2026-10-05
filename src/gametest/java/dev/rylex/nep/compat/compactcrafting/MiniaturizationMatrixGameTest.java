package dev.rylex.nep.compat.compactcrafting;

import appeng.api.AECapabilities;
import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.core.definitions.AEBlocks;
import dev.compactmods.crafting.recipes.MiniaturizationRecipe;
import dev.rylex.nep.ConfigOverrides;
import dev.rylex.nep.Nep;
import dev.rylex.nep.machine.ManualCraftFixtures;
import dev.rylex.nep.machine.ManualCraftOutcome;
import dev.rylex.nep.machine.ManualCraftResult;
import dev.rylex.nep.machine.ManualRequirement;
import dev.rylex.nep.pattern.MiniaturizationPattern;
import dev.rylex.nep.pattern.encoding.EncodedIngredients;
import dev.rylex.nep.pattern.encoding.PatternConverters;
import dev.rylex.nep.pattern.encoding.PatternOrigin;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

@GameTestHolder(Nep.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MiniaturizationMatrixGameTest {

    private static final String TEMPLATE = "empty_5x5x5";
    private static final String BATCH = "nep_miniaturization_matrix";

    private static final BlockPos MATRIX = new BlockPos(2, 1, 2);
    private static final BlockPos ME_CONTROLLER = new BlockPos(2, 1, 3);
    private static final BlockPos ENERGY_CELL = ME_CONTROLLER.above();

    private MiniaturizationMatrixGameTest() {}

    private static MiniaturizationMatrixBlockEntity place(GameTestHelper helper) {
        helper.setBlock(MATRIX, NepCompactCraftingContent.MATRIX.get().defaultBlockState());
        BlockEntity be = helper.getBlockEntity(MATRIX);
        helper.assertTrue(be instanceof MiniaturizationMatrixBlockEntity, "the matrix did not create its block entity");
        return (MiniaturizationMatrixBlockEntity) be;
    }

    private static void powerUp(GameTestHelper helper) {
        helper.setBlock(ME_CONTROLLER, AEBlocks.CONTROLLER.block());
        helper.setBlock(ENERGY_CELL, AEBlocks.CREATIVE_ENERGY_CELL.block().defaultBlockState());
    }

    private static RecipeHolder<MiniaturizationRecipe> matrixRecipe(GameTestHelper helper) {
        RecipeHolder<MiniaturizationRecipe> holder =
                MiniaturizationRecipeResolver.resolveById(helper.getLevel(), Nep.id("miniaturization_matrix"));
        helper.assertTrue(
                holder != null,
                "nep:miniaturization_matrix did not load as a miniaturization recipe, so the Matrix cannot be crafted");
        return holder;
    }

    @Nullable
    private static RecipeHolder<MiniaturizationRecipe> shortestRecipe(GameTestHelper helper) {
        RecipeHolder<MiniaturizationRecipe> shortest = null;
        for (RecipeHolder<MiniaturizationRecipe> candidate :
                MiniaturizationRecipeResolver.candidates(helper.getLevel())) {
            if (MiniaturizationRecipeIngredients.miniaturization(candidate, helper.getLevel()) == null) {
                continue;
            }
            if (shortest == null
                    || candidate.value().getCraftingTime() < shortest.value().getCraftingTime()) {
                shortest = candidate;
            }
        }
        return shortest;
    }

    private static IPatternDetails patternFor(GameTestHelper helper, RecipeHolder<MiniaturizationRecipe> holder) {
        EncodedIngredients expected = MiniaturizationRecipeIngredients.miniaturization(holder, helper.getLevel());
        helper.assertTrue(expected != null, "the recipe produced no ingredient list");
        List<GenericStack> inputs = new ArrayList<>();
        for (List<GenericStack> slot : expected.inputs()) {
            inputs.add(slot.get(0));
        }
        ItemStack encoded = MiniaturizationPattern.encode(
                holder.id(), inputs, expected.outputs().get(0));
        IPatternDetails details = PatternDetailsHelper.decodePattern(encoded, helper.getLevel());
        helper.assertTrue(details != null, "the miniaturization pattern did not decode");
        return details;
    }

    private static KeyCounter[] inputsOf(IPatternDetails details) {
        KeyCounter[] inputs = new KeyCounter[details.getInputs().length];
        for (int slot = 0; slot < inputs.length; slot++) {
            inputs[slot] = new KeyCounter();
            IPatternDetails.IInput input = details.getInputs()[slot];
            inputs[slot].add(input.getPossibleInputs()[0].what(), input.getMultiplier());
        }
        return inputs;
    }

    private static int countIn(IItemHandler handler, Item item) {
        int total = 0;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack held = handler.getStackInSlot(slot);
            if (held.is(item)) {
                total += held.getCount();
            }
        }
        return total;
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void theMatrixIsBuiltByARealMiniaturizationCraft(GameTestHelper helper) {
        RecipeHolder<MiniaturizationRecipe> holder = matrixRecipe(helper);
        ItemStack[] outputs = holder.value().getOutputs();
        helper.assertTrue(
                outputs.length == 1
                        && ItemStack.isSameItem(outputs[0], new ItemStack(NepCompactCraftingContent.MATRIX_ITEM.get())),
                "the nep:miniaturization_matrix recipe does not produce a Miniaturization Matrix");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void aRecipeCostsOneItemPerBlockItsLayersFill(GameTestHelper helper) {
        RecipeHolder<MiniaturizationRecipe> holder = matrixRecipe(helper);
        EncodedIngredients encoded = MiniaturizationRecipeIngredients.miniaturization(holder, helper.getLevel());
        helper.assertTrue(encoded != null, "the Matrix's own recipe did not encode");

        long quartzGlass = 0;
        for (List<GenericStack> slot : encoded.inputs()) {
            GenericStack option = slot.get(0);
            if (option.what() instanceof AEItemKey key
                    && key.getItem() == AEBlocks.QUARTZ_GLASS.block().asItem()) {
                quartzGlass = option.amount();
            }
        }
        helper.assertTrue(
                quartzGlass == 73,
                "a solid 25-block floor plus the three 16-block rings above it should cost 73 quartz glass, not "
                        + quartzGlass + "; the layer totals are not being counted");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void exposesItsMachineCapabilities(GameTestHelper helper) {
        place(helper);
        BlockPos pos = helper.absolutePos(MATRIX);

        helper.assertTrue(
                helper.getLevel().getCapability(AECapabilities.CRAFTING_MACHINE, pos, null) != null,
                "the matrix exposed no crafting machine capability");
        helper.assertTrue(
                helper.getLevel().getCapability(AECapabilities.IN_WORLD_GRID_NODE_HOST, pos, null) != null,
                "the matrix exposed no in-world grid node host, so it can never join a network");
        helper.assertTrue(
                helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, pos, null) != null,
                "the matrix exposed no item handler, so nothing can restock it by hand");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void aPushedPatternStagesItsIngredients(GameTestHelper helper) {
        MiniaturizationMatrixBlockEntity matrix = place(helper);
        IPatternDetails details = patternFor(helper, matrixRecipe(helper));

        helper.assertTrue(
                matrix.pushMatrixPattern(details, inputsOf(details), Direction.UP),
                "a miniaturization pattern was rejected");

        long staged = 0;
        for (int slot = 0; slot < matrix.getInputBuffer().getSlots(); slot++) {
            staged += matrix.getInputBuffer().getStackInSlot(slot).getCount();
        }
        long expected = 0;
        for (IPatternDetails.IInput input : details.getInputs()) {
            expected += input.getPossibleInputs()[0].amount() * input.getMultiplier();
        }
        helper.assertTrue(staged == expected, "staged " + staged + " items but the pattern carried " + expected);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void aNonMiniaturizationPatternIsRefused(GameTestHelper helper) {
        MiniaturizationMatrixBlockEntity matrix = place(helper);
        ItemStack encoded = PatternDetailsHelper.encodeProcessingPattern(
                List.of(new GenericStack(AEItemKey.of(Items.DIAMOND), 1)),
                List.of(new GenericStack(AEItemKey.of(Items.NETHERITE_BLOCK), 1)));
        IPatternDetails details = PatternDetailsHelper.decodePattern(encoded, helper.getLevel());
        helper.assertTrue(details != null, "the test pattern did not decode");

        helper.assertTrue(
                !matrix.pushMatrixPattern(details, inputsOf(details), Direction.UP),
                "the matrix accepted a pattern that is not a miniaturization pattern");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void hoppersCannotStageIngredientsForNoJob(GameTestHelper helper) {
        MiniaturizationMatrixBlockEntity matrix = place(helper);
        IItemHandler handler =
                helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(MATRIX), null);
        helper.assertTrue(handler != null, "the matrix exposed no item handler");

        for (int slot = 0; slot < matrix.getInputBuffer().getSlots(); slot++) {
            helper.assertTrue(
                    handler.insertItem(slot, new ItemStack(Items.OBSIDIAN, 4), false)
                                    .getCount()
                            == 4,
                    "a hopper staged ingredients in slot " + slot + " for a matrix that owes nothing");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void anUnnetworkedMatrixNeverConsumesItsInput(GameTestHelper helper) {
        MiniaturizationMatrixBlockEntity matrix = place(helper);
        IPatternDetails details = patternFor(helper, matrixRecipe(helper));

        helper.assertTrue(
                matrix.pushMatrixPattern(details, inputsOf(details), Direction.UP),
                "a miniaturization pattern was rejected by an idle matrix");

        helper.runAfterDelay(40, () -> {
            helper.assertTrue(
                    matrix.activeResult().isEmpty(),
                    "a matrix with no ME network started a craft; it must stay idle until it is on a powered grid");
            helper.assertTrue(
                    matrix.getOutputBuffer().getStackInSlot(0).isEmpty(),
                    "a matrix with no ME network produced an output");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 600)
    public static void aPoweredMatrixCompletesTheCraft(GameTestHelper helper) {
        RecipeHolder<MiniaturizationRecipe> holder = shortestRecipe(helper);
        helper.assertTrue(holder != null, "no miniaturization recipe encodes, so this test proves nothing");

        MiniaturizationMatrixBlockEntity matrix = place(helper);
        powerUp(helper);
        IPatternDetails details = patternFor(helper, holder);
        ItemStack wanted = holder.value().getOutputs()[0];

        helper.assertTrue(
                matrix.pushMatrixPattern(details, inputsOf(details), Direction.UP),
                "a miniaturization pattern was rejected");

        int ticks = holder.value().getCraftingTime() + 60;
        helper.runAfterDelay(ticks, () -> {
            helper.assertTrue(
                    countIn(matrix.getOutputBuffer(), wanted.getItem()) >= wanted.getCount(),
                    "a powered matrix produced nothing after " + ticks + " ticks for " + holder.id() + " (stall "
                            + matrix.stall() + ", refusal " + matrix.refusal() + ")");
            helper.assertTrue(!matrix.hasPending(), "the matrix still owes a craft it was only asked for once");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = "nep_miniaturization_field_ceiling")
    public static void aRecipeAboveTheFieldCeilingIsRefused(GameTestHelper helper) {
        ConfigOverrides.Restore restore =
                ConfigOverrides.override("COMPACT_CRAFTING_MATRIX_MAXIMUM_FIELD_SIZE", "small");
        try {
            RecipeHolder<MiniaturizationRecipe> oversized = null;
            for (RecipeHolder<MiniaturizationRecipe> candidate :
                    MiniaturizationRecipeResolver.candidates(helper.getLevel())) {
                if (MiniaturizationRecipeIngredients.miniaturization(candidate, helper.getLevel()) != null
                        && !MiniaturizationMatrixBlockEntity.fitsFieldCeiling(candidate.value())) {
                    oversized = candidate;
                    break;
                }
            }
            helper.assertTrue(
                    oversized != null,
                    "every loaded recipe fits a small field, so the ceiling this test checks is never exercised");

            MiniaturizationMatrixBlockEntity matrix = place(helper);
            IPatternDetails details = patternFor(helper, oversized);
            helper.assertTrue(
                    !matrix.pushMatrixPattern(details, inputsOf(details), Direction.UP),
                    "the matrix took a recipe needing a bigger field than the configured maximum");
            helper.assertTrue(
                    matrix.refusal() == MiniaturizationMatrixBlockEntity.Refusal.FIELD_TOO_LARGE,
                    "the matrix refused the pattern as " + matrix.refusal() + " instead of FIELD_TOO_LARGE");
            helper.succeed();
        } finally {
            restore.undo();
        }
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void aJeiTransferredRecipeEncodesAsAMiniaturizationPattern(GameTestHelper helper) {
        RecipeHolder<MiniaturizationRecipe> holder = matrixRecipe(helper);
        EncodedIngredients expected = MiniaturizationRecipeIngredients.miniaturization(holder, helper.getLevel());
        helper.assertTrue(expected != null, "the recipe produced no ingredient list");

        List<GenericStack> shown = new ArrayList<>();
        for (List<GenericStack> slot : expected.inputs()) {
            shown.add(slot.get(0));
        }
        ItemStack processing = PatternDetailsHelper.encodeProcessingPattern(
                shown, List.of(expected.outputs().get(0)));

        ItemStack converted = PatternConverters.convert(
                PatternOrigin.ofRecipe(holder.id()), processing, helper.makeMockPlayer(GameType.SURVIVAL));
        helper.assertTrue(
                converted != null && !converted.isEmpty(),
                "a processing pattern carrying a miniaturization recipe, as JEI transfers it, would not encode");

        IPatternDetails details = PatternDetailsHelper.decodePattern(converted, helper.getLevel());
        helper.assertTrue(
                details instanceof MiniaturizationPattern, "the converted pattern is not a miniaturization pattern");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void queuesAMiniaturizationCraftFromThePlayersInventory(GameTestHelper helper) {
        MiniaturizationMatrixBlockEntity matrix = place(helper);
        powerUp(helper);
        RecipeHolder<MiniaturizationRecipe> holder = matrixRecipe(helper);
        List<ManualRequirement> requirements = MiniaturizationRecipeIngredients.manualRequirements(holder.value());
        helper.assertTrue(requirements != null, "the Matrix recipe cannot be expressed as manual requirements");
        Player player = ManualCraftFixtures.playerWith(helper, requirements);

        ManualCraftOutcome outcome = matrix.startManualCraft(player, holder.id(), 1);

        ManualCraftFixtures.assertQueued(helper, outcome, "the Miniaturization Matrix");
        helper.assertTrue(
                ManualCraftFixtures.inventoryCount(player) == 0, "the Matrix left ingredients in the inventory");
        helper.assertTrue(
                ManualCraftFixtures.bufferedCount(matrix.getInputBuffer()) > 0,
                "the Matrix took the ingredients without staging them");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void refusesAMiniaturizationCraftTheInventoryCannotPayFor(GameTestHelper helper) {
        MiniaturizationMatrixBlockEntity matrix = place(helper);
        powerUp(helper);
        RecipeHolder<MiniaturizationRecipe> holder = matrixRecipe(helper);

        ManualCraftOutcome outcome = matrix.startManualCraft(helper.makeMockPlayer(GameType.SURVIVAL), holder.id(), 1);

        helper.assertTrue(
                outcome.status() == ManualCraftResult.MISSING_ITEMS,
                "an empty inventory did not read as missing items: " + outcome.status());
        helper.succeed();
    }
}
