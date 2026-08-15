package dev.rylex.nep.machine;

import appeng.api.config.Actionable;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Clearable;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jetbrains.annotations.Nullable;

public abstract class BufferedMatrixBlockEntity extends BlockEntity implements MenuProvider, MatrixHost, Clearable {

    private static final String CHANNEL_FAULT_KEY = "ChannelFault";

    protected final MatrixBuffer inputBuffer;
    protected final MatrixBuffer outputBuffer;
    protected final MatrixGridNode power;

    protected final Map<Item, Long> owed = new HashMap<>();
    protected final Map<Item, Long> toReturn = new HashMap<>();
    protected final List<GenericStack> missingInputs = new ArrayList<>();
    protected final List<ItemStack> claimedItems = new ArrayList<>();
    protected final PushingCpus pushingCpus = new PushingCpus();
    protected final ReturnDirections returnDirections = new ReturnDirections();

    protected ItemStack activeResult = ItemStack.EMPTY;
    protected boolean powerFault;
    protected boolean channelFault;
    protected boolean outputBlocked;
    protected boolean scanNeeded = true;
    protected int lastComparator = -1;
    protected int syncedSignature = Integer.MIN_VALUE;
    protected int runningGrace;

    protected BufferedMatrixBlockEntity(
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState state,
            int inputSlots,
            int outputSlots,
            Item nodeVisual,
            int idleMeDrain,
            int channels,
            String nodeLabel) {
        super(type, pos, state);
        this.inputBuffer = new MatrixBuffer(inputSlots, this::onBufferChanged);
        this.outputBuffer = new MatrixBuffer(outputSlots, this::onBufferChanged);
        this.power = new MatrixGridNode(this, nodeVisual, idleMeDrain, channels, nodeLabel);
    }

    private void onBufferChanged() {
        markScanNeeded();
        setChanged();
    }

    public int comparatorOutput() {
        return ComparatorSignal.of(outputBuffer);
    }

    protected abstract int readoutSignature();

    public abstract float craftProgress();

    public void markScanNeeded() {
        scanNeeded = true;
    }

    @Override
    public void clearContent() {
        clearHandler(inputBuffer);
        clearHandler(outputBuffer);
        claimedItems.clear();
        missingInputs.clear();
        owed.clear();
        toReturn.clear();
        activeResult = ItemStack.EMPTY;
    }

    protected static void clearHandler(MatrixBuffer handler) {
        for (int slot = 0; slot < handler.size(); slot++) {
            handler.clear(slot);
        }
    }

    public boolean hasPending() {
        return !owed.isEmpty();
    }

    public boolean hasPowerFault() {
        return powerFault;
    }

    public boolean hasChannelFault() {
        return channelFault;
    }

    public boolean isOutputBlocked() {
        return outputBlocked;
    }

    public ItemStack activeResult() {
        return activeResult;
    }

    public List<GenericStack> missingInputs() {
        return List.copyOf(missingInputs);
    }

    public IInWorldGridNodeHost gridNodeHost() {
        return power;
    }

    public MatrixBuffer getInputBuffer() {
        return inputBuffer;
    }

    public MatrixBuffer getOutputBuffer() {
        return outputBuffer;
    }

    protected void setPowerFault(boolean fault) {
        boolean missingChannel = fault && power.missingChannel();
        if (powerFault == fault && channelFault == missingChannel) {
            return;
        }
        powerFault = fault;
        channelFault = missingChannel;
        if (!fault) {
            markScanNeeded();
        }
        setChanged();
    }

    protected void refreshComparator(Level level) {
        int signal = comparatorOutput();
        if (signal != lastComparator) {
            lastComparator = signal;
            level.updateNeighbourForOutputSignal(getBlockPos(), getBlockState().getBlock());
        }
    }

