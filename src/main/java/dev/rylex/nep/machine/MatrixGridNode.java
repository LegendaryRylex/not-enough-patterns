package dev.rylex.nep.machine;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.config.PowerUnit;
import appeng.api.networking.GridFlags;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.networking.crafting.ICraftingRequester;
import appeng.api.networking.crafting.ICraftingService;
import appeng.api.networking.energy.IEnergyService;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.storage.IStorageProvider;
import appeng.api.storage.MEStorage;
import appeng.helpers.MultiCraftingTracker;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import com.google.common.collect.ImmutableSet;
import dev.rylex.nep.Nep;
import dev.rylex.nep.NepConfig;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class MatrixGridNode implements IInWorldGridNodeHost, ICraftingRequester, ChannelDemand {

    public static final int TRACKER_SIZE = 18;

    private static final double POWER_EPSILON = 0.01;

    private static final double NETWORK_ENERGY_RESERVE = 0.1;

    private static final IGridNodeListener<MatrixGridNode> LISTENER = new Listener();

    private final MatrixHost owner;
    private final String label;
    private final IManagedGridNode mainNode;
    private final IActionSource source;

    private final int idleMeDrain;
    private final int channels;

    private MultiCraftingTracker tracker;
    private final ThrottledLog restockLog = new ThrottledLog();

    private boolean starved;

    public MatrixGridNode(MatrixHost owner, Item visual, int idleMeDrain, int channels, String label) {
        this.owner = owner;
        this.label = label;
        this.idleMeDrain = idleMeDrain;
        this.channels = channels;
        IManagedGridNode node = GridHelper.createManagedNode(this, LISTENER)
                .setInWorldNode(true)
                .setTagName("proxy")
                .setVisualRepresentation(visual)
                .addService(ICraftingRequester.class, this);
        if (channels > 0) {
            node.setFlags(GridFlags.REQUIRE_CHANNEL, GridFlags.DENSE_CAPACITY);
        }
        this.mainNode = node;
        this.tracker = new MultiCraftingTracker(this, TRACKER_SIZE);
        this.source = IActionSource.ofMachine(this);
    }

    @Override
    public int channelDemand() {
        return channels;
    }

    /** Must be called before the node joins a grid. */
    public void provideStorage(MEStorage storage) {
        mainNode.addService(IStorageProvider.class, mounts -> mounts.mount(storage));
    }

    /** True when {@code other} is this node's own machine, which is how a mounted storage refuses to feed itself. */
    public boolean isOwnActionSource(IActionSource other) {
        return other.machine().orElse(null) == this;
    }

    public void requestStorageUpdate() {
        IStorageProvider.requestUpdate(mainNode);
    }

    public void create(Level level, BlockPos pos) {
        if (mainNode.getNode() == null) {
            mainNode.setIdlePowerUsage(idleMeDrain);
            mainNode.create(level, pos);
        }
    }

    @Nullable
    public IGrid grid() {
        return mainNode.getGrid();
    }

    public double extractPower(double amount) {
        IGrid grid = mainNode.getGrid();
        if (grid == null) {
            starved = amount > POWER_EPSILON;
            return 0;
        }
        double drawn = grid.getEnergyService().extractAEPower(amount, Actionable.MODULATE, PowerMultiplier.CONFIG);
        starved = drawn < amount - POWER_EPSILON;
        return drawn;
    }

    public long chargeBuffer(MatrixEnergyBuffer buffer, long wanted) {
        long room = buffer.receive(wanted, true);
        if (room <= 0) {
            return 0;
        }
        IGrid grid = mainNode.getGrid();
        if (grid == null) {
            return 0;
        }
        IEnergyService energy = grid.getEnergyService();
        double spare = energy.getStoredPower() - energy.getMaxStoredPower() * NETWORK_ENERGY_RESERVE;
        double wantedAe = Math.min(spare, PowerUnit.FE.convertTo(PowerUnit.AE, room));
        if (wantedAe <= POWER_EPSILON) {
            return 0;
        }
        double drawn = energy.extractAEPower(wantedAe, Actionable.MODULATE, PowerMultiplier.ONE);
        return buffer.receive((long) PowerUnit.AE.convertTo(PowerUnit.FE, drawn), false);
    }

    public List<GenericStack> restock(Level level, List<AEKey> keys, List<Long> targets) {
        List<GenericStack> unavailable = new ArrayList<>();
        IGrid grid = mainNode.getGrid();
        if (grid == null) {
            if (NepConfig.debugLogging()) {
                log(
                        level,
                        "grid",
                        String.format(
                                "%s %s: not on a grid yet (node ready=%s, active=%s)",
                                label, owner.getBlockPos(), mainNode.isReady(), mainNode.isActive()));
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
                log(
                        level,
                        key.toString(),
                        String.format(
                                "%s %s slot %d: %s -> pulled %d from stock, still-missing %d, craftable=%s,"
                                        + " craftStarted=%s",
                                label,
                                owner.getBlockPos(),
                                slot,
                                key,
                                pulled,
                                Math.max(0, missing),
                                craftable,
                                craftStarted));
            }
        }
        return unavailable;
    }

    private void log(Level level, String subject, String message) {
        if (restockLog.shouldLog(level.getGameTime(), subject, message)) {
            Nep.LOGGER.info(message);
        }
    }

    public void cancelRequests() {
        for (ICraftingLink link : tracker.getRequestedJobs()) {
            link.cancel();
        }
        tracker = new MultiCraftingTracker(this, TRACKER_SIZE);
    }

    public long dumpToNetwork(AEKey key, long amount) {
        IGrid grid = mainNode.getGrid();
        if (grid == null) {
            return 0;
        }
        return grid.getStorageService().getInventory().insert(key, amount, Actionable.MODULATE, source);
    }

    public long extractFromNetwork(AEKey key, long amount) {
        IGrid grid = mainNode.getGrid();
        if (grid == null || amount <= 0) {
            return 0;
        }
        return grid.getStorageService().getInventory().extract(key, amount, Actionable.MODULATE, source);
    }

    public long networkStock(AEKey key) {
        IGrid grid = mainNode.getGrid();
        return grid == null ? 0 : grid.getStorageService().getCachedInventory().get(key);
    }

    public boolean networkCanCraft(AEKey key) {
        IGrid grid = mainNode.getGrid();
        return grid != null && grid.getCraftingService().isCraftable(key);
    }

    /** Counts {@code output} across crafting CPUs' unpushed tasks, which a CPU still holds until each one is pushed. */
    public long scheduledOutputs(AEKey output) {
        IGrid grid = mainNode.getGrid();
        if (grid == null) {
            return 0;
        }
        long scheduled = 0;
        for (ICraftingCPU cpu : grid.getCraftingService().getCpus()) {
            if (cpu instanceof CraftingCPUCluster cluster && cluster.isBusy()) {
                scheduled += cluster.craftingLogic.getPendingOutputs(output);
            }
        }
        return scheduled;
    }

    public void destroy() {
        mainNode.destroy();
    }

    public boolean isPowered() {
        return mainNode.isActive();
    }

    public boolean missingChannel() {
        IGridNode node = mainNode.getNode();
        return node != null && !node.meetsChannelRequirements();
    }

    public boolean isStarved() {
        return starved;
    }

    public void clearStarved() {
        starved = false;
    }

    public boolean hasUsablePower() {
        return isPowered() && !starved;
    }

    public void save(CompoundTag tag) {
        mainNode.saveToNBT(tag);
        tracker.writeToNBT(tag);
    }

    public void load(CompoundTag tag) {
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
                    "{} {} received crafted {} x{} ({} rejected)", label, owner.getBlockPos(), what, amount, leftover);
        }
        return amount - leftover;
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
