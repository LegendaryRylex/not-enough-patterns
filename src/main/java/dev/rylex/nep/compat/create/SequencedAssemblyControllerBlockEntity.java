package dev.rylex.nep.compat.create;

import appeng.api.config.Actionable;
import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import com.simibubi.create.content.kinetics.belt.behaviour.DirectBeltInputBehaviour;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.rylex.nep.Nep;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.compat.create.SequencedAssemblyControllerBlock.ControllerStatus;
import dev.rylex.nep.machine.ComparatorSignal;
import dev.rylex.nep.machine.MachineItemView;
import dev.rylex.nep.machine.PushingCpus;
import dev.rylex.nep.machine.ReturnDirections;
import dev.rylex.nep.pattern.SequencedAssemblyPattern;
import dev.rylex.nep.util.ItemCounts;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Clearable;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

public class SequencedAssemblyControllerBlockEntity extends BlockEntity implements MenuProvider, Clearable {

    private static final String INPUT_KEY = "Input";
    private static final String OUTPUT_KEY = "Output";
    private static final String MACHINES_KEY = "Machines";
    private static final String STATION_KINDS_KEY = "StationKinds";
    private static final String BUFFER_KEY = "Buffer";
    private static final String OUTPUT_BUFFER_KEY = "OutputBuffer";
    private static final String BYPRODUCT_BUFFER_KEY = "ByproductBuffer";
    private static final String FLUID_BUFFER_KEY = "FluidBuffer";
    private static final String OWED_KEY = "Owed";
    private static final String TO_RETURN_KEY = "ToReturn";
    private static final String RETURN_DIR_KEY = "ReturnDir";
    private static final String RETURN_DIRS_KEY = "ReturnDirs";
    private static final String TEMPLATE_KEY = "Template";
    private static final String NODE_KEY = "Node";
    private static final String INFLIGHT_KEY = "InFlight";
    private static final String QUEUE_KEY = "Queue";
    private static final String RECIRC_KEY = "Recirc";
    private static final String RECLAIM_KEY = "Reclaim";
    private static final int POLL_INTERVAL = 10;
    private static final int BUFFER_SLOTS = 18;
    private static final int RESULT_SLOTS = 9;
    private static final int LOST_ATTEMPT_TICKS = 1200;
    private static final int TOOL_STOCK_ATTEMPTS = 1;
    private static final int BASE_STOCK_ATTEMPTS = 1;

    @Nullable
    private BlockPos input;

    @Nullable
    private BlockPos output;

    private final List<BlockPos> machines = new ArrayList<>();

    private final List<StationKind> stationKinds = new ArrayList<>();

