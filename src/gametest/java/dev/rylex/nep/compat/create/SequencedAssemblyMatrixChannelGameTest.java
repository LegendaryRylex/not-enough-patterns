package dev.rylex.nep.compat.create;

import appeng.api.networking.IGridNode;
import appeng.api.networking.pathing.ChannelMode;
import appeng.api.parts.IPartHost;
import appeng.api.util.AEColor;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEParts;
import appeng.me.service.PathingService;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import dev.rylex.nep.Nep;
import dev.rylex.nep.NepConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Nep.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SequencedAssemblyMatrixChannelGameTest {

    private static final String TEMPLATE = "empty_9x6x9";
    private static final String BATCH = "nep_matrix_channels";

    private static final BlockPos MATRIX = new BlockPos(4, 1, 4);
    private static final BlockPos NEAR = new BlockPos(4, 1, 5);
    private static final BlockPos FAR = new BlockPos(4, 1, 6);

    private SequencedAssemblyMatrixChannelGameTest() {}

    private static SequencedAssemblyMatrixBlockEntity placeMatrix(GameTestHelper helper) {
        helper.setBlock(
                MATRIX,
                NepCreateContent.MATRIX
                        .get()
                        .defaultBlockState()
                        .setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.Z));
        BlockEntity be = helper.getBlockEntity(MATRIX);
        helper.assertTrue(
                be instanceof SequencedAssemblyMatrixBlockEntity, "the matrix did not create its block entity");
        return (SequencedAssemblyMatrixBlockEntity) be;
    }

    private static void placeDenseCable(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, AEBlocks.CABLE_BUS.block());
        BlockEntity be = helper.getBlockEntity(pos);
        helper.assertTrue(be instanceof IPartHost, "the cable bus placed no part host");
        ((IPartHost) be).addPart(AEParts.SMART_DENSE_CABLE.item(AEColor.TRANSPARENT), null, null);
    }

    private static IGridNode node(GameTestHelper helper, SequencedAssemblyMatrixBlockEntity matrix) {
        IGridNode node = matrix.gridNodeHost().getGridNode(Direction.NORTH);
        helper.assertTrue(node != null && node.getGrid() != null, "the matrix never joined a grid");
        return node;
    }

    private static void assertOnline(
            GameTestHelper helper,
            SequencedAssemblyMatrixBlockEntity matrix,
            ChannelMode mode,
            boolean expectOnline,
            String wiring) {
        int demand = NepConfig.createSequencedAssemblyMatrixChannels();
        helper.startSequence()
                .thenIdle(20)
                .thenExecute(
                        () -> ((PathingService) node(helper, matrix).getGrid().getPathingService())
                                .setForcedChannelMode(mode))
                .thenIdle(40)
                .thenExecute(() -> {
                    IGridNode node = node(helper, matrix);
                    helper.assertValueEqual(
                            ((PathingService) node.getGrid().getPathingService()).getChannelMode(),
                            mode,
                            "the channel mode the grid ended up on");
                    helper.assertValueEqual(
                            node.isActive(),
                            expectOnline,
                            "whether a matrix wanting " + demand + " channels is online at " + mode + " wired as "
                                    + wiring + ", where one dense cable carries "
                                    + 32 * mode.getCableCapacityFactor() + " and an ad-hoc network carries "
                                    + mode.getAdHocNetworkChannels());
                    if (expectOnline) {
                        helper.assertValueEqual(
                                node.getUsedChannels(),
                                demand,
                                "channels the matrix ended up holding at " + mode + " wired as " + wiring);
                    }
                })
                .thenSucceed();
    }

    private static void throughAController(GameTestHelper helper, ChannelMode mode) {
        SequencedAssemblyMatrixBlockEntity matrix = placeMatrix(helper);
        helper.setBlock(NEAR, AEBlocks.CONTROLLER.block());
        helper.setBlock(FAR, AEBlocks.CREATIVE_ENERGY_CELL.block());
        assertOnline(helper, matrix, mode, true, "power to controller to matrix");
    }

    private static void throughAControllerAndDenseCable(GameTestHelper helper, ChannelMode mode) {
        SequencedAssemblyMatrixBlockEntity matrix = placeMatrix(helper);
        placeDenseCable(helper, NEAR);
        helper.setBlock(FAR, AEBlocks.CONTROLLER.block());
        helper.setBlock(FAR.above(), AEBlocks.CREATIVE_ENERGY_CELL.block());
        assertOnline(helper, matrix, mode, true, "power to controller to dense to matrix");
    }

    private static void throughADenseCableAlone(GameTestHelper helper, ChannelMode mode, boolean expectOnline) {
        SequencedAssemblyMatrixBlockEntity matrix = placeMatrix(helper);
        placeDenseCable(helper, NEAR);
        helper.setBlock(FAR, AEBlocks.CREATIVE_ENERGY_CELL.block());
        assertOnline(helper, matrix, mode, expectOnline, "power to dense to matrix");
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void aMatrixOnAControllerFaceComesOnline(GameTestHelper helper) {
        throughAController(helper, ChannelMode.DEFAULT);
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void aMatrixOnAControllerFaceComesOnlineWhenCablesCarryFourTimesAsMany(GameTestHelper helper) {
        throughAController(helper, ChannelMode.X4);
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void aMatrixDownADenseCableComesOnline(GameTestHelper helper) {
        throughAControllerAndDenseCable(helper, ChannelMode.DEFAULT);
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void aMatrixDownADenseCableComesOnlineWhenCablesCarryFourTimesAsMany(GameTestHelper helper) {
        throughAControllerAndDenseCable(helper, ChannelMode.X4);
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void aMatrixOnAnAdHocDenseCableStaysOfflineWhileTheNetworkCarriesEight(GameTestHelper helper) {
        throughADenseCableAlone(helper, ChannelMode.DEFAULT, false);
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void aMatrixOnAnAdHocDenseCableComesOnlineOnceTheNetworkCarriesThirtyTwo(GameTestHelper helper) {
        throughADenseCableAlone(helper, ChannelMode.X4, true);
    }
}
