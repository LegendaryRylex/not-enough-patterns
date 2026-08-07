package dev.rylex.nep.compat.actuallyadditions;

import appeng.api.AECapabilities;
import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import de.ellpeck.actuallyadditions.mod.blocks.ActuallyBlocks;
import de.ellpeck.actuallyadditions.mod.crafting.EmpowererRecipe;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityDisplayStand;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityEmpowerer;
import dev.rylex.nep.Nep;
import dev.rylex.nep.pattern.EmpoweringPattern;
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
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

@GameTestHolder(Nep.MOD_ID)
@PrefixGameTestTemplate(false)
public final class EmpowererCraftingMachineGameTest {

    private static final String TEMPLATE = "empty_9x6x9";
    private static final String BATCH = "nep_empowerer";

    private static final BlockPos EMPOWERER = new BlockPos(4, 1, 4);

    private EmpowererCraftingMachineGameTest() {}

    private static TileEntityEmpowerer placeEmpowerer(GameTestHelper helper) {
        helper.setBlock(EMPOWERER, ActuallyBlocks.EMPOWERER.get().defaultBlockState());
        return (TileEntityEmpowerer) helper.getBlockEntity(EMPOWERER);
    }

    private static List<TileEntityDisplayStand> placeStands(GameTestHelper helper, int count) {
        List<TileEntityDisplayStand> stands = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            BlockPos pos = EMPOWERER.relative(Direction.from2DDataValue(index), 3);
            helper.setBlock(pos, ActuallyBlocks.DISPLAY_STAND.get().defaultBlockState());
            stands.add((TileEntityDisplayStand) helper.getBlockEntity(pos));
        }
        return stands;
    }

    private static ICraftingMachine machine(GameTestHelper helper) {
        ICraftingMachine machine =
                helper.getLevel().getCapability(AECapabilities.CRAFTING_MACHINE, helper.absolutePos(EMPOWERER), null);
        helper.assertTrue(machine != null, "the Empowerer exposed no crafting machine capability");
        return machine;
    }

    private static RecipeHolder<EmpowererRecipe> quickestRecipe(GameTestHelper helper) {
        RecipeHolder<EmpowererRecipe> best = null;
        for (RecipeHolder<EmpowererRecipe> holder :
                ActuallyAdditionsRecipeResolver.empoweringCandidates(helper.getLevel())) {
            if (ActuallyAdditionsRecipeIngredients.empowering(holder.value()) == null) {
                continue;
            }
            if (best == null || holder.value().getTime() < best.value().getTime()) {
                best = holder;
            }
        }
        helper.assertTrue(best != null, "no empowering recipe was loaded");
        return best;
    }

    private static IPatternDetails patternFor(GameTestHelper helper, RecipeHolder<EmpowererRecipe> holder, long batch) {
        EncodedIngredients expected = ActuallyAdditionsRecipeIngredients.empowering(holder.value());
        helper.assertTrue(expected != null, "the recipe produced no ingredient list");

        List<GenericStack> inputs = new ArrayList<>();
        for (List<GenericStack> options : expected.inputs()) {
            GenericStack option = options.get(0);
            inputs.add(new GenericStack(option.what(), option.amount() * batch));
        }
        GenericStack result = expected.outputs().get(0);

        ItemStack encoded =
                EmpoweringPattern.encode(holder.id(), inputs, new GenericStack(result.what(), result.amount() * batch));
        IPatternDetails details = PatternDetailsHelper.decodePattern(encoded, helper.getLevel());
        helper.assertTrue(details != null, "the empowering pattern did not decode");
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

    private static void charge(List<TileEntityDisplayStand> stands) {
        for (TileEntityDisplayStand stand : stands) {
            for (int i = 0; i < 100; i++) {
                stand.storage.receiveEnergy(Integer.MAX_VALUE, false);
            }
        }
    }

    private static void assertNothingStranded(GameTestHelper helper, TileEntityEmpowerer empowerer) {
        helper.assertTrue(empowerer.inv.getStackInSlot(0).isEmpty(), "a refused push left an item in the Empowerer");
        for (int index = 0; index < 4; index++) {
            BlockPos pos = EMPOWERER.relative(Direction.from2DDataValue(index), 3);
            if (helper.getLevel().getBlockEntity(helper.absolutePos(pos)) instanceof TileEntityDisplayStand stand) {
                helper.assertTrue(stand.getStack().isEmpty(), "a refused push left an item on a Display Stand");
            }
        }
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void aFullStandArrayStagesTheBaseAndEveryModifier(GameTestHelper helper) {
        EmpowererCraftingMachine.clearCache();
        ActuallyAdditionsRecipeResolver.clearCache();

        TileEntityEmpowerer empowerer = placeEmpowerer(helper);
        List<TileEntityDisplayStand> stands = placeStands(helper, 4);
        charge(stands);

        RecipeHolder<EmpowererRecipe> holder = quickestRecipe(helper);
        IPatternDetails details = patternFor(helper, holder, 1);

        helper.assertTrue(
                machine(helper).pushPattern(details, inputsOf(details), Direction.UP),
                "an empowering pattern with four stands in place was rejected");
        helper.assertTrue(!empowerer.inv.getStackInSlot(0).isEmpty(), "the base item was not staged");
        for (TileEntityDisplayStand stand : stands) {
            helper.assertTrue(!stand.getStack().isEmpty(), "a Display Stand was left empty");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void aMissingStandIsRefusedWithoutStrandingItems(GameTestHelper helper) {
        EmpowererCraftingMachine.clearCache();
        ActuallyAdditionsRecipeResolver.clearCache();

        TileEntityEmpowerer empowerer = placeEmpowerer(helper);
        charge(placeStands(helper, 3));

        IPatternDetails details = patternFor(helper, quickestRecipe(helper), 1);
        helper.assertTrue(
                !machine(helper).pushPattern(details, inputsOf(details), Direction.UP),
                "an Empowerer missing a Display Stand accepted a pattern");
        assertNothingStranded(helper, empowerer);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void anOccupiedStandIsRefusedWithoutStrandingItems(GameTestHelper helper) {
        EmpowererCraftingMachine.clearCache();
        ActuallyAdditionsRecipeResolver.clearCache();

        TileEntityEmpowerer empowerer = placeEmpowerer(helper);
        List<TileEntityDisplayStand> stands = placeStands(helper, 4);
        charge(stands);
        stands.get(0).inv.setStackInSlot(0, new ItemStack(Items.STONE));

        IPatternDetails details = patternFor(helper, quickestRecipe(helper), 1);
        helper.assertTrue(
                !machine(helper).pushPattern(details, inputsOf(details), Direction.UP),
                "an Empowerer with an occupied Display Stand accepted a pattern");
        helper.assertTrue(empowerer.inv.getStackInSlot(0).isEmpty(), "a refused push left an item in the Empowerer");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void aMultipliedPatternIsRefusedBecauseEverySlotHoldsOneItem(GameTestHelper helper) {
        EmpowererCraftingMachine.clearCache();
        ActuallyAdditionsRecipeResolver.clearCache();

        TileEntityEmpowerer empowerer = placeEmpowerer(helper);
        charge(placeStands(helper, 4));

        RecipeHolder<EmpowererRecipe> holder = quickestRecipe(helper);
        IPatternDetails doubled = patternFor(helper, holder, 2);
        helper.assertTrue(
                !machine(helper).pushPattern(doubled, inputsOf(doubled), Direction.UP),
                "a pattern asking for two crafts at once was accepted");
        assertNothingStranded(helper, empowerer);

        IPatternDetails single = patternFor(helper, holder, 1);
        helper.assertTrue(
                machine(helper).pushPattern(single, inputsOf(single), Direction.UP),
                "refusing the multiplied pattern also blocked its single sibling");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 400)
    public static void aStagedCraftCompletesAndOnlyTheResultCanBePulledBack(GameTestHelper helper) {
        EmpowererCraftingMachine.clearCache();
        ActuallyAdditionsRecipeResolver.clearCache();

        TileEntityEmpowerer empowerer = placeEmpowerer(helper);
        List<TileEntityDisplayStand> stands = placeStands(helper, 4);
        charge(stands);

        RecipeHolder<EmpowererRecipe> holder = quickestRecipe(helper);
        IPatternDetails details = patternFor(helper, holder, 1);
        helper.assertTrue(
                machine(helper).pushPattern(details, inputsOf(details), Direction.UP),
                "the empowering pattern was rejected");

        IItemHandler handler =
                helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(EMPOWERER), null);
        helper.assertTrue(handler != null, "the Empowerer exposed no item handler");
        helper.assertTrue(
                handler.extractItem(0, 1, true).isEmpty(),
                "automation was able to pull the staged base out from under the craft");

        ItemStack expected = holder.value().getOutput();
        helper.runAfterDelay(holder.value().getTime() + 20L, () -> {
            helper.assertTrue(
                    ItemStack.isSameItem(empowerer.inv.getStackInSlot(0), expected),
                    "the Empowerer did not finish the craft it was handed");
            for (TileEntityDisplayStand stand : stands) {
                helper.assertTrue(stand.getStack().isEmpty(), "a Display Stand kept its modifier after the craft");
            }
            helper.assertTrue(
                    !handler.extractItem(0, 1, true).isEmpty(), "an Import Card could not collect the finished item");
            helper.succeed();
        });
    }
}
