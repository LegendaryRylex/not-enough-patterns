package dev.rylex.nep.compat.malum;

import appeng.api.AECapabilities;
import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import com.sammy.malum.common.block.curiosities.spirit_crucible.SpiritCrucibleCoreBlockEntity;
import com.sammy.malum.common.recipe.SpiritFocusingRecipe;
import com.sammy.malum.registry.common.block.MalumBlocks;
import dev.rylex.nep.Nep;
import dev.rylex.nep.pattern.SpiritFocusingPattern;
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
public final class SpiritCrucibleCraftingMachineGameTest {

    private static final String TEMPLATE = "empty_9x6x9";
    private static final String BATCH = "nep_spirit_crucible";

    private static final BlockPos CRUCIBLE = new BlockPos(4, 1, 4);
    private static final BlockPos COMPONENT = CRUCIBLE.above();
    private static final BlockPos RETURN_TARGET = CRUCIBLE.north();

    private SpiritCrucibleCraftingMachineGameTest() {}

    private static SpiritCrucibleCoreBlockEntity placeCrucible(GameTestHelper helper) {
        SpiritCrucibleCraftingMachine.clearCache();
        SpiritFocusingResolver.clearCache();
        helper.setBlock(CRUCIBLE, MalumBlocks.SPIRIT_CRUCIBLE.get().defaultBlockState());
        helper.setBlock(COMPONENT, MalumBlocks.SPIRIT_CRUCIBLE_COMPONENT.get().defaultBlockState());
        SpiritCrucibleCoreBlockEntity crucible =
                helper.getBlockEntity(CRUCIBLE) instanceof SpiritCrucibleCoreBlockEntity be ? be : null;
        helper.assertTrue(crucible != null, "the Spirit Crucible has no core block entity");
        return crucible;
    }

    private static ICraftingMachine machine(GameTestHelper helper) {
        ICraftingMachine machine =
                helper.getLevel().getCapability(AECapabilities.CRAFTING_MACHINE, helper.absolutePos(CRUCIBLE), null);
        helper.assertTrue(machine != null, "the Spirit Crucible exposed no crafting machine capability");
        return machine;
    }

    private static RecipeHolder<SpiritFocusingRecipe> quickestRecipe(GameTestHelper helper) {
        RecipeHolder<SpiritFocusingRecipe> best = null;
        for (RecipeHolder<SpiritFocusingRecipe> holder : SpiritFocusingResolver.candidates(helper.getLevel())) {
            SpiritFocusingRecipe recipe = holder.value();
            if (!holder.id().getNamespace().equals("malum")
                    || recipe.input.getItems().length == 0
                    || MalumRecipeIngredients.spiritFocusing(recipe) == null) {
                continue;
            }
            if (best == null || recipe.time < best.value().time) {
                best = holder;
            }
        }
        helper.assertTrue(best != null, "no spirit focusing recipe was loaded");
        return best;
    }

    private static ItemStack impetusFor(RecipeHolder<SpiritFocusingRecipe> holder) {
        return holder.value().input.getItems()[0].copy();
    }

