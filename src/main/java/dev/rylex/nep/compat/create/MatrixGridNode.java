package dev.rylex.nep.compat.create;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.networking.crafting.ICraftingRequester;
import appeng.api.networking.crafting.ICraftingService;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.storage.MEStorage;
import appeng.helpers.MultiCraftingTracker;
import com.google.common.collect.ImmutableSet;
import dev.rylex.nep.Nep;
import dev.rylex.nep.NepConfig;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

final class MatrixGridNode implements IInWorldGridNodeHost, ICraftingRequester {

    static final int TRACKER_SIZE = 18;

    private static final IGridNodeListener<MatrixGridNode> LISTENER = new Listener();

    private final SequencedAssemblyMatrixBlockEntity owner;
    private final IManagedGridNode mainNode;
    private final IActionSource source;

    private MultiCraftingTracker tracker;

    MatrixGridNode(SequencedAssemblyMatrixBlockEntity owner) {
        this.owner = owner;
        this.mainNode = GridHelper.createManagedNode(this, LISTENER)
                .setInWorldNode(true)
                .setTagName("proxy")
                .setVisualRepresentation(NepCreateContent.MATRIX_ITEM.get())
                .addService(ICraftingRequester.class, this);
        this.tracker = new MultiCraftingTracker(this, TRACKER_SIZE);
        this.source = IActionSource.ofMachine(this);
    }

    void create(Level level, BlockPos pos) {
        if (mainNode.getNode() == null) {
            mainNode.setIdlePowerUsage(NepConfig.createSequencedAssemblyMatrixIdlePower());
            mainNode.create(level, pos);
        }
    }

    double extractPower(double amount) {
        IGrid grid = mainNode.getGrid();
        if (grid == null) {
            return 0;
        }
        return grid.getEnergyService().extractAEPower(amount, Actionable.MODULATE, PowerMultiplier.CONFIG);
    }

    List<GenericStack> restock(Level level, List<AEKey> keys, List<Long> targets) {
        List<GenericStack> unavailable = new ArrayList<>();
        IGrid grid = mainNode.getGrid();
        if (grid == null) {
            if (NepConfig.debugLogging()) {
                Nep.LOGGER.info(
                        "SA matrix {}: not on a grid yet (node ready={}, active={})",
                        owner.getBlockPos(),
                        mainNode.isReady(),
                        mainNode.isActive());
            }
            for (int slot = 0; slot < keys.size() && slot < TRACKER_SIZE; slot++) {
                AEKey key = keys.get(slot);
                long missing = targets.get(slot) - owner.bufferedAmount(key);
                if (missing > 0) {
                    unavailable.add(new GenericStack(key, missing));
                }
            }
            return unavailable;
        }
        MEStorage storage = grid.getStorageService().getInventory();
        ICraftingService crafting = grid.getCraftingService();
        for (int slot = 0; slot < keys.size() && slot < TRACKER_SIZE; slot++) {
            AEKey key = keys.get(slot);
            long missing = targets.get(slot) - owner.bufferedAmount(key);
            if (missing <= 0) {
                continue;
            }
            long capacity = missing - owner.acceptCrafted(key, missing, Actionable.SIMULATE);
            long pulled = 0;
            if (capacity > 0) {
                long extracted = storage.extract(key, capacity, Actionable.MODULATE, source);
                if (extracted > 0) {
                    long refused = owner.acceptCrafted(key, extracted, Actionable.MODULATE);
                    if (refused > 0) {
                        storage.insert(key, refused, Actionable.MODULATE, source);
                    }
                    pulled = extracted - refused;
                    missing -= pulled;
                }
            }
            boolean craftStarted = false;
            boolean craftable = false;
            if (missing > 0) {
                craftable = crafting.isCraftable(key);
                craftStarted = tracker.handleCrafting(slot, key, missing, level, crafting, source);
                if (!craftable && capacity > 0) {
                    unavailable.add(new GenericStack(key, missing));
                }
            }
            if (NepConfig.debugLogging()) {
                Nep.LOGGER.info(
                        "SA matrix {} slot {}: {} -> pulled {} from stock, still-missing {}, craftable={}, craftStarted={}",
                        owner.getBlockPos(),
                        slot,
                        key,
                        pulled,
                        Math.max(0, missing),
                        craftable,
                        craftStarted);
            }
        }
        return unavailable;
    }

    void cancelRequests() {
        for (ICraftingLink link : tracker.getRequestedJobs()) {
            link.cancel();
        }
        tracker = new MultiCraftingTracker(this, TRACKER_SIZE);
    }

    long dumpToNetwork(AEKey key, long amount) {
        IGrid grid = mainNode.getGrid();
        if (grid == null) {
            return 0;
        }
        return grid.getStorageService().getInventory().insert(key, amount, Actionable.MODULATE, source);
    }

    void destroy() {
        mainNode.destroy();
    }

    boolean isPowered() {
        return mainNode.isActive();
    }

    void save(CompoundTag tag) {
        mainNode.saveToNBT(tag);
        tracker.writeToNBT(tag);
    }

    void load(CompoundTag tag) {
        mainNode.loadFromNBT(tag);
        tracker.readFromNBT(tag);
    }

    @Override
    public IGridNode getActionableNode() {
        return mainNode.getNode();
    }

    @Override
    public ImmutableSet<ICraftingLink> getRequestedJobs() {
        return tracker.getRequestedJobs();
    }

    @Override
    public long insertCraftedItems(ICraftingLink link, AEKey what, long amount, Actionable mode) {
        long leftover = owner.acceptCrafted(what, amount, mode);
        if (NepConfig.debugLogging() && mode == Actionable.MODULATE) {
            Nep.LOGGER.info(
                    "SA matrix {} received crafted {} x{} ({} rejected)", owner.getBlockPos(), what, amount, leftover);
        }
        return leftover;
    }

    @Override
    public void jobStateChange(ICraftingLink link) {
        tracker.jobStateChange(link);
    }

    @Nullable
    @Override
    public IGridNode getGridNode(Direction dir) {
        return mainNode.getNode();
    }

    private static final class Listener implements IGridNodeListener<MatrixGridNode> {
        @Override
        public void onSaveChanges(MatrixGridNode node, IGridNode gridNode) {
            node.owner.setChanged();
        }

        @Override
        public void onStateChanged(MatrixGridNode node, IGridNode gridNode, State state) {
            node.owner.onGridStateChanged();
        }
    }
}