    private final ItemStackHandler buffer = new ItemStackHandler(BUFFER_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private final ItemStackHandler outputBuffer = new ItemStackHandler(RESULT_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private final ItemStackHandler byproductBuffer = new ItemStackHandler(RESULT_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private final AssemblyFluidBuffer fluids =
            new AssemblyFluidBuffer(NepConfig::createSequencedAssemblyTankCapacity, this::setChanged);

    private final IItemHandler machineView = MachineItemView.feedable(buffer, null);

    private final Map<Item, Long> owed = new HashMap<>();
    private final Map<Item, Long> toReturn = new HashMap<>();
    private long lastProgressTick = Long.MIN_VALUE;
    private final Map<Item, Long> inFlight = new HashMap<>();
    private long lastResolveTick = Long.MIN_VALUE;
    private long reclaimUntil = Long.MIN_VALUE;
    private boolean halted;
    private boolean outputBlocked;
    private boolean starved;
    private int lastComparator = -1;

    private final ReturnDirections returnDirections = new ReturnDirections();

    private List<SequencedAssemblyRecipe> lineRecipes = List.of();
    private long lineRecipesExpiry = Long.MIN_VALUE;
    private int lineRecipesGeneration = -1;

    private final Map<BlockPos, BlockCapabilityCache<IItemHandler, Direction>> machineItemCaps = new HashMap<>();
    private final Map<BlockPos, BlockCapabilityCache<IFluidHandler, Direction>> machineFluidCaps = new HashMap<>();

    private record StationSnapshot(long tick, List<ItemStack> deployerHeld, List<FluidStack> spoutFluids) {}

    @Nullable
    private StationSnapshot stationSnapshot;

    private final Map<Item, Template> templates = new HashMap<>();
    private final PushingCpus pushingCpus = new PushingCpus();
    private final List<Item> queue = new ArrayList<>();
    private final List<ItemStack> recirc = new ArrayList<>();

    private final SequencedAssemblyRequester requester = new SequencedAssemblyRequester(this);

    private final AssemblyFluidBuffer.OverflowSink fluidOverflow = overflow -> {
        AEFluidKey key = AEFluidKey.of(overflow);
        return key == null
                ? 0
                : (int) Math.min(overflow.getAmount(), requester.dumpToNetwork(key, overflow.getAmount()));
    };

    private long pending() {
        long total = 0;
        for (long value : owed.values()) {
            total += value;
        }
        return total;
    }

    private long inFlightTotal() {
        long total = 0;
        for (long value : inFlight.values()) {
            total += value;
        }
        return total;
    }

    private long inFlightFor(Item output) {
        return inFlight.getOrDefault(output, 0L);
    }

    private void addInFlight(Item output, long amount) {
        inFlight.merge(output, amount, Long::sum);
    }

    private List<Item> owedInOrder() {
        List<Item> outputs = new ArrayList<>(owed.keySet());
        outputs.sort(Comparator.comparing(BuiltInRegistries.ITEM::getKey));
        return outputs;
    }

    private void syncQueue() {
        for (Item output : owedInOrder()) {
            if (!queue.contains(output)) {
                queue.add(output);
            }
        }
        queue.removeIf(output -> !owed.containsKey(output) && inFlightFor(output) <= 0);
    }

    @Nullable
    private Item activeOutput() {
        return queue.isEmpty() ? null : queue.get(0);
    }

    private void syncManualDemand(Level level, List<SequencedAssemblyRecipe> recipes) {
        for (SequencedAssemblyRecipe recipe : recipes) {
            Item out = recipe.getResultItem(level.registryAccess()).getItem();
            if (templates.containsKey(out)) {
                continue;
            }
            long demand = countBufferBase(recipe.getIngredient()) + inFlightFor(out);
            if (demand > 0) {
                owed.put(out, demand);
            } else {
                owed.remove(out);
            }
        }
    }

    private long countBufferBase(Ingredient base) {
        long total = 0;
        for (int slot = 0; slot < buffer.getSlots(); slot++) {
            ItemStack stack = buffer.getStackInSlot(slot);
            if (!stack.isEmpty() && base.test(stack)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private List<Item> requestOrder(@Nullable Item active) {
        List<Item> order = new ArrayList<>();
        if (active != null && owed.containsKey(active)) {
            order.add(active);
        }
        for (Item output : owedInOrder()) {
            if (!order.contains(output)) {
                order.add(output);
            }
        }
        return order;
    }

    private record Template(List<AEKey> keys, List<Long> counts) {}

    public SequencedAssemblyControllerBlockEntity(BlockPos pos, BlockState state) {
        super(NepCreateContent.CONTROLLER_BLOCK_ENTITY.get(), pos, state);
    }

    void applyPlan(@Nullable BlockPos plannedInput, @Nullable BlockPos plannedOutput, List<BlockPos> plannedMachines) {
        input = plannedInput == null ? null : plannedInput.immutable();
        output = plannedOutput == null ? null : plannedOutput.immutable();
        machines.clear();
        for (BlockPos pos : plannedMachines) {
            machines.add(pos.immutable());
        }
        forgetStations();
        Level level = getLevel();
        if (level != null && !level.isClientSide) {
            rememberStations(level);
        }
        setChanged();
        refreshVisualState();
        syncLinks();
    }

    private void forgetStations() {
        stationKinds.clear();
        for (int slot = 0; slot < machines.size(); slot++) {
            stationKinds.add(StationKind.UNKNOWN);
        }
    }

    private void rememberStations(Level level) {
        if (stationKinds.size() != machines.size()) {
            forgetStations();
        }
        for (int slot = 0; slot < machines.size(); slot++) {
            StationKind kind = Stations.detect(level.getBlockState(machines.get(slot)));
            if (kind.recognized() && stationKinds.get(slot) != kind) {
                stationKinds.set(slot, kind);
                setChanged();
            }
        }
    }

    private StationKind rememberedStation(int slot) {
        return slot < stationKinds.size() ? stationKinds.get(slot) : StationKind.UNKNOWN;
    }

    private void refreshVisualState() {
        Level level = getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        BlockState state = getBlockState();
        if (!state.hasProperty(SequencedAssemblyControllerBlock.STATUS)) {
            return;
        }
        ControllerStatus desired = desiredStatus();
        if (state.getValue(SequencedAssemblyControllerBlock.STATUS) != desired) {
            level.setBlock(
                    getBlockPos(),
                    state.setValue(SequencedAssemblyControllerBlock.STATUS, desired),
                    Block.UPDATE_CLIENTS);
        }
    }

    private ControllerStatus desiredStatus() {
        if (halted || outputBlocked) {
            return ControllerStatus.HALTED;
        }
        return switch (statusCode()) {
            case 1 -> ControllerStatus.ON;
            case 2 -> ControllerStatus.HALTED;
            default -> ControllerStatus.OFF;
        };
    }

    IInWorldGridNodeHost gridNodeHost() {
        return requester;
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        requester.destroy();
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        requester.destroy();
    }

    void serverTick() {
        Level level = getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        requester.create(level, getBlockPos());

        if (Math.floorMod(level.getGameTime() + getBlockPos().hashCode(), POLL_INTERVAL) != 0) {
            return;
        }
        refreshComparator(level);

        if (fluids.overCapacity()) {
            fluids.trimToCapacity(fluidOverflow);
        }

        refreshVisualState();
        rememberStations(level);

        if (!NepConfig.createSequencedAssembly() || input == null || output == null) {
            updateHalted(level, false);
            setOutputBlocked(level, false);
            return;
        }

        List<SequencedAssemblyRecipe> recipes = lineRecipes(level);
        if (recipes.isEmpty()) {
            updateHalted(level, false);
            setOutputBlocked(level, false);
            return;
        }

        syncManualDemand(level, recipes);
        syncQueue();
        Item active = activeOutput();
        SequencedAssemblyRecipe activeRecipe = active == null ? null : recipeForOutput(recipes, level, active);

        if (activeRecipe != null) {
            distribute(level, activeRecipe, active);
        }
        collectResults(level, recipes);
        reconcileInFlight(level);
        flushOutput(level);
        flushByproduct(level);
        if (activeRecipe != null
                && NepConfig.createSequencedAssemblyAutoRequest()
                && owed.getOrDefault(active, 0L) > 0) {
            autoRequest(level, recipes, active);
        }
        recomputeHalted(level, activeRecipe);
        manageQueue(level, active, activeRecipe);
    }

    private boolean anythingOnTheLine() {
        return pending() > 0 || inFlightTotal() > 0 || !recirc.isEmpty();
    }

    private void startReclaim(Level level) {
        int grace = NepConfig.createSequencedAssemblyReclaimGrace();
        long until = grace <= 0 ? Long.MIN_VALUE : level.getGameTime() + grace;
        if (until == reclaimUntil) {
            return;
        }
        reclaimUntil = until;
        setChanged();
    }

    private boolean reclaiming(Level level) {
        if (reclaimUntil == Long.MIN_VALUE) {
            return false;
        }
        if (level.getGameTime() >= reclaimUntil) {
            reclaimUntil = Long.MIN_VALUE;
            setChanged();
            if (NepConfig.debugLogging()) {
                Nep.LOGGER.info("SA controller {} closed its reclaim window", getBlockPos());
            }
            return false;
        }
        return true;
    }

    void clearPending() {
        if (owed.isEmpty() && templates.isEmpty()) {
            return;
        }
        boolean onTheLine = anythingOnTheLine();
        int cancelled = pushingCpus.cancelJobsFor(requester.grid(), Set.copyOf(templates.keySet()));
        if (NepConfig.debugLogging()) {
            Nep.LOGGER.info(
                    "SA controller {} cleared {} pending output(s) and cancelled {} network job(s)",
                    getBlockPos(),
                    owed.size(),
                    cancelled);
        }
        owed.clear();
        templates.clear();
        pushingCpus.clear();
        inFlight.clear();
        queue.clear();
        for (ItemStack held : recirc) {
            long dumped = requester.dumpToNetwork(AEItemKey.of(held), held.getCount());
            if (dumped < held.getCount()) {
                ItemHandlerHelper.insertItem(
                        byproductBuffer, held.copyWithCount((int) (held.getCount() - dumped)), false);
            }
        }
        recirc.clear();
        lastResolveTick = Long.MIN_VALUE;
        requester.cancelRequests();
        setChanged();
        Level level = getLevel();
        if (level != null) {
            updateHalted(level, false);
            markProgress(level);
            if (onTheLine) {
                startReclaim(level);
            }
        }
        refreshVisualState();
    }

    private void autoRequest(Level level, List<SequencedAssemblyRecipe> recipes, @Nullable Item active) {
        if (templates.isEmpty()) {
            return;
        }
        for (Item output : requestOrder(active)) {
            Template template = templates.get(output);
            if (template == null || template.keys().isEmpty()) {
                continue;
            }
            long owe = owed.getOrDefault(output, 0L);
            if (owe <= 0) {
                continue;
            }
            long baseAttempts = Math.max(0, Math.min(owe, BASE_STOCK_ATTEMPTS) - inFlightFor(output));
            long toolAttempts = Math.min(owe, TOOL_STOCK_ATTEMPTS);
            SequencedAssemblyRecipe recipe = recipeForOutput(recipes, level, output);
            List<Ingredient> bases = recipe == null ? List.of() : List.of(recipe.getIngredient());
            List<Long> targets = new ArrayList<>(template.counts().size());
            boolean shortfall = false;
            for (int i = 0; i < template.keys().size(); i++) {
                long attempts = isBase(bases, template.keys().get(i)) ? baseAttempts : toolAttempts;
                long target = template.counts().get(i) * attempts;
                targets.add(target);
                shortfall |= target > bufferedAmount(template.keys().get(i));
            }
            if (!shortfall) {
                continue;
            }
            if (NepConfig.debugLogging()) {
                Nep.LOGGER.info(
                        "SA controller {} auto-request for {}: owed={}, inFlight={}, base attempts={}, tool attempts={}, template entries={}",
                        getBlockPos(),
                        output,
                        owe,
                        inFlightFor(output),
                        baseAttempts,
                        toolAttempts,
                        template.keys().size());
            }
            requester.restock(level, template.keys(), targets);
            return;
        }
    }

    private static boolean isBase(List<Ingredient> bases, AEKey key) {
        return key instanceof AEItemKey itemKey && matchesBase(bases, itemKey.toStack(1));
    }

    @Nullable
    private static SequencedAssemblyRecipe recipeForOutput(
            List<SequencedAssemblyRecipe> recipes, Level level, Item output) {
        for (SequencedAssemblyRecipe recipe : recipes) {
            if (recipe.getResultItem(level.registryAccess()).getItem() == output) {
                return recipe;
            }
        }
        return SequencedAssemblyResolver.resolveByResult(level, new ItemStack(output));
    }

    private void captureTemplate(Item output, Map<AEItemKey, Long> items, Map<AEFluidKey, Long> fluids) {
        List<AEKey> keys = new ArrayList<>();
        List<Long> counts = new ArrayList<>();
        for (Map.Entry<AEItemKey, Long> entry : items.entrySet()) {
            keys.add(entry.getKey());
            counts.add(entry.getValue());
        }
        for (Map.Entry<AEFluidKey, Long> entry : fluids.entrySet()) {
            keys.add(entry.getKey());
            counts.add(entry.getValue());
        }
        templates.put(output, new Template(List.copyOf(keys), List.copyOf(counts)));
    }

    long bufferedAmount(AEKey key) {
        Level level = getLevel();
        long total = 0;
        if (key instanceof AEItemKey itemKey) {
            for (int slot = 0; slot < buffer.getSlots(); slot++) {
                ItemStack stack = buffer.getStackInSlot(slot);
                if (!stack.isEmpty() && itemKey.matches(stack)) {
                    total += stack.getCount();
                }
            }
            if (level != null) {
                for (ItemStack held : stationSnapshot(level).deployerHeld()) {
                    if (itemKey.matches(held)) {
                        total += held.getCount();
                    }
                }
            }
        } else if (key instanceof AEFluidKey fluidKey) {
            for (int tank = 0; tank < AssemblyFluidBuffer.TANKS; tank++) {
                FluidStack staged = fluids.getFluidInTank(tank);
                if (fluidKey.matches(staged)) {
                    total += staged.getAmount();
                }
            }
            if (level != null) {
                for (FluidStack held : stationSnapshot(level).spoutFluids()) {
                    if (fluidKey.matches(held)) {
                        total += held.getAmount();
                    }
                }
            }
        }
        return total;
    }

    private StationSnapshot stationSnapshot(Level level) {
        long now = level.getGameTime();
        StationSnapshot snapshot = stationSnapshot;
        if (snapshot != null && snapshot.tick() == now) {
            return snapshot;
        }
        List<ItemStack> deployerHeld = new ArrayList<>();
        List<FluidStack> spoutFluids = new ArrayList<>();
        for (BlockPos machine : machines) {
            switch (Stations.detect(level.getBlockState(machine))) {
                case DEPLOYER -> {
                    IItemHandler handler = machineItemHandler(level, machine);
                    if (handler != null && handler.getSlots() > 0) {
                        ItemStack held = handler.getStackInSlot(handler.getSlots() - 1);
                        if (!held.isEmpty()) {
                            deployerHeld.add(held);
                        }
                    }
                }
                case SPOUT -> {
                    IFluidHandler tank = machineFluidHandler(level, machine);
                    if (tank != null) {
                        for (int t = 0; t < tank.getTanks(); t++) {
                            FluidStack held = tank.getFluidInTank(t);
                            if (!held.isEmpty()) {
                                spoutFluids.add(held);
                            }
                        }
                    }
                }
                default -> {}
            }
        }
        snapshot = new StationSnapshot(now, deployerHeld, spoutFluids);
        stationSnapshot = snapshot;
        return snapshot;
    }

    @Nullable
    private IItemHandler machineItemHandler(Level level, BlockPos machine) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return level.getCapability(Capabilities.ItemHandler.BLOCK, machine, null);
        }
        return machineItemCaps
                .computeIfAbsent(
                        machine,
                        pos -> BlockCapabilityCache.create(Capabilities.ItemHandler.BLOCK, serverLevel, pos, null))
                .getCapability();
    }

    @Nullable
    private IFluidHandler machineFluidHandler(Level level, BlockPos machine) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return level.getCapability(Capabilities.FluidHandler.BLOCK, machine, Direction.UP);
        }
        return machineFluidCaps
                .computeIfAbsent(
                        machine,
                        pos -> BlockCapabilityCache.create(
                                Capabilities.FluidHandler.BLOCK, serverLevel, pos, Direction.UP))
                .getCapability();
    }

    long acceptCrafted(AEKey what, long amount, Actionable mode) {
        boolean simulate = mode == Actionable.SIMULATE;
        if (what instanceof AEItemKey itemKey) {
            long leftover = insertCraftedItem(itemKey, amount, simulate);
            if (!simulate && leftover < amount) {
                setChanged();
            }
            return leftover;
        }
        if (what instanceof AEFluidKey fluidKey) {
            FluidStack stack = fluidKey.toStack((int) Math.min(amount, Integer.MAX_VALUE));
            int accepted = fluids.fill(
                    stack, simulate ? IFluidHandler.FluidAction.SIMULATE : IFluidHandler.FluidAction.EXECUTE);
            if (!simulate && accepted > 0) {
                setChanged();
            }
            return amount - accepted;
        }
        return amount;
    }

    private long insertCraftedItem(AEItemKey key, long amount, boolean simulate) {
        int maxStack = key.toStack(1).getMaxStackSize();
        if (simulate) {
            long room = 0;
            for (int slot = 0; slot < buffer.getSlots() && room < amount; slot++) {
                ItemStack held = buffer.getStackInSlot(slot);
                int limit = Math.min(buffer.getSlotLimit(slot), maxStack);
                if (held.isEmpty()) {
                    room += limit;
                } else if (key.matches(held)) {
                    room += Math.max(0, limit - held.getCount());
                }
            }
            return Math.max(0, amount - room);
        }
        long remaining = amount;
        while (remaining > 0) {
            int chunk = (int) Math.min(remaining, maxStack);
            ItemStack leftover = ItemHandlerHelper.insertItem(buffer, key.toStack(chunk), false);
            int inserted = chunk - leftover.getCount();
            if (inserted <= 0) {
                break;
            }
            remaining -= inserted;
        }
        return remaining;
    }

    private void flushOutput(Level level) {
        if (returnDirections.isEmpty() || toReturn.isEmpty()) {
            return;
        }
        for (int slot = 0; slot < outputBuffer.getSlots(); slot++) {
            ItemStack stack = outputBuffer.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            long owedReturn = toReturn.getOrDefault(stack.getItem(), 0L);
            if (owedReturn <= 0) {
                continue;
            }
            IItemHandler target = returnDirections.targetFor(level, getBlockPos(), stack.getItem());
            if (target == null) {
                continue;
            }
            int pushable = (int) Math.min(stack.getCount(), owedReturn);
            ItemStack remainder = ItemHandlerHelper.insertItem(target, stack.copyWithCount(pushable), false);
            int moved = pushable - remainder.getCount();
            if (moved > 0) {
                if (NepConfig.debugLogging()) {
                    Nep.LOGGER.info(
                            "SA controller {} returned {} x{} to the network via {}",
                            getBlockPos(),
                            stack.getItem(),
                            moved,
                            returnDirections.directionFor(stack.getItem()));
                }
                outputBuffer.extractItem(slot, moved, false);
                decrementToReturn(stack.getItem(), moved);
            }
        }
    }

    private void decrementToReturn(Item item, long amount) {
        long remaining = toReturn.getOrDefault(item, 0L) - amount;
        if (remaining > 0) {
            toReturn.put(item, remaining);
            return;
        }
        toReturn.remove(item);
        if (!owed.containsKey(item) && inFlightFor(item) <= 0) {
            returnDirections.forget(item);
        }
    }

    private void flushByproduct(Level level) {
        for (int slot = 0; slot < byproductBuffer.getSlots(); slot++) {
            ItemStack stack = byproductBuffer.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            long inserted = requester.dumpToNetwork(AEItemKey.of(stack), stack.getCount());
            if (inserted > 0) {
                byproductBuffer.extractItem(slot, (int) inserted, false);
                if (NepConfig.debugLogging()) {
                    Nep.LOGGER.info(
                            "SA controller {} dumped byproduct {} x{} into network storage",
                            getBlockPos(),
                            stack.getItem(),
                            inserted);
                }
            }
        }
    }

    private void collectResults(Level level, List<SequencedAssemblyRecipe> recipes) {
        setOutputBlocked(level, scanResults(level, recipes));
    }

    private boolean scanResults(Level level, List<SequencedAssemblyRecipe> recipes) {
        IItemHandler out = level.getCapability(Capabilities.ItemHandler.BLOCK, output, null);
        if (out == null) {
            return false;
        }

        boolean pending = pending() > 0 || inFlightTotal() > 0;
        boolean reclaiming = !pending && reclaiming(level);
        Set<Item> transitionals = new HashSet<>();
        Set<Item> primaries = new HashSet<>();
        for (SequencedAssemblyRecipe recipe : recipes) {
            transitionals.add(recipe.getTransitionalItem().getItem());
            primaries.add(recipe.getResultItem(level.registryAccess()).getItem());
        }

        for (int slot = 0; slot < out.getSlots(); slot++) {
            ItemStack onDepot = out.getStackInSlot(slot);
            if (onDepot.isEmpty()) {
                continue;
            }
            Item item = onDepot.getItem();
            if (transitionals.contains(item)) {
                if (pending) {
                    holdTransitional(out, slot, onDepot);
                } else if (reclaiming) {
                    reclaimFromDepot(level, out, slot, onDepot);
                }
                return false;
            }
            long owe = owed.getOrDefault(item, 0L);
            if (owe > 0) {
                int captured = capture(out, slot, onDepot, outputBuffer, (int) Math.min(owe, Integer.MAX_VALUE));
                if (captured <= 0) {
                    return true;
                }
                decrementOwed(item, captured);
                resolveAttempts(level, captured, item);
                markProgress(level);
            } else if (reclaiming) {
                if (reclaimFromDepot(level, out, slot, onDepot) <= 0) {
                    return true;
                }
            } else if (pending) {
                int captured = capture(out, slot, onDepot, byproductBuffer, onDepot.getCount());
                if (captured <= 0) {
                    return true;
                }
                Item attributed = primaries.contains(item) ? item : attributeJunk(level, recipes, item);
                resolveAttempts(level, captured, attributed);
                markProgress(level);
            }
            return false;
        }
        return false;
    }

    private void holdTransitional(IItemHandler out, int slot, ItemStack onDepot) {
        ItemStack taken = out.extractItem(slot, onDepot.getCount(), false);
        if (!taken.isEmpty()) {
            recirc.add(taken);
            setChanged();
        }
    }

    private int reclaimFromDepot(Level level, IItemHandler out, int slot, ItemStack onDepot) {
        int captured = capture(out, slot, onDepot, byproductBuffer, onDepot.getCount());
        if (captured <= 0) {
            return 0;
        }
        startReclaim(level);
        if (NepConfig.debugLogging()) {
            Nep.LOGGER.info(
                    "SA controller {} reclaimed {} x{} left over on the line",
                    getBlockPos(),
                    onDepot.getItem(),
                    captured);
        }
        return captured;
    }

    private void decrementOwed(Item item, long amount) {
        long remaining = owed.getOrDefault(item, 0L) - amount;
        if (remaining > 0) {
            owed.put(item, remaining);
        } else {
            owed.remove(item);
        }
        setChanged();
    }

    private int capture(IItemHandler out, int slot, ItemStack onDepot, ItemStackHandler target, int maxCount) {
        int want = Math.min(onDepot.getCount(), maxCount);
        if (want <= 0) {
            return 0;
        }
        ItemStack simulated = ItemHandlerHelper.insertItem(target, onDepot.copyWithCount(want), true);
        int accepted = want - simulated.getCount();
        if (accepted <= 0) {
            return 0;
        }
        ItemStack taken = out.extractItem(slot, accepted, false);
        if (taken.isEmpty()) {
            return 0;
        }
        ItemHandlerHelper.insertItem(target, taken, false);
        return taken.getCount();
    }

    private void markProgress(Level level) {
        lastProgressTick = level.getGameTime();
    }

    private void resolveAttempts(Level level, int count, @Nullable Item output) {
        if (output != null) {
            long remaining = Math.max(0, inFlightFor(output) - count);
            if (remaining > 0) {
                inFlight.put(output, remaining);
            } else {
                inFlight.remove(output);
            }
        }
        lastResolveTick = level.getGameTime();
    }

    @Nullable
    private Item attributeJunk(Level level, List<SequencedAssemblyRecipe> recipes, Item junk) {
        Item mapped = null;
        for (SequencedAssemblyRecipe recipe : recipes) {
            for (int i = 1; i < recipe.resultPool.size(); i++) {
                if (recipe.resultPool.get(i).getStack().getItem() == junk) {
                    Item output = recipe.getResultItem(level.registryAccess()).getItem();
                    if (mapped != null && mapped != output) {
                        mapped = null;
                        break;
                    }
                    mapped = output;
                }
            }
        }
        if (mapped != null && inFlightFor(mapped) > 0) {
            return mapped;
        }
        Item busiest = null;
        long best = 0;
        for (Map.Entry<Item, Long> entry : inFlight.entrySet()) {
            if (entry.getValue() > best) {
                best = entry.getValue();
                busiest = entry.getKey();
            }
        }
        return busiest;
    }

    private void reconcileInFlight(Level level) {
        if (inFlightTotal() <= 0) {
            lastResolveTick = Long.MIN_VALUE;
            return;
        }
        if (lastResolveTick == Long.MIN_VALUE || starved || !recirc.isEmpty()) {
            lastResolveTick = level.getGameTime();
            return;
        }
        if (level.getGameTime() - lastResolveTick > LOST_ATTEMPT_TICKS) {
            if (NepConfig.debugLogging()) {
                Nep.LOGGER.info(
                        "SA controller {} clearing {} stuck in-flight attempt(s) — no result landed for a while (items likely lost off the line)",
                        getBlockPos(),
                        inFlightTotal());
            }
            inFlight.clear();
            lastResolveTick = Long.MIN_VALUE;
        }
    }

    private void recomputeHalted(Level level, @Nullable SequencedAssemblyRecipe activeRecipe) {
        if (activeRecipe == null || pending() <= 0) {
            updateHalted(level, false);
            return;
        }
        if (lastProgressTick == Long.MIN_VALUE) {
            lastProgressTick = level.getGameTime();
        }
        boolean stalled = (starved || !hasBaseMaterial(activeRecipe))
                && level.getGameTime() - lastProgressTick > NepConfig.createSequencedAssemblyHaltGrace();
        updateHalted(level, stalled);
    }

    private boolean hasBaseMaterial(SequencedAssemblyRecipe recipe) {
        Ingredient base = recipe.getIngredient();
        for (int slot = 0; slot < buffer.getSlots(); slot++) {
            ItemStack stack = buffer.getStackInSlot(slot);
            if (!stack.isEmpty() && base.test(stack)) {
                return true;
            }
        }
        return false;
    }

    private void manageQueue(Level level, @Nullable Item active, @Nullable SequencedAssemblyRecipe activeRecipe) {
        if (active == null || activeRecipe == null) {
            return;
        }
        if (owed.getOrDefault(active, 0L) <= 0 && inFlightFor(active) <= 0) {
            cleanupStations(level);
            queue.remove(active);
            inFlight.remove(active);
            templates.remove(active);
            if (templates.isEmpty()) {
                pushingCpus.clear();
            }
            updateHalted(level, false);
            markProgress(level);
            setChanged();
            if (NepConfig.debugLogging()) {
                Nep.LOGGER.info(
                        "SA controller {} finished {}, advancing queue ({} recipe(s) left)",
                        getBlockPos(),
                        active,
                        queue.size());
            }
            return;
        }
        if (halted && queue.size() > 1) {
            cleanupStations(level);
            queue.remove(active);
            queue.add(active);
            updateHalted(level, false);
            markProgress(level);
            setChanged();
            if (NepConfig.debugLogging()) {
                Nep.LOGGER.info("SA controller {} rotated halted {} to the back of the queue", getBlockPos(), active);
            }
        }
    }

    private void cleanupStations(Level level) {
        for (BlockPos machine : machines) {
            StationKind kind = Stations.detect(level.getBlockState(machine));
            if (kind == StationKind.DEPLOYER) {
                IItemHandler handler = machineItemHandler(level, machine);
                if (handler == null || handler.getSlots() == 0) {
                    continue;
                }
                ItemStack tool = handler.extractItem(handler.getSlots() - 1, Integer.MAX_VALUE, false);
                if (!tool.isEmpty()) {
                    reclaimItem(tool);
                }
            } else if (kind == StationKind.SPOUT) {
                IFluidHandler tank = machineFluidHandler(level, machine);
                if (tank == null) {
                    continue;
                }
                for (int t = 0; t < tank.getTanks(); t++) {
                    FluidStack drained = tank.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE);
                    if (drained.isEmpty()) {
                        break;
                    }
                    reclaimFluid(drained);
                }
            }
        }
    }

    private void reclaimItem(ItemStack stack) {
        long dumped = requester.dumpToNetwork(AEItemKey.of(stack), stack.getCount());
        if (dumped >= stack.getCount()) {
            return;
        }
        ItemStack rest = stack.copyWithCount((int) (stack.getCount() - dumped));
        ItemStack leftover = ItemHandlerHelper.insertItem(buffer, rest, false);
        if (!leftover.isEmpty()) {
            ItemHandlerHelper.insertItem(byproductBuffer, leftover, false);
        }
    }

    private void reclaimFluid(FluidStack stack) {
        long dumped = requester.dumpToNetwork(AEFluidKey.of(stack), stack.getAmount());
        long remaining = stack.getAmount() - dumped;
        if (remaining > 0) {
            FluidStack rest = stack.copy();
            rest.setAmount((int) remaining);
            fluids.fill(rest, IFluidHandler.FluidAction.EXECUTE);
        }
    }

    private void setOutputBlocked(Level level, boolean value) {
        if (outputBlocked == value) {
            return;
        }
        outputBlocked = value;
        setChanged();
        refreshVisualState();
        level.updateNeighbourForOutputSignal(getBlockPos(), getBlockState().getBlock());
        if (NepConfig.debugLogging()) {
            Nep.LOGGER.info(
                    "SA controller {} output is now {}",
                    getBlockPos(),
                    value ? "blocked — buffer and network both full" : "accepting results again");
        }
    }

    boolean isOutputBlocked() {
        return outputBlocked;
    }

    private void updateHalted(Level level, boolean value) {
        if (halted == value) {
            return;
        }
        halted = value;
        setChanged();
        refreshVisualState();
        level.updateNeighbourForOutputSignal(getBlockPos(), getBlockState().getBlock());
    }

    boolean isHalted() {
        return halted;
    }

    int comparatorOutput() {
        return ComparatorSignal.of(outputBuffer);
    }

    private List<SequencedAssemblyRecipe> lineRecipes(Level level) {
        long now = level.getGameTime();
        int generation = SequencedAssemblyResolver.generation();
        if (now >= lineRecipesExpiry || generation != lineRecipesGeneration) {
            lineRecipes = SequencedAssemblyResolver.recipesForLine(level, input, output, machines);
            lineRecipesExpiry = now + POLL_INTERVAL;
            lineRecipesGeneration = generation;
        }
        return lineRecipes;
    }

    private void refreshComparator(Level level) {
        int signal = comparatorOutput();
        if (signal != lastComparator) {
            lastComparator = signal;
            level.updateNeighbourForOutputSignal(getBlockPos(), getBlockState().getBlock());
        }
    }

    private ItemStack injectAtInput(Level level, ItemStack stack, boolean simulate) {
        DirectBeltInputBehaviour belt = BlockEntityBehaviour.get(level, input, DirectBeltInputBehaviour.TYPE);
        if (belt != null) {
            for (Direction side : Direction.values()) {
                if (belt.canInsertFromSide(side)) {
                    return belt.handleInsertion(stack, side, simulate);
                }
            }
        }
        IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, input, null);
        if (handler != null) {
            return ItemHandlerHelper.insertItem(handler, stack, simulate);
        }
        return stack;
    }

