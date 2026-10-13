package dev.rylex.nep.compat.oritech;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import dev.rylex.nep.Nep;
import dev.rylex.nep.NepConfig;
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
import dev.rylex.nep.pattern.ParticleCollisionPattern;
import dev.rylex.nep.util.ItemCounts;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.init.recipes.OritechRecipe;

public class ParticleCollisionMatrixBlockEntity extends BufferedMatrixBlockEntity implements ManualCraftHost {

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
    private static final String SPEED_KEY = "Speed";
    private static final String PROGRESS_KEY = "Progress";
    private static final String MISSING_KEY = "Missing";
    private static final String BLOCKED_KEY = "Blocked";
    private static final String POWER_FAULT_KEY = "PowerFault";
    private static final String ENERGY_FAULT_KEY = "EnergyFault";
    private static final String STALL_KEY = "Stall";
    private static final String REFUSAL_KEY = "Refusal";
    private static final String REFUSAL_TICKS_KEY = "RefusalTicks";

    private final MatrixEnergyBuffer energy =
            new MatrixEnergyBuffer(NepConfig.oritechMatrixCapacity(), NepConfig.oritechMatrixChargeRate());

    private final Map<AEItemKey, Template> templates = new HashMap<>();
    private final ResourceHandler<ItemResource> outputView = new OutputView();
    private final ResourceHandler<ItemResource> machineView =
            MachineItemView.demandLimited(inputBuffer, outputView, this::manualDemandFor);

    private long paid;
    private long craftCost;
    private long craftSpeed;
    private int progress;

