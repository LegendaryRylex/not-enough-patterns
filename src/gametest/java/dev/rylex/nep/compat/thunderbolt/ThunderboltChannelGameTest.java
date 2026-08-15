package dev.rylex.nep.compat.thunderbolt;

import appeng.api.networking.GridHelper;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.parts.IPartHost;
import appeng.api.parts.IPartItem;
import appeng.api.util.AEColor;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEParts;
import dev.rylex.nep.Nep;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.hub.NepContent;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Nep.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ThunderboltChannelGameTest {

    private static final String TEMPLATE = "empty_9x6x9";
    private static final String BATCH = "nep_thunderbolt_channels";

    private static final BlockPos HUB = new BlockPos(4, 1, 4);
    private static final BlockPos SECOND_HUB = new BlockPos(3, 1, 5);
    private static final BlockPos CABLE = new BlockPos(4, 1, 5);
    private static final BlockPos CONTROLLER = new BlockPos(4, 1, 6);

    private ThunderboltChannelGameTest() {}

    private static int demand() {
        return NepConfig.machineHubChannels();
    }

    private static void placeHub(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, NepContent.MACHINE_HUB.get());
        helper.assertTrue(helper.getBlockEntity(pos) != null, "the hub at " + pos + " placed no block entity");
    }

    private static void placeCable(GameTestHelper helper, IPartItem<?> cable) {
        helper.setBlock(CABLE, AEBlocks.CABLE_BUS.block());
        BlockEntity be = helper.getBlockEntity(CABLE);
        helper.assertTrue(be instanceof IPartHost, "the cable bus placed no part host");
        ((IPartHost) be).addPart(cable, null, null);
    }

    private static void placeController(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, AEBlocks.CONTROLLER.block());
        helper.setBlock(pos.above(), AEBlocks.CREATIVE_ENERGY_CELL.block());
    }

    private static IGridNode node(GameTestHelper helper, BlockPos pos) {
        IInWorldGridNodeHost host = GridHelper.getNodeHost(helper.getLevel(), helper.absolutePos(pos));
        helper.assertTrue(host != null, "no grid node host at " + pos);
        IGridNode node = host.getGridNode(Direction.NORTH);
        helper.assertTrue(node != null && node.getGrid() != null, "the hub at " + pos + " never joined a grid");
        return node;
    }

    private static void assertHoldsWholeDemand(GameTestHelper helper, String wiring) {
        helper.startSequence()
                .thenIdle(60)
                .thenExecute(() -> {
                    IGridNode node = node(helper, HUB);
                    helper.assertValueEqual(
                            node.getUsedChannels(),
                            demand(),
                            "channels the hub holds " + wiring
                                    + "; Thunderbolt reassigns every channel from its own max flow, so a bare 1 here"
                                    + " means nep's demand never reached its solver");
                    helper.assertTrue(node.isActive(), "the hub never came online " + wiring);
                    helper.assertValueEqual(
                            node.getGrid().getPathingService().getUsedChannels(),
                            demand(),
                            "channels the network reports in use " + wiring);
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void aHubOnAControllerFaceHoldsItsWholeDemand(GameTestHelper helper) {
        placeHub(helper, HUB);
        placeController(helper, CABLE);
        assertHoldsWholeDemand(helper, "on a controller face");
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void aHubDownADenseCableHoldsItsWholeDemand(GameTestHelper helper) {
        placeHub(helper, HUB);
        placeCable(helper, AEParts.SMART_DENSE_CABLE.item(AEColor.TRANSPARENT));
        placeController(helper, CONTROLLER);
        assertHoldsWholeDemand(helper, "down a dense cable");
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void aHubThatCannotBeFullyFedGivesItsChannelsBack(GameTestHelper helper) {
        List<BlockPos> hubs = List.of(HUB, SECOND_HUB);
        for (BlockPos pos : hubs) {
            placeHub(helper, pos);
        }
        placeCable(helper, AEParts.SMART_CABLE.item(AEColor.TRANSPARENT));
        placeController(helper, CONTROLLER);

        helper.startSequence()
                .thenIdle(60)
                .thenExecute(() -> {
                    int online = 0;
                    for (BlockPos pos : hubs) {
                        IGridNode node = node(helper, pos);
                        int held = node.getUsedChannels();
                        if (node.isActive()) {
                            online++;
                            helper.assertValueEqual(held, demand(), "channels the online hub at " + pos + " holds");
                        } else {
                            helper.assertValueEqual(
                                    held,
                                    0,
                                    "channels the offline hub at " + pos + " still holds; a max flow will happily"
                                            + " part fill both hubs, and a hub that cannot be filled has to hand its"
                                            + " share back rather than strand its neighbour too");
                        }
                    }
                    helper.assertValueEqual(
                            online, 1, "hubs online behind a plain cable carrying " + 8 + ", each wanting " + demand());
                })
                .thenSucceed();
    }
}
