package dev.rylex.nep.compat.malum;

import appeng.api.networking.IGridNode;
import appeng.api.networking.pathing.ChannelMode;
import appeng.api.parts.IPartHost;
import appeng.api.util.AEColor;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEParts;
import appeng.me.service.PathingService;
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
public final class FocusedSpiritMatrixChannelGameTest {

    private static final String TEMPLATE = "empty_5x5x5";
    private static final String BATCH = "nep_focused_spirit_channels";

    private static final BlockPos MATRIX = new BlockPos(2, 1, 1);
    private static final BlockPos NEAR = new BlockPos(2, 1, 2);
    private static final BlockPos FAR = new BlockPos(2, 1, 3);

    private FocusedSpiritMatrixChannelGameTest() {}

    private static FocusedSpiritMatrixBlockEntity placeMatrix(GameTestHelper helper) {
        helper.setBlock(MATRIX, NepMalumContent.MATRIX.get().defaultBlockState());
        BlockEntity be = helper.getBlockEntity(MATRIX);
        helper.assertTrue(be instanceof FocusedSpiritMatrixBlockEntity, "the Matrix did not create its block entity");
        return (FocusedSpiritMatrixBlockEntity) be;
    }

    private static void placeDenseCable(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, AEBlocks.CABLE_BUS.block());
        BlockEntity be = helper.getBlockEntity(pos);
        helper.assertTrue(be instanceof IPartHost, "the cable bus placed no part host");
        ((IPartHost) be).addPart(AEParts.SMART_DENSE_CABLE.item(AEColor.TRANSPARENT), null, null);
    }

    private static IGridNode node(GameTestHelper helper, FocusedSpiritMatrixBlockEntity matrix) {
        IGridNode node = matrix.gridNodeHost().getGridNode(Direction.NORTH);
        helper.assertTrue(node != null && node.getGrid() != null, "the Matrix never joined a grid");
        return node;
    }

    private static void assertOnline(
            GameTestHelper helper,
            FocusedSpiritMatrixBlockEntity matrix,
            ChannelMode mode,
            boolean expectOnline,
            String wiring) {
        int demand = NepConfig.malumFocusedSpiritMatrixChannels();
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
                            "whether a Matrix wanting " + demand + " channels is online at " + mode + " wired as "
                                    + wiring + ", where one dense cable carries "
                                    + 32 * mode.getCableCapacityFactor() + " and an ad-hoc network carries "
                                    + mode.getAdHocNetworkChannels());
                    if (expectOnline) {
                        helper.assertValueEqual(
                                node.getUsedChannels(),
                                demand,
                                "channels the Matrix ended up holding at " + mode + " wired as " + wiring);
                    }
                })
                .thenSucceed();
    }

    private static void throughAController(GameTestHelper helper, ChannelMode mode) {
        FocusedSpiritMatrixBlockEntity matrix = placeMatrix(helper);
        helper.setBlock(NEAR, AEBlocks.CONTROLLER.block());
        helper.setBlock(FAR, AEBlocks.CREATIVE_ENERGY_CELL.block());
        assertOnline(helper, matrix, mode, true, "power to controller to matrix");
    }

    private static void throughAControllerAndDenseCable(GameTestHelper helper, ChannelMode mode) {
        FocusedSpiritMatrixBlockEntity matrix = placeMatrix(helper);
        placeDenseCable(helper, NEAR);
        helper.setBlock(FAR, AEBlocks.CONTROLLER.block());
        helper.setBlock(FAR.above(), AEBlocks.CREATIVE_ENERGY_CELL.block());
        assertOnline(helper, matrix, mode, true, "power to controller to dense to matrix");
    }

    private static void throughADenseCableAlone(GameTestHelper helper, ChannelMode mode, boolean expectOnline) {
        FocusedSpiritMatrixBlockEntity matrix = placeMatrix(helper);
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
    public static void aMatrixOnAnAdHocDenseCableStaysOfflineWhileTheNetworkCarriesEight(GameTestHelper helper) {
        throughADenseCableAlone(helper, ChannelMode.DEFAULT, false);
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void aMatrixOnAnAdHocDenseCableComesOnlineOnceTheNetworkCarriesThirtyTwo(GameTestHelper helper) {
        throughADenseCableAlone(helper, ChannelMode.X4, true);
    }
}