    boolean readyForPatterns() {
        return input != null && output != null;
    }

    boolean pushSequencedPattern(IPatternDetails pattern, KeyCounter[] inputs, Direction ejectionDirection) {
        Level level = getLevel();
        if (level == null || level.isClientSide || !NepConfig.createSequencedAssembly()) {
            return false;
        }
        if (!(pattern instanceof SequencedAssemblyPattern assembly)) {
            return false;
        }
        List<GenericStack> outputs = pattern.getOutputs();
        if (outputs.isEmpty() || !(outputs.get(0).what() instanceof AEItemKey outputKey)) {
            return reject("pattern has no item output");
        }
        if (!outputKey.toStack().getComponentsPatch().isEmpty()) {
            return reject("pattern output carries data components");
        }
        SequencedAssemblyRecipe recipe = SequencedAssemblyResolver.resolveById(level, assembly.recipe());
        if (recipe == null) {
            return reject("no sequenced assembly recipe with id " + assembly.recipe());
        }
        if (!lineRecipes(level).contains(recipe)) {
            return reject("linked line does not match this recipe's station layout");
        }
        Map<AEItemKey, Long> items = new HashMap<>();
        Map<AEFluidKey, Long> fluids = new HashMap<>();
        if (!DepotMachines.collectInputs(inputs, items, fluids)) {
            return reject("unreadable pattern inputs");
        }
        if (!bufferAll(items, fluids)) {
            return reject("controller buffer is full");
        }
        Item producedItem = outputKey.getItem();
        returnDirections.record(producedItem, ejectionDirection);
        long producedAmount = Math.max(1, outputs.get(0).amount());
        captureTemplate(producedItem, items, fluids);
        owed.merge(producedItem, producedAmount, Long::sum);
        pushingCpus.record();
        toReturn.merge(producedItem, producedAmount, Long::sum);
        if (lastProgressTick == Long.MIN_VALUE) {
            markProgress(level);
        }
        setChanged();
        return accept(outputKey);
    }

