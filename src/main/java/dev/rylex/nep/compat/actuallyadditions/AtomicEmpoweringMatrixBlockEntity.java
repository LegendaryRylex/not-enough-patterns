package dev.rylex.nep.compat.actuallyadditions;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import de.ellpeck.actuallyadditions.mod.crafting.EmpowererRecipe;
import de.ellpeck.actuallyadditions.mod.crafting.LaserRecipe;
import dev.rylex.nep.Nep;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.compat.actuallyadditions.ActuallyAdditionsRecipeIngredients.Demand;
import dev.rylex.nep.machine.BufferedMatrixBlockEntity;
import dev.rylex.nep.machine.MachineItemView;
import dev.rylex.nep.machine.ManualCraftHost;
import dev.rylex.nep.machine.ManualCraftOutcome;
import dev.rylex.nep.machine.ManualCraftResult;
import dev.rylex.nep.machine.ManualRequirement;
import dev.rylex.nep.machine.ManualStaging;
import dev.rylex.nep.machine.MatrixEnergyBuffer;
import dev.rylex.nep.machine.MatrixGridNode;
import dev.rylex.nep.machine.MatrixStatus;
import dev.rylex.nep.pattern.AtomicReconstructionPattern;
import dev.rylex.nep.pattern.EmpoweringPattern;
import dev.rylex.nep.pattern.RecipePattern;
import dev.rylex.nep.util.ItemCounts;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AtomicEmpoweringMatrixBlockEntity extends BufferedMatrixBlockEntity implements ManualCraftHost {

    static final int INPUT_SLOTS = 21;
    static final int OUTPUT_SLOTS = 9;

    private static final int RESTOCK_INTERVAL = 10;
    private static final int RUNNING_GRACE_TICKS = 10;
    private static final int ENERGY_STARVED_TICKS = 40;
    private static final int REFUSAL_MEMORY_TICKS = 200;

    private static final String INPUT_KEY = "Input";
    private static final String OUTPUT_KEY = "Output";
    private static final String OWED_KEY = "Owed";
    private static final String RETURN_KEY = "ToReturn";
    private static final String RETURN_DIRS_KEY = "ReturnDirs";
    private static final String TEMPLATE_KEY = "Template";
    private static final String NODE_KEY = "Node";
    private static final String ENERGY_KEY = "Energy";
    private static final String ACTIVE_KEY = "Active";
    private static final String CLAIMED_KEY = "Claimed";
    private static final String PAID_KEY = "Paid";
    private static final String COST_KEY = "Cost";
    private static final String PROGRESS_KEY = "Progress";
    private static final String MISSING_KEY = "Missing";
    private static final String BLOCKED_KEY = "Blocked";
    private static final String POWER_FAULT_KEY = "PowerFault";
    private static final String ENERGY_FAULT_KEY = "EnergyFault";
    private static final String STALL_KEY = "Stall";
    private static final String REFUSAL_KEY = "Refusal";
    private static final String REFUSAL_TICKS_KEY = "RefusalTicks";
    private static final String ACTIVE_KIND_KEY = "ActiveKind";

    private final MatrixEnergyBuffer energy = new MatrixEnergyBuffer(
            NepConfig.actuallyAdditionsMatrixCapacity(), NepConfig.actuallyAdditionsMatrixChargeRate());

    private final Map<AEItemKey, Template> templates = new HashMap<>();
    private final IItemHandler outputView = new OutputView();
    private final IItemHandler machineView =
            MachineItemView.demandLimited(inputBuffer, outputView, this::manualDemandFor);

    private long paid;
    private long craftCost;
    private int progress;
    private Kind activeKind = Kind.EMPOWERING;

    private boolean energyFault;
    private int energyIdleTicks;
    private Stall stall = Stall.NONE;
    private Refusal refusal = Refusal.NONE;
    private int refusalTicks;

    enum Kind {
        EMPOWERING,
        LASER
    }

    enum Stall {
        NONE,
        INGREDIENTS,
        OUTPUT_FULL,
        NO_RECIPE;

        private static Stall worse(Stall first, Stall second) {
            return first.ordinal() >= second.ordinal() ? first : second;
        }
    }

    enum Refusal {
        NONE,
        NOT_AN_ACTUALLY_ADDITIONS_PATTERN,
        NO_ITEM_OUTPUT,
        UNKNOWN_RECIPE,
        MIXED_RECIPES,
        ITEMS_ONLY,
        TOO_MANY_INPUTS,
        BUFFER_FULL
    }

    private record Template(Kind kind, List<AEKey> keys, List<Long> counts, ResourceLocation recipe) {}

    public AtomicEmpoweringMatrixBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(
                type,
                pos,
                state,
                INPUT_SLOTS,
                OUTPUT_SLOTS,
                NepActuallyAdditionsContent.MATRIX_ITEM.get(),
                NepConfig.actuallyAdditionsMatrixIdleMeDrain(),
                NepConfig.actuallyAdditionsMatrixChannels(),
                "Atomic empowering matrix");
    }

    public static void serverTick(
            Level level, BlockPos pos, BlockState state, AtomicEmpoweringMatrixBlockEntity matrix) {
        matrix.tick(level);
    }

    private void tick(Level level) {
        if (runningGrace > 0) {
            runningGrace--;
        }
        if (refusalTicks > 0 && --refusalTicks == 0) {
            refusal = Refusal.NONE;
        }
        flushOutput(level);
        refreshCapacity();
        if (!NepConfig.actuallyAdditionsMatrix()) {
            setPowerFault(false);
            setEnergyFault(false);
            refreshComparator(level);
            refreshVisualState(level);
            syncIfChanged(level);
            return;
        }
        power.create(level, getBlockPos());

        long phase = level.getGameTime() + getBlockPos().hashCode();
        if (Math.floorMod(phase, RESTOCK_INTERVAL) == 0) {
            if (owed.isEmpty()) {
                setMissingInputs(List.of());
            } else {
                autoRequest(level);
            }
        }

        if (owed.isEmpty()) {
            setStall(Stall.NONE);
        }

        if (power.isPowered()) {
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
            } else if (payForThisTick() && drawActivePower()) {
                progress++;
                runningGrace = RUNNING_GRACE_TICKS;
                if (progress >= craftTicks() && paid >= craftCost) {
                    finishCraft(level);
                }
            }
        }
        setPowerFault(!power.hasUsablePower());
        refreshEnergyFault();

        refreshComparator(level);
        refreshVisualState(level);
        syncIfChanged(level);
    }

    @Override
    protected int readoutSignature() {
        int hash = activeResult.isEmpty() ? 0 : activeResult.getItem().hashCode();
        hash = 31 * hash + activeKind.ordinal();
        hash = 31 * hash + Math.round(craftProgress() * 128.0F);
        hash = 31 * hash + Boolean.hashCode(outputBlocked);
        hash = 31 * hash + Boolean.hashCode(powerFault);
        hash = 31 * hash + Boolean.hashCode(energyFault);
        hash = 31 * hash + missingInputs.hashCode();
        hash = 31 * hash + stall.ordinal();
        hash = 31 * hash + refusal.ordinal();
        hash = 31 * hash + energy.syncLevel();
        hash = 31 * hash + Long.hashCode(craftCost);
        hash = 31 * hash + Long.hashCode(pendingJobs());
        return hash;
    }

    private void refreshCapacity() {
        if (energy.resize(NepConfig.actuallyAdditionsMatrixCapacity(), NepConfig.actuallyAdditionsMatrixChargeRate())) {
            setChanged();
        }
    }

    int craftTicks() {
        return activeKind == Kind.EMPOWERING
                ? NepConfig.actuallyAdditionsMatrixEmpoweringCraftTicks()
                : NepConfig.actuallyAdditionsMatrixAtomicReconstructionCraftTicks();
    }

    private boolean payForThisTick() {
        long outstanding = craftCost - paid;
        if (outstanding <= 0) {
            return true;
        }
        int ticks = Math.max(1, craftTicks());
        long instalment = Math.min(outstanding, Math.max(1L, (craftCost + ticks - 1) / ticks));
        if (energy.stored() < instalment
                && NepConfig.actuallyAdditionsMatrixMeCharge()
                && power.chargeBuffer(energy, instalment - energy.stored()) > 0) {
            setChanged();
        }
        if (energy.stored() < instalment) {
            return false;
        }
        paid += energy.spend(instalment);
        energyIdleTicks = 0;
        setChanged();
        return true;
    }

    private boolean drawActivePower() {
        double extra = NepConfig.actuallyAdditionsMatrixMeDrain() - NepConfig.actuallyAdditionsMatrixIdleMeDrain();
        if (extra <= 0) {
            power.clearStarved();
            return true;
        }
        power.extractPower(extra);
        return !power.isStarved();
    }

    private boolean beginCraft(Level level) {
        Stall reason = Stall.NONE;
        for (AEItemKey wanted : List.copyOf(owed.keySet())) {
            if (owed.getOrDefault(wanted, 0L) <= 0) {
                continue;
            }
            Template template = templates.get(wanted);
            if (template == null) {
                reason = Stall.worse(reason, Stall.NO_RECIPE);
                continue;
            }
            Stall attempt = claimIngredients(level, template);
            if (attempt == Stall.NONE) {
                setStall(Stall.NONE);
                return true;
            }
            reason = Stall.worse(reason, attempt);
        }
        setStall(owed.isEmpty() ? Stall.NONE : reason);
        return false;
    }

    private Stall claimIngredients(Level level, Template template) {
        ItemStack result;
        List<Demand> demands;
        long cost;
        if (template.kind() == Kind.EMPOWERING) {
            RecipeHolder<EmpowererRecipe> holder =
                    ActuallyAdditionsRecipeResolver.empoweringById(level, template.recipe());
            if (holder == null) {
                return Stall.NO_RECIPE;
            }
            result = holder.value().getOutput().copy();
            demands = ActuallyAdditionsRecipeIngredients.empoweringDemand(holder.value());
            cost = (long) holder.value().getEnergyPerStand() * EmpowererLayout.STANDS;
        } else {
            RecipeHolder<LaserRecipe> holder = ActuallyAdditionsRecipeResolver.laserById(level, template.recipe());
            if (holder == null) {
                return Stall.NO_RECIPE;
            }
            result = holder.value().getResultItem(level.registryAccess()).copy();
            demands = ActuallyAdditionsRecipeIngredients.laserDemand(holder.value());
            cost = holder.value().getEnergy();
        }
        if (result.isEmpty()) {
            return Stall.NO_RECIPE;
        }
        if (!fitsInOutput(result)) {
            return Stall.OUTPUT_FULL;
        }
        List<int[]> takes = planConsumption(demands);
        if (takes == null) {
            return Stall.INGREDIENTS;
        }

        claimedItems.clear();
        for (int[] take : takes) {
            ItemStack taken = inputBuffer.extractItem(take[0], take[1], false);
            if (!taken.isEmpty()) {
                claimedItems.add(taken);
            }
        }
        activeResult = result;
        activeKind = template.kind();
        craftCost = cost * NepConfig.actuallyAdditionsMatrixEnergyCostPercent() / 100L;
        paid = 0;
        progress = 0;
        setChanged();
        if (NepConfig.debugLogging()) {
            Nep.LOGGER.info(
                    "Atomic empowering matrix {} started {} {} x{} for {} FE",
                    getBlockPos(),
                    template.kind() == Kind.EMPOWERING ? "empowering" : "reconstructing",
                    activeResult.getItem(),
                    activeResult.getCount(),
                    craftCost);
        }
        return Stall.NONE;
    }

    @Nullable
    private List<int[]> planConsumption(List<Demand> demands) {
        int[] slotLeft = new int[inputBuffer.getSlots()];
        for (int slot = 0; slot < slotLeft.length; slot++) {
            slotLeft[slot] = inputBuffer.getStackInSlot(slot).getCount();
        }
        List<int[]> takes = new ArrayList<>();
        for (Demand demand : demands) {
            int need = demand.count();
            for (int slot = 0; slot < slotLeft.length && need > 0; slot++) {
                if (slotLeft[slot] <= 0 || !demand.ingredient().test(inputBuffer.getStackInSlot(slot))) {
                    continue;
                }
                int take = Math.min(need, slotLeft[slot]);
                need -= take;
                slotLeft[slot] -= take;
                takes.add(new int[] {slot, take});
            }
            if (need > 0) {
                return null;
            }
        }
        return takes;
    }

    private void setStall(Stall reason) {
        if (stall != reason) {
            stall = reason;
            setChanged();
        }
    }

    private void refreshEnergyFault() {
        if (energy.capacity() <= 0) {
            energyIdleTicks = 0;
            setEnergyFault(false);
            return;
        }
        if (energy.stored() > 0) {
            energyIdleTicks = 0;
        } else if (energyIdleTicks < ENERGY_STARVED_TICKS) {
            energyIdleTicks++;
        }
        setEnergyFault(energyIdleTicks >= ENERGY_STARVED_TICKS);
    }

    private void setEnergyFault(boolean fault) {
        if (energyFault == fault) {
            return;
        }
        energyFault = fault;
        setChanged();
    }

    private void finishCraft(Level level) {
        ItemStack result = activeResult.copy();
        if (!fitsInOutput(result)) {
            outputBlocked = true;
            return;
        }
        outputBlocked = false;
        ItemHandlerHelper.insertItem(outputBuffer, result.copy(), false);
        AEItemKey produced = AEItemKey.of(result);
        if (produced != null && owed.getOrDefault(produced, 0L) > 0) {
            boolean manual = isManualJob(produced);
            decrement(owed, produced, 1);
            if (manual) {
                decrement(manualOwed, produced, 1);
            }
            if (!owed.containsKey(produced)) {
                templates.remove(produced);
            }
            if (owed.isEmpty()) {
                pushingCpus.clear();
            }
            if (!manual) {
                toReturn.merge(produced, (long) result.getCount(), Long::sum);
            }
        }
        completeCraft();
        flushOutput(level);
        if (NepConfig.debugLogging()) {
            Nep.LOGGER.info(
                    "Atomic empowering matrix {} made {} x{} ({} still owed)",
                    getBlockPos(),
                    produced,
                    result.getCount(),
                    owed.getOrDefault(produced, 0L));
        }
    }

    private void completeCraft() {
        activeResult = ItemStack.EMPTY;
        claimedItems.clear();
        paid = 0;
        craftCost = 0;
        progress = 0;
        outputBlocked = false;
        markScanNeeded();
        setChanged();
    }

    boolean readyForPatterns() {
        return NepConfig.actuallyAdditionsMatrix();
    }

    boolean pushMatrixPattern(IPatternDetails pattern, KeyCounter[] inputs, Direction ejectionDirection) {
        Level level = getLevel();
        if (level == null || level.isClientSide || !NepConfig.actuallyAdditionsMatrix()) {
            return false;
        }
        Kind kind;
        if (pattern instanceof EmpoweringPattern) {
            kind = Kind.EMPOWERING;
        } else if (pattern instanceof AtomicReconstructionPattern) {
            kind = Kind.LASER;
        } else {
            return reject(
                    Refusal.NOT_AN_ACTUALLY_ADDITIONS_PATTERN,
                    "pattern is neither an empowering nor an atomic reconstruction pattern");
        }
        ResourceLocation recipe = ((RecipePattern) pattern).recipe();

        List<GenericStack> outputs = pattern.getOutputs();
        if (outputs.isEmpty() || !(outputs.get(0).what() instanceof AEItemKey outputKey)) {
            return reject(Refusal.NO_ITEM_OUTPUT, "pattern has no item output");
        }

        long batch;
        if (kind == Kind.EMPOWERING) {
            RecipeHolder<EmpowererRecipe> holder = ActuallyAdditionsRecipeResolver.empoweringById(level, recipe);
            if (holder == null) {
                return reject(Refusal.UNKNOWN_RECIPE, "no empowering recipe with id " + recipe);
            }
            ActuallyAdditionsRecipeResolver.EmpoweringPlan plan =
                    ActuallyAdditionsRecipeResolver.planEmpowering(holder, pattern);
            if (plan == null) {
                return reject(Refusal.UNKNOWN_RECIPE, "recipe " + recipe + " does not match the pattern");
            }
            batch = plan.batch();
        } else {
            RecipeHolder<LaserRecipe> holder = ActuallyAdditionsRecipeResolver.laserById(level, recipe);
            if (holder == null) {
                return reject(Refusal.UNKNOWN_RECIPE, "no atomic reconstruction recipe with id " + recipe);
            }
            ActuallyAdditionsRecipeResolver.LaserPlan plan =
                    ActuallyAdditionsRecipeResolver.planLaser(holder, pattern, level);
            if (plan == null) {
                return reject(Refusal.UNKNOWN_RECIPE, "recipe " + recipe + " does not match the pattern");
            }
            batch = plan.batch();
        }

        AEItemKey producedItem = outputKey;
        if (isManualJob(producedItem)) {
            return reject(Refusal.MIXED_RECIPES, "a manual craft of " + producedItem + " is already queued");
        }
        Template existing = templates.get(producedItem);
        if (existing != null && !existing.recipe().equals(recipe)) {
            return reject(
                    Refusal.MIXED_RECIPES,
                    "a craft of " + producedItem + " from " + existing.recipe() + " is already queued");
        }

        Map<AEItemKey, Long> items = new HashMap<>();
        for (KeyCounter counter : inputs) {
            for (var entry : counter) {
                if (!(entry.getKey() instanceof AEItemKey itemKey) || entry.getLongValue() <= 0) {
                    return reject(Refusal.ITEMS_ONLY, "this machine takes items only");
                }
                items.merge(itemKey, entry.getLongValue(), Long::sum);
            }
        }
        if (items.size() > MatrixGridNode.TRACKER_SIZE) {
            return reject(
                    Refusal.TOO_MANY_INPUTS,
                    "pattern needs more than " + MatrixGridNode.TRACKER_SIZE + " distinct inputs");
        }
        if (!bufferAll(items)) {
            return reject(Refusal.BUFFER_FULL, "matrix buffer is full");
        }

        returnDirections.record(producedItem, ejectionDirection);
        captureTemplate(producedItem, kind, items, batch, recipe);
        owed.merge(producedItem, batch, Long::sum);
        pushingCpus.record();
        clearRefusal();
        markScanNeeded();
        setChanged();
        if (NepConfig.debugLogging()) {
            Nep.LOGGER.info("Atomic empowering matrix {} accepted pattern for {} x{}", getBlockPos(), outputKey, batch);
        }
        return true;
    }

    @Override
    public ManualCraftOutcome startManualCraft(Player player, ResourceLocation recipe, int batches) {
        Level level = getLevel();
        if (level == null || level.isClientSide) {
            return ManualCraftOutcome.failed(ManualCraftResult.UNKNOWN_RECIPE);
        }
        if (!NepConfig.manualCrafting() || !NepConfig.actuallyAdditionsMatrix()) {
            return ManualCraftOutcome.failed(ManualCraftResult.DISABLED);
        }

        Kind kind;
        List<ManualRequirement> requirements;
        ItemStack result;
        RecipeHolder<EmpowererRecipe> empowering = ActuallyAdditionsRecipeResolver.empoweringById(level, recipe);
        if (empowering != null) {
            kind = Kind.EMPOWERING;
            requirements = ActuallyAdditionsRecipeIngredients.empoweringRequirements(empowering.value());
            result = empowering.value().getOutput().copy();
        } else {
            RecipeHolder<LaserRecipe> laser = ActuallyAdditionsRecipeResolver.laserById(level, recipe);
            if (laser == null) {
                return ManualCraftOutcome.failed(ManualCraftResult.UNKNOWN_RECIPE);
            }
            kind = Kind.LASER;
            requirements = ActuallyAdditionsRecipeIngredients.laserRequirements(laser.value());
            result = laser.value().getResultItem(level.registryAccess()).copy();
        }
        if (result.isEmpty() || requirements.isEmpty()) {
            return ManualCraftOutcome.failed(ManualCraftResult.UNSUPPORTED);
        }

        AEItemKey produced = AEItemKey.of(result);
        Template existing = templates.get(produced);
        if (existing != null && (existing.kind() != kind || !recipe.equals(existing.recipe()))) {
            return ManualCraftOutcome.failed(ManualCraftResult.BUSY);
        }
        if (owed.containsKey(produced) && !isManualJob(produced)) {
            return ManualCraftOutcome.failed(ManualCraftResult.BUSY);
        }

        ManualStaging.Result staged = pullFromPlayer(player, requirements, batches);
        if (staged.status() != ManualCraftResult.STARTED) {
            return ManualCraftOutcome.failed(staged.status());
        }

        captureTemplate(produced, kind, staged.perCraft(), 1, recipe);
        owed.merge(produced, (long) staged.batches(), Long::sum);
        manualOwed.merge(produced, (long) staged.batches(), Long::sum);
        clearRefusal();
        markScanNeeded();
        setChanged();
        if (NepConfig.debugLogging()) {
            Nep.LOGGER.info(
                    "Atomic empowering matrix {} queued {} manual craft(s) of {} for {}",
                    getBlockPos(),
                    staged.batches(),
                    produced,
                    player.getName().getString());
        }
        return ManualCraftOutcome.started(staged.batches(), result);
    }

    private void captureTemplate(
            AEItemKey output, Kind kind, Map<AEItemKey, Long> items, long batch, ResourceLocation recipe) {
        List<AEKey> keys = new ArrayList<>(items.size());
        List<Long> counts = new ArrayList<>(items.size());
        for (Map.Entry<AEItemKey, Long> entry : items.entrySet()) {
            keys.add(entry.getKey());
            counts.add(entry.getValue() / batch);
        }
        templates.put(output, new Template(kind, List.copyOf(keys), List.copyOf(counts), recipe));
    }

    private void autoRequest(Level level) {
        if (!NepConfig.actuallyAdditionsMatrixAutoRequest()) {
            setMissingInputs(List.of());
            return;
        }
        List<AEKey> keys = new ArrayList<>();
        List<Long> targets = new ArrayList<>();
        for (Map.Entry<AEItemKey, Long> entry : owed.entrySet()) {
            Template template = templates.get(entry.getKey());
            if (template == null || isManualJob(entry.getKey())) {
                continue;
            }
            for (int i = 0; i < template.keys().size(); i++) {
                long target = template.counts().get(i) * entry.getValue();
                int existing = keys.indexOf(template.keys().get(i));
                if (existing >= 0) {
                    targets.set(existing, targets.get(existing) + target);
                } else {
                    keys.add(template.keys().get(i));
                    targets.add(target);
                }
            }
        }
        if (keys.isEmpty()) {
            setMissingInputs(List.of());
            return;
        }
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < keys.size(); i++) {
            order.add(i);
        }
        order.sort(Comparator.comparing(i -> keys.get(i).toString()));
        List<AEKey> stableKeys = new ArrayList<>(keys.size());
        List<Long> stableTargets = new ArrayList<>(targets.size());
        for (int i : order) {
            stableKeys.add(keys.get(i));
            stableTargets.add(targets.get(i));
        }
        setMissingInputs(power.restock(level, stableKeys, stableTargets));
    }

    int manualDemandFor(ItemStack stack) {
        AEItemKey key = AEItemKey.of(stack);
        return key == null ? 0 : reportedDemandFor(key);
    }

    private int reportedDemandFor(AEKey key) {
        long target = 0;
        for (Map.Entry<AEItemKey, Long> entry : owed.entrySet()) {
            Template template = templates.get(entry.getKey());
            if (template == null) {
                continue;
            }
            int index = template.keys().indexOf(key);
            if (index >= 0) {
                target += template.counts().get(index) * entry.getValue();
            }
        }
        return (int) Math.max(0, Math.min(Integer.MAX_VALUE, target - bufferedAmount(key)));
    }

    @Override
    public void onGridStateChanged() {
        markScanNeeded();
        setChanged();
    }

    private boolean reject(Refusal kind, String reason) {
        if (refusal != kind) {
            refusal = kind;
            setChanged();
        }
        refusalTicks = REFUSAL_MEMORY_TICKS;
        if (NepConfig.debugLogging()) {
            Nep.LOGGER.info("Atomic empowering matrix {} rejected pattern: {}", getBlockPos(), reason);
        }
        return false;
    }

    private void clearRefusal() {
        if (refusal != Refusal.NONE) {
            refusal = Refusal.NONE;
            setChanged();
        }
        refusalTicks = 0;
    }

    private void refreshVisualState(Level level) {
        BlockState state = getBlockState();
        if (!state.hasProperty(AtomicEmpoweringMatrixBlock.STATUS)) {
            return;
        }
        MatrixStatus desired = desiredStatus();
        if (state.getValue(AtomicEmpoweringMatrixBlock.STATUS) != desired) {
            level.setBlock(
                    getBlockPos(), state.setValue(AtomicEmpoweringMatrixBlock.STATUS, desired), Block.UPDATE_CLIENTS);
        }
    }

    private MatrixStatus desiredStatus() {
        if (runningGrace > 0) {
            return MatrixStatus.RUNNING;
        }
        if (!activeResult.isEmpty() || stall != Stall.NONE) {
            return MatrixStatus.STALLED;
        }
        return MatrixStatus.IDLE;
    }

    @Override
    public float craftProgress() {
        if (activeResult.isEmpty()) {
            return 0.0F;
        }
        int ticks = craftTicks();
        return Math.min(1.0F, ticks <= 0 ? 1.0F : progress / (float) ticks);
    }

    long craftPaid() {
        return paid;
    }

    long craftCost() {
        return craftCost;
    }

    long storedEnergy() {
        return energy.stored();
    }

    long energyCapacity() {
        return energy.capacity();
    }

    boolean hasEnergyFault() {
        return energyFault;
    }

    long pendingJobs() {
        long total = 0;
        for (long count : owed.values()) {
            total += count;
        }
        return total;
    }

    Stall stall() {
        return stall;
    }

    Refusal refusal() {
        return refusal;
    }

    void clearPending() {
        clearRefusal();
        if (owed.isEmpty() && templates.isEmpty()) {
            return;
        }
        int cancelled = pushingCpus.cancelJobsFor(power.grid(), Set.copyOf(templates.keySet()));
        if (NepConfig.debugLogging()) {
            Nep.LOGGER.info(
                    "Atomic empowering matrix {} cleared {} pending craft(s) and cancelled {} network job(s)",
                    getBlockPos(),
                    owed.size(),
                    cancelled);
        }
        owed.clear();
        manualOwed.clear();
        templates.clear();
        pushingCpus.clear();
        power.cancelRequests();
        setMissingInputs(List.of());
        setStall(Stall.NONE);
        setChanged();
    }

    void clearBufferTo(Player player) {
        emptyHandlerTo(player, inputBuffer);
        emptyHandlerTo(player, outputBuffer);
        setChanged();
    }

    IItemHandler itemHandlerForSide() {
        return machineView;
    }

    MatrixEnergyBuffer energyStorage() {
        return energy;
    }

    void dropBuffers(Level level, BlockPos pos) {
        for (int slot = 0; slot < inputBuffer.getSlots(); slot++) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), inputBuffer.getStackInSlot(slot));
        }
        for (int slot = 0; slot < outputBuffer.getSlots(); slot++) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), outputBuffer.getStackInSlot(slot));
        }
        for (ItemStack stack : claimedItems) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
        }
        clearContent();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put(INPUT_KEY, inputBuffer.serializeNBT(registries));
        tag.put(OUTPUT_KEY, outputBuffer.serializeNBT(registries));
        tag.put(ENERGY_KEY, energy.save());
        tag.put(OWED_KEY, ItemCounts.save(owed, registries));
        tag.put(RETURN_KEY, ItemCounts.save(toReturn, registries));
        tag.put(RETURN_DIRS_KEY, returnDirections.save(registries));
        tag.put(TEMPLATE_KEY, saveTemplates(registries));
        if (!activeResult.isEmpty()) {
            tag.put(ACTIVE_KEY, activeResult.save(registries));
        }
        ListTag claimed = new ListTag();
        for (ItemStack stack : claimedItems) {
            if (!stack.isEmpty()) {
                claimed.add(stack.save(registries));
            }
        }
        tag.put(CLAIMED_KEY, claimed);
        tag.putLong(PAID_KEY, paid);
        tag.putLong(COST_KEY, craftCost);
        tag.putInt(PROGRESS_KEY, progress);
        tag.putBoolean(BLOCKED_KEY, outputBlocked);
        tag.putBoolean(POWER_FAULT_KEY, powerFault);
        tag.putBoolean(ENERGY_FAULT_KEY, energyFault);
        tag.putString(STALL_KEY, stall.name());
        tag.putString(REFUSAL_KEY, refusal.name());
        tag.putInt(REFUSAL_TICKS_KEY, refusalTicks);
        tag.putString(ACTIVE_KIND_KEY, activeKind.name());
        ListTag missing = new ListTag();
        for (GenericStack stack : missingInputs) {
            missing.add(GenericStack.writeTag(registries, stack));
        }
        tag.put(MISSING_KEY, missing);
        pushingCpus.save(tag);
        CompoundTag node = new CompoundTag();
        power.save(node);
        tag.put(NODE_KEY, node);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        loadBuffer(inputBuffer, registries, tag.getCompound(INPUT_KEY));
        loadBuffer(outputBuffer, registries, tag.getCompound(OUTPUT_KEY));
        energy.load(tag.getCompound(ENERGY_KEY));
        ItemCounts.load(owed, tag, OWED_KEY, registries);
        ItemCounts.load(toReturn, tag, RETURN_KEY, registries);
        returnDirections.load(tag, RETURN_DIRS_KEY, "ReturnDir", toReturn.keySet(), registries);
        loadTemplates(tag.getList(TEMPLATE_KEY, Tag.TAG_COMPOUND), registries);
        activeResult = tag.contains(ACTIVE_KEY)
                ? ItemStack.parse(registries, tag.getCompound(ACTIVE_KEY)).orElse(ItemStack.EMPTY)
                : ItemStack.EMPTY;
        claimedItems.clear();
        ListTag claimed = tag.getList(CLAIMED_KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < claimed.size(); i++) {
            ItemStack.parse(registries, claimed.getCompound(i)).ifPresent(claimedItems::add);
        }
        paid = tag.getLong(PAID_KEY);
        craftCost = tag.getLong(COST_KEY);
        progress = tag.getInt(PROGRESS_KEY);
        outputBlocked = tag.getBoolean(BLOCKED_KEY);
        powerFault = tag.getBoolean(POWER_FAULT_KEY);
        energyFault = tag.getBoolean(ENERGY_FAULT_KEY);
        missingInputs.clear();
        ListTag missing = tag.getList(MISSING_KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < missing.size(); i++) {
            GenericStack stack = GenericStack.readTag(registries, missing.getCompound(i));
            if (stack != null) {
                missingInputs.add(stack);
            }
        }
        pushingCpus.load(tag);
        power.load(tag.getCompound(NODE_KEY));
        stall = byName(Stall.values(), tag.getString(STALL_KEY), Stall.NONE);
        refusal = byName(Refusal.values(), tag.getString(REFUSAL_KEY), Refusal.NONE);
        refusalTicks = refusal == Refusal.NONE
                ? 0
                : Math.max(1, Math.min(REFUSAL_MEMORY_TICKS, tag.getInt(REFUSAL_TICKS_KEY)));
        activeKind = byName(Kind.values(), tag.getString(ACTIVE_KIND_KEY), Kind.EMPOWERING);
        markScanNeeded();
    }

    private static <E extends Enum<E>> E byName(E[] values, String name, E fallback) {
        for (E value : values) {
            if (value.name().equals(name)) {
                return value;
            }
        }
        return fallback;
    }

    private ListTag saveTemplates(HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Map.Entry<AEItemKey, Template> entry : templates.entrySet()) {
            CompoundTag tag = new CompoundTag();
            ItemCounts.putKey(tag, entry.getKey(), registries);
            tag.putString("Kind", entry.getValue().kind().name());
            ListTag stacks = new ListTag();
            for (int i = 0; i < entry.getValue().keys().size(); i++) {
                stacks.add(GenericStack.writeTag(
                        registries,
                        new GenericStack(
                                entry.getValue().keys().get(i),
                                entry.getValue().counts().get(i))));
            }
            tag.put("Stacks", stacks);
            tag.putString("Recipe", entry.getValue().recipe().toString());
            list.add(tag);
        }
        return list;
    }

    private void loadTemplates(ListTag list, HolderLookup.Provider registries) {
        templates.clear();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            AEItemKey item = ItemCounts.key(tag, registries);
            ResourceLocation recipe = ResourceLocation.tryParse(tag.getString("Recipe"));
            if (item == null || recipe == null) {
                continue;
            }
            ListTag stacks = tag.getList("Stacks", Tag.TAG_COMPOUND);
            List<AEKey> keys = new ArrayList<>();
            List<Long> counts = new ArrayList<>();
            for (int s = 0; s < stacks.size(); s++) {
                GenericStack stack = GenericStack.readTag(registries, stacks.getCompound(s));
                if (stack != null) {
                    keys.add(stack.what());
                    counts.add(stack.amount());
                }
            }
            if (!keys.isEmpty()) {
                templates.put(
                        item,
                        new Template(
                                byName(Kind.values(), tag.getString("Kind"), Kind.EMPOWERING),
                                List.copyOf(keys),
                                List.copyOf(counts),
                                recipe));
            }
        }
    }

    @Override
    public Component getDisplayName() {
        return NepActuallyAdditionsContent.MATRIX.get().getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory inventory, Player player) {
        return new AtomicEmpoweringMatrixMenu(windowId, inventory, getBlockPos());
    }

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
            ItemStack stack = outputBuffer.getStackInSlot(slot);
            AEItemKey key = AEItemKey.of(stack);
            if (key == null || toReturn.getOrDefault(key, 0L) > 0) {
                return ItemStack.EMPTY;
            }
            return outputBuffer.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return outputBuffer.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return false;
        }
    }
}
