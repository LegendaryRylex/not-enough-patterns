package dev.rylex.nep.compat.occultism;

import static dev.rylex.nep.GameTestFixtures.inputsOf;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.GenericStack;
import appeng.core.definitions.AEBlocks;
import com.klikli_dev.occultism.Occultism;
import com.klikli_dev.occultism.crafting.recipe.TieredSingleRecipeInput;
import com.klikli_dev.occultism.registry.OccultismBlocks;
import com.klikli_dev.occultism.registry.OccultismRecipes;
import dev.rylex.nep.NepGameTests;
import dev.rylex.nep.machine.ManualCraftOutcome;
import dev.rylex.nep.machine.ManualCraftResult;
import dev.rylex.nep.util.Recipes;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.transfer.item.ItemResource;

public final class BoundSpiritMatrixGameTest {

    private static final BlockPos MATRIX = new BlockPos(2, 1, 2);
    private static final BlockPos ME_CONTROLLER = new BlockPos(2, 1, 3);
    private static final BlockPos ENERGY_CELL = ME_CONTROLLER.above();

    private BoundSpiritMatrixGameTest() {}

    public static void register(NepGameTests.Batch batch0, NepGameTests.Batch batch1) {
        batch0.add("spirit_fire_runs_without_a_book", 200, BoundSpiritMatrixGameTest::spiritFireRunsWithoutABook)
                .add(
                        "crushing_is_refused_without_a_crusher_book",
                        BoundSpiritMatrixGameTest::crushingIsRefusedWithoutACrusherBook)
                .add(
                        "a_book_only_fits_the_slot_of_its_own_job",
                        BoundSpiritMatrixGameTest::aBookOnlyFitsTheSlotOfItsOwnJob)
                .add(
                        "a_marid_crusher_yields_its_tier_multiplier",
                        400,
                        BoundSpiritMatrixGameTest::aMaridCrusherYieldsItsTierMultiplier)
                .add(
                        "a_crushing_recipe_runs_by_hand_into_the_output_buffer",
                        400,
                        BoundSpiritMatrixGameTest::aCrushingRecipeRunsByHandIntoTheOutputBuffer);
        batch1.add(
                "a_spirit_with_several_operations_crushes_them_in_one_craft",
                400,
                BoundSpiritMatrixGameTest::aSpiritWithSeveralOperationsCrushesThemInOneCraft);
    }

    private static BoundSpiritMatrixBlockEntity placeMatrix(GameTestHelper helper) {
        helper.setBlock(ME_CONTROLLER, AEBlocks.CONTROLLER.block());
        helper.setBlock(ENERGY_CELL, AEBlocks.CREATIVE_ENERGY_CELL.block().defaultBlockState());
        helper.setBlock(MATRIX, NepOccultismContent.MATRIX.get().defaultBlockState());
        BoundSpiritMatrixBlockEntity matrix = helper.getBlockEntity(MATRIX, BoundSpiritMatrixBlockEntity.class);
        helper.assertTrue(matrix != null, "the Bound Spirit Matrix has no block entity");
        return matrix;
    }

    private static void bind(BoundSpiritMatrixBlockEntity matrix, BoundSpirit.Tier tier, BoundSpirit.Work work) {
        int slot = work == BoundSpirit.Work.CRUSHER
                ? BoundSpiritMatrixBlockEntity.CRUSHER_SLOT
                : BoundSpiritMatrixBlockEntity.CRYSTALLIZER_SLOT;
        matrix.books().put(slot, SpiritInfusedBookItem.of(new BoundSpirit(tier, work)));
    }

    private static IPatternDetails pattern(GameTestHelper helper, ItemStack input, ItemStack output) {
        ItemStack encoded = PatternDetailsHelper.encodeProcessingPattern(
                List.of(GenericStack.fromItemStack(input)), List.of(GenericStack.fromItemStack(output)));
        IPatternDetails details = PatternDetailsHelper.decodePattern(encoded, helper.getLevel());
        helper.assertTrue(details != null, "the processing pattern did not decode");
        return details;
    }

    private static int outputCount(BoundSpiritMatrixBlockEntity matrix, ItemStack expected) {
        int found = 0;
        for (int slot = 0; slot < matrix.getOutputBuffer().size(); slot++) {
            ItemStack stack = matrix.getOutputBuffer().stackAt(slot);
            if (ItemStack.isSameItem(stack, expected)) {
                found += stack.getCount();
            }
        }
        return found;
    }

