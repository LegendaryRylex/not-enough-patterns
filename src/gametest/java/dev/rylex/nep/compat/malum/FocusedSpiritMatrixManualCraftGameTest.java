package dev.rylex.nep.compat.malum;

import static dev.rylex.nep.compat.malum.MalumFixtures.outputHolds;

import com.sammy.malum.common.recipe.SpiritFocusingRecipe;
import com.sammy.malum.common.recipe.SpiritInfusionRecipe;
import dev.rylex.nep.Nep;
import dev.rylex.nep.machine.ManualCraftFixtures;
import dev.rylex.nep.machine.ManualCraftOutcome;
import dev.rylex.nep.machine.ManualCraftResult;
import dev.rylex.nep.machine.ManualRequirement;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
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
        return MalumFixtures.placeMatrix(helper, ME_CONTROLLER, ENERGY_CELL, MATRIX);
    }

    private static RecipeHolder<SpiritInfusionRecipe> handStartableInfusion(GameTestHelper helper) {
        return infusion(
                helper, recipe -> recipe.extraInputs.isEmpty(), "no spirit infusion recipe could be started by hand");
    }

    private static RecipeHolder<SpiritInfusionRecipe> carryOverInfusion(GameTestHelper helper) {
        return infusion(
                helper,
                recipe -> recipe.carryOverComponentData,
                "no spirit infusion recipe carries component data over");
    }

    private static RecipeHolder<SpiritInfusionRecipe> infusion(
            GameTestHelper helper, Predicate<SpiritInfusionRecipe> wanted, String failure) {
        return MalumFixtures.pickBest(
                helper,
                SpiritInfusionResolver.candidates(helper.getLevel()),
                holder -> holder.id().getNamespace().equals("malum")
                        && wanted.test(holder.value())
                        && MalumRecipeIngredients.manualSpiritInfusion(holder.value()) != null,
                MalumFixtures.byId(),
                failure);
    }

    private static RecipeHolder<SpiritFocusingRecipe> handStartableFocusing(GameTestHelper helper) {
        return MalumFixtures.pickBest(
                helper,
                SpiritFocusingResolver.candidates(helper.getLevel()),
                holder -> holder.id().getNamespace().equals("malum")
                        && MalumRecipeIngredients.manualSpiritFocusing(holder.value()) != null,
                Comparator.comparingInt(holder -> holder.value().time),
                "no spirit focusing recipe could be started by hand");
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