    private static IPatternDetails patternFor(GameTestHelper helper, RecipeHolder<SpiritFocusingRecipe> holder) {
        EncodedIngredients expected = MalumRecipeIngredients.spiritFocusing(holder.value());
        helper.assertTrue(expected != null, "the focusing recipe produced no ingredient list");

        List<GenericStack> inputs = new ArrayList<>();
        for (List<GenericStack> options : expected.inputs()) {
            inputs.add(options.get(0));
        }
        ItemStack encoded = SpiritFocusingPattern.encode(
                holder.id(), inputs, expected.outputs().get(0));
        IPatternDetails details = PatternDetailsHelper.decodePattern(encoded, helper.getLevel());
        helper.assertTrue(details != null, "the spirit focusing pattern did not decode");
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
    public static void aCrucibleTakesAFocusingCraftInOnePush(GameTestHelper helper) {
        SpiritCrucibleCoreBlockEntity crucible = placeCrucible(helper);

        RecipeHolder<SpiritFocusingRecipe> holder = quickestRecipe(helper);
        crucible.inventory.setStackInSlot(0, impetusFor(holder));

        IPatternDetails details = patternFor(helper, holder);
        helper.assertTrue(
                machine(helper).pushPattern(details, inputsOf(details), Direction.NORTH),
                "a spirit focusing pattern was rejected by a crucible holding the right impetus");
        helper.assertTrue(!isEmpty(crucible.spiritInventory), "the recipe's spirits were not staged");
        helper.assertTrue(crucible.recipe == holder.value(), "the crucible did not pick up the recipe it was handed");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void aCrucibleHoldingTheWrongImpetusRefuses(GameTestHelper helper) {
        SpiritCrucibleCoreBlockEntity crucible = placeCrucible(helper);

        RecipeHolder<SpiritFocusingRecipe> holder = quickestRecipe(helper);
        ItemStack wrong = ItemStack.EMPTY;
        for (RecipeHolder<SpiritFocusingRecipe> other : SpiritFocusingResolver.candidates(helper.getLevel())) {
            if (other.value().input.getItems().length == 0) {
                continue;
            }
            ItemStack candidate = impetusFor(other);
            if (!SpiritFocusingResolver.acceptsImpetus(holder.value(), candidate)) {
                wrong = candidate;
                break;
            }
        }
        helper.assertTrue(
                !wrong.isEmpty(), "every loaded focusing recipe shares an impetus, so there is no wrong one to test");
        crucible.inventory.setStackInSlot(0, wrong);

        IPatternDetails details = patternFor(helper, holder);
        helper.assertTrue(
                !machine(helper).pushPattern(details, inputsOf(details), Direction.NORTH),
                "a crucible holding " + wrong + " accepted a pattern for " + holder.id());
        helper.assertTrue(isEmpty(crucible.spiritInventory), "a refused push left spirits in the crucible");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void anEmptyCrucibleRefusesRatherThanStrandingSpirits(GameTestHelper helper) {
        SpiritCrucibleCoreBlockEntity crucible = placeCrucible(helper);

        IPatternDetails details = patternFor(helper, quickestRecipe(helper));
        helper.assertTrue(
                !machine(helper).pushPattern(details, inputsOf(details), Direction.NORTH),
                "a crucible with no impetus at all accepted a focusing pattern");
        helper.assertTrue(isEmpty(crucible.spiritInventory), "a refused push left spirits in the crucible");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 800)
    public static void aWardedCrucibleSpendsNoImpetusDurability(GameTestHelper helper) {
        SpiritCrucibleCoreBlockEntity crucible = placeCrucible(helper);
        helper.setBlock(RETURN_TARGET, Blocks.BARREL.defaultBlockState());

        RecipeHolder<SpiritFocusingRecipe> holder = quickestRecipe(helper);
        helper.assertTrue(
                holder.value().durabilityCost > 0,
                "the focusing recipe under test charges no durability, so it proves nothing");
        crucible.inventory.setStackInSlot(0, impetusFor(holder));

        IPatternDetails details = patternFor(helper, holder);
        helper.assertTrue(
                machine(helper).pushPattern(details, inputsOf(details), Direction.NORTH),
                "the spirit focusing pattern was rejected");
        ImpetusWard.ward(crucible, holder.value().time * 4);

        ItemStack expected = holder.value().output;
        helper.succeedWhen(() -> {
            IItemHandler barrel = helper.getLevel()
                    .getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(RETURN_TARGET), null);
            helper.assertTrue(barrel != null, "the return target exposed no inventory");
            boolean finished = false;
            for (int slot = 0; slot < barrel.getSlots(); slot++) {
                if (ItemStack.isSameItem(barrel.getStackInSlot(slot), expected)) {
                    finished = true;
                    break;
                }
            }
            helper.assertTrue(finished, "the warded crucible never finished the focusing");
            ItemStack impetus = crucible.inventory.getStackInSlot(0);
            helper.assertTrue(!impetus.isEmpty(), "the impetus vanished from the warded crucible");
            helper.assertTrue(
                    impetus.getDamageValue() == 0,
                    "the warded crucible still spent " + impetus.getDamageValue() + " impetus durability");
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 800)
    public static void aFinishedFocusingGoesBackToTheProvider(GameTestHelper helper) {
        SpiritCrucibleCoreBlockEntity crucible = placeCrucible(helper);
        helper.setBlock(RETURN_TARGET, Blocks.BARREL.defaultBlockState());

        RecipeHolder<SpiritFocusingRecipe> holder = quickestRecipe(helper);
        crucible.inventory.setStackInSlot(0, impetusFor(holder));

        IPatternDetails details = patternFor(helper, holder);
        helper.assertTrue(
                machine(helper).pushPattern(details, inputsOf(details), Direction.NORTH),
                "the spirit focusing pattern was rejected");

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
            helper.fail("the finished node never reached the inventory the pattern was pushed from");
        });
    }
}
