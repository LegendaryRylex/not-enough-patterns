package dev.rylex.nep.compat.create;

import appeng.api.config.Actionable;
import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import com.simibubi.create.infrastructure.config.AllConfigs;
import dev.rylex.nep.Nep;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.compat.create.SequencedAssemblyMatrixBlock.MatrixStatus;
import dev.rylex.nep.compat.create.newage.CreateNewAgeCompat;
import dev.rylex.nep.machine.ComparatorSignal;
import dev.rylex.nep.machine.MachineItemView;
import dev.rylex.nep.machine.MatrixEnergyBuffer;
import dev.rylex.nep.machine.MatrixGridNode;
import dev.rylex.nep.machine.MatrixHost;
import dev.rylex.nep.machine.OverstackedItemHandler;
import dev.rylex.nep.machine.PushingCpus;
import dev.rylex.nep.machine.ReturnDirections;
import dev.rylex.nep.pattern.SequencedAssemblyPattern;
import dev.rylex.nep.util.ItemCounts;
import dev.rylex.nep.util.ItemRecipeIds;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Clearable;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SequencedAssemblyMatrixBlockEntity extends KineticBlockEntity
        implements MenuProvider, MatrixHost, Clearable {

    static final int INPUT_SLOTS = 18;
    static final int OUTPUT_SLOTS = 9;

    static final int FLAG_POWERED = 1;
    static final int FLAG_ROTATING = 2;
    static final int FLAG_OVERSTRESSED = 4;
    static final int FLAG_FAST_ENOUGH = 8;
    static final int FLAG_STARVED = 16;
    static final int FLAG_OUTPUT_BLOCKED = 32;
    static final int FLAG_NO_ENERGY = 64;
    static final int FLAG_NO_CHANNEL = 128;

    static final boolean NEW_AGE_LOADED = ModList.get().isLoaded(CreateNewAgeCompat.MOD_ID);

    private static final int PROGRESS_SYNC_STEPS = 32;
    private static final int RUNNING_GRACE_TICKS = 10;
    private static final int RESTOCK_INTERVAL = 10;
    private static final int STARVE_GRACE_TICKS = RESTOCK_INTERVAL * 2;
    private static final int TRIM_INTERVAL = 20;

    private static final String INPUT_KEY = "Input";
    private static final String OUTPUT_KEY = "Output";
    private static final String FLUIDS_KEY = "Fluids";
    private static final String OWED_KEY = "Owed";
    private static final String RETURN_KEY = "ToReturn";
    private static final String RETURN_DIR_KEY = "ReturnDir";
    private static final String RETURN_DIRS_KEY = "ReturnDirs";
    private static final String TEMPLATE_KEY = "Template";
    private static final String JOBS_KEY = "Jobs";
    private static final String RECIPE_IDS_KEY = "RecipeIds";
    private static final String MISSING_KEY = "Missing";
    private static final String NODE_KEY = "Node";
    private static final String FLAGS_KEY = "Flags";
    private static final String ACTIVE_KEY = "Active";
    private static final String ROLLED_KEY = "Rolled";
    private static final String CLAIMED_ITEMS_KEY = "ClaimedItems";
    private static final String CLAIMED_FLUIDS_KEY = "ClaimedFluids";
    private static final String PROGRESS_KEY = "Progress";
    private static final String STRESS_KEY = "Stress";
    private static final String ENERGY_KEY = "Energy";
    private static final String ENERGY_STORED_KEY = "EnergyStored";
    private static final String ENERGY_PENDING_KEY = "EnergyPending";

    private final ItemStackHandler inputBuffer = new OverstackedItemHandler(INPUT_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            markScanNeeded();
        }
    };

    private final ItemStackHandler outputBuffer = new OverstackedItemHandler(OUTPUT_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            markScanNeeded();
        }
    };

    private final AssemblyFluidBuffer fluids =
            new AssemblyFluidBuffer(NepConfig::createSequencedAssemblyMatrixTankCapacity, () -> {
                setChanged();
                markScanNeeded();
            });

    private final Map<Item, Long> owed = new HashMap<>();
    private final PushingCpus pushingCpus = new PushingCpus();
    private final Map<Item, Long> toReturn = new HashMap<>();

    private final Map<Item, Template> templates = new HashMap<>();
    private final Map<Item, MatrixJob> jobs = new HashMap<>();
    private final Map<Item, ResourceLocation> recipeIds = new HashMap<>();
    private final List<GenericStack> missingInputs = new ArrayList<>();

    private final MatrixGridNode power = new MatrixGridNode(
            this,
            NepCreateContent.MATRIX_ITEM.get(),
            NepConfig.createSequencedAssemblyMatrixIdleMeDrain(),
            NepConfig.createSequencedAssemblyMatrixChannels(),
            "SA matrix");
    private final IItemHandler outputView = new OutputView();
    private final IItemHandler machineView =
            MachineItemView.demandLimited(inputBuffer, outputView, this::manualDemandFor);

    private final AssemblyFluidBuffer.OverflowSink fluidOverflow = overflow -> {
        AEFluidKey key = AEFluidKey.of(overflow);
        return key == null ? 0 : (int) Math.min(overflow.getAmount(), power.dumpToNetwork(key, overflow.getAmount()));
    };

    private final ReturnDirections returnDirections = new ReturnDirections();

    @Nullable
    private SequencedAssemblyRecipe activeRecipe;

    @Nullable
    private MatrixJob activeJob;

    private ItemStack activeResult = ItemStack.EMPTY;
    private ItemStack rolledResult = ItemStack.EMPTY;
    private final List<ItemStack> claimedItems = new ArrayList<>();
    private final List<FluidStack> claimedFluids = new ArrayList<>();
    private long progress;

    private final MatrixEnergyBuffer energy = new MatrixEnergyBuffer(
            NepConfig.createSequencedAssemblyMatrixEnergyCapacity(),
            NepConfig.createSequencedAssemblyMatrixChargeRate());
    private long pendingEnergyCost;

    private boolean outputBlocked;
    private boolean scanNeeded = true;
    private int syncedSignature = Integer.MIN_VALUE;
    private int lastComparator = -1;
    private int runningGrace;
    private int starveGrace;
    private int clientFlags;
    private int clientStress;
    private float clientProgress;
    private long clientEnergy;
    private long clientEnergyPending;

    public SequencedAssemblyMatrixBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public float calculateStressApplied() {
        float speed = Math.abs(getTheoreticalSpeed());
        float impact = speed <= 0 ? 0 : stressDrawAt(speed) / speed;
        this.lastStressApplied = impact;
        return impact;
    }

    static float peakSpeed() {
        return AllConfigs.server().kinetics.maxRotationSpeed.get();
    }

    static float minimumSpeed() {
        return MatrixStressCurve.effectiveMinimumSpeed(
                NepConfig.createSequencedAssemblyMatrixMinimumSpeed(), peakSpeed());
    }

    static int stressDrawAt(float speed) {
        return MatrixStressCurve.stressAt(
                speed,
                NepConfig.createSequencedAssemblyMatrixMinimumSpeed(),
                peakSpeed(),
                NepConfig.createSequencedAssemblyMatrixStressMinimum(),
                NepConfig.createSequencedAssemblyMatrixStress());
    }

    int stressDraw() {
        Level level = getLevel();
        if (level != null && level.isClientSide) {
            return clientStress;
        }
        return stressDrawAt(Math.abs(getSpeed()));
    }

    float operatingFraction() {
        float peakSpeed = peakSpeed();
        return peakSpeed <= 0 ? 0.0F : Math.min(1.0F, Math.abs(getSpeed()) / peakSpeed);
    }

    float processingFraction() {
        int maximum = NepConfig.createSequencedAssemblyMatrixStress();
        return maximum <= 0 ? 0.0F : Math.min(1.0F, stressDraw() / (float) maximum);
    }

    private static long workPerCraft() {
        return MatrixStressCurve.workPerCraft(
                NepConfig.createSequencedAssemblyMatrixCraftTicks(), NepConfig.createSequencedAssemblyMatrixStress());
    }

    float craftProgress() {
        Level level = getLevel();
        if (level != null && level.isClientSide) {
            return clientProgress;
        }
        if (activeResult.isEmpty()) {
            return 0.0F;
        }
        return Math.min(1.0F, (float) ((double) progress / workPerCraft()));
    }

    @Override
    public void tick() {
        super.tick();
        Level level = getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        if (runningGrace > 0) {
            runningGrace--;
        }
        if (starveGrace > 0) {
            starveGrace--;
        }
        flushOutput(level);
        if (!NepConfig.createSequencedAssemblyMatrix()) {
            refreshComparator(level);
            refreshVisualState(level);
            syncIfChanged();
            return;
        }
        power.create(level, getBlockPos());
        energy.resize(
                Math.max(NepConfig.createSequencedAssemblyMatrixEnergyCapacity(), pendingEnergyCost),
                NepConfig.createSequencedAssemblyMatrixChargeRate());
        long phase = level.getGameTime() + getBlockPos().hashCode();
        if (Math.floorMod(phase, TRIM_INTERVAL) == 0 && fluids.overCapacity()) {
            fluids.trimToCapacity(fluidOverflow);
        }
        if (Math.floorMod(phase, RESTOCK_INTERVAL) == 0) {
            republishStress();
            if (owed.isEmpty()) {
                setMissingInputs(List.of());
                pendingEnergyCost = 0;
            } else {
                autoRequest(level);
            }
        }
        if (isRunnable()) {
            if (activeResult.isEmpty()) {
                power.clearStarved();
                if (scanNeeded && !beginCraft(level)) {
                    scanNeeded = false;
                }
            } else if (outputBlocked) {
                if (scanNeeded || Math.floorMod(phase, RESTOCK_INTERVAL) == 0) {
                    scanNeeded = false;
                    finishCraft(level);
                }
            } else if (drawActivePower()) {
                progress += stressDrawAt(Math.abs(getSpeed()));
                runningGrace = RUNNING_GRACE_TICKS;
                starveGrace = STARVE_GRACE_TICKS;
                if (progress >= workPerCraft()) {
                    finishCraft(level);
                }
            }
        }
        refreshComparator(level);
        refreshVisualState(level);
        syncIfChanged();
    }

    int comparatorOutput() {
        return ComparatorSignal.of(outputBuffer);
    }

    private void refreshComparator(Level level) {
        int signal = comparatorOutput();
        if (signal != lastComparator) {
            lastComparator = signal;
            level.updateNeighbourForOutputSignal(getBlockPos(), getBlockState().getBlock());
        }
    }

    private void refreshVisualState(Level level) {
        BlockState state = getBlockState();
        if (!state.hasProperty(SequencedAssemblyMatrixBlock.STATUS)) {
            return;
        }
        MatrixStatus desired = desiredStatus();
        if (state.getValue(SequencedAssemblyMatrixBlock.STATUS) != desired) {
            level.setBlock(
                    getBlockPos(), state.setValue(SequencedAssemblyMatrixBlock.STATUS, desired), Block.UPDATE_CLIENTS);
        }
    }

    private MatrixStatus desiredStatus() {
        if (runningGrace > 0) {
            return MatrixStatus.RUNNING;
        }
        return activeResult.isEmpty() ? MatrixStatus.IDLE : MatrixStatus.STALLED;
    }

    private boolean isRunnable() {
        return power.isPowered() && stressDrawAt(Math.abs(getSpeed())) > 0;
    }

    private boolean drawActivePower() {
        double extra = meDrain() - NepConfig.createSequencedAssemblyMatrixIdleMeDrain();
        if (extra <= 0) {
            power.clearStarved();
            return true;
        }
        power.extractPower(extra);
        return !power.isStarved();
    }

    int meDrain() {
        return activeResult.isEmpty()
                ? NepConfig.createSequencedAssemblyMatrixIdleMeDrain()
                : NepConfig.createSequencedAssemblyMatrixMeDrain();
    }

    private void finishCraft(Level level) {
        List<ItemStack> kept = activeJob == null ? List.of() : activeJob.retainedStacks();
        if (rolledResult.isEmpty()) {
            rolledResult = decideResult(level);
            setChanged();
            if (rolledResult.isEmpty()) {
                if (NepConfig.debugLogging()) {
                    Nep.LOGGER.info("SA matrix {} rolled nothing at all, attempt lost", getBlockPos());
                }
                completeCraft();
                return;
            }
        }
        boolean wanted =
                ItemStack.isSameItem(rolledResult, activeResult) || owed.getOrDefault(rolledResult.getItem(), 0L) > 0;
        if (!wanted) {
            AEItemKey key = AEItemKey.of(rolledResult);
            int dumped = key == null
                    ? 0
                    : (int) Math.min(rolledResult.getCount(), power.dumpToNetwork(key, rolledResult.getCount()));
            if (dumped > 0) {
                if (NepConfig.debugLogging()) {
                    Nep.LOGGER.info(
                            "SA matrix {} rolled junk {} x{} and dumped it into network storage",
                            getBlockPos(),
                            rolledResult.getItem(),
                            dumped);
                }
                rolledResult.shrink(dumped);
                setChanged();
                if (rolledResult.isEmpty()) {
                    completeCraft();
                    return;
                }
            }
        }
        if (!fitsInOutput(produce(rolledResult, activeJob))) {
            progress = workPerCraft();
            outputBlocked = true;
            return;
        }
        outputBlocked = false;
        ItemStack result = rolledResult;
        ItemHandlerHelper.insertItem(outputBuffer, result.copy(), false);
        for (ItemStack stack : kept) {
            ItemHandlerHelper.insertItem(outputBuffer, stack.copy(), false);
            toReturn.merge(stack.getItem(), (long) stack.getCount(), Long::sum);
        }
        Item produced = result.getItem();
        long claimed = Math.min(owed.getOrDefault(produced, 0L), result.getCount());
        if (claimed > 0) {
            decrement(owed, produced, claimed);
            if (!owed.containsKey(produced)) {
                templates.remove(produced);
                jobs.remove(produced);
                recipeIds.remove(produced);
            }
            if (owed.isEmpty()) {
                pushingCpus.clear();
            }
            toReturn.merge(produced, claimed, Long::sum);
        }
        completeCraft();
        flushOutput(level);
        if (NepConfig.debugLogging()) {
            Nep.LOGGER.info(
                    "SA matrix {} assembled {} x{} ({} still owed)",
                    getBlockPos(),
                    produced,
                    result.getCount(),
                    owed.getOrDefault(produced, 0L));
        }
    }

    private void completeCraft() {
        activeResult = ItemStack.EMPTY;
        rolledResult = ItemStack.EMPTY;
        claimedItems.clear();
        claimedFluids.clear();
        activeRecipe = null;
        activeJob = null;
        progress = 0;
        outputBlocked = false;
        markScanNeeded();
        setChanged();
    }

    private ItemStack decideResult(Level level) {
        if (activeJob != null || NepConfig.createSequencedAssemblyMatrixGuaranteedResults()) {
            return activeResult.copy();
        }
        SequencedAssemblyRecipe recipe = activeRecipe != null
                ? activeRecipe
                : SequencedAssemblyResolver.resolveFor(level, recipeIds.get(activeResult.getItem()), activeResult);
        return recipe == null ? activeResult.copy() : roll(recipe, level.getRandom());
    }

    private static ItemStack roll(SequencedAssemblyRecipe recipe, RandomSource random) {
        float total = 0;
        for (ProcessingOutput entry : recipe.resultPool) {
            total += entry.getChance();
        }
        if (total <= 0) {
            return ItemStack.EMPTY;
        }
        float number = random.nextFloat() * total;
        for (ProcessingOutput entry : recipe.resultPool) {
            number -= entry.getChance();
            if (number < 0) {
                return entry.getStack().copy();
            }
        }
        return ItemStack.EMPTY;
    }

    private boolean beginCraft(Level level) {
        for (Item wanted : List.copyOf(owed.keySet())) {
            if (owed.getOrDefault(wanted, 0L) <= 0) {
                continue;
            }
            MatrixJob job = jobs.get(wanted);
            if (job != null) {
                if (claim(job.outputStack(), job.demand(), null, job)) {
                    return true;
                }
                continue;
            }
            SequencedAssemblyRecipe recipe =
                    SequencedAssemblyResolver.resolveFor(level, recipeIds.get(wanted), new ItemStack(wanted));
            if (recipe != null
                    && claim(
                            recipe.getResultItem(level.registryAccess()),
                            MatrixDemand.of(SequencedAssemblyResolver.demandOf(recipe)),
                            recipe,
                            null)) {
                return true;
            }
        }
        return false;
    }

    private boolean claim(
            ItemStack result, MatrixDemand demand, @Nullable SequencedAssemblyRecipe recipe, @Nullable MatrixJob job) {
        if (result.isEmpty()) {
            return false;
        }
        Plan plan = planConsumption(demand);
        if (plan == null || !fitsInOutput(produce(result, job))) {
            return false;
        }
        if (demand.energy() > 0) {
            if (energy.stored() < demand.energy()) {
                pendingEnergyCost = demand.energy();
                setChanged();
                return false;
            }
            energy.spend(demand.energy());
        }
        pendingEnergyCost = 0;
        claimedItems.clear();
        claimedFluids.clear();
        for (int[] take : plan.itemTakes()) {
            ItemStack taken = inputBuffer.extractItem(take[0], take[1], false);
            if (!taken.isEmpty()) {
                claimedItems.add(taken);
            }
        }
        for (int[] take : plan.fluidTakes()) {
            FluidStack held = fluids.getFluidInTank(take[0]);
            if (!held.isEmpty()) {
                claimedFluids.add(held.copyWithAmount(take[1]));
            }
            fluids.takeFrom(take[0], take[1]);
        }
        activeResult = result.copy();
        rolledResult = ItemStack.EMPTY;
        activeRecipe = recipe;
        activeJob = job;
        progress = 0;
        setChanged();
        if (NepConfig.debugLogging()) {
            Nep.LOGGER.info(
                    "SA matrix {} started assembling {} x{} at {} SU",
                    getBlockPos(),
                    activeResult.getItem(),
                    activeResult.getCount(),
                    stressDrawAt(Math.abs(getSpeed())));
        }
        return true;
    }

    private boolean fitsInOutput(List<ItemStack> results) {
        ItemStackHandler probe = new OverstackedItemHandler(outputBuffer.getSlots());
        for (int slot = 0; slot < outputBuffer.getSlots(); slot++) {
            probe.setStackInSlot(slot, outputBuffer.getStackInSlot(slot).copy());
        }
        for (ItemStack result : results) {
            if (!ItemHandlerHelper.insertItem(probe, result.copy(), false).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static List<ItemStack> produce(ItemStack result, @Nullable MatrixJob job) {
        if (job == null) {
            return List.of(result);
        }
        List<ItemStack> stacks = new ArrayList<>();
        stacks.add(result);
        stacks.addAll(job.retainedStacks());
        return stacks;
    }

    @Nullable
    private Plan planConsumption(MatrixDemand demand) {
        int[] slotLeft = new int[inputBuffer.getSlots()];
        for (int slot = 0; slot < slotLeft.length; slot++) {
            slotLeft[slot] = inputBuffer.getStackInSlot(slot).getCount();
        }
        List<int[]> itemTakes = new ArrayList<>();
        for (MatrixDemand.ItemNeed entry : demand.items()) {
            int need = entry.count();
            for (int slot = 0; slot < slotLeft.length && need > 0; slot++) {
                if (slotLeft[slot] <= 0 || !entry.matches().test(inputBuffer.getStackInSlot(slot))) {
                    continue;
                }
                int take = Math.min(need, slotLeft[slot]);
                slotLeft[slot] -= take;
                need -= take;
                itemTakes.add(new int[] {slot, take});
            }
            if (need > 0) {
                return null;
            }
        }

        int[] tankLeft = new int[AssemblyFluidBuffer.TANKS];
        for (int tank = 0; tank < tankLeft.length; tank++) {
            tankLeft[tank] = fluids.getFluidInTank(tank).getAmount();
        }
        List<int[]> fluidTakes = new ArrayList<>();
        for (MatrixDemand.FluidNeed entry : demand.fluids()) {
            int need = entry.amount();
            for (int tank = 0; tank < tankLeft.length && need > 0; tank++) {
                if (tankLeft[tank] <= 0 || !entry.matches().test(fluids.getFluidInTank(tank))) {
                    continue;
                }
                int take = Math.min(need, tankLeft[tank]);
                tankLeft[tank] -= take;
                need -= take;
                fluidTakes.add(new int[] {tank, take});
            }
            if (need > 0) {
                return null;
            }
        }
        return new Plan(itemTakes, fluidTakes);
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
                if (NepConfig.debugLogging()) {
                    Nep.LOGGER.info(
                            "SA matrix {} returned {} x{} to the network via {}",
                            getBlockPos(),
                            stack.getItem(),
                            moved,
                            returnDirections.directionFor(stack.getItem()));
                }
                outputBuffer.extractItem(slot, moved, false);
                decrement(toReturn, stack.getItem(), moved);
                forgetReturnIfSettled(stack.getItem());
            }
        }
    }

    private void forgetReturnIfSettled(Item item) {
        if (!toReturn.containsKey(item) && !owed.containsKey(item)) {
            returnDirections.forget(item);
        }
    }

    void clearPending() {
        if (owed.isEmpty() && templates.isEmpty()) {
            return;
        }
        int cancelled = pushingCpus.cancelJobsFor(power.grid(), Set.copyOf(templates.keySet()));
        if (NepConfig.debugLogging()) {
            Nep.LOGGER.info(
                    "SA matrix {} cleared {} pending output(s) and cancelled {} network job(s)",
                    getBlockPos(),
                    owed.size(),
                    cancelled);
        }
        owed.clear();
        templates.clear();
        jobs.clear();
        recipeIds.clear();
        missingInputs.clear();
        pushingCpus.clear();
        power.cancelRequests();
        markScanNeeded();
        setChanged();
    }

    boolean hasPending() {
        return !owed.isEmpty();
    }

    void clearBufferTo(Player player) {
        emptyHandlerTo(player, inputBuffer);
        for (int slot = 0; slot < outputBuffer.getSlots(); slot++) {
            ItemStack taken = outputBuffer.extractItem(slot, Integer.MAX_VALUE, false);
            if (taken.isEmpty()) {
                continue;
            }
            decrement(toReturn, taken.getItem(), taken.getCount());
            forgetReturnIfSettled(taken.getItem());
            player.getInventory().placeItemBackInInventory(taken);
        }
        for (int tank = 0; tank < AssemblyFluidBuffer.TANKS; tank++) {
            FluidStack held = fluids.getFluidInTank(tank);
            if (held.isEmpty()) {
                continue;
            }
            AEFluidKey key = AEFluidKey.of(held);
            long dumped = key == null ? 0 : power.dumpToNetwork(key, held.getAmount());
            if (dumped > 0) {
                fluids.takeFrom(tank, (int) dumped);
            }
        }
        markScanNeeded();
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

    private void autoRequest(Level level) {
        if (templates.isEmpty()) {
            setMissingInputs(List.of());
            return;
        }
        for (Item output : owedInOrder()) {
            Template template = templates.get(output);
            if (template == null || template.keys().isEmpty()) {
                continue;
            }
            long attempts = attemptsFor(level, output);
            if (attempts <= 0) {
                continue;
            }
            List<Long> targets = new ArrayList<>(template.counts().size());
            boolean shortfall = false;
            for (int i = 0; i < template.keys().size(); i++) {
                long target = template.counts().get(i) * attempts;
                targets.add(target);
                shortfall |= target > bufferedAmount(template.keys().get(i));
            }
            if (!shortfall) {
                continue;
            }
            if (!NepConfig.createSequencedAssemblyMatrixAutoRequest()) {
                setMissingInputs(shortfallOf(template, targets));
                return;
            }
            if (NepConfig.debugLogging()) {
                Nep.LOGGER.info(
                        "SA matrix {} auto-request for {}: owed={}, attempts={}, template entries={}",
                        getBlockPos(),
                        output,
                        owed.getOrDefault(output, 0L),
                        attempts,
                        template.keys().size());
            }
            setMissingInputs(power.restock(level, template.keys(), targets));
            return;
        }
        setMissingInputs(List.of());
    }

    private List<GenericStack> shortfallOf(Template template, List<Long> targets) {
        List<GenericStack> shortfall = new ArrayList<>();
        for (int i = 0; i < template.keys().size(); i++) {
            AEKey key = template.keys().get(i);
            long missing = targets.get(i) - bufferedAmount(key);
            if (missing > 0) {
                shortfall.add(new GenericStack(key, missing));
            }
        }
        return shortfall;
    }

    private void setMissingInputs(List<GenericStack> missing) {
        if (missingInputs.equals(missing)) {
            return;
        }
        missingInputs.clear();
        missingInputs.addAll(missing);
        setChanged();
    }

    List<GenericStack> missingInputs() {
        return List.copyOf(missingInputs);
    }

    int manualDemandFor(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        AEItemKey key = AEItemKey.of(stack);
        if (key == null) {
            return 0;
        }
        Level level = getLevel();
        return level == null || level.isClientSide ? reportedDemandFor(key) : liveDemandFor(level, key);
    }

    private int reportedDemandFor(AEKey key) {
        for (GenericStack missing : missingInputs) {
            if (key.equals(missing.what())) {
                return (int) Math.min(Integer.MAX_VALUE, missing.amount());
            }
        }
        return 0;
    }

    private int liveDemandFor(Level level, AEKey key) {
        long target = 0;
        for (Item output : owed.keySet()) {
            Template template = templates.get(output);
            if (template == null) {
                continue;
            }
            long attempts = attemptsFor(level, output);
            if (attempts <= 0) {
                continue;
            }
            for (int i = 0; i < template.keys().size(); i++) {
                if (key.equals(template.keys().get(i))) {
                    target += template.counts().get(i) * attempts;
                }
            }
        }
        return (int) Math.max(0, Math.min(Integer.MAX_VALUE, target - bufferedAmount(key)));
    }

    private long attemptsFor(Level level, Item output) {
        long owe = owed.getOrDefault(output, 0L);
        if (owe <= 0) {
            return 0;
        }
        long per = perCraft(level, output);
        long attempts = (owe + per - 1) / per;
        if (!activeResult.isEmpty() && activeResult.getItem() == output) {
            attempts--;
        }
        return attempts;
    }

    private long perCraft(Level level, Item output) {
        MatrixJob job = jobs.get(output);
        if (job != null) {
            return Math.max(1, job.outputCount());
        }
        SequencedAssemblyRecipe recipe =
                SequencedAssemblyResolver.resolveFor(level, recipeIds.get(output), new ItemStack(output));
        return recipe == null
                ? 1
                : Math.max(1, recipe.getResultItem(level.registryAccess()).getCount());
    }

    private List<Item> owedInOrder() {
        List<Item> outputs = new ArrayList<>(owed.keySet());
        outputs.sort(Comparator.comparing(BuiltInRegistries.ITEM::getKey));
        return outputs;
    }

    private void captureTemplate(Item output, Map<AEItemKey, Long> items, Map<AEFluidKey, Long> patternFluids) {
        List<AEKey> keys = new ArrayList<>();
        List<Long> counts = new ArrayList<>();
        for (Map.Entry<AEItemKey, Long> entry : items.entrySet()) {
            keys.add(entry.getKey());
            counts.add(entry.getValue());
        }
        for (Map.Entry<AEFluidKey, Long> entry : patternFluids.entrySet()) {
            keys.add(entry.getKey());
            counts.add(entry.getValue());
        }
        templates.put(output, new Template(List.copyOf(keys), List.copyOf(counts)));
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
        } else if (key instanceof AEFluidKey fluidKey) {
            for (int tank = 0; tank < AssemblyFluidBuffer.TANKS; tank++) {
                FluidStack held = fluids.getFluidInTank(tank);
                if (fluidKey.matches(held)) {
                    total += held.getAmount();
                }
            }
        }
        return total;
    }

    @Override
    public long acceptCrafted(AEKey what, long amount, Actionable mode) {
        boolean simulate = mode == Actionable.SIMULATE;
        if (what instanceof AEItemKey itemKey) {
            long leftover = insertCraftedItem(itemKey, amount, simulate);
            if (!simulate && leftover < amount) {
                markScanNeeded();
                setChanged();
            }
            return leftover;
        }
        if (what instanceof AEFluidKey fluidKey) {
            FluidStack stack = fluidKey.toStack((int) Math.min(amount, Integer.MAX_VALUE));
            int filled = fluids.fill(
                    stack, simulate ? IFluidHandler.FluidAction.SIMULATE : IFluidHandler.FluidAction.EXECUTE);
            if (!simulate && filled > 0) {
                markScanNeeded();
                setChanged();
            }
            return amount - filled;
        }
        return amount;
    }

    private long insertCraftedItem(AEItemKey key, long amount, boolean simulate) {
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
        return remaining;
    }

    boolean readyForPatterns() {
        return NepConfig.createSequencedAssemblyMatrix();
    }

    boolean pushMatrixPattern(IPatternDetails pattern, KeyCounter[] inputs, Direction ejectionDirection) {
        Level level = getLevel();
        if (level == null || level.isClientSide || !NepConfig.createSequencedAssemblyMatrix()) {
            return false;
        }
        List<GenericStack> outputs = pattern.getOutputs();
        if (outputs.isEmpty() || !(outputs.get(0).what() instanceof AEItemKey outputKey)) {
            return reject("pattern has no item output");
        }
        if (!outputKey.toStack().getComponentsPatch().isEmpty()) {
            return reject("pattern output carries data components");
        }
        int outputCount = (int) Math.max(1, Math.min(outputs.get(0).amount(), Integer.MAX_VALUE));
        MatrixJob job = null;
        ResourceLocation assemblyRecipe = null;
        if (pattern instanceof SequencedAssemblyPattern assembly) {
            if (SequencedAssemblyResolver.resolveById(level, assembly.recipe()) == null) {
                return reject("no sequenced assembly recipe with id " + assembly.recipe());
            }
            assemblyRecipe = assembly.recipe();
        } else {
            MatrixJobs.Outcome outcome = MatrixJobs.resolve(pattern, level, outputKey, outputCount);
            if (outcome.job() == null) {
                return reject(outcome.reason());
            }
            job = outcome.job();
        }
        Item producedItem = outputKey.getItem();
        if (owed.containsKey(producedItem)
                && (!Objects.equals(jobs.get(producedItem), job)
                        || !Objects.equals(recipeIds.get(producedItem), assemblyRecipe))) {
            return reject("already assembling " + producedItem + " by a recipe of another kind");
        }
        Map<AEItemKey, Long> items = new HashMap<>();
        Map<AEFluidKey, Long> patternFluids = new HashMap<>();
        if (!DepotMachines.collectInputs(inputs, items, patternFluids)) {
            return reject("unreadable pattern inputs");
        }
        if (items.size() + patternFluids.size() > MatrixGridNode.TRACKER_SIZE) {
            return reject("pattern needs more than " + MatrixGridNode.TRACKER_SIZE + " distinct inputs");
        }
        if (!bufferAll(items, patternFluids)) {
            return reject("matrix buffer is full");
        }
        returnDirections.record(producedItem, ejectionDirection);
        if (assemblyRecipe != null) {
            recipeIds.put(producedItem, assemblyRecipe);
        }
        if (job != null) {
            jobs.put(producedItem, job);
            for (ItemStack stack : job.retainedStacks()) {
                returnDirections.record(stack.getItem(), ejectionDirection);
            }
        }
        captureTemplate(producedItem, items, patternFluids);
        owed.merge(producedItem, (long) outputCount, Long::sum);
        pushingCpus.record();
        markScanNeeded();
        setChanged();
        if (NepConfig.debugLogging()) {
            Nep.LOGGER.info(
                    "SA matrix {} accepted a {} pattern for {}",
                    getBlockPos(),
                    job == null ? "sequenced assembly" : job.kind(),
                    outputKey);
        }
        return true;
    }

    private boolean bufferAll(Map<AEItemKey, Long> items, Map<AEFluidKey, Long> patternFluids) {
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
            ItemHandlerHelper.insertItem(inputBuffer, stack, false);
        }
        for (FluidStack stack : fluidStacks) {
            fluids.fill(stack, IFluidHandler.FluidAction.EXECUTE);
        }
        return true;
    }

    private boolean reject(String reason) {
        if (NepConfig.debugLogging()) {
            Nep.LOGGER.info("SA matrix {} rejected pattern: {}", getBlockPos(), reason);
        }
        return false;
    }

    private static void decrement(Map<Item, Long> map, Item item, long amount) {
        long remaining = map.getOrDefault(item, 0L) - amount;
        if (remaining > 0) {
            map.put(item, remaining);
        } else {
            map.remove(item);
        }
    }

    void markScanNeeded() {
        scanNeeded = true;
    }

    @Override
    public void onGridStateChanged() {
        markScanNeeded();
        setChanged();
    }

    @Override
    public void onSpeedChanged(float previousSpeed) {
        super.onSpeedChanged(previousSpeed);
        markScanNeeded();
        republishStress();
    }

    private void republishStress() {
        Level level = getLevel();
        if (level == null || level.isClientSide || !hasNetwork()) {
            return;
        }
        float previousImpact = lastStressApplied;
        float impact = calculateStressApplied();
        if (impact != previousImpact) {
            getOrCreateNetwork().updateStressFor(this, impact);
        }
    }

    IInWorldGridNodeHost gridNodeHost() {
        return power;
    }

    IItemHandler itemHandlerForSide() {
        return machineView;
    }

    IFluidHandler fluidHandler() {
        return fluids;
    }

    IItemHandler getInputBuffer() {
        return inputBuffer;
    }

    IItemHandler getOutputBuffer() {
        return outputBuffer;
    }

    AssemblyFluidBuffer getFluids() {
        return fluids;
    }

    List<ItemStack> makingNow() {
        List<ItemStack> making = new ArrayList<>();
        for (Map.Entry<Item, Long> entry : owed.entrySet()) {
            making.add(new ItemStack(entry.getKey(), (int) Math.min(entry.getValue(), Integer.MAX_VALUE)));
        }
        making.sort((a, b) -> Integer.compare(b.getCount(), a.getCount()));
        if (making.isEmpty() && !activeResult.isEmpty()) {
            making.add(activeResult.copy());
        }
        return making;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);

        List<ItemStack> making = makingNow();
        int flags = statusFlags();
        tooltip.add(Component.translatable("tooltip.nep.sequenced_assembly_matrix.goggles")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(indented(MatrixReadout.status(flags, making.isEmpty())
                .copy()
                .withStyle(MatrixReadout.faulted(flags) ? ChatFormatting.RED : ChatFormatting.WHITE)));

        float progress = craftProgress();
        if (progress > 0.0F) {
            tooltip.add(indented(Component.translatable(
                            "tooltip.nep.sequenced_assembly_matrix.progress", MatrixReadout.percent(progress))
                    .withStyle(ChatFormatting.WHITE)));
        }
        if (!making.isEmpty()) {
            ItemStack first = making.get(0);
            MutableComponent crafting = first.getHoverName().copy().withStyle(ChatFormatting.WHITE);
            if (first.getCount() > 1) {
                crafting.append(Component.literal(" ")
                        .append(Component.translatable("gui.nep.sequenced_assembly_matrix.count", first.getCount()))
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
            tooltip.add(indented(Component.translatable("gui.nep.sequenced_assembly_matrix.making")
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(" "))
                    .append(crafting)));
        }
        tooltip.add(indented(MatrixReadout.meDrain(meDrain()).copy().withStyle(ChatFormatting.AQUA)));
        if (NEW_AGE_LOADED) {
            tooltip.add(indented(MatrixReadout.energy(energyStored(), energyCapacity())
                    .copy()
                    .withStyle(ChatFormatting.AQUA)));
        }
        return true;
    }

    long energyStored() {
        Level level = getLevel();
        return level != null && level.isClientSide ? clientEnergy : energy.stored();
    }

    long energyCapacity() {
        Level level = getLevel();
        return level != null && level.isClientSide
                ? Math.max(NepConfig.createSequencedAssemblyMatrixEnergyCapacity(), clientEnergyPending)
                : energy.capacity();
    }

    long energyPending() {
        Level level = getLevel();
        return level != null && level.isClientSide ? clientEnergyPending : pendingEnergyCost;
    }

    MatrixEnergyBuffer energyStorage() {
        return energy;
    }

    private static Component indented(Component line) {
        return Component.literal("    ").append(line);
    }

    int statusFlags() {
        Level level = getLevel();
        if (level != null && level.isClientSide) {
            return clientFlags;
        }
        int flags = 0;
        if (power.hasUsablePower()) {
            flags |= FLAG_POWERED;
        }
        if (power.missingChannel()) {
            flags |= FLAG_NO_CHANNEL;
        }
        if (getSpeed() != 0) {
            flags |= FLAG_ROTATING;
        }
        if (isOverStressed()) {
            flags |= FLAG_OVERSTRESSED;
        }
        if (stressDrawAt(Math.abs(getSpeed())) > 0) {
            flags |= FLAG_FAST_ENOUGH;
        }
        if (activeResult.isEmpty() && starveGrace <= 0 && !missingInputs.isEmpty()) {
            flags |= FLAG_STARVED;
        }
        if (outputBlocked) {
            flags |= FLAG_OUTPUT_BLOCKED;
        }
        if (activeResult.isEmpty() && pendingEnergyCost > 0 && energy.stored() < pendingEnergyCost) {
            flags |= FLAG_NO_ENERGY;
        }
        return flags;
    }

    void dropBuffers(Level level, BlockPos pos) {
        dropHandler(level, pos, inputBuffer);
        dropHandler(level, pos, outputBuffer);
        if (!rolledResult.isEmpty()) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), rolledResult.copy());
            if (activeJob != null) {
                for (ItemStack stack : activeJob.retainedStacks()) {
                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack.copy());
                }
            }
        } else {
            for (ItemStack stack : claimedItems) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack.copy());
            }
            for (FluidStack stack : claimedFluids) {
                fluids.fill(stack, IFluidHandler.FluidAction.EXECUTE);
            }
        }
        activeResult = ItemStack.EMPTY;
        rolledResult = ItemStack.EMPTY;
        claimedItems.clear();
        claimedFluids.clear();
        for (int tank = 0; tank < AssemblyFluidBuffer.TANKS; tank++) {
            FluidStack held = fluids.getFluidInTank(tank);
            if (held.isEmpty()) {
                continue;
            }
            AEFluidKey key = AEFluidKey.of(held);
            long dumped = key == null ? 0 : power.dumpToNetwork(key, held.getAmount());
            if (dumped > 0) {
                fluids.takeFrom(tank, (int) dumped);
            }
        }
        clearContent();
    }

    @Override
    public void clearContent() {
        clearHandler(inputBuffer);
        clearHandler(outputBuffer);
        fluids.clear();
        claimedItems.clear();
        claimedFluids.clear();
        missingInputs.clear();
        owed.clear();
        jobs.clear();
        recipeIds.clear();
        toReturn.clear();
        rolledResult = ItemStack.EMPTY;
        activeResult = ItemStack.EMPTY;
        activeJob = null;
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

    private void syncIfChanged() {
        int signature = statusFlags();
        signature = signature * 31 + stressDrawAt(Math.abs(getSpeed()));
        signature = signature * 31 + (int) (energy.stored() * 16 / Math.max(1, energy.capacity()));
        signature = signature * 31 + Long.hashCode(pendingEnergyCost);
        signature = signature * 31 + (int) (craftProgress() * PROGRESS_SYNC_STEPS);
        signature = signature * 31 + activeResult.getItem().hashCode();
        for (Map.Entry<Item, Long> entry : owed.entrySet()) {
            signature = signature * 31 + entry.getKey().hashCode();
            signature = signature * 31 + Long.hashCode(entry.getValue());
        }
        for (GenericStack stack : missingInputs) {
            signature = signature * 31 + stack.what().hashCode();
            signature = signature * 31 + Long.hashCode(stack.amount());
        }
        for (int tank = 0; tank < AssemblyFluidBuffer.TANKS; tank++) {
            FluidStack held = fluids.getFluidInTank(tank);
            signature = signature * 31 + held.getFluid().hashCode();
            signature = signature * 31 + held.getAmount();
        }
        if (signature != syncedSignature) {
            syncedSignature = signature;
            sendData();
        }
    }

    @Override
    public void remove() {
        super.remove();
        power.destroy();
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        power.destroy();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.nep.sequenced_assembly_matrix");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory inventory, Player player) {
        return new SequencedAssemblyMatrixMenu(windowId, inventory, getBlockPos());
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.put(FLUIDS_KEY, fluids.save(registries));
        tag.put(OWED_KEY, ItemCounts.save(owed));
        if (!activeResult.isEmpty()) {
            tag.put(ACTIVE_KEY, activeResult.save(registries));
        }
        if (!rolledResult.isEmpty()) {
            tag.put(ROLLED_KEY, rolledResult.save(registries));
        }
        if (!missingInputs.isEmpty()) {
            ListTag missingList = new ListTag();
            for (GenericStack stack : missingInputs) {
                missingList.add(GenericStack.writeTag(registries, stack));
            }
            tag.put(MISSING_KEY, missingList);
        }
        if (clientPacket) {
            tag.putInt(FLAGS_KEY, statusFlags());
            tag.putInt(STRESS_KEY, stressDrawAt(Math.abs(getSpeed())));
            tag.putFloat(PROGRESS_KEY, craftProgress());
            tag.putLong(ENERGY_STORED_KEY, energy.stored());
            tag.putLong(ENERGY_PENDING_KEY, pendingEnergyCost);
            return;
        }
        tag.putLong(PROGRESS_KEY, progress);
        tag.put(ENERGY_KEY, energy.save());
        tag.putLong(ENERGY_PENDING_KEY, pendingEnergyCost);
        if (!claimedItems.isEmpty()) {
            ListTag claimedList = new ListTag();
            for (ItemStack stack : claimedItems) {
                claimedList.add(stack.save(registries));
            }
            tag.put(CLAIMED_ITEMS_KEY, claimedList);
        }
        if (!claimedFluids.isEmpty()) {
            ListTag claimedList = new ListTag();
            for (FluidStack stack : claimedFluids) {
                claimedList.add(stack.save(registries));
            }
            tag.put(CLAIMED_FLUIDS_KEY, claimedList);
        }
        tag.put(INPUT_KEY, inputBuffer.serializeNBT(registries));
        tag.put(OUTPUT_KEY, outputBuffer.serializeNBT(registries));
        tag.put(RETURN_KEY, ItemCounts.save(toReturn));
        if (!returnDirections.isEmpty()) {
            tag.put(RETURN_DIRS_KEY, returnDirections.save());
        }
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
        if (!jobs.isEmpty()) {
            ListTag jobList = new ListTag();
            for (Map.Entry<Item, MatrixJob> entry : jobs.entrySet()) {
                CompoundTag jobTag = entry.getValue().save(registries);
                jobTag.putString(
                        "Output", BuiltInRegistries.ITEM.getKey(entry.getKey()).toString());
                jobList.add(jobTag);
            }
            tag.put(JOBS_KEY, jobList);
        }
        if (!recipeIds.isEmpty()) {
            tag.put(RECIPE_IDS_KEY, ItemRecipeIds.save(recipeIds));
        }
        pushingCpus.save(tag);
        CompoundTag nodeTag = new CompoundTag();
        power.save(nodeTag);
        tag.put(NODE_KEY, nodeTag);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        if (tag.contains(FLUIDS_KEY, Tag.TAG_LIST)) {
            fluids.load(registries, tag.getList(FLUIDS_KEY, Tag.TAG_COMPOUND));
        }
        ItemCounts.load(owed, tag, OWED_KEY);
        activeResult = tag.contains(ACTIVE_KEY, Tag.TAG_COMPOUND)
                ? ItemStack.parseOptional(registries, tag.getCompound(ACTIVE_KEY))
                : ItemStack.EMPTY;
        rolledResult = tag.contains(ROLLED_KEY, Tag.TAG_COMPOUND)
                ? ItemStack.parseOptional(registries, tag.getCompound(ROLLED_KEY))
                : ItemStack.EMPTY;
        missingInputs.clear();
        if (tag.contains(MISSING_KEY, Tag.TAG_LIST)) {
            ListTag missingList = tag.getList(MISSING_KEY, Tag.TAG_COMPOUND);
            for (int i = 0; i < missingList.size(); i++) {
                GenericStack stack = GenericStack.readTag(registries, missingList.getCompound(i));
                if (stack != null) {
                    missingInputs.add(stack);
                }
            }
        }
        if (clientPacket) {
            clientFlags = tag.getInt(FLAGS_KEY);
            clientStress = tag.getInt(STRESS_KEY);
            clientProgress = tag.getFloat(PROGRESS_KEY);
            clientEnergy = tag.getLong(ENERGY_STORED_KEY);
            clientEnergyPending = tag.getLong(ENERGY_PENDING_KEY);
            return;
        }
        progress = tag.getLong(PROGRESS_KEY);
        pendingEnergyCost = tag.getLong(ENERGY_PENDING_KEY);
        energy.resize(
                Math.max(NepConfig.createSequencedAssemblyMatrixEnergyCapacity(), pendingEnergyCost),
                NepConfig.createSequencedAssemblyMatrixChargeRate());
        energy.load(tag.getCompound(ENERGY_KEY));
        claimedItems.clear();
        if (tag.contains(CLAIMED_ITEMS_KEY, Tag.TAG_LIST)) {
            ListTag claimedList = tag.getList(CLAIMED_ITEMS_KEY, Tag.TAG_COMPOUND);
            for (int i = 0; i < claimedList.size(); i++) {
                ItemStack stack = ItemStack.parseOptional(registries, claimedList.getCompound(i));
                if (!stack.isEmpty()) {
                    claimedItems.add(stack);
                }
            }
        }
        claimedFluids.clear();
        if (tag.contains(CLAIMED_FLUIDS_KEY, Tag.TAG_LIST)) {
            ListTag claimedList = tag.getList(CLAIMED_FLUIDS_KEY, Tag.TAG_COMPOUND);
            for (int i = 0; i < claimedList.size(); i++) {
                FluidStack stack = FluidStack.parseOptional(registries, claimedList.getCompound(i));
                if (!stack.isEmpty()) {
                    claimedFluids.add(stack);
                }
            }
        }
        if (tag.contains(INPUT_KEY)) {
            inputBuffer.deserializeNBT(registries, tag.getCompound(INPUT_KEY));
        }
        if (tag.contains(OUTPUT_KEY)) {
            outputBuffer.deserializeNBT(registries, tag.getCompound(OUTPUT_KEY));
        }
        ItemCounts.load(toReturn, tag, RETURN_KEY);
        returnDirections.load(tag, RETURN_DIRS_KEY, RETURN_DIR_KEY, toReturn.keySet());
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
        jobs.clear();
        if (tag.contains(JOBS_KEY, Tag.TAG_LIST)) {
            ListTag jobList = tag.getList(JOBS_KEY, Tag.TAG_COMPOUND);
            for (int i = 0; i < jobList.size(); i++) {
                CompoundTag jobTag = jobList.getCompound(i);
                Item outputItem = ItemCounts.item(jobTag.getString("Output"));
                MatrixJob job = MatrixJob.load(registries, jobTag);
                if (outputItem != null && job != null) {
                    jobs.put(outputItem, job);
                }
            }
        }
        ItemRecipeIds.load(recipeIds, tag, RECIPE_IDS_KEY);
        activeJob = activeResult.isEmpty() ? null : jobs.get(activeResult.getItem());
        pushingCpus.load(tag);
        if (tag.contains(NODE_KEY)) {
            power.load(tag.getCompound(NODE_KEY));
        }
        markScanNeeded();
    }

    private record Plan(List<int[]> itemTakes, List<int[]> fluidTakes) {}

    private record Template(List<AEKey> keys, List<Long> counts) {}

    private final class OutputView implements IItemHandler {

        @Override
        public int getSlots() {
            return outputBuffer.getSlots();
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            return outputBuffer.getStackInSlot(slot);
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            ItemStack held = outputBuffer.getStackInSlot(slot);
            if (held.isEmpty()) {
                return ItemStack.EMPTY;
            }
            int free = totalOf(held.getItem())
                    - (int) Math.min(toReturn.getOrDefault(held.getItem(), 0L), Integer.MAX_VALUE);
            int allowed = Math.min(Math.min(amount, held.getCount()), free);
            if (allowed <= 0) {
                return ItemStack.EMPTY;
            }
            return outputBuffer.extractItem(slot, allowed, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return outputBuffer.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return false;
        }

        private int totalOf(Item item) {
            int total = 0;
            for (int slot = 0; slot < outputBuffer.getSlots(); slot++) {
                ItemStack stack = outputBuffer.getStackInSlot(slot);
                if (stack.getItem() == item) {
                    total += stack.getCount();
                }
            }
            return total;
        }
    }
}
