package dev.rylex.nep.compat.malum;

import static dev.rylex.nep.GameTestFixtures.inputsOf;
import static dev.rylex.nep.GameTestFixtures.isEmpty;
import static dev.rylex.nep.compat.malum.MalumFixtures.recipeWithExtras;

import appeng.api.crafting.IPatternDetails;
import appeng.api.implementations.blockentities.ICraftingMachine;
import com.sammy.malum.common.block.curiosities.spirit_altar.SpiritAltarBlockEntity;
import com.sammy.malum.common.recipe.SpiritInfusionRecipe;
import com.sammy.malum.registry.common.block.MalumBlocks;
import dev.rylex.nep.GameTestFixtures;
import dev.rylex.nep.Nep;
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
        SpiritAltarBlockEntity altar = helper.getBlockEntity(ALTAR) instanceof SpiritAltarBlockEntity be ? be : null;
        helper.assertTrue(altar != null, "the Spirit Altar has no block entity");
        return altar;
    }

    private static void placePedestals(GameTestHelper helper, int count) {
        for (int index = 0; index < count; index++) {
            helper.setBlock(
                    ALTAR.offset(PEDESTAL_OFFSETS.get(index)),
                    MalumBlocks.RUNEWOOD_ITEM_PEDESTAL.get().defaultBlockState());
        }
    }

    private static ICraftingMachine machine(GameTestHelper helper) {
        return GameTestFixtures.craftingMachine(
                helper, ALTAR, "the Spirit Altar exposed no crafting machine capability");
    }

    private static IPatternDetails patternFor(GameTestHelper helper, RecipeHolder<SpiritInfusionRecipe> holder) {
        return MalumFixtures.spiritInfusionPattern(helper, holder);
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
        helper.succeedWhen(() -> {
            IItemHandler barrel = helper.getLevel()
                    .getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(RETURN_TARGET), null);
            helper.assertTrue(barrel != null, "the return target exposed no inventory");
            helper.assertTrue(
                    GameTestFixtures.holds(barrel, expected, 1),
                    "the finished infusion never reached the inventory the pattern was pushed from");
        });
    }
}
