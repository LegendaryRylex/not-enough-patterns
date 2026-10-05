package dev.rylex.nep.compat.malum;

import appeng.api.AECapabilities;
import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import com.sammy.malum.common.block.curiosities.runic_workbench.RunicWorkbenchBlockEntity;
import com.sammy.malum.common.recipe.RuneworkingRecipe;
import com.sammy.malum.registry.common.block.MalumBlocks;
import dev.rylex.nep.Nep;
import dev.rylex.nep.pattern.RuneworkingPattern;
import dev.rylex.nep.pattern.encoding.EncodedIngredients;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

@GameTestHolder(Nep.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RunicWorkbenchCraftingMachineGameTest {

    private static final String TEMPLATE = "empty_9x6x9";
    private static final String BATCH = "nep_runic_workbench";

    private static final BlockPos WORKBENCH = new BlockPos(4, 1, 4);
    private static final BlockPos RETURN_TARGET = WORKBENCH.north();

    private RunicWorkbenchCraftingMachineGameTest() {}

    private static RunicWorkbenchBlockEntity placeWorkbench(GameTestHelper helper) {
        RunicWorkbenchCraftingMachine.clearCache();
        RuneworkingResolver.clearCache();
        helper.setBlock(WORKBENCH, MalumBlocks.RUNIC_WORKBENCH.get().defaultBlockState());
        RunicWorkbenchBlockEntity workbench =
                helper.getBlockEntity(WORKBENCH) instanceof RunicWorkbenchBlockEntity be ? be : null;
        helper.assertTrue(workbench != null, "the Runic Workbench has no block entity");
        return workbench;
    }

    private static ICraftingMachine machine(GameTestHelper helper) {
        ICraftingMachine machine =
                helper.getLevel().getCapability(AECapabilities.CRAFTING_MACHINE, helper.absolutePos(WORKBENCH), null);
        helper.assertTrue(machine != null, "the Runic Workbench exposed no crafting machine capability");
        return machine;
    }

    private static RecipeHolder<RuneworkingRecipe> aRecipe(GameTestHelper helper) {
        RecipeHolder<RuneworkingRecipe> best = null;
        for (RecipeHolder<RuneworkingRecipe> holder : RuneworkingResolver.candidates(helper.getLevel())) {
            if (!holder.id().getNamespace().equals("malum")
                    || MalumRecipeIngredients.runeworking(holder.value()) == null) {
                continue;
            }
            if (best == null || holder.id().compareTo(best.id()) < 0) {
                best = holder;
            }
        }
        helper.assertTrue(best != null, "no runeworking recipe was loaded");
        return best;
    }

    private static IPatternDetails patternFor(GameTestHelper helper, RecipeHolder<RuneworkingRecipe> holder) {
        EncodedIngredients expected = MalumRecipeIngredients.runeworking(holder.value());
        helper.assertTrue(expected != null, "the runeworking recipe produced no ingredient list");

        List<GenericStack> inputs = new ArrayList<>();
        for (List<GenericStack> options : expected.inputs()) {
            inputs.add(options.get(0));
        }
        ItemStack encoded = RuneworkingPattern.encode(
                holder.id(), inputs, expected.outputs().get(0));
        IPatternDetails details = PatternDetailsHelper.decodePattern(encoded, helper.getLevel());
        helper.assertTrue(details != null, "the runeworking pattern did not decode");
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

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void aWorkbenchTakesBothIngredientsInOnePush(GameTestHelper helper) {
        RunicWorkbenchBlockEntity workbench = placeWorkbench(helper);

        RecipeHolder<RuneworkingRecipe> holder = aRecipe(helper);
        IPatternDetails details = patternFor(helper, holder);
        helper.assertTrue(
                machine(helper).pushPattern(details, inputsOf(details), Direction.NORTH),
                "the workbench refused a runeworking pattern it had nothing in the way of");
        helper.assertTrue(
                ((RunicWorkbenchState) workbench).nep$isCrafting(),
                "the push was accepted but the workbench never started shaping; the second ingredient a player "
                        + "would be holding was not supplied");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void aWorkbenchAlreadyHoldingSomethingRefuses(GameTestHelper helper) {
        RunicWorkbenchBlockEntity workbench = placeWorkbench(helper);
        workbench.inventory.setStackInSlot(0, new ItemStack(Items.DIAMOND));

        IPatternDetails details = patternFor(helper, aRecipe(helper));
        helper.assertTrue(
                !machine(helper).pushPattern(details, inputsOf(details), Direction.NORTH),
                "a workbench with a player's item on it accepted a pattern and would have overwritten it");
        helper.assertTrue(
                workbench.inventory.getStackInSlot(0).is(Items.DIAMOND),
                "a refused push replaced what was already on the bench");
        helper.assertTrue(!((RunicWorkbenchState) workbench).nep$isCrafting(), "a refused push started a craft anyway");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void aWorkbenchAlreadyShapingARuneRefusesASecond(GameTestHelper helper) {
        RunicWorkbenchBlockEntity workbench = placeWorkbench(helper);

        RecipeHolder<RuneworkingRecipe> holder = aRecipe(helper);
        helper.assertTrue(
                machine(helper)
                        .pushPattern(patternFor(helper, holder), inputsOf(patternFor(helper, holder)), Direction.NORTH),
                "the workbench refused the first runeworking pattern");
        helper.assertTrue(((RunicWorkbenchState) workbench).nep$isCrafting(), "the first push never started a craft");

        helper.assertTrue(
                !machine(helper)
                        .pushPattern(patternFor(helper, holder), inputsOf(patternFor(helper, holder)), Direction.NORTH),
                "a workbench mid-craft accepted a second rune and would have discarded the one in flight");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 400)
    public static void aFinishedRuneGoesBackToTheProvider(GameTestHelper helper) {
        placeWorkbench(helper);
        helper.setBlock(RETURN_TARGET, Blocks.BARREL.defaultBlockState());

        RecipeHolder<RuneworkingRecipe> holder = aRecipe(helper);
        IPatternDetails details = patternFor(helper, holder);
        helper.assertTrue(
                machine(helper).pushPattern(details, inputsOf(details), Direction.NORTH),
                "the runeworking pattern was rejected");

        ItemStack expected = holder.value().output;
        helper.succeedWhen(() -> {
            IItemHandler barrel = helper.getLevel()
                    .getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(RETURN_TARGET), null);
            helper.assertTrue(barrel != null, "the return target exposed no inventory");
            for (int slot = 0; slot < barrel.getSlots(); slot++) {
                if (ItemStack.isSameItem(barrel.getStackInSlot(slot), expected)) {
                    return;
                }
            }
            helper.fail("the finished rune never reached the inventory the pattern was pushed from");
        });
    }
}