    private boolean bufferAll(Map<AEItemKey, Long> items, Map<AEFluidKey, Long> patternFluids) {
        List<ItemStack> stacks = new ArrayList<>();
        for (Map.Entry<AEItemKey, Long> entry : items.entrySet()) {
            long count = entry.getValue();
            while (count > 0) {
                int chunk = (int) Math.min(count, 64);
                stacks.add(entry.getKey().toStack(chunk));
                count -= chunk;
            }
        }
        ItemStackHandler probe = new ItemStackHandler(buffer.getSlots());
        for (int i = 0; i < buffer.getSlots(); i++) {
            probe.setStackInSlot(i, buffer.getStackInSlot(i).copy());
        }
        for (ItemStack stack : stacks) {
            if (!ItemHandlerHelper.insertItem(probe, stack.copy(), false).isEmpty()) {
                return false;
            }
        }
        List<FluidStack> fluidStacks = new ArrayList<>(patternFluids.size());
        for (Map.Entry<AEFluidKey, Long> entry : patternFluids.entrySet()) {
            if (entry.getValue() > Integer.MAX_VALUE) {
                return false;
            }
            fluidStacks.add(entry.getKey().toStack(entry.getValue().intValue()));
        }
        if (!fluids.canFillAll(fluidStacks)) {
            return false;
        }
        for (ItemStack stack : stacks) {
            ItemHandlerHelper.insertItem(buffer, stack, false);
        }
        for (FluidStack stack : fluidStacks) {
            fluids.fill(stack, IFluidHandler.FluidAction.EXECUTE);
        }
        return true;
    }

