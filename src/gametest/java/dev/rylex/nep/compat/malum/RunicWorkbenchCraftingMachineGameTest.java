package dev.rylex.nep.compat.malum;

import static dev.rylex.nep.GameTestFixtures.inputsOf;

import appeng.api.crafting.IPatternDetails;
import appeng.api.implementations.blockentities.ICraftingMachine;
import com.sammy.malum.common.block.curiosities.runic_workbench.RunicWorkbenchBlockEntity;
import com.sammy.malum.common.recipe.RuneworkingRecipe;
import com.sammy.malum.registry.common.block.MalumBlocks;
import dev.rylex.nep.GameTestFixtures;
import dev.rylex.nep.Nep;
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
        return GameTestFixtures.craftingMachine(
                helper, WORKBENCH, "the Runic Workbench exposed no crafting machine capability");
    }

    private static RecipeHolder<RuneworkingRecipe> aRecipe(GameTestHelper helper) {
        return MalumFixtures.pickBest(
                helper,
                RuneworkingResolver.candidates(helper.getLevel()),
                holder -> holder.id().getNamespace().equals("malum")
                        && MalumRecipeIngredients.runeworking(holder.value()) != null,
                MalumFixtures.byId(),
                "no runeworking recipe was loaded");
    }

    private static IPatternDetails patternFor(GameTestHelper helper, RecipeHolder<RuneworkingRecipe> holder) {
        return MalumFixtures.runeworkingPattern(helper, holder);
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
        IPatternDetails details = patternFor(helper, holder);
        helper.assertTrue(
                machine(helper).pushPattern(details, inputsOf(details), Direction.NORTH),
                "the workbench refused the first runeworking pattern");
        helper.assertTrue(((RunicWorkbenchState) workbench).nep$isCrafting(), "the first push never started a craft");

        helper.assertTrue(
                !machine(helper).pushPattern(details, inputsOf(details), Direction.NORTH),
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
            helper.assertTrue(
                    GameTestFixtures.holds(barrel, expected, 1),
                    "the finished rune never reached the inventory the pattern was pushed from");
        });
    }
}
