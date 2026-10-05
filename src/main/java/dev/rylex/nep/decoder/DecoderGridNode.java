package dev.rylex.nep.decoder;

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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class DecoderGridNode implements IInWorldGridNodeHost, ChannelDemand {

    private static final IGridNodeListener<DecoderGridNode> LISTENER = new Listener();

    private final PatternDecoderBlockEntity owner;
    private final IManagedGridNode mainNode;
    private final int channels;

    DecoderGridNode(PatternDecoderBlockEntity owner) {
        this.owner = owner;
        this.channels = NepConfig.patternDecoderChannels();
        IManagedGridNode node = GridHelper.createManagedNode(this, LISTENER)
                .setInWorldNode(true)
                .setTagName("proxy")
                .setIdlePowerUsage(NepConfig.patternDecoderIdleMeDrain())
                .setVisualRepresentation(DecoderContent.PATTERN_DECODER_ITEM.get());
        if (channels > 0) {
            node.setFlags(GridFlags.REQUIRE_CHANNEL);
        }
        this.mainNode = node;
    }

    int modules() {
        return owner.modules();
    }

    @Override
    public int channelDemand() {
        return channels;
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

    DecoderStatus status() {
        IGridNode node = mainNode.getNode();
        if (node == null || node.getConnections().isEmpty()) {
            return DecoderStatus.NO_NETWORK;
        }
        if (!node.meetsChannelRequirements()) {
            return DecoderStatus.NO_CHANNEL;
        }
        return mainNode.isActive() ? DecoderStatus.ONLINE : DecoderStatus.OFFLINE;
    }

    void save(CompoundTag tag) {
        mainNode.saveToNBT(tag);
    }

    void load(CompoundTag tag) {
        mainNode.loadFromNBT(tag);
    }

    @Nullable
    @Override
    public IGridNode getGridNode(Direction dir) {
        return mainNode.getNode();
    }

    private static final class Listener implements IGridNodeListener<DecoderGridNode> {
        @Override
        public void onSaveChanges(DecoderGridNode node, IGridNode gridNode) {
            node.owner.setChanged();
        }
    }
}