    private static ItemStack crushedRawIron(GameTestHelper helper, BoundSpirit.Tier tier) {
        OccultismMatrixRecipes.Match match = OccultismMatrixRecipes.find(
                helper.getLevel(),
                new ItemStack(Items.RAW_IRON),
                dustFor(helper, tier),
                new BoundSpirit(tier, BoundSpirit.Work.CRUSHER),
                null);
        helper.assertTrue(match != null, "no crushing recipe for raw iron was found");
        OccultismMatrixRecipes.Craft craft = OccultismMatrixRecipes.craft(
                helper.getLevel(),
                match.kind(),
                match.recipe(),
                new ItemStack(Items.RAW_IRON),
                new BoundSpirit(tier, BoundSpirit.Work.CRUSHER),
                1);
        helper.assertTrue(craft != null, "the raw iron crushing recipe made nothing");
        return craft.result();
    }

    private static Item dustFor(GameTestHelper helper, BoundSpirit.Tier tier) {
        TieredSingleRecipeInput input = new TieredSingleRecipeInput(new ItemStack(Items.RAW_IRON), tier.level());
        return Recipes.allOf(helper.getLevel(), OccultismRecipes.CRUSHING_TYPE.get()).stream()
                .filter(holder -> holder.value().matches(input, helper.getLevel()))
                .map(holder -> holder.value().assemble(input))
                .filter(stack -> !stack.isEmpty())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("raw iron crushes into nothing"))
                .getItem();
    }

    public static void spiritFireRunsWithoutABook(GameTestHelper helper) {
        BoundSpiritMatrixBlockEntity matrix = placeMatrix(helper);
        ItemStack otherstone = new ItemStack(OccultismBlocks.OTHERSTONE.get());
        IPatternDetails details = pattern(helper, new ItemStack(Items.ANDESITE, 2), otherstone.copyWithCount(2));

        helper.assertTrue(
                matrix.pushMatrixPattern(details, inputsOf(details), Direction.UP),
                "the Matrix refused a spirit fire pattern with no book bound");
        helper.assertTrue(matrix.pendingJobs() == 2, "a pattern for two andesite did not queue two crafts");

        helper.succeedWhen(() -> helper.assertTrue(
                outputCount(matrix, otherstone) >= 2, "the Matrix never finished the spirit fire crafts"));
    }

    public static void crushingIsRefusedWithoutACrusherBook(GameTestHelper helper) {
        BoundSpiritMatrixBlockEntity matrix = placeMatrix(helper);
        ItemStack dust = crushedRawIron(helper, BoundSpirit.Tier.FOLIOT);
        IPatternDetails details = pattern(helper, new ItemStack(Items.RAW_IRON), dust);

        helper.assertFalse(
                matrix.pushMatrixPattern(details, inputsOf(details), Direction.UP),
                "the Matrix took a crushing pattern with no crusher bound");
        helper.assertTrue(
                matrix.refusal() == BoundSpiritMatrixBlockEntity.Refusal.NEEDS_CRUSHER,
                "the refusal was " + matrix.refusal() + ", not NEEDS_CRUSHER");

        bind(matrix, BoundSpirit.Tier.FOLIOT, BoundSpirit.Work.CRYSTALLIZER);
        helper.assertFalse(
                matrix.pushMatrixPattern(details, inputsOf(details), Direction.UP),
                "a crystallizer book let the Matrix take a crushing pattern");
        helper.succeed();
    }

    public static void aBookOnlyFitsTheSlotOfItsOwnJob(GameTestHelper helper) {
        BoundSpiritMatrixBlockEntity matrix = placeMatrix(helper);
        ItemStack crusher =
                SpiritInfusedBookItem.of(new BoundSpirit(BoundSpirit.Tier.DJINNI, BoundSpirit.Work.CRUSHER));
        helper.assertTrue(
                matrix.books().isValid(BoundSpiritMatrixBlockEntity.CRUSHER_SLOT, ItemResource.of(crusher)),
                "the crusher slot refused a crusher book");
        helper.assertFalse(
                matrix.books().isValid(BoundSpiritMatrixBlockEntity.CRYSTALLIZER_SLOT, ItemResource.of(crusher)),
                "the crystallizer slot took a crusher book");
        helper.assertFalse(
                matrix.books()
                        .isValid(BoundSpiritMatrixBlockEntity.CRUSHER_SLOT, ItemResource.of(new ItemStack(Items.BOOK))),
                "the crusher slot took a plain book");
        helper.succeed();
    }

    public static void aMaridCrusherYieldsItsTierMultiplier(GameTestHelper helper) {
        BoundSpiritMatrixBlockEntity matrix = placeMatrix(helper);
        bind(matrix, BoundSpirit.Tier.MARID, BoundSpirit.Work.CRUSHER);
        ItemStack foliot = crushedRawIron(helper, BoundSpirit.Tier.FOLIOT);
        ItemStack marid = crushedRawIron(helper, BoundSpirit.Tier.MARID);
        helper.assertTrue(
                marid.getCount() > foliot.getCount(),
                "a marid crushes raw iron into " + marid.getCount() + ", no more than a foliot's " + foliot.getCount());

        IPatternDetails details = pattern(helper, new ItemStack(Items.RAW_IRON), foliot);
        helper.assertTrue(
                matrix.pushMatrixPattern(details, inputsOf(details), Direction.UP),
                "the Matrix refused a crushing pattern with a marid crusher bound");

        helper.succeedWhen(() -> helper.assertTrue(
                outputCount(matrix, marid) >= marid.getCount(),
                "the Matrix has not crushed " + marid.getCount() + " " + marid + " yet"));
    }

    public static void aCrushingRecipeRunsByHandIntoTheOutputBuffer(GameTestHelper helper) {
        BoundSpiritMatrixBlockEntity matrix = placeMatrix(helper);
        ItemStack dust = crushedRawIron(helper, BoundSpirit.Tier.MARID);
        OccultismMatrixRecipes.Match match = OccultismMatrixRecipes.find(
                helper.getLevel(),
                new ItemStack(Items.RAW_IRON),
                dust.getItem(),
                new BoundSpirit(BoundSpirit.Tier.MARID, BoundSpirit.Work.CRUSHER),
                null);
        helper.assertTrue(match != null, "no crushing recipe for raw iron was found");
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getInventory().add(new ItemStack(Items.RAW_IRON, 2));

        ManualCraftOutcome refused = matrix.startManualCraft(player, match.recipe(), 2);
        helper.assertTrue(
                refused.status() == ManualCraftResult.NO_SPIRIT,
                "a hand-started crushing craft with no book bound gave " + refused.status());
        helper.assertTrue(player.getInventory().countItem(Items.RAW_IRON) == 2, "a refused craft took the raw iron");

        bind(matrix, BoundSpirit.Tier.MARID, BoundSpirit.Work.CRUSHER);
        ManualCraftOutcome outcome = matrix.startManualCraft(player, match.recipe(), 2);
        helper.assertTrue(
                outcome.status() == ManualCraftResult.STARTED && outcome.batches() == 2,
                "the Matrix did not queue two hand-started crushing crafts: " + outcome.status());
        helper.assertTrue(player.getInventory().countItem(Items.RAW_IRON) == 0, "the raw iron stayed in the inventory");

        helper.succeedWhen(() -> {
            helper.assertTrue(
                    outputCount(matrix, dust) >= dust.getCount() * 2,
                    "the hand-started crafts have not left their dust in the output buffer yet");
            helper.assertTrue(matrix.pendingJobs() == 0, "the hand-started crafts are still queued");
        });
    }

    public static void aSpiritWithSeveralOperationsCrushesThemInOneCraft(GameTestHelper helper) {
        ModConfigSpec.IntValue operations = Occultism.SERVER_CONFIG.spiritJobs.crusherMarid.operationCount;
        int previous = operations.get();
        operations.set(4);
        helper.runAtTickTime(399, () -> operations.set(previous));

        BoundSpiritMatrixBlockEntity matrix;
        ItemStack single;
        try {
            matrix = placeMatrix(helper);
            bind(matrix, BoundSpirit.Tier.MARID, BoundSpirit.Work.CRUSHER);
            single = crushedRawIron(helper, BoundSpirit.Tier.MARID);
            IPatternDetails details =
                    pattern(helper, new ItemStack(Items.RAW_IRON, 4), single.copyWithCount(single.getCount() * 4));
            helper.assertTrue(
                    matrix.pushMatrixPattern(details, inputsOf(details), Direction.UP),
                    "the Matrix refused four raw iron with a marid crusher bound");
        } catch (RuntimeException e) {
            operations.set(previous);
            throw e;
        }
        ItemStack crushed = single;
        BoundSpiritMatrixBlockEntity crusher = matrix;

        helper.succeedWhen(() -> {
            int made = outputCount(crusher, crushed);
            helper.assertTrue(made > 0, "the Matrix has not crushed anything yet");
            operations.set(previous);
            helper.assertTrue(
                    made >= crushed.getCount() * 4 && crusher.pendingJobs() == 0,
                    "the first craft made " + made + " dust, so the four raw iron were not crushed together");
        });
    }
}
