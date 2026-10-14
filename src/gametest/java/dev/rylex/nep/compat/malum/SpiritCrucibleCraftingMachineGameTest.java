package dev.rylex.nep.compat.malum;

import static dev.rylex.nep.GameTestFixtures.inputsOf;
import static dev.rylex.nep.GameTestFixtures.isEmpty;

import appeng.api.crafting.IPatternDetails;
import appeng.api.implementations.blockentities.ICraftingMachine;
import com.sammy.malum.common.block.curiosities.spirit_crucible.SpiritCrucibleCoreBlockEntity;
import com.sammy.malum.common.recipe.SpiritFocusingRecipe;
import com.sammy.malum.registry.common.block.MalumBlocks;
import dev.rylex.nep.GameTestFixtures;
import dev.rylex.nep.Nep;
import java.util.Comparator;
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
        return GameTestFixtures.craftingMachine(
                helper, CRUCIBLE, "the Spirit Crucible exposed no crafting machine capability");
    }

    private static RecipeHolder<SpiritFocusingRecipe> quickestRecipe(GameTestHelper helper) {
        return MalumFixtures.pickBest(
                helper,
                SpiritFocusingResolver.candidates(helper.getLevel()),
                holder -> holder.id().getNamespace().equals("malum")
                        && holder.value().input.getItems().length != 0
                        && MalumRecipeIngredients.spiritFocusing(holder.value()) != null,
                Comparator.comparingInt(holder -> holder.value().time),
                "no spirit focusing recipe was loaded");
    }

    private static ItemStack impetusFor(RecipeHolder<SpiritFocusingRecipe> holder) {
        return holder.value().input.getItems()[0].copy();
    }

    private static IPatternDetails patternFor(GameTestHelper helper, RecipeHolder<SpiritFocusingRecipe> holder) {
        return MalumFixtures.spiritFocusingPattern(helper, holder);
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
            helper.assertTrue(
                    GameTestFixtures.holds(barrel, expected, 1), "the warded crucible never finished the focusing");
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
            helper.assertTrue(
                    GameTestFixtures.holds(barrel, expected, 1),
                    "the finished focusing never reached the inventory the pattern was pushed from");
        });
    }
}
