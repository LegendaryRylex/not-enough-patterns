package dev.rylex.nep.compat.ars;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import com.hollingsworth.arsnouveau.common.block.TableBlock;
import com.hollingsworth.arsnouveau.common.block.ThreePartBlock;
import com.hollingsworth.arsnouveau.common.block.tile.ScribesTile;
import com.hollingsworth.arsnouveau.common.crafting.recipes.GlyphRecipe;
import com.hollingsworth.arsnouveau.setup.registry.BlockRegistry;
import com.hollingsworth.arsnouveau.setup.registry.RecipeRegistry;
import dev.rylex.nep.ConfigOverrides;
import dev.rylex.nep.Nep;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

@GameTestHolder(Nep.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ArcaneLecternGameTest {

    private static final String TEMPLATE = "empty_9x6x9";
    private static final String BATCH = "nep_ars_lectern";
    private static final String RANGE_BATCH = "nep_ars_lectern_range";
    private static final String RANGE_FIELD = "ARS_ARCANE_LECTERN_SCRIBES_RANGE";

    private static final BlockPos LECTERN = ArsFixtures.CONSUMER;
    private static final BlockPos TABLE_FOOT = new BlockPos(4, 1, 8);
    private static final BlockPos TABLE_HEAD = new BlockPos(4, 1, 7);
    private static final int STOCK_PER_REAGENT = 4;

    private ArcaneLecternGameTest() {}

    private static void placeLectern(GameTestHelper helper) {
        helper.setBlock(LECTERN, NepArsContent.ARCANE_LECTERN.get().defaultBlockState());
    }

    private static ScribesTile placeTable(GameTestHelper helper, RecipeHolder<GlyphRecipe> recipe) {
        var block = BlockRegistry.SCRIBES_BLOCK.get();
        helper.setBlock(
                TABLE_FOOT,
                block.defaultBlockState()
                        .setValue(TableBlock.FACING, Direction.NORTH)
                        .setValue(TableBlock.PART, ThreePartBlock.FOOT));
        helper.setBlock(
                TABLE_HEAD,
                block.defaultBlockState()
                        .setValue(TableBlock.FACING, Direction.NORTH)
                        .setValue(TableBlock.PART, ThreePartBlock.HEAD));
        ScribesTile table = helper.getBlockEntity(TABLE_HEAD);
        table.autoYoink = false;
        table.recipe = recipe;
        return table;
    }

    private static RecipeHolder<GlyphRecipe> glyphRecipe(GameTestHelper helper) {
        List<RecipeHolder<GlyphRecipe>> candidates =
                helper.getLevel().getRecipeManager().getAllRecipesFor(RecipeRegistry.GLYPH_TYPE.get()).stream()
                        .filter(holder ->
                                holder.id().getNamespace().equals("ars_nouveau") && distinctReagents(holder.value()))
                        .sorted(Comparator.comparingInt((RecipeHolder<GlyphRecipe> holder) ->
                                        holder.value().inputs.size())
                                .thenComparing(holder -> holder.id().toString()))
                        .toList();
        if (candidates.isEmpty()) {
            helper.fail("no glyph recipe with distinct reagents was found");
        }
        return candidates.get(0);
    }

    private static boolean distinctReagents(GlyphRecipe recipe) {
        if (recipe.inputs.isEmpty()) {
            return false;
        }
        Set<Item> seen = new HashSet<>();
        for (Ingredient ingredient : recipe.inputs) {
            ItemStack[] options = ingredient.getItems();
            if (options.length == 0 || options[0].isEmpty() || !seen.add(options[0].getItem())) {
                return false;
            }
        }
        return true;
    }

    private static List<AEItemKey> reagents(GlyphRecipe recipe) {
        List<AEItemKey> keys = new ArrayList<>();
        for (Ingredient ingredient : recipe.inputs) {
            keys.add(AEItemKey.of(ingredient.getItems()[0]));
        }
        return keys;
    }

    private static List<GenericStack> stockOf(List<AEItemKey> keys) {
        List<GenericStack> stock = new ArrayList<>();
        for (AEItemKey key : keys) {
            stock.add(new GenericStack(key, STOCK_PER_REAGENT));
        }
        return stock;
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void theLecternReportsNetworkAmountsAboveOneStackInASingleSlot(GameTestHelper helper) {
        AEItemKey cobblestone = AEItemKey.of(Items.COBBLESTONE);
        ArsFixtures.poweredNetwork(helper, () -> placeLectern(helper), List.of(new GenericStack(cobblestone, 200)))
                .thenExecute(() -> {
                    ArcaneLecternBlockEntity lectern = helper.getBlockEntity(LECTERN);
                    IItemHandler handler = lectern.itemHandler();
                    int slot = -1;
                    for (int index = 0; index < handler.getSlots(); index++) {
                        if (handler.getStackInSlot(index).is(Items.COBBLESTONE)) {
                            slot = index;
                        }
                    }
                    helper.assertTrue(slot >= 0, "the lectern did not list the cobblestone in the network");
                    helper.assertTrue(
                            handler.getStackInSlot(slot).getCount() == 200,
                            "the slot reported " + handler.getStackInSlot(slot).getCount() + " instead of 200");
                    helper.assertTrue(
                            handler.extractItem(slot, 200, true).getCount() == 64,
                            "an extract was not capped at one stack");
                    ItemStack taken = handler.extractItem(slot, 200, false);
                    helper.assertTrue(taken.getCount() == 64, "the real extract returned " + taken.getCount());
                    helper.assertTrue(
                            ArsFixtures.stored(helper, cobblestone) == 136,
                            "the network holds " + ArsFixtures.stored(helper, cobblestone) + " after a 64 extract");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void aScribesTableInRangeTakesItsReagentsFromTheNetwork(GameTestHelper helper) {
        RecipeHolder<GlyphRecipe> recipe = glyphRecipe(helper);
        List<AEItemKey> reagents = reagents(recipe.value());
        ArsFixtures.poweredNetwork(helper, () -> placeLectern(helper), stockOf(reagents))
                .thenExecute(() -> {
                    ScribesTile table = placeTable(helper, recipe);
                    table.takeNearby();
                    helper.assertTrue(
                            table.getRemainingRequired().isEmpty(),
                            "the table still needs "
                                    + table.getRemainingRequired().size() + " reagents");
                    helper.assertTrue(
                            table.consumedStacks.size() == reagents.size(),
                            "the table holds " + table.consumedStacks.size() + " stacks for " + reagents.size()
                                    + " reagents");
                    for (AEItemKey key : reagents) {
                        helper.assertTrue(
                                ArsFixtures.stored(helper, key) == STOCK_PER_REAGENT - 1,
                                key + " left " + ArsFixtures.stored(helper, key) + " in the network");
                    }
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, batch = RANGE_BATCH, timeoutTicks = 200)
    public static void aScribesTableBeyondTheConfiguredRangeGetsNothing(GameTestHelper helper) {
        RecipeHolder<GlyphRecipe> recipe = glyphRecipe(helper);
        List<AEItemKey> reagents = reagents(recipe.value());
        ArsFixtures.poweredNetwork(helper, () -> placeLectern(helper), stockOf(reagents))
                .thenExecute(() -> {
                    ConfigOverrides.Restore restore = ConfigOverrides.override(RANGE_FIELD, 1);
                    try {
                        ScribesTile table = placeTable(helper, recipe);
                        table.takeNearby();
                        helper.assertTrue(
                                table.consumedStacks.isEmpty(), "a table beyond the range was handed reagents");
                        for (AEItemKey key : reagents) {
                            helper.assertTrue(
                                    ArsFixtures.stored(helper, key) == STOCK_PER_REAGENT,
                                    key + " was drawn from the network for an out-of-range table");
                        }
                    } finally {
                        restore.undo();
                    }
                })
                .thenSucceed();
    }
}
