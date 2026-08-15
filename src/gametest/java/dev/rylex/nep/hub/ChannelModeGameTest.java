package dev.rylex.nep.hub;

import appeng.api.networking.IGridNode;
import appeng.api.networking.pathing.ChannelMode;
import appeng.core.definitions.AEBlocks;
import appeng.me.service.PathingService;
import dev.rylex.nep.ConfigOverrides;
import dev.rylex.nep.Nep;
import dev.rylex.nep.NepConfig;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Nep.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ChannelModeGameTest {

    private static final String TEMPLATE = "empty_5x5x5";
    private static final String BATCH = "nep_channel_modes";

    private static final BlockPos HUB = new BlockPos(2, 1, 2);
    private static final BlockPos ME_CONTROLLER = new BlockPos(2, 2, 2);
    private static final BlockPos FAR_CONTROLLER = new BlockPos(2, 3, 2);
    private static final BlockPos PROVIDER = new BlockPos(2, 2, 2);

    private static final String CHANNELS_FIELD = "MACHINE_HUB_CHANNELS";
    private static final String CHANNELS_PER_LINK_FIELD = "MACHINE_HUB_CHANNELS_PER_LINK";

    private ChannelModeGameTest() {}

    private static MachineHubBlockEntity hub(GameTestHelper helper) {
        helper.setBlock(HUB, NepContent.MACHINE_HUB.get());
        MachineHubBlockEntity hub = MachineHubBlockEntity.at(helper.getLevel(), helper.absolutePos(HUB));
        helper.assertTrue(hub != null, "the Machine Hub placed no block entity");
        return hub;
    }

    private static IGridNode node(GameTestHelper helper, MachineHubBlockEntity hub) {
        IGridNode node = hub.gridNodeHost().getGridNode(Direction.NORTH);
        helper.assertTrue(node != null && node.getGrid() != null, "the hub never joined a grid");
        return node;
    }

    private static void force(GameTestHelper helper, MachineHubBlockEntity hub, ChannelMode mode) {
        ((PathingService) node(helper, hub).getGrid().getPathingService()).setForcedChannelMode(mode);
    }

    private static void energy(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, AEBlocks.CREATIVE_ENERGY_CELL.block().defaultBlockState());
    }

    private static void atMode(
            GameTestHelper helper, MachineHubBlockEntity hub, ChannelMode mode, Runnable assertions) {
        helper.startSequence()
                .thenIdle(20)
                .thenExecute(() -> force(helper, hub, mode))
                .thenIdle(40)
                .thenExecute(assertions)
                .thenSucceed();
    }

    private static void againstAController(GameTestHelper helper, ChannelMode mode, int demand, boolean expectOnline) {
        ConfigOverrides.Restore restore = ConfigOverrides.override(CHANNELS_FIELD, demand);
        MachineHubBlockEntity hub = hub(helper);
        restore.undo();
        helper.setBlock(ME_CONTROLLER, AEBlocks.CONTROLLER.block());
        energy(helper, ME_CONTROLLER.above());

        atMode(helper, hub, mode, () -> {
            IGridNode node = node(helper, hub);
            helper.assertValueEqual(
                    ((PathingService) node.getGrid().getPathingService()).getChannelMode(),
                    mode,
                    "the channel mode the grid ended up on");
            helper.assertValueEqual(
                    node.isActive(),
                    expectOnline,
                    "whether a hub wanting " + demand + " channels is online at " + mode + ", where one dense cable"
                            + " carries " + 32 * mode.getCableCapacityFactor());
            if (expectOnline) {
                helper.assertValueEqual(
                        node.getGrid().getPathingService().getUsedChannels(),
                        demand,
                        "channels the network reports in use at " + mode);
            }
        });
    }

    private static void behindAPlainDevice(GameTestHelper helper, ChannelMode mode, int demand, boolean expectOnline) {
        ConfigOverrides.Restore restore = ConfigOverrides.override(CHANNELS_FIELD, demand);
        MachineHubBlockEntity hub = hub(helper);
        restore.undo();
        helper.setBlock(PROVIDER, AEBlocks.PATTERN_PROVIDER.block());
        helper.setBlock(FAR_CONTROLLER, AEBlocks.CONTROLLER.block());
        energy(helper, FAR_CONTROLLER.above());

        atMode(helper, hub, mode, () -> {
            IGridNode node = node(helper, hub);
            helper.assertValueEqual(
                    node.isActive(),
                    expectOnline,
                    "whether a hub wanting " + demand + " channels is online behind a pattern provider at " + mode
                            + ", where one plain device carries " + 8 * mode.getCableCapacityFactor());
            if (expectOnline) {
                helper.assertValueEqual(
                        node.getGrid().getPathingService().getUsedChannels(),
                        demand + 1,
                        "channels the network reports in use at " + mode);
            }
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void aHubBillsTheSameChannelsWhenCablesCarryTwiceAsMany(GameTestHelper helper) {
        againstAController(helper, ChannelMode.X2, NepConfig.machineHubChannels(), true);
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void aHubBillsTheSameChannelsWhenCablesCarryFourTimesAsMany(GameTestHelper helper) {
        againstAController(helper, ChannelMode.X4, NepConfig.machineHubChannels(), true);
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void aHubWantingMoreThanOneDenseCableCarriesStaysOffline(GameTestHelper helper) {
        againstAController(helper, ChannelMode.DEFAULT, 33, false);
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void aHubWantingMoreThanOneDenseCableCarriesComesOnlineOnceCablesCarryIt(GameTestHelper helper) {
        againstAController(helper, ChannelMode.X4, 33, true);
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void aHubMayTakeUpAWholeQuadrupledDenseCable(GameTestHelper helper) {
        againstAController(helper, ChannelMode.X4, 128, true);
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void aHubIsHeldToWhatThePlainDeviceInFrontOfItCarries(GameTestHelper helper) {
        behindAPlainDevice(helper, ChannelMode.X2, 20, false);
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void aHubBehindAPlainDeviceComesOnlineOnceThatDeviceCarriesEnough(GameTestHelper helper) {
        behindAPlainDevice(helper, ChannelMode.X4, 20, true);
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void aHubClaimsItsWholeDemandFromAnAdHocNetworkThatCanCarryIt(GameTestHelper helper) {
        MachineHubBlockEntity hub = hub(helper);
        energy(helper, HUB.above());

        int expected = NepConfig.machineHubChannels();

        atMode(helper, hub, ChannelMode.X4, () -> {
            IGridNode node = node(helper, hub);
            helper.assertTrue(
                    node.isActive(),
                    "a hub wanting " + expected + " channels never came online on an ad-hoc network that carries 32"
                            + " at X4");
            helper.assertValueEqual(
                    node.getGrid().getPathingService().getUsedChannels(),
                    expected,
                    "channels the ad-hoc network reports in use at X4");
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void aLinkedHubStillBillsEveryLinkWhenCablesCarryFourTimesAsMany(GameTestHelper helper) {
        ConfigOverrides.Restore restore = ConfigOverrides.override(CHANNELS_PER_LINK_FIELD, 4);
        MachineHubBlockEntity hub = hub(helper);
        restore.undo();
        helper.setBlock(ME_CONTROLLER, AEBlocks.CONTROLLER.block());
        energy(helper, ME_CONTROLLER.above());
        helper.setBlock(HUB.north(), Blocks.CHEST);
        hub.applyPlan(List.of(new HubLink(helper.absolutePos(HUB.north()), HubRole.INPUT)));

        int expected = NepConfig.machineHubChannels() + 4;

        atMode(helper, hub, ChannelMode.X4, () -> {
            IGridNode node = node(helper, hub);
            helper.assertTrue(node.isActive(), "a hub with one link never came online at X4");
            helper.assertValueEqual(
                    node.getGrid().getPathingService().getUsedChannels(),
                    expected,
                    "channels the network reports in use at X4 for a hub with one link");
        });
    }
}
