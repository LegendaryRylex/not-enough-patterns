package dev.rylex.nep.compat.create;

import static dev.rylex.nep.GameTestFixtures.countIn;

import appeng.core.definitions.AEBlocks;
import dev.rylex.nep.Nep;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

@GameTestHolder(Nep.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SequencedAssemblyMatrixReturnGameTest {

    private static final String TEMPLATE = "empty_9x6x9";
    private static final String BATCH = "nep_matrix_return";

    private static final BlockPos MATRIX = new BlockPos(4, 1, 4);
    private static final BlockPos MOTOR = new BlockPos(4, 1, 3);
    private static final BlockPos ME_CONTROLLER = new BlockPos(5, 1, 4);
    private static final BlockPos ENERGY = ME_CONTROLLER.above();
    private static final BlockPos TARGET = new BlockPos(3, 1, 4);

    private static final Direction EJECTION = Direction.WEST;

    private SequencedAssemblyMatrixReturnGameTest() {}

    private static SequencedAssemblyMatrixBlockEntity placePoweredMatrix(GameTestHelper helper) {
        return CreateFixtures.poweredMatrix(helper, MATRIX, MOTOR, ME_CONTROLLER, ENERGY);
    }

    private static void pushPlatePattern(GameTestHelper helper) {
        CreateFixtures.pushPlatePattern(helper, MATRIX, EJECTION);
    }

    private static int outputCount(SequencedAssemblyMatrixBlockEntity matrix, Item item) {
        return countIn(matrix.getOutputBuffer(), item);
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 400)
    public static void theMatrixPushesItsResultBackAlongTheEjectionSide(GameTestHelper helper) {
        SequencedAssemblyMatrixBlockEntity matrix = placePoweredMatrix(helper);
        helper.setBlock(TARGET, Blocks.CHEST.defaultBlockState());
        pushPlatePattern(helper);

        helper.succeedWhen(() -> {
            IItemHandler chest = helper.getLevel()
                    .getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(TARGET), EJECTION.getOpposite());
            helper.assertTrue(chest != null, "the chest exposed no item handler");
            helper.assertTrue(
                    countIn(chest, NepCreateContent.HARDENED_OBSIDIAN_PLATE.get()) == 1,
                    "the matrix never pushed its result out of the ejection side");
            helper.assertTrue(
                    outputCount(matrix, NepCreateContent.HARDENED_OBSIDIAN_PLATE.get()) == 0,
                    "the matrix kept a copy of the result it pushed out");
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 600)
    public static void everyResultOfABatchedRequestFindsItsWayBack(GameTestHelper helper) {
        SequencedAssemblyMatrixBlockEntity matrix = placePoweredMatrix(helper);
        helper.setBlock(TARGET, Blocks.CHEST.defaultBlockState());
        pushPlatePattern(helper);
        pushPlatePattern(helper);
        pushPlatePattern(helper);

        helper.succeedWhen(() -> {
            IItemHandler chest = helper.getLevel()
                    .getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(TARGET), EJECTION.getOpposite());
            helper.assertTrue(chest != null, "the chest exposed no item handler");
            int returned = countIn(chest, NepCreateContent.HARDENED_OBSIDIAN_PLATE.get());
            int stranded = outputCount(matrix, NepCreateContent.HARDENED_OBSIDIAN_PLATE.get());
            helper.assertTrue(
                    returned == 3,
                    "only " + returned + " of 3 batched results came back, " + stranded + " stranded in the output "
                            + "buffer; a request for several items leaves all but the first behind");
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 400)
    public static void theMatrixPushesItsResultBackIntoAPatternProvider(GameTestHelper helper) {
        SequencedAssemblyMatrixBlockEntity matrix = placePoweredMatrix(helper);
        helper.setBlock(TARGET, AEBlocks.PATTERN_PROVIDER.block().defaultBlockState());

        IItemHandler provider = helper.getLevel()
                .getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(TARGET), EJECTION.getOpposite());
        helper.assertTrue(
                provider != null,
                "the pattern provider exposed no item handler on the face touching the matrix; nothing can be "
                        + "returned to it");

        pushPlatePattern(helper);

        helper.succeedWhen(() -> {
            IItemHandler returnInv = helper.getLevel()
                    .getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(TARGET), EJECTION.getOpposite());
            helper.assertTrue(returnInv != null, "the pattern provider stopped exposing its item handler");
            helper.assertTrue(
                    countIn(returnInv, NepCreateContent.HARDENED_OBSIDIAN_PLATE.get()) == 1,
                    "the matrix never returned its result to the pattern provider that pushed the pattern");
            helper.assertTrue(
                    outputCount(matrix, NepCreateContent.HARDENED_OBSIDIAN_PLATE.get()) == 0,
                    "the matrix kept a copy of the result it returned");
        });
    }
}
