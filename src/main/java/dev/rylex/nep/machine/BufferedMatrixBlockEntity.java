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
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

public abstract class BufferedMatrixBlockEntity extends BlockEntity implements MenuProvider, MatrixHost, Clearable {

    private static final String CHANNEL_FAULT_KEY = "ChannelFault";

    protected final ItemStackHandler inputBuffer;
    protected final ItemStackHandler outputBuffer;
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
        this.inputBuffer = new OverstackedItemHandler(inputSlots) {
            @Override
            protected void onContentsChanged(int slot) {
                markScanNeeded();
                setChanged();
            }
        };
        this.outputBuffer = new OverstackedItemHandler(outputSlots) {
            @Override
            protected void onContentsChanged(int slot) {
                markScanNeeded();
                setChanged();
            }
        };
        this.power = new MatrixGridNode(this, nodeVisual, idleMeDrain, channels, nodeLabel);
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

    protected static void clearHandler(ItemStackHandler handler) {
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            handler.setStackInSlot(slot, ItemStack.EMPTY);
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

    public IItemHandler getInputBuffer() {
        return inputBuffer;
    }

    public IItemHandler getOutputBuffer() {
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
        ItemStackHandler probe = new OverstackedItemHandler(outputBuffer.getSlots());
        for (int slot = 0; slot < outputBuffer.getSlots(); slot++) {
            probe.setStackInSlot(slot, outputBuffer.getStackInSlot(slot).copy());
        }
        return ItemHandlerHelper.insertItem(probe, result.copy(), false).isEmpty();
    }

    protected boolean bufferAll(Map<AEItemKey, Long> items) {
        List<ItemStack> stacks = new ArrayList<>();
        for (Map.Entry<AEItemKey, Long> entry : items.entrySet()) {
            long count = entry.getValue();
            while (count > 0) {
                int chunk = (int) Math.min(count, OverstackedItemHandler.SLOT_LIMIT);
                stacks.add(entry.getKey().toStack(chunk));
                count -= chunk;
            }
        }
        ItemStackHandler probe = new OverstackedItemHandler(inputBuffer.getSlots());
        for (int slot = 0; slot < inputBuffer.getSlots(); slot++) {
            probe.setStackInSlot(slot, inputBuffer.getStackInSlot(slot).copy());
        }
        for (ItemStack stack : stacks) {
            if (!ItemHandlerHelper.insertItem(probe, stack.copy(), false).isEmpty()) {
                return false;
            }
        }
        for (ItemStack stack : stacks) {
            ItemHandlerHelper.insertItem(inputBuffer, stack, false);
        }
        return true;
    }

    @Override
    public long bufferedAmount(AEKey key) {
        long total = 0;
        if (key instanceof AEItemKey itemKey) {
            for (int slot = 0; slot < inputBuffer.getSlots(); slot++) {
                ItemStack stack = inputBuffer.getStackInSlot(slot);
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
        boolean simulate = mode == Actionable.SIMULATE;
        ItemStackHandler target = inputBuffer;
        if (simulate) {
            target = new OverstackedItemHandler(inputBuffer.getSlots());
            for (int slot = 0; slot < inputBuffer.getSlots(); slot++) {
                target.setStackInSlot(slot, inputBuffer.getStackInSlot(slot).copy());
            }
        }
        int maxStack = OverstackedItemHandler.SLOT_LIMIT;
        long remaining = amount;
        while (remaining > 0) {
            int chunk = (int) Math.min(remaining, maxStack);
            ItemStack leftover = ItemHandlerHelper.insertItem(target, key.toStack(chunk), false);
            int inserted = chunk - leftover.getCount();
            if (inserted <= 0) {
                break;
            }
            remaining -= inserted;
        }
        if (!simulate && remaining < amount) {
            markScanNeeded();
            setChanged();
        }
        return remaining;
    }

    protected void flushOutput(Level level) {
        if (returnDirections.isEmpty() || toReturn.isEmpty()) {
            return;
        }
        for (int slot = 0; slot < outputBuffer.getSlots(); slot++) {
            ItemStack stack = outputBuffer.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            long pending = toReturn.getOrDefault(stack.getItem(), 0L);
            if (pending <= 0) {
                continue;
            }
            IItemHandler target = returnDirections.targetFor(level, getBlockPos(), stack.getItem());
            if (target == null) {
                continue;
            }
            int want = (int) Math.min(pending, stack.getCount());
            ItemStack remainder = ItemHandlerHelper.insertItem(target, stack.copyWithCount(want), false);
            int moved = want - remainder.getCount();
            if (moved > 0) {
                outputBuffer.extractItem(slot, moved, false);
                decrement(toReturn, stack.getItem(), moved);
                if (!toReturn.containsKey(stack.getItem()) && !owed.containsKey(stack.getItem())) {
                    returnDirections.forget(stack.getItem());
                }
            }
        }
    }

    protected static void loadBuffer(ItemStackHandler handler, HolderLookup.Provider registries, CompoundTag tag) {
        CompoundTag sized = tag.copy();
        sized.putInt("Size", handler.getSlots());
        handler.deserializeNBT(registries, sized);
    }

    protected static void emptyHandlerTo(Player player, ItemStackHandler handler) {
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            handler.setStackInSlot(slot, ItemStack.EMPTY);
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
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean(CHANNEL_FAULT_KEY, channelFault);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        channelFault = tag.getBoolean(CHANNEL_FAULT_KEY);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
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