    List<FluidStack> stagedFluids() {
        List<FluidStack> staged = new ArrayList<>(AssemblyFluidBuffer.TANKS);
        for (int tank = 0; tank < AssemblyFluidBuffer.TANKS; tank++) {
            staged.add(fluids.getFluidInTank(tank).copy());
        }
        return staged;
    }

    private void distribute(Level level, SequencedAssemblyRecipe activeRecipe, Item active) {
        List<SequencedAssemblyRecipe> only = List.of(activeRecipe);
        boolean recircBlocked =
                drainRecirc(level, activeRecipe.getTransitionalItem().getItem());

        List<List<Ingredient>> layout = SequencedAssemblyResolver.deployerToolLayout(only);
        List<BlockPos> deployers = orderedDeployers(level);
        for (int index = 0; index < deployers.size() && index < layout.size(); index++) {
            stockDeployer(level, deployers.get(index), layout.get(index), index);
        }
        stockSpouts(level, active);
        updateStarved(level, deployers, layout);
        if (starved || recircBlocked) {
            return;
        }

        Ingredient base = activeRecipe.getIngredient();
        for (int slot = 0; slot < buffer.getSlots(); slot++) {
            if (inFlightFor(active) >= owed.getOrDefault(active, 0L)) {
                break;
            }
            ItemStack stack = buffer.getStackInSlot(slot);
            if (stack.isEmpty() || !base.test(stack)) {
                continue;
            }
            ItemStack one = stack.copyWithCount(1);
            if (injectAtInput(level, one, true).isEmpty()) {
                injectAtInput(level, buffer.extractItem(slot, 1, false), false);
                addInFlight(active, 1);
                if (lastResolveTick == Long.MIN_VALUE) {
                    lastResolveTick = level.getGameTime();
                }
                markProgress(level);
            }
        }
    }

    private boolean drainRecirc(Level level, Item transitional) {
        boolean blocked = false;
        Iterator<ItemStack> it = recirc.iterator();
        while (it.hasNext()) {
            ItemStack held = it.next();
            if (held.getItem() != transitional) {
                continue;
            }
            if (injectAtInput(level, held.copy(), true).isEmpty()) {
                injectAtInput(level, held, false);
                it.remove();
                setChanged();
                markProgress(level);
            } else {
                blocked = true;
                break;
            }
        }
        return blocked;
    }

    private void updateStarved(Level level, List<BlockPos> deployers, List<List<Ingredient>> layout) {
        boolean now = !stationsSupplied(level, deployers, layout);
        if (now == starved) {
            return;
        }
        starved = now;
        if (NepConfig.debugLogging()) {
            Nep.LOGGER.info(
                    "SA controller {} line is now {} — {} feeding base items",
                    getBlockPos(),
                    now ? "short of station ingredients" : "fully supplied",
                    now ? "pausing" : "resuming");
        }
    }

