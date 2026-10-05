package dev.rylex.nep.compat.malum;

import appeng.core.definitions.AEBlocks;
import com.sammy.malum.common.recipe.SpiritFocusingRecipe;
import com.sammy.malum.common.recipe.SpiritInfusionRecipe;
import dev.rylex.nep.Nep;
import dev.rylex.nep.machine.ManualCraftFixtures;
import dev.rylex.nep.machine.ManualCraftOutcome;
import dev.rylex.nep.machine.ManualCraftResult;
import dev.rylex.nep.machine.ManualRequirement;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

@GameTestHolder(Nep.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FocusedSpiritMatrixManualCraftGameTest {

    private static final String TEMPLATE = "empty_5x5x5";
    private static final String BATCH = "nep_focused_spirit_matrix_manual";

    private static final BlockPos MATRIX = new BlockPos(2, 1, 2);
    private static final BlockPos ME_CONTROLLER = new BlockPos(2, 1, 3);
    private static final BlockPos ENERGY_CELL = ME_CONTROLLER.above();

    private FocusedSpiritMatrixManualCraftGameTest() {}

    private static FocusedSpiritMatrixBlockEntity placeMatrix(GameTestHelper helper) {
        SpiritInfusionResolver.clearCache();
        SpiritFocusingResolver.clearCache();
        helper.setBlock(ME_CONTROLLER, AEBlocks.CONTROLLER.block());
        helper.setBlock(ENERGY_CELL, AEBlocks.CREATIVE_ENERGY_CELL.block().defaultBlockState());
        helper.setBlock(MATRIX, NepMalumContent.MATRIX.get().defaultBlockState());
        FocusedSpiritMatrixBlockEntity matrix =
                helper.getBlockEntity(MATRIX) instanceof FocusedSpiritMatrixBlockEntity be ? be : null;
        helper.assertTrue(matrix != null, "the Focused Spirit Matrix has no block entity");
        return matrix;
    }

    private static RecipeHolder<SpiritInfusionRecipe> handStartableInfusion(GameTestHelper helper) {
        RecipeHolder<SpiritInfusionRecipe> best = null;
        for (RecipeHolder<SpiritInfusionRecipe> holder : SpiritInfusionResolver.candidates(helper.getLevel())) {
            if (!holder.id().getNamespace().equals("malum")
                    || !holder.value().extraInputs.isEmpty()
                    || MalumRecipeIngredients.manualSpiritInfusion(holder.value()) == null) {
                continue;
            }
            if (best == null || holder.id().compareTo(best.id()) < 0) {
                best = holder;
            }
        }
        helper.assertTrue(best != null, "no spirit infusion recipe could be started by hand");
        return best;
    }

    private static RecipeHolder<SpiritInfusionRecipe> carryOverInfusion(GameTestHelper helper) {
        RecipeHolder<SpiritInfusionRecipe> best = null;
        for (RecipeHolder<SpiritInfusionRecipe> holder : SpiritInfusionResolver.candidates(helper.getLevel())) {
            if (!holder.id().getNamespace().equals("malum")
                    || !holder.value().carryOverComponentData
                    || MalumRecipeIngredients.manualSpiritInfusion(holder.value()) == null) {
                continue;
            }
            if (best == null || holder.id().compareTo(best.id()) < 0) {
                best = holder;
            }
        }
        helper.assertTrue(best != null, "no spirit infusion recipe carries component data over");
        return best;
    }

    private static RecipeHolder<SpiritFocusingRecipe> handStartableFocusing(GameTestHelper helper) {
        RecipeHolder<SpiritFocusingRecipe> best = null;
        for (RecipeHolder<SpiritFocusingRecipe> holder : SpiritFocusingResolver.candidates(helper.getLevel())) {
            if (!holder.id().getNamespace().equals("malum")
                    || MalumRecipeIngredients.manualSpiritFocusing(holder.value()) == null) {
                continue;
            }
            if (best == null || holder.value().time < best.value().time) {
                best = holder;
            }
        }
        helper.assertTrue(best != null, "no spirit focusing recipe could be started by hand");
        return best;
    }

    private static boolean outputHolds(FocusedSpiritMatrixBlockEntity matrix, ItemStack expected, int count) {
        int found = 0;
        IItemHandler output = matrix.getOutputBuffer();
        for (int slot = 0; slot < output.getSlots(); slot++) {
            ItemStack stack = output.getStackInSlot(slot);
            if (ItemStack.isSameItem(stack, expected)) {
                found += stack.getCount();
            }
        }
        return found >= count;
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 400)
    public static void aHandStartedInfusionStagesItsSpiritsInTheBank(GameTestHelper helper) {
        FocusedSpiritMatrixBlockEntity matrix = placeMatrix(helper);
        RecipeHolder<SpiritInfusionRecipe> holder = handStartableInfusion(helper);
        List<ManualRequirement> requirements = MalumRecipeIngredients.manualSpiritInfusion(holder.value());
        Player player = ManualCraftFixtures.playerWith(helper, requirements);

        ManualCraftOutcome outcome = matrix.startManualCraft(player, holder.id(), 1);

        ManualCraftFixtures.assertQueued(helper, outcome, "the Focused Spirit Matrix");
        helper.assertTrue(
                ManualCraftFixtures.inventoryCount(player) == 0, "the Matrix left ingredients in the inventory");
        helper.assertTrue(
                ManualCraftFixtures.bufferedCount(matrix.getSpiritBank()) > 0,
                "the Matrix took the spirits without staging them in the bank");
        helper.assertTrue(
                ManualCraftFixtures.bufferedCount(matrix.getInputBuffer()) > 0,
                "the Matrix took the item input without staging it in the input buffer");

        ItemStack result = holder.value().result;
        helper.succeedWhen(() -> helper.assertTrue(
                outputHolds(matrix, result, 1), "the Matrix never finished the hand-started infusion"));
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void refusesAnInfusionTheInventoryCannotPayFor(GameTestHelper helper) {
        FocusedSpiritMatrixBlockEntity matrix = placeMatrix(helper);
        RecipeHolder<SpiritInfusionRecipe> holder = handStartableInfusion(helper);

        ManualCraftOutcome outcome = matrix.startManualCraft(helper.makeMockPlayer(GameType.SURVIVAL), holder.id(), 1);

        helper.assertTrue(
                outcome.status() == ManualCraftResult.MISSING_ITEMS,
                "an empty inventory did not read as missing items: " + outcome.status());
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void refusesFocusingByHandWithoutAMatrixImpetus(GameTestHelper helper) {
        FocusedSpiritMatrixBlockEntity matrix = placeMatrix(helper);
        RecipeHolder<SpiritFocusingRecipe> holder = handStartableFocusing(helper);
        List<ManualRequirement> requirements = MalumRecipeIngredients.manualSpiritFocusing(holder.value());
        Player player = ManualCraftFixtures.playerWith(helper, requirements);

        ManualCraftOutcome outcome = matrix.startManualCraft(player, holder.id(), 1);

        helper.assertTrue(
                outcome.status() == ManualCraftResult.NO_IMPETUS,
                "an empty impetus slot did not refuse a hand-started focusing: " + outcome.status());
        helper.assertTrue(
                ManualCraftFixtures.inventoryCount(player) > 0, "the refused focusing still took the player's spirits");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 1200)
    public static void aHandStartedFocusingRunsOnTheMatrixImpetus(GameTestHelper helper) {
        FocusedSpiritMatrixBlockEntity matrix = placeMatrix(helper);
        helper.assertTrue(
                matrix.getImpetusSlot()
                        .insertItem(0, new ItemStack(NepMalumContent.MATRIX_IMPETUS.get()), false)
                        .isEmpty(),
                "the impetus slot would not take a Matrix Impetus");
        RecipeHolder<SpiritFocusingRecipe> holder = handStartableFocusing(helper);
        List<ManualRequirement> requirements = MalumRecipeIngredients.manualSpiritFocusing(holder.value());
        Player player = ManualCraftFixtures.playerWith(helper, requirements);

        ManualCraftOutcome outcome = matrix.startManualCraft(player, holder.id(), 1);

        ManualCraftFixtures.assertQueued(helper, outcome, "the Focused Spirit Matrix");
        helper.assertTrue(
                matrix.getInputBuffer().getStackInSlot(0).isEmpty(),
                "focusing has no item input, but something was staged in the input buffer");

        ItemStack result = holder.value().output;
        helper.succeedWhen(() -> helper.assertTrue(
                outputHolds(matrix, result, 1), "the Matrix never finished the hand-started focusing"));
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 400)
    public static void aCarryOverInfusionKeepsTheHeldItemsComponents(GameTestHelper helper) {
        FocusedSpiritMatrixBlockEntity matrix = placeMatrix(helper);
        RecipeHolder<SpiritInfusionRecipe> holder = carryOverInfusion(helper);
        List<ManualRequirement> requirements = MalumRecipeIngredients.manualSpiritInfusion(holder.value());
        Player player = ManualCraftFixtures.playerWith(helper, requirements);

        int carrier = -1;
        for (int slot = 0; slot < player.getInventory().items.size(); slot++) {
            if (holder.value().input.ingredient().test(player.getInventory().getItem(slot))) {
                carrier = slot;
                break;
            }
        }
        helper.assertTrue(carrier >= 0, "the player was not given the item the recipe carries data from");
        ItemStack marked = player.getInventory().getItem(carrier);
        marked.set(DataComponents.CUSTOM_NAME, Component.literal("Carried"));
        player.getInventory().selected = carrier;

        ManualCraftOutcome outcome = matrix.startManualCraft(player, holder.id(), 1);

        ManualCraftFixtures.assertQueued(helper, outcome, "the Focused Spirit Matrix");
        helper.succeedWhen(() -> {
            ItemStack finished = ItemStack.EMPTY;
            IItemHandler output = matrix.getOutputBuffer();
            for (int slot = 0; slot < output.getSlots(); slot++) {
                if (!output.getStackInSlot(slot).isEmpty()) {
                    finished = output.getStackInSlot(slot);
                    break;
                }
            }
            helper.assertTrue(!finished.isEmpty(), "the Matrix never finished the carry-over infusion");
            helper.assertTrue(
                    finished.has(DataComponents.CUSTOM_NAME),
                    "the result lost the component data the recipe was supposed to carry over");
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void aCarryOverInfusionQueuesOneCraftAtATime(GameTestHelper helper) {
        FocusedSpiritMatrixBlockEntity matrix = placeMatrix(helper);
        RecipeHolder<SpiritInfusionRecipe> holder = carryOverInfusion(helper);
        List<ManualRequirement> requirements = MalumRecipeIngredients.manualSpiritInfusion(holder.value());
        Player player = ManualCraftFixtures.playerWith(helper, requirements, 3);

        ManualCraftOutcome outcome = matrix.startManualCraft(player, holder.id(), 3);

        ManualCraftFixtures.assertQueued(helper, outcome, "the Focused Spirit Matrix");
        helper.assertTrue(
                ManualCraftFixtures.inventoryCount(player) > 0,
                "a carry-over infusion took ingredients for more than the one craft it queued");
        helper.succeed();
    }
}
