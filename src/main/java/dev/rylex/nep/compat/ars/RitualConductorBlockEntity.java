package dev.rylex.nep.compat.ars;

import appeng.api.config.Actionable;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.MEStorage;
import com.hollingsworth.arsnouveau.common.block.tile.RitualBrazierTile;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.util.SubLevels;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RitualConductorBlockEntity extends BlockEntity implements IInWorldGridNodeHost, IActionHost, MenuProvider {

    private static final String NODE_KEY = "node";
    private static final String RITUAL_KEY = "ritual";
    private static final String ARMED_FOR_KEY = "armedFor";
    private static final String NEXT_AUGMENT_KEY = "nextAugment";
    private static final String RUN_INTERVAL_KEY = "runInterval";
    private static final String WEATHER_KEY = "weather";
    private static final String DAYLIGHT_KEY = "daylight";
    private static final String COLLECT_KEY = "collect";
    private static final String NEXT_RUN_KEY = "nextRun";

    private static final int MISSING_BRAZIER_BACKOFF = 5;
    private static final int BRAZIER_RESCAN_CYCLES = 5;
    private static final int TICKS_PER_SECOND = 20;

    public static final int MAX_RUN_INTERVAL = 3_600;

    private static final IGridNodeListener<RitualConductorBlockEntity> LISTENER = new Listener();

    private final IManagedGridNode mainNode;
    private final IActionSource source;
    private final NonNullList<ItemStack> augments = NonNullList.withSize(ArsRituals.MAX_AUGMENTS, ItemStack.EMPTY);
    private final NetworkSink sink = new NetworkSink();

    @Nullable
    private ResourceLocation ritual;

    @Nullable
    private ResourceLocation armedFor;

    @Nullable
    private BlockPos brazierPos;

    private ConductorState state = ConductorState.UNCONFIGURED;
    private RitualWeather weather = RitualWeather.ANY;
    private RitualDaylight daylight = RitualDaylight.ANY;
    private boolean collectOutput = true;
    private int runInterval;
    private long nextRunGameTime;
    private int nextAugment;
    private int cooldown;
    private int brazierRescanIn;

    public RitualConductorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.mainNode = GridHelper.createManagedNode(this, LISTENER)
                .setInWorldNode(true)
                .setTagName("proxy")
                .setVisualRepresentation(NepArsContent.RITUAL_CONDUCTOR_ITEM.get());
        this.source = IActionSource.ofMachine(this);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide() && mainNode.getNode() == null) {
            mainNode.setIdlePowerUsage(NepConfig.arsRitualConductorIdleMeDrain());
            mainNode.create(level, getBlockPos());
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        mainNode.destroy();
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        mainNode.destroy();
    }

    @Nullable
    @Override
    public IGridNode getGridNode(Direction dir) {
        return mainNode.getNode();
    }

    @Override
    public IGridNode getActionableNode() {
        return mainNode.getNode();
    }

    public IItemHandler itemHandler() {
        return sink;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.nep.ritual_conductor");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory playerInv, Player player) {
        return new RitualConductorMenu(windowId, playerInv, worldPosition);
    }

    @Nullable
    public ResourceLocation ritual() {
        return ritual;
    }

    public List<ItemStack> augments() {
        return List.copyOf(augments);
    }

    @Nullable
    public BlockPos brazierPos() {
        return brazierPos;
    }

    public RitualWeather weather() {
        return weather;
    }

    public RitualDaylight daylight() {
        return daylight;
    }

    public boolean collectOutput() {
        return collectOutput;
    }

    public int runInterval() {
        return runInterval;
    }

    public int secondsUntilNextRun() {
        if (level == null || runInterval <= 0) {
            return 0;
        }
        long remaining = nextRunGameTime - level.getGameTime();
        return remaining <= 0
                ? 0
                : (int) Math.min(MAX_RUN_INTERVAL, (remaining + TICKS_PER_SECOND - 1) / TICKS_PER_SECOND);
    }

    public boolean setRitual(@Nullable ResourceLocation selected) {
        if (selected != null && !ArsRituals.known(selected)) {
            return false;
        }
        ritual = selected;
        setChanged();
        return true;
    }

    public void setAugment(int index, ItemStack template) {
        if (index < 0 || index >= ArsRituals.MAX_AUGMENTS) {
            return;
        }
        augments.set(index, template.isEmpty() ? ItemStack.EMPTY : template.copyWithCount(1));
        setChanged();
    }

    public void cycleWeather() {
        weather = weather.next();
        setChanged();
    }

    public void cycleDaylight() {
        daylight = daylight.next();
        setChanged();
    }

    public void cycleWeatherBack() {
        weather = weather.previous();
        setChanged();
    }

    public void cycleDaylightBack() {
        daylight = daylight.previous();
        setChanged();
    }

    public void toggleCollectOutput() {
        collectOutput = !collectOutput;
        setChanged();
    }

    public void adjustRunInterval(int delta) {
        runInterval = Math.max(0, Math.min(MAX_RUN_INTERVAL, runInterval + delta));
        if (level != null) {
            nextRunGameTime = Math.min(nextRunGameTime, level.getGameTime() + (long) runInterval * TICKS_PER_SECOND);
        }
        setChanged();
    }

    public void clearConfiguration() {
        ritual = null;
        java.util.Collections.fill(augments, ItemStack.EMPTY);
        armedFor = null;
        nextAugment = 0;
        runInterval = 0;
        nextRunGameTime = 0L;
        weather = RitualWeather.ANY;
        daylight = RitualDaylight.ANY;
        collectOutput = true;
        setChanged();
    }

    public void serverTick(ServerLevel level) {
        if (cooldown > 0) {
            cooldown--;
            return;
        }
        int interval = NepConfig.arsRitualConductorInterval();
        cooldown = interval - 1;

        if (!NepConfig.arsRitualConductor()) {
            publish(ConductorState.UNCONFIGURED, level);
            return;
        }

        RitualBrazierTile brazier = brazier(level);
        if (brazier == null) {
            cooldown = interval * MISSING_BRAZIER_BACKOFF;
            publish(ritual == null ? ConductorState.UNCONFIGURED : ConductorState.NO_BRAZIER, level);
            return;
        }
        if (ritual == null) {
            publish(ConductorState.UNCONFIGURED, level);
            return;
        }

        if (state.ritualActive() && (brazier.ritual == null || !brazier.ritual.isRunning())) {
            onRitualFinished(level, brazier);
        }

        syncArming(brazier);

        boolean powered = level.hasNeighborSignal(worldPosition);
        MEStorage storage = storage();
        if (storage != null && !powered) {
            if (brazier.ritual == null) {
                if (!readyToStart(level)) {
                    publish(ConductorState.HELD, level);
                    return;
                }
                arm(brazier, storage);
            } else if (!brazier.ritual.isRunning()) {
                feedAndStart(brazier, storage);
            }
        }

        ConductorState observed = observe(brazier);
        publish(powered && !observed.ritualActive() ? ConductorState.PAUSED : observed, level);
    }

    private boolean readyToStart(ServerLevel level) {
        return level.getGameTime() >= nextRunGameTime && weather.satisfiedBy(level) && daylight.satisfiedBy(level);
    }

    private void onRitualFinished(ServerLevel level, RitualBrazierTile brazier) {
        nextRunGameTime = level.getGameTime() + (long) runInterval * TICKS_PER_SECOND;
        if (collectOutput) {
            sweep(level, brazier.getBlockPos());
        }
    }

    private void sweep(ServerLevel level, BlockPos around) {
        int radius = NepConfig.arsRitualConductorCollectionRadius();
        MEStorage storage = storage();
        if (radius <= 0 || storage == null) {
            return;
        }
        AABB area = new AABB(around).inflate(radius);
        for (ItemEntity entity :
                level.getEntitiesOfClass(ItemEntity.class, area, RitualConductorBlockEntity::sweepable)) {
            ItemStack held = entity.getItem();
            AEItemKey key = AEItemKey.of(held);
            if (key == null) {
                continue;
            }
            long accepted = storage.insert(key, held.getCount(), Actionable.MODULATE, source);
            if (accepted <= 0) {
                continue;
            }
            held.shrink((int) accepted);
            if (held.isEmpty()) {
                entity.discard();
            } else {
                entity.setItem(held);
            }
        }
    }

    private static boolean sweepable(ItemEntity entity) {
        return entity.isAlive() && !entity.hasPickUpDelay() && entity.getOwner() == null;
    }

    private static ConductorState observe(RitualBrazierTile brazier) {
        if (brazier.ritual == null) {
            return ConductorState.WAITING;
        }
        if (!brazier.ritual.isRunning()) {
            return ConductorState.ARMED;
        }
        return brazier.ritual.needsSourceNow() ? ConductorState.STARVED : ConductorState.RUNNING;
    }

    private void publish(ConductorState updated, ServerLevel level) {
        showConducting(level, updated.ritualActive());
        if (state == updated) {
            return;
        }
        state = updated;
        setChanged();
        level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
    }

    private void showConducting(ServerLevel level, boolean conducting) {
        BlockState current = getBlockState();
        if (current.hasProperty(RitualConductorBlock.CONDUCTING)
                && current.getValue(RitualConductorBlock.CONDUCTING) != conducting) {
            level.setBlock(
                    worldPosition, current.setValue(RitualConductorBlock.CONDUCTING, conducting), Block.UPDATE_CLIENTS);
        }
    }

    private void syncArming(RitualBrazierTile brazier) {
        ResourceLocation current = brazier.ritual == null ? null : brazier.ritual.getRegistryName();
        if (!Objects.equals(current, armedFor)) {
            armedFor = current;
            nextAugment = 0;
            setChanged();
        }
    }

    private void arm(RitualBrazierTile brazier, MEStorage storage) {
        AEItemKey tablet = ArsRituals.tabletKey(ritual);
        if (tablet == null || storage.extract(tablet, 1, Actionable.MODULATE, source) < 1) {
            return;
        }
        brazier.setRitual(ritual);
        if (brazier.ritual == null) {
            storage.insert(tablet, 1, Actionable.MODULATE, source);
            return;
        }
        armedFor = ritual;
        nextAugment = 0;
        setChanged();
    }

    private void feedAndStart(RitualBrazierTile brazier, MEStorage storage) {
        while (nextAugment < augments.size()) {
            ItemStack template = augments.get(nextAugment);
            if (template.isEmpty()) {
                nextAugment++;
                continue;
            }
            AEItemKey key = AEItemKey.of(template);
            if (key == null || !brazier.ritual.canConsumeItem(key.toStack(1))) {
                nextAugment++;
                setChanged();
                continue;
            }
            if (storage.extract(key, 1, Actionable.MODULATE, source) < 1) {
                return;
            }
            ItemStack fed = key.toStack(1);
            if (!brazier.tryBurnStack(fed)) {
                storage.insert(key, 1, Actionable.MODULATE, source);
            } else if (!fed.isEmpty()) {
                storage.insert(key, fed.getCount(), Actionable.MODULATE, source);
            }
            nextAugment++;
            setChanged();
        }
        if (brazier.ritual.canStart(null)) {
            brazier.startRitual(null);
        }
    }

    @Nullable
    private RitualBrazierTile brazier(ServerLevel level) {
        if (brazierPos != null) {
            if (level.getBlockEntity(brazierPos) instanceof RitualBrazierTile cached
                    && !cached.isRemoved()
                    && SubLevels.sameSubLevel(level, worldPosition, brazierPos)) {
                if (cached.ritual != null || --brazierRescanIn > 0) {
                    return cached;
                }
            } else {
                brazierPos = null;
            }
        }
        brazierRescanIn = BRAZIER_RESCAN_CYCLES;
        brazierPos = null;

        int range = NepConfig.arsRitualConductorRange();
        RitualBrazierTile best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(
                worldPosition.offset(-range, -range, -range), worldPosition.offset(range, range, range))) {
            if (!level.isLoaded(pos) || !(level.getBlockEntity(pos) instanceof RitualBrazierTile candidate)) {
                continue;
            }
            if (!SubLevels.sameSubLevel(level, worldPosition, pos)) {
                continue;
            }
            double distance = pos.distSqr(worldPosition);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = candidate;
                brazierPos = pos.immutable();
            }
        }
        return best;
    }

    @Nullable
    private MEStorage storage() {
        if (!mainNode.isActive()) {
            return null;
        }
        IGrid grid = mainNode.getGrid();
        return grid == null ? null : grid.getStorageService().getInventory();
    }

    public ConductorState state() {
        return state;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        CompoundTag node = new CompoundTag();
        mainNode.saveToNBT(node);
        tag.put(NODE_KEY, node);
        if (ritual != null) {
            tag.putString(RITUAL_KEY, ritual.toString());
        }
        if (armedFor != null) {
            tag.putString(ARMED_FOR_KEY, armedFor.toString());
        }
        tag.putInt(NEXT_AUGMENT_KEY, nextAugment);
        tag.putInt(RUN_INTERVAL_KEY, runInterval);
        tag.putString(WEATHER_KEY, weather.name());
        tag.putString(DAYLIGHT_KEY, daylight.name());
        tag.putBoolean(COLLECT_KEY, collectOutput);
        tag.putLong(NEXT_RUN_KEY, nextRunGameTime);
        ContainerHelper.saveAllItems(tag, augments, true, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains(NODE_KEY)) {
            mainNode.loadFromNBT(tag.getCompound(NODE_KEY));
        }
        ritual = tag.contains(RITUAL_KEY) ? ResourceLocation.tryParse(tag.getString(RITUAL_KEY)) : null;
        armedFor = tag.contains(ARMED_FOR_KEY) ? ResourceLocation.tryParse(tag.getString(ARMED_FOR_KEY)) : null;
        nextAugment = tag.getInt(NEXT_AUGMENT_KEY);
        runInterval = Math.max(0, Math.min(MAX_RUN_INTERVAL, tag.getInt(RUN_INTERVAL_KEY)));
        weather = RitualWeather.byName(tag.getString(WEATHER_KEY));
        daylight = RitualDaylight.byName(tag.getString(DAYLIGHT_KEY));
        collectOutput = !tag.contains(COLLECT_KEY) || tag.getBoolean(COLLECT_KEY);
        nextRunGameTime = tag.getLong(NEXT_RUN_KEY);
        java.util.Collections.fill(augments, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, augments, registries);
    }

    public enum ConductorState {
        UNCONFIGURED(0),
        NO_BRAZIER(0),
        HELD(2),
        PAUSED(2),
        WAITING(5),
        ARMED(10),
        STARVED(10),
        RUNNING(15);

        private final int signal;

        ConductorState(int signal) {
            this.signal = signal;
        }

        public int signal() {
            return signal;
        }

        boolean ritualActive() {
            return this == RUNNING || this == STARVED;
        }

        public String key() {
            return "gui.nep.ritual_conductor.state." + name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    private final class NetworkSink implements IItemHandler {

        @Override
        public int getSlots() {
            return 1;
        }

        @NotNull
        @Override
        public ItemStack getStackInSlot(int slot) {
            return ItemStack.EMPTY;
        }

        @NotNull
        @Override
        public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (slot != 0 || stack.isEmpty() || !collectOutput) {
                return stack;
            }
            MEStorage storage = storage();
            AEItemKey key = AEItemKey.of(stack);
            if (storage == null || key == null) {
                return stack;
            }
            long accepted =
                    storage.insert(key, stack.getCount(), simulate ? Actionable.SIMULATE : Actionable.MODULATE, source);
            if (accepted >= stack.getCount()) {
                return ItemStack.EMPTY;
            }
            return key.toStack((int) (stack.getCount() - accepted));
        }

        @NotNull
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return slot == 0;
        }
    }

    private static final class Listener implements IGridNodeListener<RitualConductorBlockEntity> {
        @Override
        public void onSaveChanges(RitualConductorBlockEntity host, IGridNode node) {
            host.setChanged();
        }
    }
}
