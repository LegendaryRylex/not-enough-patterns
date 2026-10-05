package dev.rylex.nep.compat.malum;

import appeng.api.AECapabilities;
import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import com.sammy.malum.common.block.curiosities.spirit_altar.SpiritAltarBlockEntity;
import com.sammy.malum.common.recipe.SpiritInfusionRecipe;
import com.sammy.malum.registry.common.block.MalumBlocks;
import dev.rylex.nep.Nep;
import dev.rylex.nep.pattern.SpiritInfusionPattern;
import dev.rylex.nep.pattern.encoding.EncodedIngredients;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import team.lodestar.lodestone.systems.blockentity.LodestoneBlockEntityInventory;

@GameTestHolder(Nep.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SpiritAltarCraftingMachineGameTest {

    private static final String TEMPLATE = "empty_9x6x9";
    private static final String BATCH = "nep_spirit_altar";

    private static final BlockPos ALTAR = new BlockPos(4, 1, 4);
    private static final BlockPos RETURN_TARGET = ALTAR.above();

    private static final List<BlockPos> PEDESTAL_OFFSETS =
            List.of(new BlockPos(2, 0, 0), new BlockPos(0, 0, 2), new BlockPos(-2, 0, 0), new BlockPos(0, 0, -2));

    private SpiritAltarCraftingMachineGameTest() {}

    private static SpiritAltarBlockEntity placeAltar(GameTestHelper helper) {
        SpiritAltarCraftingMachine.clearCache();
        SpiritInfusionResolver.clearCache();
        helper.setBlock(ALTAR, MalumBlocks.SPIRIT_ALTAR.get().defaultBlockState());
        return (SpiritAltarBlockEntity) helper.getBlockEntity(ALTAR);
    }

    private static void placePedestals(GameTestHelper helper, int count) {
        for (int index = 0; index < count; index++) {
            helper.setBlock(
                    ALTAR.offset(PEDESTAL_OFFSETS.get(index)),
                    MalumBlocks.RUNEWOOD_ITEM_PEDESTAL.get().defaultBlockState());
        }
    }

    private static ICraftingMachine machine(GameTestHelper helper) {
        ICraftingMachine machine =
                helper.getLevel().getCapability(AECapabilities.CRAFTING_MACHINE, helper.absolutePos(ALTAR), null);
        helper.assertTrue(machine != null, "the Spirit Altar exposed no crafting machine capability");
        return machine;
    }

    private static RecipeHolder<SpiritInfusionRecipe> recipeWithExtras(GameTestHelper helper, int extras) {
        RecipeHolder<SpiritInfusionRecipe> best = null;
        for (RecipeHolder<SpiritInfusionRecipe> holder : SpiritInfusionResolver.candidates(helper.getLevel())) {
            SpiritInfusionRecipe recipe = holder.value();
            if (!holder.id().getNamespace().equals("malum")
                    || recipe.carryOverComponentData
                    || recipe.extraInputs.size() != extras
                    || MalumRecipeIngredients.spiritInfusion(recipe) == null) {
                continue;
            }
            if (best == null || holder.id().compareTo(best.id()) < 0) {
                best = holder;
            }
        }
        helper.assertTrue(best != null, "no spirit infusion recipe with " + extras + " extra ingredient(s) was loaded");
        return best;
    }

    private static IPatternDetails patternFor(GameTestHelper helper, RecipeHolder<SpiritInfusionRecipe> holder) {
        EncodedIngredients expected = MalumRecipeIngredients.spiritInfusion(holder.value());
        helper.assertTrue(expected != null, "the recipe produced no ingredient list");

        List<GenericStack> inputs = new ArrayList<>();
        for (List<GenericStack> options : expected.inputs()) {
            inputs.add(options.get(0));
        }
        ItemStack encoded = SpiritInfusionPattern.encode(
                holder.id(), inputs, expected.outputs().get(0));
        IPatternDetails details = PatternDetailsHelper.decodePattern(encoded, helper.getLevel());
        helper.assertTrue(details != null, "the spirit infusion pattern did not decode");
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

    private static boolean isEmpty(LodestoneBlockEntityInventory inventory) {
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            if (!inventory.getStackInSlot(slot).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void anAltarTakesTheItemAndItsSpiritsInOnePush(GameTestHelper helper) {
        SpiritAltarBlockEntity altar = placeAltar(helper);

        RecipeHolder<SpiritInfusionRecipe> holder = recipeWithExtras(helper, 0);
        IPatternDetails details = patternFor(helper, holder);

        helper.assertTrue(
                machine(helper).pushPattern(details, inputsOf(details), Direction.UP),
                "a spirit infusion pattern needing no pedestals was rejected");
        helper.assertTrue(!altar.inventory.getStackInSlot(0).isEmpty(), "the altar item was not staged");
        helper.assertTrue(!isEmpty(altar.spiritInventory), "the recipe's spirits were not staged");
        helper.assertTrue(altar.recipe == holder.value(), "the altar did not pick up the recipe it was handed");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void aRecipeWantingPedestalsIsRefusedWithoutStrandingItems(GameTestHelper helper) {
        SpiritAltarBlockEntity altar = placeAltar(helper);

        IPatternDetails details = patternFor(helper, recipeWithExtras(helper, 1));
        helper.assertTrue(
                !machine(helper).pushPattern(details, inputsOf(details), Direction.UP),
                "a Spirit Altar with no free pedestal accepted a pattern that needs one");
        helper.assertTrue(isEmpty(altar.inventory), "a refused push left an item in the Spirit Altar");
        helper.assertTrue(isEmpty(altar.spiritInventory), "a refused push left spirits in the Spirit Altar");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void anExtraIngredientIsStagedOnAPedestal(GameTestHelper helper) {
        SpiritAltarBlockEntity altar = placeAltar(helper);
        placePedestals(helper, 1);

        RecipeHolder<SpiritInfusionRecipe> holder = recipeWithExtras(helper, 1);
        IPatternDetails details = patternFor(helper, holder);
        helper.assertTrue(
                machine(helper).pushPattern(details, inputsOf(details), Direction.UP),
                "a spirit infusion pattern with a free pedestal in range was rejected");

        BlockPos pedestal = ALTAR.offset(PEDESTAL_OFFSETS.get(0));
        helper.assertTrue(
                !helper.getLevel()
                        .getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(pedestal), null)
                        .getStackInSlot(0)
                        .isEmpty(),
                "the extra ingredient never reached the pedestal");
        helper.assertTrue(altar.recipe == holder.value(), "the altar did not pick up the recipe it was handed");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 600)
    public static void aFinishedInfusionGoesBackToTheProvider(GameTestHelper helper) {
        placeAltar(helper);
        helper.setBlock(RETURN_TARGET, Blocks.BARREL.defaultBlockState());

        RecipeHolder<SpiritInfusionRecipe> holder = recipeWithExtras(helper, 0);
        IPatternDetails details = patternFor(helper, holder);
        helper.assertTrue(
                machine(helper).pushPattern(details, inputsOf(details), Direction.UP),
                "the spirit infusion pattern was rejected");

        ItemStack expected = holder.value().result;
        helper.runAfterDelay(360L, () -> {
            IItemHandler barrel = helper.getLevel()
                    .getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(RETURN_TARGET), null);
            helper.assertTrue(barrel != null, "the return target exposed no inventory");
            for (int slot = 0; slot < barrel.getSlots(); slot++) {
                if (ItemStack.isSameItem(barrel.getStackInSlot(slot), expected)) {
                    helper.succeed();
                    return;
                }
            }
            helper.fail("the finished infusion never reached the inventory the pattern was pushed from");
        });
    }
}