    private boolean stationsSupplied(Level level, List<BlockPos> deployers, List<List<Ingredient>> layout) {
        for (int index = 0; index < deployers.size() && index < layout.size(); index++) {
            if (firstTool(layout.get(index)) == null) {
                continue;
            }
            IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, deployers.get(index), null);
            if (handler == null || handler.getSlots() == 0) {
                continue;
            }
            if (handler.getStackInSlot(handler.getSlots() - 1).isEmpty()) {
                return false;
            }
        }
        for (BlockPos machine : machines) {
            if (Stations.detect(level.getBlockState(machine)) != StationKind.SPOUT) {
                continue;
            }
            IFluidHandler tank = level.getCapability(Capabilities.FluidHandler.BLOCK, machine, Direction.UP);
            if (tank == null || tank.getTanks() == 0) {
                continue;
            }
            boolean filled = false;
            for (int t = 0; t < tank.getTanks() && !filled; t++) {
                filled = !tank.getFluidInTank(t).isEmpty();
            }
            if (!filled) {
                return false;
            }
        }
        return true;
    }

    @Nullable
    private static ItemStack firstTool(List<Ingredient> tools) {
        for (Ingredient tool : tools) {
            if (tool.isEmpty()) {
                continue;
            }
            ItemStack[] items = tool.getItems();
            if (items.length > 0) {
                return items[0];
            }
        }
        return null;
    }

    private List<BlockPos> orderedDeployers(Level level) {
        List<BlockPos> deployers = new ArrayList<>();
        for (BlockPos machine : machines) {
            if (Stations.detect(level.getBlockState(machine)) == StationKind.DEPLOYER) {
                deployers.add(machine);
            }
        }
        return deployers;
    }

    private static boolean matchesBase(List<Ingredient> bases, ItemStack stack) {
        for (Ingredient base : bases) {
            if (base.test(stack)) {
                return true;
            }
        }
        return false;
    }

    private boolean activeFluidAllowed(@Nullable Item active, FluidStack buffered) {
        Template template = active == null ? null : templates.get(active);
        if (template == null) {
            return true;
        }
        for (AEKey key : template.keys()) {
            if (key instanceof AEFluidKey fluidKey && fluidKey.matches(buffered)) {
                return true;
            }
        }
        return false;
    }

    private void stockSpouts(Level level, Item active) {
        if (fluids.isEmpty()) {
            return;
        }
        for (BlockPos machine : machines) {
            if (Stations.detect(level.getBlockState(machine)) != StationKind.SPOUT) {
                continue;
            }
            IFluidHandler tank = machineFluidHandler(level, machine);
            if (tank == null || tank.getTanks() == 0) {
                continue;
            }
            FluidStack held = tank.getFluidInTank(0);
            for (int source = 0; source < AssemblyFluidBuffer.TANKS; source++) {
                FluidStack buffered = fluids.getFluidInTank(source);
                if (buffered.isEmpty() || !activeFluidAllowed(active, buffered)) {
                    continue;
                }
                if (!held.isEmpty() && !FluidStack.isSameFluidSameComponents(held, buffered)) {
                    continue;
                }
                int filled = tank.fill(buffered.copy(), IFluidHandler.FluidAction.EXECUTE);
                if (filled > 0) {
                    fluids.takeFrom(source, filled);
                    break;
                }
            }
        }
    }

    private void stockDeployer(Level level, BlockPos machine, List<Ingredient> tools, int index) {
        if (tools.isEmpty()) {
            return;
        }
        IItemHandler handler = machineItemHandler(level, machine);
        if (handler == null || handler.getSlots() == 0) {
            return;
        }
        int held = handler.getSlots() - 1;
        for (int slot = 0; slot < buffer.getSlots(); slot++) {
            ItemStack inBuffer = buffer.getStackInSlot(slot);
            if (inBuffer.isEmpty() || !matchesAnyTool(tools, inBuffer)) {
                continue;
            }
            ItemStack current = handler.getStackInSlot(held);
            if (!current.isEmpty() && !ItemStack.isSameItemSameComponents(current, inBuffer)) {
                continue;
            }
            ItemStack remainder = handler.insertItem(held, inBuffer, true);
            int inserted = inBuffer.getCount() - remainder.getCount();
            if (inserted <= 0) {
                continue;
            }
            handler.insertItem(held, buffer.extractItem(slot, inserted, false), false);
            if (NepConfig.debugLogging()) {
                Nep.LOGGER.info(
                        "SA controller {} stocked deployer #{} @ {} with {} x{}",
                        getBlockPos(),
                        index + 1,
                        machine,
                        inBuffer.getItem(),
                        inserted);
            }
        }
    }

    private static boolean matchesAnyTool(List<Ingredient> tools, ItemStack stack) {
        for (Ingredient tool : tools) {
            if (!tool.isEmpty() && tool.test(stack)) {
                return true;
            }
        }
        return false;
    }

    void dropBuffer(Level level, BlockPos pos) {
        dropHandler(level, pos, buffer);
        dropHandler(level, pos, outputBuffer);
        dropHandler(level, pos, byproductBuffer);
        for (ItemStack held : recirc) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), held);
        }
        recirc.clear();
        for (int tank = 0; tank < AssemblyFluidBuffer.TANKS; tank++) {
            FluidStack held = fluids.getFluidInTank(tank);
            if (held.isEmpty()) {
                continue;
            }
            long dumped = requester.dumpToNetwork(AEFluidKey.of(held), held.getAmount());
            if (dumped > 0) {
                fluids.takeFrom(tank, (int) dumped);
            }
        }
        clearContent();
    }

    @Override
    public void clearContent() {
        clearHandler(buffer);
        clearHandler(outputBuffer);
        clearHandler(byproductBuffer);
        fluids.clear();
        recirc.clear();
        owed.clear();
        toReturn.clear();
    }

    private static void clearHandler(ItemStackHandler handler) {
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            handler.setStackInSlot(slot, ItemStack.EMPTY);
        }
    }

    private static void dropHandler(Level level, BlockPos pos, ItemStackHandler handler) {
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
            }
        }
    }

    private boolean reject(String reason) {
        if (NepConfig.debugLogging()) {
            Nep.LOGGER.info("SA controller {} rejected pattern: {}", getBlockPos(), reason);
        }
        return false;
    }

    private boolean accept(AEItemKey output) {
        if (NepConfig.debugLogging()) {
            Nep.LOGGER.info("SA controller {} accepted pattern for {}", getBlockPos(), output);
        }
        return true;
    }

    void clearLinks() {
        input = null;
        output = null;
        machines.clear();
        forgetStations();
        setChanged();
        refreshVisualState();
        syncLinks();
    }

    @Nullable
    BlockPos linkedInput() {
        return input;
    }

    @Nullable
    BlockPos linkedOutput() {
        return output;
    }

    List<BlockPos> linkedMachines() {
        return List.copyOf(machines);
    }

    private void syncLinks() {
        Level level = getLevel();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    IItemHandler getBuffer() {
        return buffer;
    }

    IItemHandler itemHandlerForSide() {
        return machineView;
    }

    IItemHandler getOutputBuffer() {
        return outputBuffer;
    }

    IFluidHandler fluidHandler() {
        return fluids;
    }

    SequencedAssemblyState buildState() {
        Level level = getLevel();
        List<SequencedAssemblyState.Station> stationList = new ArrayList<>(machines.size());
        if (level != null) {
            List<StationKind> inferred = null;
            for (int slot = 0; slot < machines.size(); slot++) {
                BlockPos machine = machines.get(slot);
                BlockState state = level.getBlockState(machine);
                StationKind kind = Stations.detect(state);
                SequencedAssemblyState.Issue issue;
                if (!kind.recognized()) {
                    issue = SequencedAssemblyState.Issue.UNRECOGNIZED;
                } else if (kind == StationKind.DEPLOYER && !Stations.deployerFacesDown(state)) {
                    issue = SequencedAssemblyState.Issue.WRONG_FACING;
                } else if (Stations.isUnpowered(level, machine, kind)) {
                    issue = SequencedAssemblyState.Issue.UNPOWERED;
                } else {
                    issue = SequencedAssemblyState.Issue.OK;
                }
                StationKind hint = StationKind.UNKNOWN;
                if (issue == SequencedAssemblyState.Issue.UNRECOGNIZED) {
                    hint = rememberedStation(slot);
                    if (!hint.recognized()) {
                        if (inferred == null) {
                            inferred =
                                    SequencedAssemblyResolver.expectedStations(level, machines, queuedRecipes(level));
                        }
                        hint = inferred.get(slot);
                    }
                }
                stationList.add(new SequencedAssemblyState.Station(kind, machine.immutable(), issue, hint));
            }
        }
        List<SequencedAssemblyState.Making> makingList = new ArrayList<>(owed.size());
        for (Map.Entry<Item, Long> entry : owed.entrySet()) {
            makingList.add(new SequencedAssemblyState.Making(new ItemStack(entry.getKey()), entry.getValue()));
        }
        makingList.sort((a, b) -> Long.compare(b.count(), a.count()));
        List<GenericStack> missing = halted && level != null ? collectMissing(level) : List.of();
        return new SequencedAssemblyState(
                statusCode(), halted, outputBlocked, stationList, makingList, missing, stagedFluids());
    }

    private List<SequencedAssemblyRecipe> queuedRecipes(Level level) {
        List<SequencedAssemblyRecipe> recipes = new ArrayList<>(queue.size());
        for (Item item : queue) {
            SequencedAssemblyRecipe recipe = SequencedAssemblyResolver.resolveByResult(level, new ItemStack(item));
            if (recipe != null && !recipes.contains(recipe)) {
                recipes.add(recipe);
            }
        }
        return recipes;
    }

    private List<GenericStack> collectMissing(Level level) {
        List<GenericStack> missing = new ArrayList<>();
        Set<Item> seen = new HashSet<>();
        List<BlockPos> deployers = orderedDeployers(level);
        Item active = activeOutput();
        if (active == null) {
            return missing;
        }
        SequencedAssemblyRecipe recipe = SequencedAssemblyResolver.resolveByResult(level, new ItemStack(active));
        if (recipe == null) {
            return missing;
        }
        ItemStack[] items = recipe.getIngredient().getItems();
        if (items.length > 0 && !hasBaseMaterial(recipe) && seen.add(items[0].getItem())) {
            addMissingItem(missing, items[0]);
        }
        List<List<Ingredient>> layout = SequencedAssemblyResolver.deployerToolLayout(List.of(recipe));
        for (int index = 0; index < deployers.size() && index < layout.size(); index++) {
            ItemStack tool = firstTool(layout.get(index));
            if (tool == null || !deployerEmpty(level, deployers.get(index))) {
                continue;
            }
            if (seen.add(tool.getItem())) {
                addMissingItem(missing, tool);
            }
        }
        collectMissingFluids(level, recipe, missing);
        return missing;
    }

    private static void addMissingItem(List<GenericStack> missing, ItemStack stack) {
        AEItemKey key = AEItemKey.of(stack);
        if (key != null) {
            missing.add(new GenericStack(key, 1));
        }
    }

    private void collectMissingFluids(Level level, SequencedAssemblyRecipe recipe, List<GenericStack> missing) {
        for (SequencedAssemblyResolver.FluidDemand demand :
                SequencedAssemblyResolver.demandOf(recipe).fluids()) {
            FluidStack[] candidates = demand.ingredient().getStacks();
            if (candidates.length == 0 || spoutHolds(level, demand)) {
                continue;
            }
            if (stagedFluid(demand) > 0) {
                continue;
            }
            AEFluidKey key = AEFluidKey.of(candidates[0]);
            if (key != null) {
                missing.add(new GenericStack(key, demand.amount()));
            }
        }
    }

    private boolean spoutHolds(Level level, SequencedAssemblyResolver.FluidDemand demand) {
        for (BlockPos machine : machines) {
            if (Stations.detect(level.getBlockState(machine)) != StationKind.SPOUT) {
                continue;
            }
            IFluidHandler tank = level.getCapability(Capabilities.FluidHandler.BLOCK, machine, Direction.UP);
            if (tank == null) {
                continue;
            }
            for (int t = 0; t < tank.getTanks(); t++) {
                if (demand.ingredient().test(tank.getFluidInTank(t))) {
                    return true;
                }
            }
        }
        return false;
    }

    private int stagedFluid(SequencedAssemblyResolver.FluidDemand demand) {
        int total = 0;
        for (int tank = 0; tank < AssemblyFluidBuffer.TANKS; tank++) {
            FluidStack held = fluids.getFluidInTank(tank);
            if (demand.ingredient().test(held)) {
                total += held.getAmount();
            }
        }
        return total;
    }

    private boolean deployerEmpty(Level level, BlockPos deployer) {
        IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, deployer, null);
        return handler != null
                && handler.getSlots() > 0
                && handler.getStackInSlot(handler.getSlots() - 1).isEmpty();
    }

    void clearBufferTo(Player player) {
        boolean onTheLine = anythingOnTheLine();
        emptyHandlerTo(player, buffer);
        for (int slot = 0; slot < outputBuffer.getSlots(); slot++) {
            ItemStack taken = outputBuffer.extractItem(slot, Integer.MAX_VALUE, false);
            if (!taken.isEmpty()) {
                decrementToReturn(taken.getItem(), taken.getCount());
                player.getInventory().placeItemBackInInventory(taken);
            }
        }
        emptyHandlerTo(player, byproductBuffer);
        for (ItemStack held : recirc) {
            player.getInventory().placeItemBackInInventory(held);
        }
        recirc.clear();
        for (int tank = 0; tank < AssemblyFluidBuffer.TANKS; tank++) {
            FluidStack held = fluids.getFluidInTank(tank);
            if (held.isEmpty()) {
                continue;
            }
            long dumped = requester.dumpToNetwork(AEFluidKey.of(held), held.getAmount());
            if (dumped > 0) {
                fluids.takeFrom(tank, (int) dumped);
            }
        }
        Level level = getLevel();
        if (onTheLine && level != null) {
            startReclaim(level);
        }
        setChanged();
    }

    private static void emptyHandlerTo(Player player, ItemStackHandler handler) {
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.extractItem(slot, Integer.MAX_VALUE, false);
            if (!stack.isEmpty()) {
                player.getInventory().placeItemBackInInventory(stack);
            }
        }
    }

    private int statusCode() {
        Level level = getLevel();
        if (level == null || input == null || output == null) {
            return 0;
        }
        return SequencedAssemblyResolver.lineHealthy(level, input, output, machines) ? 1 : 2;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.nep.sequenced_assembly_controller");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory inventory, Player player) {
        return new SequencedAssemblyControllerMenu(windowId, inventory, getBlockPos());
    }

    void sendStatus(Player player) {
        Level level = getLevel();
        player.displayClientMessage(
                Component.translatable("block.nep.sequenced_assembly_controller")
                        .withStyle(ChatFormatting.GOLD)
                        .append(Component.literal(" " + format(getBlockPos())).withStyle(ChatFormatting.GRAY)),
                false);
        player.displayClientMessage(endpointLine("chat.nep.sequenced_assembly.input", input), false);
        player.displayClientMessage(endpointLine("chat.nep.sequenced_assembly.output", output), false);
        if (machines.isEmpty()) {
            player.displayClientMessage(
                    Component.translatable("chat.nep.sequenced_assembly.no_machines")
                            .withStyle(ChatFormatting.GRAY),
                    false);
        } else {
            for (int i = 0; i < machines.size(); i++) {
                BlockPos pos = machines.get(i);
                StationKind kind = level == null ? StationKind.UNKNOWN : Stations.detect(level.getBlockState(pos));
                player.displayClientMessage(
                        Component.translatable(
                                        "chat.nep.sequenced_assembly.step",
                                        i + 1,
                                        kind.displayName()
                                                .copy()
                                                .withStyle(
                                                        kind.recognized() ? ChatFormatting.GREEN : ChatFormatting.RED),
                                        Component.literal(format(pos)).withStyle(ChatFormatting.DARK_GRAY))
                                .withStyle(ChatFormatting.GRAY),
                        false);
            }
        }
        if (level != null) {
            player.displayClientMessage(armStatusLine(level), false);
        }
    }

    private Component armStatusLine(Level level) {
        if (input == null || output == null) {
            return Component.translatable("chat.nep.sequenced_assembly.line.unlinked")
                    .withStyle(ChatFormatting.GRAY);
        }
        if (!SequencedAssemblyResolver.lineHealthy(level, input, output, machines)) {
            return Component.translatable("chat.nep.sequenced_assembly.line.problem")
                    .withStyle(ChatFormatting.YELLOW);
        }
        int matches = lineRecipes(level).size();
        if (matches == 0) {
            return Component.translatable("chat.nep.sequenced_assembly.line.no_recipe")
                    .withStyle(ChatFormatting.GRAY);
        }
        String key = matches == 1
                ? "chat.nep.sequenced_assembly.line.ready"
                : "chat.nep.sequenced_assembly.line.ready.plural";
        return Component.translatable(key, matches).withStyle(ChatFormatting.GREEN);
    }

    private static Component endpointLine(String key, @Nullable BlockPos pos) {
        Component value = pos == null
                ? Component.translatable("chat.nep.sequenced_assembly.unset").withStyle(ChatFormatting.RED)
                : Component.literal(format(pos)).withStyle(ChatFormatting.WHITE);
        return Component.translatable(key, value).withStyle(ChatFormatting.GRAY);
    }

    private static String format(BlockPos pos) {
        return "(" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ")";
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        writeLinks(tag);
        if (StationLayouts.anyKnown(stationKinds)) {
            ListTag kinds = new ListTag();
            for (StationKind kind : stationKinds) {
                kinds.add(StringTag.valueOf(kind.name()));
            }
            tag.put(STATION_KINDS_KEY, kinds);
        }
        tag.put(BUFFER_KEY, buffer.serializeNBT(registries));
        tag.put(OUTPUT_BUFFER_KEY, outputBuffer.serializeNBT(registries));
        tag.put(BYPRODUCT_BUFFER_KEY, byproductBuffer.serializeNBT(registries));
        if (!owed.isEmpty()) {
            tag.put(OWED_KEY, ItemCounts.save(owed));
        }
        if (!toReturn.isEmpty()) {
            tag.put(TO_RETURN_KEY, ItemCounts.save(toReturn));
        }
        if (!inFlight.isEmpty()) {
            tag.put(INFLIGHT_KEY, ItemCounts.save(inFlight));
        }
        if (!returnDirections.isEmpty()) {
            tag.put(RETURN_DIRS_KEY, returnDirections.save());
        }
        pushingCpus.save(tag);
        if (!templates.isEmpty()) {
            ListTag templateList = new ListTag();
            for (Map.Entry<Item, Template> entry : templates.entrySet()) {
                Template template = entry.getValue();
                ListTag entries = new ListTag();
                for (int i = 0; i < template.keys().size(); i++) {
                    CompoundTag keyTag = new CompoundTag();
                    keyTag.put("Key", template.keys().get(i).toTagGeneric(registries));
                    keyTag.putLong("Per", template.counts().get(i));
                    entries.add(keyTag);
                }
                CompoundTag templateTag = new CompoundTag();
                templateTag.putString(
                        "Output", BuiltInRegistries.ITEM.getKey(entry.getKey()).toString());
                templateTag.put("Entries", entries);
                templateList.add(templateTag);
            }
            tag.put(TEMPLATE_KEY, templateList);
        }
        if (!queue.isEmpty()) {
            ListTag queueList = new ListTag();
            for (Item output : queue) {
                CompoundTag entryTag = new CompoundTag();
                entryTag.putString("Id", BuiltInRegistries.ITEM.getKey(output).toString());
                queueList.add(entryTag);
            }
            tag.put(QUEUE_KEY, queueList);
        }
        if (!recirc.isEmpty()) {
            ListTag recircList = new ListTag();
            for (ItemStack held : recirc) {
                recircList.add(held.save(registries));
            }
            tag.put(RECIRC_KEY, recircList);
        }
        if (reclaimUntil != Long.MIN_VALUE) {
            tag.putLong(RECLAIM_KEY, reclaimUntil);
        }
        CompoundTag nodeTag = new CompoundTag();
        requester.save(nodeTag);
        tag.put(NODE_KEY, nodeTag);
        tag.put(FLUID_BUFFER_KEY, fluids.save(registries));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        writeLinks(tag);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        readLinks(tag);
    }

    @Override
    public void onDataPacket(
            Connection net, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries) {
        CompoundTag tag = packet.getTag();
        if (!tag.isEmpty()) {
            readLinks(tag);
        }
    }

    private void writeLinks(CompoundTag tag) {
        if (input != null) {
            tag.putLong(INPUT_KEY, input.asLong());
        }
        if (output != null) {
            tag.putLong(OUTPUT_KEY, output.asLong());
        }
        long[] packed = new long[machines.size()];
        for (int i = 0; i < machines.size(); i++) {
            packed[i] = machines.get(i).asLong();
        }
        tag.putLongArray(MACHINES_KEY, packed);
    }

    private void readLinks(CompoundTag tag) {
        input = tag.contains(INPUT_KEY) ? BlockPos.of(tag.getLong(INPUT_KEY)) : null;
        output = tag.contains(OUTPUT_KEY) ? BlockPos.of(tag.getLong(OUTPUT_KEY)) : null;
        machines.clear();
        for (long packed : tag.getLongArray(MACHINES_KEY)) {
            machines.add(BlockPos.of(packed));
        }
        forgetStations();
        machineItemCaps.clear();
        machineFluidCaps.clear();
        stationSnapshot = null;
        lineRecipesExpiry = Long.MIN_VALUE;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        readLinks(tag);
        if (tag.contains(STATION_KINDS_KEY, Tag.TAG_LIST)) {
            ListTag kinds = tag.getList(STATION_KINDS_KEY, Tag.TAG_STRING);
            for (int slot = 0; slot < kinds.size() && slot < stationKinds.size(); slot++) {
                stationKinds.set(slot, StationKind.byName(kinds.getString(slot)));
            }
        }
        if (tag.contains(BUFFER_KEY)) {
            buffer.deserializeNBT(registries, tag.getCompound(BUFFER_KEY));
        }
        if (tag.contains(OUTPUT_BUFFER_KEY)) {
            outputBuffer.deserializeNBT(registries, tag.getCompound(OUTPUT_BUFFER_KEY));
        }
        if (tag.contains(BYPRODUCT_BUFFER_KEY)) {
            byproductBuffer.deserializeNBT(registries, tag.getCompound(BYPRODUCT_BUFFER_KEY));
        }
        ItemCounts.load(owed, tag, OWED_KEY);
        ItemCounts.load(toReturn, tag, TO_RETURN_KEY);
        ItemCounts.load(inFlight, tag, INFLIGHT_KEY);
        returnDirections.load(tag, RETURN_DIRS_KEY, RETURN_DIR_KEY, toReturn.keySet());
        pushingCpus.load(tag);
        templates.clear();
        if (tag.contains(TEMPLATE_KEY, Tag.TAG_LIST)) {
            ListTag templateList = tag.getList(TEMPLATE_KEY, Tag.TAG_COMPOUND);
            for (int i = 0; i < templateList.size(); i++) {
                CompoundTag templateTag = templateList.getCompound(i);
                ListTag entries = templateTag.getList("Entries", Tag.TAG_COMPOUND);
                List<AEKey> keys = new ArrayList<>(entries.size());
                List<Long> counts = new ArrayList<>(entries.size());
                for (int entry = 0; entry < entries.size(); entry++) {
                    CompoundTag keyTag = entries.getCompound(entry);
                    AEKey key = AEKey.fromTagGeneric(registries, keyTag.getCompound("Key"));
                    if (key != null) {
                        keys.add(key);
                        counts.add(keyTag.getLong("Per"));
                    }
                }
                Item outputItem = ItemCounts.item(templateTag.getString("Output"));
                if (!keys.isEmpty() && outputItem != null) {
                    templates.put(outputItem, new Template(List.copyOf(keys), List.copyOf(counts)));
                }
            }
        }
        queue.clear();
        if (tag.contains(QUEUE_KEY, Tag.TAG_LIST)) {
            ListTag queueList = tag.getList(QUEUE_KEY, Tag.TAG_COMPOUND);
            for (int i = 0; i < queueList.size(); i++) {
                Item output = ItemCounts.item(queueList.getCompound(i).getString("Id"));
                if (output != null && !queue.contains(output)) {
                    queue.add(output);
                }
            }
        }
        recirc.clear();
        if (tag.contains(RECIRC_KEY, Tag.TAG_LIST)) {
            ListTag recircList = tag.getList(RECIRC_KEY, Tag.TAG_COMPOUND);
            for (int i = 0; i < recircList.size(); i++) {
                ItemStack held = ItemStack.parseOptional(registries, recircList.getCompound(i));
                if (!held.isEmpty()) {
                    recirc.add(held);
                }
            }
        }
        reclaimUntil = tag.contains(RECLAIM_KEY) ? tag.getLong(RECLAIM_KEY) : Long.MIN_VALUE;
        if (tag.contains(NODE_KEY)) {
            requester.load(tag.getCompound(NODE_KEY));
        }
        if (tag.contains(FLUID_BUFFER_KEY, Tag.TAG_LIST)) {
            fluids.load(registries, tag.getList(FLUID_BUFFER_KEY, Tag.TAG_COMPOUND));
        }
    }
}
