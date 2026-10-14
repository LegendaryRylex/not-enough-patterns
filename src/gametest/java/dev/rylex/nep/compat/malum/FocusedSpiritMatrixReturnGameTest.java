package dev.rylex.nep.compat.malum;

import static dev.rylex.nep.GameTestFixtures.countIn;
import static dev.rylex.nep.GameTestFixtures.inputsOf;
import static dev.rylex.nep.compat.malum.MalumFixtures.UMBRAL_SLOT;
import static dev.rylex.nep.compat.malum.MalumFixtures.node;

import appeng.api.config.Actionable;
import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEItemKey;
import appeng.core.definitions.AEBlocks;
import com.sammy.malum.registry.common.item.MalumItems;
import dev.rylex.nep.Nep;
import dev.rylex.nep.machine.MatrixGridNode;
import dev.rylex.nep.machine.OverstackedItemHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

@GameTestHolder(Nep.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FocusedSpiritMatrixReturnGameTest {

    private static final String TEMPLATE = "empty_5x5x5";
    private static final String BATCH = "nep_focused_spirit_return";

    private static final BlockPos MATRIX = new BlockPos(2, 1, 2);
    private static final BlockPos ME_CONTROLLER = new BlockPos(2, 1, 3);
    private static final BlockPos ENERGY_CELL = ME_CONTROLLER.above();
    private static final BlockPos TARGET = new BlockPos(1, 1, 2);

    private static final Direction EJECTION = Direction.WEST;

    private static final ResourceLocation RECIPE = ResourceLocation.fromNamespaceAndPath("test", "matrix_spirit_heavy");

    private FocusedSpiritMatrixReturnGameTest() {}

    private static FocusedSpiritMatrixBlockEntity placeMatrix(GameTestHelper helper) {
        return MalumFixtures.placeMatrix(helper, ME_CONTROLLER, ENERGY_CELL, MATRIX);
    }

    private static IPatternDetails patternFor(GameTestHelper helper) {
        return MalumFixtures.spiritInfusionPattern(helper, RECIPE);
    }

    private static void push(GameTestHelper helper, FocusedSpiritMatrixBlockEntity matrix) {
        IPatternDetails details = patternFor(helper);
        helper.assertTrue(
                matrix.pushMatrixPattern(details, inputsOf(details), EJECTION), "the Matrix refused the test pattern");
    }

    private static IItemHandler target(GameTestHelper helper) {
        IItemHandler handler = helper.getLevel()
                .getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(TARGET), EJECTION.getOpposite());
        helper.assertTrue(handler != null, "the block on the ejection side exposed no item handler");
        return handler;
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 400)
    public static void theMatrixPushesItsResultBackAlongTheEjectionSide(GameTestHelper helper) {
        FocusedSpiritMatrixBlockEntity matrix = placeMatrix(helper);
        helper.setBlock(TARGET, Blocks.CHEST.defaultBlockState());
        push(helper, matrix);

        helper.succeedWhen(() -> {
            helper.assertTrue(
                    countIn(target(helper), Items.COPPER_INGOT) == 1,
                    "the Matrix never pushed its result out of the ejection side");
            helper.assertTrue(
                    countIn(matrix.getOutputBuffer(), Items.COPPER_INGOT) == 0,
                    "the Matrix kept a copy of the result it pushed out");
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 600)
    public static void theMatrixReturnsItsResultToAPatternProvider(GameTestHelper helper) {
        FocusedSpiritMatrixBlockEntity matrix = placeMatrix(helper);
        helper.setBlock(TARGET, AEBlocks.PATTERN_PROVIDER.block().defaultBlockState());
        push(helper, matrix);

        helper.succeedWhen(() -> {
            helper.assertTrue(
                    countIn(target(helper), Items.COPPER_INGOT) == 1,
                    "the Matrix never returned its result to the pattern provider that pushed the pattern");
            helper.assertTrue(
                    countIn(matrix.getOutputBuffer(), Items.COPPER_INGOT) == 0,
                    "the Matrix kept a copy of the result it returned");
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 800)
    public static void everyResultOfABatchedRequestFindsItsWayBack(GameTestHelper helper) {
        FocusedSpiritMatrixBlockEntity matrix = placeMatrix(helper);
        helper.setBlock(TARGET, Blocks.CHEST.defaultBlockState());
        push(helper, matrix);
        push(helper, matrix);
        push(helper, matrix);

        helper.succeedWhen(() -> {
            int returned = countIn(target(helper), Items.COPPER_INGOT);
            int stranded = countIn(matrix.getOutputBuffer(), Items.COPPER_INGOT);
            helper.assertTrue(
                    returned == 3,
                    "only " + returned + " of 3 batched results came back, " + stranded + " stranded in the output "
                            + "buffer");
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void aCraftedSpiritDeliveredByTheNetworkLandsInTheBank(GameTestHelper helper) {
        FocusedSpiritMatrixBlockEntity matrix = placeMatrix(helper);
        AEItemKey umbral = AEItemKey.of(MalumItems.UMBRAL_SPIRIT.get());
        int slotLimit = matrix.getSpiritBank().getSlotLimit(UMBRAL_SLOT);
        long offered = slotLimit + 36L;

        helper.assertValueEqual(
                matrix.acceptCrafted(umbral, offered, Actionable.SIMULATE),
                offered - slotLimit,
                "the spirits a simulated delivery says it cannot take");
        helper.assertTrue(
                countIn(matrix.getSpiritBank(), MalumItems.UMBRAL_SPIRIT.get()) == 0,
                "a simulated delivery banked spirits anyway");

        helper.assertValueEqual(
                matrix.acceptCrafted(umbral, offered, Actionable.MODULATE),
                offered - slotLimit,
                "the spirits a real delivery says it cannot take");
        helper.assertTrue(
                countIn(matrix.getSpiritBank(), MalumItems.UMBRAL_SPIRIT.get()) == slotLimit,
                "a real delivery did not fill the bank slot it belongs in");

        helper.assertValueEqual(
                matrix.acceptCrafted(umbral, 10L, Actionable.MODULATE),
                10L,
                "the spirits a delivery into a full bank says it cannot take");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void theGridNodeReportsWhatItTookRatherThanWhatItRefused(GameTestHelper helper) {
        FocusedSpiritMatrixBlockEntity matrix = placeMatrix(helper);
        MatrixGridNode node = node(helper, matrix);
        AEItemKey saddle = AEItemKey.of(Items.SADDLE);
        long capacity = (long) matrix.getInputBuffer().getSlots() * OverstackedItemHandler.SLOT_LIMIT;

        helper.assertValueEqual(
                node.insertCraftedItems(null, saddle, 8L, Actionable.MODULATE),
                8L,
                "what the grid node reports having taken of a delivery that fits");
        helper.assertTrue(
                countIn(matrix.getInputBuffer(), Items.SADDLE) == 8,
                "the delivery the grid node claimed never reached the input buffer");

        helper.assertValueEqual(
                node.insertCraftedItems(null, saddle, capacity, Actionable.MODULATE),
                capacity - 8L,
                "what the grid node reports having taken of a delivery that only partly fits; reporting the "
                        + "refused half instead duplicates or destroys the difference on the network");
        helper.assertValueEqual(
                node.insertCraftedItems(null, saddle, 4L, Actionable.MODULATE),
                0L,
                "what the grid node reports having taken of a delivery into a full buffer");
        helper.succeed();
    }
}
