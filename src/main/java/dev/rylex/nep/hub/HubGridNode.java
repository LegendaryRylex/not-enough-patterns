package dev.rylex.nep.hub;

import appeng.api.networking.GridFlags;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.IManagedGridNode;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.machine.ChannelDemand;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

final class HubGridNode implements IInWorldGridNodeHost, ChannelDemand {

    private static final double IDLE_ME_DRAIN = 10.0;

    private static final IGridNodeListener<HubGridNode> LISTENER = new Listener();

    private final MachineHubBlockEntity owner;
    private final IManagedGridNode mainNode;

    private final int baseChannels;
    private final int channelsPerLink;

    HubGridNode(MachineHubBlockEntity owner) {
        this.owner = owner;
        this.baseChannels = NepConfig.machineHubChannels();
        this.channelsPerLink = NepConfig.machineHubChannelsPerLink();
        IManagedGridNode node = GridHelper.createManagedNode(this, LISTENER)
                .setInWorldNode(true)
                .setTagName("proxy")
                .setIdlePowerUsage(IDLE_ME_DRAIN)
                .setVisualRepresentation(NepContent.MACHINE_HUB_ITEM.get());
        if (channelDemand() > 0) {
            node.setFlags(GridFlags.REQUIRE_CHANNEL, GridFlags.DENSE_CAPACITY);
        }
        this.mainNode = node;
    }

    @Override
    public int channelDemand() {
        return baseChannels + channelsPerLink * owner.links().size();
    }

    void create(Level level, BlockPos pos) {
        if (mainNode.getNode() == null) {
            mainNode.create(level, pos);
        }
    }

    void destroy() {
        mainNode.destroy();
    }

    @Nullable
    IGrid grid() {
        return mainNode.getGrid();
    }

    boolean connected() {
        IGridNode node = mainNode.getNode();
        return node != null && !node.getConnections().isEmpty();
    }

    boolean isActive() {
        return mainNode.isActive();
    }

    boolean missingChannel() {
        IGridNode node = mainNode.getNode();
        return node != null && !node.meetsChannelRequirements();
    }

    void repath() {
        IGrid grid = mainNode.getGrid();
        if (grid != null) {
            grid.getPathingService().repath();
        }
    }

    void save(ValueOutput output) {
        mainNode.serialize(output);
    }

    void load(ValueInput input) {
        mainNode.deserialize(input);
    }

    @Nullable
    @Override
    public IGridNode getGridNode(Direction dir) {
        return mainNode.getNode();
    }

    private static final class Listener implements IGridNodeListener<HubGridNode> {
        @Override
        public void onSaveChanges(HubGridNode node, IGridNode gridNode) {
            node.owner.setChanged();
        }
    }
}