    private boolean energyFault;
    private int energyIdleTicks;
    private Stall stall = Stall.NONE;
    private Refusal refusal = Refusal.NONE;
    private int refusalTicks;

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
        NOT_A_COLLISION_PATTERN,
        NO_ITEM_OUTPUT,
        UNKNOWN_RECIPE,
        MIXED_RECIPES,
        ITEMS_ONLY,
        TOO_MANY_INPUTS,
        BUFFER_FULL
    }

    private record Template(List<AEKey> keys, List<Long> counts, Identifier recipe) {}

    public ParticleCollisionMatrixBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(
                type,
                pos,
                state,
                INPUT_SLOTS,
                OUTPUT_SLOTS,
                NepOritechContent.MATRIX_ITEM.get(),
                NepConfig.oritechMatrixIdleMeDrain(),
                NepConfig.oritechMatrixChannels(),
                "Particle collision matrix");
    }

    public static void serverTick(
            Level level, BlockPos pos, BlockState state, ParticleCollisionMatrixBlockEntity matrix) {
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
        if (!NepConfig.oritechParticleCollisionMatrix()) {
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
                progress = Math.min(craftTicks(), progress + 1);
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
        hash = 31 * hash + Math.round(craftProgress() * 128.0F);
        hash = 31 * hash + Boolean.hashCode(outputBlocked);
        hash = 31 * hash + Boolean.hashCode(powerFault);
        hash = 31 * hash + Boolean.hashCode(energyFault);
        hash = 31 * hash + missingInputs.hashCode();
        hash = 31 * hash + stall.ordinal();
        hash = 31 * hash + refusal.ordinal();
        hash = 31 * hash + energy.syncLevel();
        hash = 31 * hash + Long.hashCode(craftCost);
        hash = 31 * hash + Long.hashCode(craftSpeed);
        hash = 31 * hash + Long.hashCode(pendingJobs());
        return hash;
    }

    private void refreshCapacity() {
        if (energy.resize(NepConfig.oritechMatrixCapacity(), NepConfig.oritechMatrixChargeRate())) {
            setChanged();
        }
    }

    int craftTicks() {
        return NepConfig.oritechMatrixCraftTicks();
    }

    private boolean payForThisTick() {
        long outstanding = craftCost - paid;
        if (outstanding <= 0) {
            return true;
        }
        int ticks = Math.max(1, craftTicks());
        long share = Math.max(1L, (craftCost + ticks - 1) / ticks);
        long instalment = Math.min(outstanding, Math.min(share, Math.max(1L, energy.capacity())));
        if (energy.stored() < instalment
                && NepConfig.oritechMatrixMeCharge()
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
        double extra = NepConfig.oritechMatrixMeDrain() - NepConfig.oritechMatrixIdleMeDrain();
        if (extra <= 0) {
            power.clearStarved();
            return true;
        }
        power.extractPower(extra);
        return !power.isStarved();
    }

    static long energyCost(OritechRecipe recipe) {
        return ParticleEnergy.toReach(ParticleEnergy.launchSpeedFor(recipe.time()))
                * NepConfig.oritechMatrixEnergyCostPercent()
                / 100L;
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
        RecipeHolder<OritechRecipe> holder = OritechRecipeResolver.collisionById(level, template.recipe());
        if (holder == null) {
            return Stall.NO_RECIPE;
        }
        ItemStack result = OritechRecipeIngredients.fullResult(holder.value());
        if (result.isEmpty()) {
            return Stall.NO_RECIPE;
        }
        if (!fitsInOutput(result)) {
            return Stall.OUTPUT_FULL;
        }
        List<int[]> takes = planConsumption(holder.value().itemInputs());
        if (takes == null) {
            return Stall.INGREDIENTS;
        }

        claimedItems.clear();
        for (int[] take : takes) {
            ItemStack taken = takeFromInput(take[0], take[1]);
            if (!taken.isEmpty()) {
                claimedItems.add(taken);
            }
        }
        activeResult = result;
        craftSpeed = holder.value().time();
        craftCost = energyCost(holder.value());
        paid = 0;
        progress = 0;
        setChanged();
        if (NepConfig.debugLogging()) {
            Nep.LOGGER.info(
                    "Particle collision matrix {} started {} x{} at speed {} for {} FE",
                    getBlockPos(),
                    activeResult.getItem(),
                    activeResult.getCount(),
                    craftSpeed,
                    craftCost);
        }
        return Stall.NONE;
    }

    private ItemStack takeFromInput(int slot, int amount) {
        ItemResource resource = inputBuffer.getResource(slot);
        try (Transaction tx = Transaction.openRoot()) {
            int taken = inputBuffer.extract(slot, resource, amount, tx);
            if (taken <= 0) {
                return ItemStack.EMPTY;
            }
            tx.commit();
            return resource.toStack(taken);
        }
    }

    @Nullable
    private List<int[]> planConsumption(List<Ingredient> particles) {
        int[] slotLeft = new int[inputBuffer.size()];
        for (int slot = 0; slot < slotLeft.length; slot++) {
            slotLeft[slot] = inputBuffer.getAmountAsInt(slot);
        }
        List<int[]> takes = new ArrayList<>();
        for (Ingredient particle : particles) {
            int found = -1;
            for (int slot = 0; slot < slotLeft.length; slot++) {
                if (slotLeft[slot] > 0 && particle.test(inputBuffer.stackAt(slot))) {
                    found = slot;
                    break;
                }
            }
            if (found < 0) {
                return null;
            }
            slotLeft[found]--;
            takes.add(new int[] {found, 1});
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
        ResourceHandlerUtil.insertStacking(outputBuffer, ItemResource.of(result), result.getCount(), null);
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
                    "Particle collision matrix {} made {} x{} ({} still owed)",
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
        craftSpeed = 0;
        progress = 0;
        outputBlocked = false;
        markScanNeeded();
        setChanged();
    }

    boolean readyForPatterns() {
        return NepConfig.oritechParticleCollisionMatrix();
    }

    boolean pushMatrixPattern(IPatternDetails pattern, KeyCounter[] inputs, Direction ejectionDirection) {
        Level level = getLevel();
        if (level == null || level.isClientSide() || !NepConfig.oritechParticleCollisionMatrix()) {
            return false;
        }
        if (!(pattern instanceof ParticleCollisionPattern collision)) {
            return reject(Refusal.NOT_A_COLLISION_PATTERN, "pattern is not a particle collision pattern");
        }
        Identifier recipe = collision.recipe();
        List<GenericStack> outputs = pattern.getOutputs();
        if (outputs.isEmpty() || !(outputs.get(0).what() instanceof AEItemKey producedItem)) {
            return reject(Refusal.NO_ITEM_OUTPUT, "pattern has no item output");
        }
        RecipeHolder<OritechRecipe> holder = OritechRecipeResolver.collisionById(level, recipe);
        OritechRecipeResolver.CollisionPlan plan =
                holder == null ? null : OritechRecipeResolver.planCollision(holder, pattern, level);
        if (plan == null) {
            return reject(Refusal.UNKNOWN_RECIPE, "no particle collision recipe matches " + recipe);
        }
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
                    return reject(Refusal.ITEMS_ONLY, "particle collisions take items only");
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
        captureTemplate(producedItem, plan.expectedItems(), recipe);
        owed.merge(producedItem, plan.batch(), Long::sum);
        pushingCpus.record();
        clearRefusal();
        markScanNeeded();
        setChanged();
        if (NepConfig.debugLogging()) {
            Nep.LOGGER.info(
                    "Particle collision matrix {} accepted pattern for {} x{}",
                    getBlockPos(),
                    producedItem,
                    plan.batch());
        }
        return true;
    }

    @Override
    public ManualCraftOutcome startManualCraft(Player player, Identifier recipe, int batches) {
        Level level = getLevel();
        if (level == null || level.isClientSide()) {
            return ManualCraftOutcome.failed(ManualCraftResult.UNKNOWN_RECIPE);
        }
        if (!NepConfig.manualCrafting() || !NepConfig.oritechParticleCollisionMatrix()) {
            return ManualCraftOutcome.failed(ManualCraftResult.DISABLED);
        }
        RecipeHolder<OritechRecipe> holder = OritechRecipeResolver.collisionById(level, recipe);
        if (holder == null) {
            return ManualCraftOutcome.failed(ManualCraftResult.UNKNOWN_RECIPE);
        }
        List<ManualRequirement> requirements = OritechRecipeIngredients.manualRequirements(holder.value());
        ItemStack result = OritechRecipeIngredients.fullResult(holder.value());
        if (result.isEmpty() || requirements == null) {
            return ManualCraftOutcome.failed(ManualCraftResult.UNSUPPORTED);
        }

        AEItemKey produced = AEItemKey.of(result);
        Template existing = templates.get(produced);
        if (existing != null && !recipe.equals(existing.recipe())) {
            return ManualCraftOutcome.failed(ManualCraftResult.BUSY);
        }
        if (owed.containsKey(produced) && !isManualJob(produced)) {
            return ManualCraftOutcome.failed(ManualCraftResult.BUSY);
        }

        ManualStaging.Result staged = pullFromPlayer(player, requirements, batches);
        if (staged.status() != ManualCraftResult.STARTED) {
            return ManualCraftOutcome.failed(staged.status());
        }

        captureTemplate(produced, staged.perCraft(), recipe);
        owed.merge(produced, (long) staged.batches(), Long::sum);
        manualOwed.merge(produced, (long) staged.batches(), Long::sum);
        clearRefusal();
        markScanNeeded();
        setChanged();
        if (NepConfig.debugLogging()) {
            Nep.LOGGER.info(
                    "Particle collision matrix {} queued {} manual craft(s) of {} for {}",
                    getBlockPos(),
                    staged.batches(),
                    produced,
                    player.getName().getString());
        }
        return ManualCraftOutcome.started(staged.batches(), result);
    }

    private void captureTemplate(AEItemKey output, Map<AEItemKey, Long> items, Identifier recipe) {
        List<AEKey> keys = new ArrayList<>(items.size());
        List<Long> counts = new ArrayList<>(items.size());
        for (Map.Entry<AEItemKey, Long> entry : items.entrySet()) {
            keys.add(entry.getKey());
            counts.add(entry.getValue());
        }
        templates.put(output, new Template(List.copyOf(keys), List.copyOf(counts), recipe));
    }

    private void autoRequest(Level level) {
        if (!NepConfig.oritechMatrixAutoRequest()) {
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

    int manualDemandFor(ItemResource resource) {
        AEItemKey key = AEItemKey.of(resource);
        if (key == null) {
            return 0;
        }
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
            Nep.LOGGER.info("Particle collision matrix {} rejected pattern: {}", getBlockPos(), reason);
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
        if (!state.hasProperty(ParticleCollisionMatrixBlock.STATUS)) {
            return;
        }
        MatrixStatus desired = desiredStatus();
        if (state.getValue(ParticleCollisionMatrixBlock.STATUS) != desired) {
            level.setBlock(
                    getBlockPos(), state.setValue(ParticleCollisionMatrixBlock.STATUS, desired), Block.UPDATE_CLIENTS);
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
        float time = ticks <= 0 ? 1.0F : progress / (float) ticks;
        float charge = craftCost <= 0 ? 1.0F : paid / (float) craftCost;
        return Math.min(1.0F, Math.min(time, charge));
    }

    long craftPaid() {
        return paid;
    }

    long craftCost() {
        return craftCost;
    }

    long craftSpeed() {
        return craftSpeed;
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
                    "Particle collision matrix {} cleared {} pending craft(s) and cancelled {} network job(s)",
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

    ResourceHandler<ItemResource> itemHandlerForSide() {
        return machineView;
    }

    MatrixEnergyBuffer energyStorage() {
        return energy;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null) {
            dropBuffers(level, pos);
        }
        super.preRemoveSideEffects(pos, state);
    }

    void dropBuffers(Level level, BlockPos pos) {
        for (int slot = 0; slot < inputBuffer.size(); slot++) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), inputBuffer.stackAt(slot));
        }
        for (int slot = 0; slot < outputBuffer.size(); slot++) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), outputBuffer.stackAt(slot));
        }
        for (ItemStack stack : claimedItems) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
        }
        clearContent();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        inputBuffer.serialize(output.child(INPUT_KEY));
        outputBuffer.serialize(output.child(OUTPUT_KEY));
        energy.save(output.child(ENERGY_KEY));
        ItemCounts.save(output, OWED_KEY, owed);
        ItemCounts.save(output, RETURN_KEY, toReturn);
        returnDirections.save(output, RETURN_DIRS_KEY);
        saveTemplates(output);
        if (!activeResult.isEmpty()) {
            output.store(ACTIVE_KEY, ItemStack.CODEC, activeResult);
        }
        ValueOutput.TypedOutputList<ItemStack> claimed = output.list(CLAIMED_KEY, ItemStack.CODEC);
        for (ItemStack stack : claimedItems) {
            if (!stack.isEmpty()) {
                claimed.add(stack);
            }
        }
        output.putLong(PAID_KEY, paid);
        output.putLong(COST_KEY, craftCost);
        output.putLong(SPEED_KEY, craftSpeed);
        output.putInt(PROGRESS_KEY, progress);
        output.putBoolean(BLOCKED_KEY, outputBlocked);
        output.putBoolean(POWER_FAULT_KEY, powerFault);
        output.putBoolean(ENERGY_FAULT_KEY, energyFault);
        output.putString(STALL_KEY, stall.name());
        output.putString(REFUSAL_KEY, refusal.name());
        output.putInt(REFUSAL_TICKS_KEY, refusalTicks);
        ValueOutput.TypedOutputList<GenericStack> missing = output.list(MISSING_KEY, GenericStack.CODEC);
        for (GenericStack stack : missingInputs) {
            missing.add(stack);
        }
        pushingCpus.save(output);
        power.save(output.child(NODE_KEY));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        inputBuffer.deserialize(input.childOrEmpty(INPUT_KEY));
        outputBuffer.deserialize(input.childOrEmpty(OUTPUT_KEY));
        energy.load(input.childOrEmpty(ENERGY_KEY));
        ItemCounts.load(owed, input, OWED_KEY);
        ItemCounts.load(toReturn, input, RETURN_KEY);
        returnDirections.load(input, RETURN_DIRS_KEY);
        loadTemplates(input);
        activeResult = input.read(ACTIVE_KEY, ItemStack.CODEC).orElse(ItemStack.EMPTY);
        claimedItems.clear();
        for (ItemStack stack : input.listOrEmpty(CLAIMED_KEY, ItemStack.CODEC)) {
            claimedItems.add(stack);
        }
        paid = input.getLongOr(PAID_KEY, 0L);
        craftCost = input.getLongOr(COST_KEY, 0L);
        craftSpeed = input.getLongOr(SPEED_KEY, 0L);
        progress = input.getIntOr(PROGRESS_KEY, 0);
        outputBlocked = input.getBooleanOr(BLOCKED_KEY, false);
        powerFault = input.getBooleanOr(POWER_FAULT_KEY, false);
        energyFault = input.getBooleanOr(ENERGY_FAULT_KEY, false);
        missingInputs.clear();
        for (GenericStack stack : input.listOrEmpty(MISSING_KEY, GenericStack.CODEC)) {
            missingInputs.add(stack);
        }
        pushingCpus.load(input);
        power.load(input.childOrEmpty(NODE_KEY));
        stall = byName(Stall.values(), input.getStringOr(STALL_KEY, ""), Stall.NONE);
        refusal = byName(Refusal.values(), input.getStringOr(REFUSAL_KEY, ""), Refusal.NONE);
        refusalTicks = refusal == Refusal.NONE
                ? 0
                : Math.max(1, Math.min(REFUSAL_MEMORY_TICKS, input.getIntOr(REFUSAL_TICKS_KEY, 0)));
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

    private void saveTemplates(ValueOutput output) {
        ValueOutput.ValueOutputList list = output.childrenList(TEMPLATE_KEY);
        for (Map.Entry<AEItemKey, Template> entry : templates.entrySet()) {
            ValueOutput entryOutput = list.addChild();
            ItemCounts.putKey(entryOutput, entry.getKey());
            ValueOutput.TypedOutputList<GenericStack> stacks = entryOutput.list("Stacks", GenericStack.CODEC);
            for (int i = 0; i < entry.getValue().keys().size(); i++) {
                stacks.add(new GenericStack(
                        entry.getValue().keys().get(i),
                        entry.getValue().counts().get(i)));
            }
            entryOutput.putString("Recipe", entry.getValue().recipe().toString());
        }
    }

    private void loadTemplates(ValueInput input) {
        templates.clear();
        for (ValueInput entryInput : input.childrenListOrEmpty(TEMPLATE_KEY)) {
            AEItemKey item = ItemCounts.key(entryInput);
            Identifier recipe =
                    entryInput.getString("Recipe").map(Identifier::tryParse).orElse(null);
            if (item == null || recipe == null) {
                continue;
            }
            List<AEKey> keys = new ArrayList<>();
            List<Long> counts = new ArrayList<>();
            for (GenericStack stack : entryInput.listOrEmpty("Stacks", GenericStack.CODEC)) {
                keys.add(stack.what());
                counts.add(stack.amount());
            }
            if (!keys.isEmpty()) {
                templates.put(item, new Template(List.copyOf(keys), List.copyOf(counts), recipe));
            }
        }
    }

    @Override
    public Component getDisplayName() {
        return NepOritechContent.MATRIX.get().getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory inventory, Player player) {
        return new ParticleCollisionMatrixMenu(windowId, inventory, getBlockPos());
    }

    private final class OutputView implements ResourceHandler<ItemResource> {
        @Override
        public int size() {
            return outputBuffer.size();
        }

        @Override
        public ItemResource getResource(int index) {
            return outputBuffer.getResource(index);
        }

        @Override
        public long getAmountAsLong(int index) {
            return outputBuffer.getAmountAsLong(index);
        }

        @Override
        public long getCapacityAsLong(int index, ItemResource resource) {
            return outputBuffer.getCapacityAsLong(index, resource);
        }

        @Override
        public boolean isValid(int index, ItemResource resource) {
            return false;
        }

        @Override
        public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
            return 0;
        }

        @Override
        public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
            if (resource.isEmpty() || toReturn.getOrDefault(AEItemKey.of(resource), 0L) > 0) {
                return 0;
            }
            return outputBuffer.extract(index, resource, amount, transaction);
        }
    }
}