    protected void syncIfChanged(Level level) {
        int signature = 31 * readoutSignature() + Boolean.hashCode(channelFault);
        if (signature != syncedSignature) {
            syncedSignature = signature;
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    protected void setMissingInputs(List<GenericStack> missing) {
        if (!missingInputs.equals(missing)) {
            missingInputs.clear();
            missingInputs.addAll(missing);
            setChanged();
        }
    }

    protected boolean fitsInOutput(ItemStack result) {
        try (Transaction tx = Transaction.openRoot()) {
            return ResourceHandlerUtil.insertStacking(outputBuffer, ItemResource.of(result), result.getCount(), tx)
                    == result.getCount();
        }
    }

    protected boolean bufferAll(Map<AEItemKey, Long> items) {
        try (Transaction tx = Transaction.openRoot()) {
            for (Map.Entry<AEItemKey, Long> entry : items.entrySet()) {
                int want = clampToInt(entry.getValue());
                if (ResourceHandlerUtil.insertStacking(inputBuffer, resourceOf(entry.getKey()), want, tx) != want) {
                    return false;
                }
            }
            tx.commit();
            return true;
        }
    }

    protected static ItemResource resourceOf(AEItemKey key) {
        return ItemResource.of(key.toStack(1));
    }

    protected static int clampToInt(long amount) {
        return (int) Math.min(Math.max(amount, 0L), Integer.MAX_VALUE);
    }

    @Override
    public long bufferedAmount(AEKey key) {
        long total = 0;
        if (key instanceof AEItemKey itemKey) {
            for (int slot = 0; slot < inputBuffer.size(); slot++) {
                ItemStack stack = inputBuffer.stackAt(slot);
                if (!stack.isEmpty() && itemKey.matches(stack)) {
                    total += stack.getCount();
                }
            }
            for (ItemStack stack : claimedItems) {
                if (!stack.isEmpty() && itemKey.matches(stack)) {
                    total += stack.getCount();
                }
            }
        }
        return total;
    }

    @Override
    public long acceptCrafted(AEKey what, long amount, Actionable mode) {
        if (!(what instanceof AEItemKey key)) {
            return amount;
        }
        try (Transaction tx = Transaction.openRoot()) {
            int inserted = ResourceHandlerUtil.insertStacking(inputBuffer, resourceOf(key), clampToInt(amount), tx);
            if (mode == Actionable.MODULATE) {
                tx.commit();
            }
            return amount - inserted;
        }
    }

    protected void flushOutput(Level level) {
        if (returnDirections.isEmpty() || toReturn.isEmpty()) {
            return;
        }
        for (int slot = 0; slot < outputBuffer.size(); slot++) {
            ItemResource resource = outputBuffer.getResource(slot);
            if (resource.isEmpty()) {
                continue;
            }
            Item item = resource.getItem();
            long pending = toReturn.getOrDefault(item, 0L);
            if (pending <= 0) {
                continue;
            }
            ResourceHandler<ItemResource> target = returnDirections.targetFor(level, getBlockPos(), item);
            if (target == null) {
                continue;
            }
            int want = (int) Math.min(pending, outputBuffer.getAmountAsInt(slot));
            int moved = 0;
            try (Transaction tx = Transaction.openRoot()) {
                int inserted = ResourceHandlerUtil.insertStacking(target, resource, want, tx);
                if (inserted > 0 && outputBuffer.extract(slot, resource, inserted, tx) == inserted) {
                    moved = inserted;
                    tx.commit();
                }
            }
            if (moved > 0) {
                decrement(toReturn, item, moved);
                if (!toReturn.containsKey(item) && !owed.containsKey(item)) {
                    returnDirections.forget(item);
                }
            }
        }
    }

    protected static void emptyHandlerTo(Player player, MatrixBuffer handler) {
        for (int slot = 0; slot < handler.size(); slot++) {
            ItemStack stack = handler.stackAt(slot);
            if (stack.isEmpty()) {
                continue;
            }
            handler.clear(slot);
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
        }
    }

    protected static void decrement(Map<Item, Long> map, Item item, long amount) {
        long remaining = map.getOrDefault(item, 0L) - amount;
        if (remaining > 0) {
            map.put(item, remaining);
        } else {
            map.remove(item);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean(CHANNEL_FAULT_KEY, channelFault);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        channelFault = input.getBooleanOr(CHANNEL_FAULT_KEY, false);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        power.destroy();
    }
}
