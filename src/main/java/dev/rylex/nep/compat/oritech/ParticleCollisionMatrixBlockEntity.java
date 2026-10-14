package dev.rylex.nep.compat.oritech;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import dev.rylex.nep.Nep;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.machine.BufferedMatrixBlockEntity;
import dev.rylex.nep.machine.DemandTally;
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
import dev.rylex.nep.util.EnumNames;
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
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.api.energy.containers.DynamicEnergyStorage;
import rearth.oritech.api.item.ItemApi;
import rearth.oritech.init.OritechConfig;
import rearth.oritech.init.recipes.OritechRecipe;
import rearth.oritech.util.MachineAddonController;
import rearth.oritech.util.ScreenProvider;

public class ParticleCollisionMatrixBlockEntity extends BufferedMatrixBlockEntity
        implements ManualCraftHost, MachineAddonController {

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
    private static final String BATCH_KEY = "Batch";
    private static final String ADDONS_KEY = "Addons";
    private static final String ADDON_LIST_KEY = "AddonList";
    private static final String CORE_KEY = "Core";
    private static final String LAYERED_KEY = "LayeredExtenders";
    private static final int ADDON_CHECK_INTERVAL = 20;

    private final MatrixEnergyBuffer energy =
            new MatrixEnergyBuffer(NepConfig.oritechMatrixCapacity(), NepConfig.oritechMatrixChargeRate());

    private final Map<AEItemKey, CollisionTemplate> templates = new HashMap<>();
    private final IItemHandler outputView = new OutputView();
    private final IItemHandler machineView =
            MachineItemView.demandLimited(inputBuffer, outputView, this::manualDemandFor);

    private final MatrixAddonStorage addonStorage = new MatrixAddonStorage(energy);
    private final List<BlockPos> connectedAddons = new ArrayList<>();
    private final List<BlockPos> openAddonSlots = new ArrayList<>();
    private final List<AddonEntry> addonEntries = new ArrayList<>();
    private final ItemStackHandler coreSlot = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return MatrixAddons.isCore(stack);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            coreChanged = level != null && !level.isClientSide;
            addonRevision++;
            setChanged();
        }
    };
    private boolean coreChanged;
    private boolean layeredExtenders;
    private int addonRevision;
    private BaseAddonData addonData = BaseAddonData.DEFAULT_ADDON_DATA;
    private float speedMultiplier = 1.0F;
    private float efficiencyMultiplier = 1.0F;
    private int extraChambers;

    private long paid;
    private long craftCost;
    private long craftSpeed;
    private int progress;
    private int activeBatch = 1;

    private boolean energyFault;
    private int energyIdleTicks;
    private Stall stall = Stall.NONE;
    private Refusal refusal = Refusal.NONE;
    private int refusalTicks;

    enum Stall {
        NONE(0),
        INGREDIENTS(1),
        OUTPUT_FULL(2),
        NO_RECIPE(3);

        private final int severity;

        Stall(int severity) {
            this.severity = severity;
        }

        private static Stall worse(Stall first, Stall second) {
            return first.severity >= second.severity ? first : second;
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
        refreshAddons(level);
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
        hash = 31 * hash + addonData.hashCode();
        hash = 31 * hash + addonEntries.hashCode();
        hash = 31 * hash + coreSlot.getStackInSlot(0).getItem().hashCode();
        hash = 31 * hash + activeBatch;
        return hash;
    }

    private void refreshCapacity() {
        if (energy.resize(
                NepConfig.oritechMatrixCapacity() + addonData.energyBonusCapacity(),
                NepConfig.oritechMatrixChargeRate() + addonData.energyBonusTransfer())) {
            setChanged();
        }
    }

    private void refreshAddons(Level level) {
        if (coreChanged) {
            coreChanged = false;
            initAddons();
            return;
        }
        long phase = level.getGameTime() + getBlockPos().hashCode();
        if (Math.floorMod(phase, ADDON_CHECK_INTERVAL) == 0
                && (addonEntries.size() != connectedAddons.size()
                        || MatrixAddons.outOfDate(level, getBlockPos(), connectedAddons))) {
            initAddons();
        }
    }

    int baseCraftTicks() {
        return NepConfig.oritechMatrixCraftTicks();
    }

    int craftTicks() {
        int base = baseCraftTicks();
        return base <= 0 ? 0 : Math.max(1, Math.round(base * speedMultiplier));
    }

    float speedMultiplier() {
        return speedMultiplier;
    }

    float efficiencyMultiplier() {
        return efficiencyMultiplier;
    }

    int extraChambers() {
        return extraChambers;
    }

    List<AddonEntry> addonEntries() {
        return addonEntries;
    }

    int addonRevision() {
        return addonRevision;
    }

    ItemStackHandler coreSlot() {
        return coreSlot;
    }

    int extenderLimit() {
        return (int) getCoreQuality();
    }

    boolean layeredExtenders() {
        return layeredExtenders;
    }

    private void setAddonEntries(List<AddonEntry> entries) {
        List<AddonEntry> sorted = new ArrayList<>(entries);
        sorted.sort(Comparator.comparing(entry -> entry.block().toString()));
        if (!sorted.equals(addonEntries)) {
            addonEntries.clear();
            addonEntries.addAll(sorted);
            addonRevision++;
            setChanged();
        }
    }

    @Override
    public float getCoreQuality() {
        return MatrixAddons.coreQuality(coreSlot.getStackInSlot(0), MachineAddonController.super.getCoreQuality());
    }

    @Override
    public void writeAddons(List<AddonBlock> addons) {
        MachineAddonController.super.writeAddons(addons);
        setAddonEntries(addons.stream().map(AddonEntry::of).toList());
    }

    @Override
    public void resetAddons() {
        MachineAddonController.super.resetAddons();
        setAddonEntries(List.of());
    }

    int addonCount() {
        return connectedAddons.size();
    }

    long addonCapacityBonus() {
        return addonData.energyBonusCapacity();
    }

    private void applyAddonData(BaseAddonData data) {
        addonData = data;
        speedMultiplier = Math.max(0.01F, data.speed());
        efficiencyMultiplier = Math.max(0.0F, data.efficiency());
        extraChambers = Math.max(0, data.extraChambers());
    }

    @Override
    public List<BlockPos> getConnectedAddons() {
        return connectedAddons;
    }

    @Override
    public List<BlockPos> getOpenAddonSlots() {
        return openAddonSlots;
    }

    @Override
    public BlockPos getPosForAddon() {
        return getBlockPos();
    }

    @Override
    public Level getWorldForAddon() {
        return getLevel();
    }

    @Override
    public Direction getFacingForAddon() {
        return Direction.SOUTH;
    }

    @Override
    public DynamicEnergyStorage getStorageForAddon() {
        return addonStorage;
    }

    @Nullable
    @Override
    public ItemApi.InventoryStorage getInventoryForAddon() {
        return null;
    }

    @Nullable
    @Override
    public ScreenProvider getScreenProvider() {
        return null;
    }

    @Override
    public List<Vec3i> getAddonSlots() {
        return MatrixAddons.SLOTS;
    }

    @Override
    public BaseAddonData getBaseAddonData() {
        return addonData;
    }

    @Override
    public void setBaseAddonData(BaseAddonData data) {
        applyAddonData(data);
        refreshCapacity();
        setChanged();
    }

    @Override
    public long getDefaultCapacity() {
        return NepConfig.oritechMatrixCapacity();
    }

    @Override
    public long getDefaultInsertRate() {
        return NepConfig.oritechMatrixChargeRate();
    }

    @Override
    public void updateEnergyContainer() {
        refreshCapacity();
    }

    @Override
    public List<AddonBlock> getAllAddons(@Nullable BlockPos brokenAddon) {
        List<AddonBlock> found = new ArrayList<>(MachineAddonController.super.getAllAddons(brokenAddon));
        found.removeIf(addon -> !MatrixAddons.honours(addon));
        return found;
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
        return ParticleEnergy.toReach(ParticleEnergy.launchSpeedFor(recipe.getTime()))
                * NepConfig.oritechMatrixEnergyCostPercent()
                / 100L;
    }

    private boolean beginCraft(Level level) {
        Stall reason = Stall.NONE;
        for (AEItemKey wanted : List.copyOf(owed.keySet())) {
            if (owed.getOrDefault(wanted, 0L) <= 0) {
                continue;
            }
            CollisionTemplate template = templates.get(wanted);
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

    private Stall claimIngredients(Level level, CollisionTemplate template) {
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
        List<Ingredient> particles = holder.value().getInputs();
        List<int[]> takes = planConsumption(particles);
        if (takes == null) {
            return Stall.INGREDIENTS;
        }

        claimedItems.clear();
        claim(takes);
        AEItemKey resultKey = AEItemKey.of(result);
        long spare = Math.min(extraChambers, (resultKey == null ? 0L : owed.getOrDefault(resultKey, 0L)) - 1);
        int batches = 1;
        while (batches <= spare) {
            List<int[]> more = planConsumption(particles);
            if (more == null || !fitsInOutput(result.copyWithCount(result.getCount() * (batches + 1)))) {
                break;
            }
            claim(more);
            batches++;
        }
        activeBatch = batches;
        activeResult = result;
        craftSpeed = holder.value().getTime();
        craftCost = Math.round(energyCost(holder.value()) * (double) efficiencyMultiplier);
        paid = 0;
        progress = 0;
        setChanged();
        if (NepConfig.debugLogging()) {
            Nep.LOGGER.info(
                    "Particle collision matrix {} started {} x{} in {} chamber(s) at speed {} for {} FE",
                    getBlockPos(),
                    activeResult.getItem(),
                    activeResult.getCount(),
                    activeBatch,
                    craftSpeed,
                    craftCost);
        }
        return Stall.NONE;
    }

    private void claim(List<int[]> takes) {
        for (int[] take : takes) {
            ItemStack taken = inputBuffer.extractItem(take[0], take[1], false);
            if (!taken.isEmpty()) {
                claimedItems.add(taken);
            }
        }
    }

    @Nullable
    private List<int[]> planConsumption(List<Ingredient> particles) {
        int[] slotLeft = new int[inputBuffer.getSlots()];
        for (int slot = 0; slot < slotLeft.length; slot++) {
            slotLeft[slot] = inputBuffer.getStackInSlot(slot).getCount();
        }
        List<int[]> takes = new ArrayList<>();
        for (Ingredient particle : particles) {
            int found = -1;
            for (int slot = 0; slot < slotLeft.length; slot++) {
                if (slotLeft[slot] > 0 && particle.test(inputBuffer.getStackInSlot(slot))) {
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
        ItemStack result = activeResult.copyWithCount(activeResult.getCount() * activeBatch);
        if (!fitsInOutput(result)) {
            outputBlocked = true;
            return;
        }
        outputBlocked = false;
        ItemHandlerHelper.insertItem(outputBuffer, result.copy(), false);
        AEItemKey produced = AEItemKey.of(result);
        if (produced != null && owed.getOrDefault(produced, 0L) > 0) {
            boolean manual = isManualJob(produced);
            long settled = Math.min(activeBatch, owed.get(produced));
            decrement(owed, produced, settled);
            if (manual) {
                decrement(manualOwed, produced, settled);
            }
            if (!owed.containsKey(produced)) {
                templates.remove(produced);
            }
            if (owed.isEmpty()) {
                pushingCpus.clear();
            }
            if (!manual) {
                toReturn.merge(produced, settled * activeResult.getCount(), Long::sum);
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
        activeBatch = 1;
        outputBlocked = false;
        markScanNeeded();
        setChanged();
    }

    boolean readyForPatterns() {
        return NepConfig.oritechParticleCollisionMatrix();
    }

    boolean pushMatrixPattern(IPatternDetails pattern, KeyCounter[] inputs, Direction ejectionDirection) {
        Level level = getLevel();
        if (level == null || level.isClientSide || !NepConfig.oritechParticleCollisionMatrix()) {
            return false;
        }
        if (!(pattern instanceof ParticleCollisionPattern collision)) {
            return reject(Refusal.NOT_A_COLLISION_PATTERN, "pattern is not a particle collision pattern");
        }
        ResourceLocation recipe = collision.recipe();
        List<GenericStack> outputs = pattern.getOutputs();
        if (outputs.isEmpty() || !(outputs.get(0).what() instanceof AEItemKey producedItem)) {
            return reject(Refusal.NO_ITEM_OUTPUT, "pattern has no item output");
        }
        RecipeHolder<OritechRecipe> holder = OritechRecipeResolver.collisionById(level, recipe);
        OritechRecipeResolver.CollisionPlan plan =
                holder == null ? null : OritechRecipeResolver.planCollision(holder, pattern);
        if (plan == null) {
            return reject(Refusal.UNKNOWN_RECIPE, "no particle collision recipe matches " + recipe);
        }
        if (isManualJob(producedItem)) {
            return reject(Refusal.MIXED_RECIPES, "a manual craft of " + producedItem + " is already queued");
        }
        CollisionTemplate existing = templates.get(producedItem);
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
    public ManualCraftOutcome startManualCraft(Player player, ResourceLocation recipe, int batches) {
        Level level = getLevel();
        if (level == null || level.isClientSide) {
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
        CollisionTemplate existing = templates.get(produced);
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

    private void captureTemplate(AEItemKey output, Map<AEItemKey, Long> items, ResourceLocation recipe) {
        templates.put(output, CollisionTemplate.capture(items, recipe));
    }

    private void autoRequest(Level level) {
        if (!NepConfig.oritechMatrixAutoRequest()) {
            setMissingInputs(List.of());
            return;
        }
        DemandTally tally = new DemandTally();
        for (Map.Entry<AEItemKey, Long> entry : owed.entrySet()) {
            CollisionTemplate template = templates.get(entry.getKey());
            if (template == null || isManualJob(entry.getKey())) {
                continue;
            }
            template.addDemand(tally, entry.getValue());
        }
        if (tally.isEmpty()) {
            setMissingInputs(List.of());
            return;
        }
        setMissingInputs(tally.restock(level, power));
    }

    int manualDemandFor(ItemStack stack) {
        AEItemKey key = AEItemKey.of(stack);
        if (key == null) {
            return 0;
        }
        long target = 0;
        for (Map.Entry<AEItemKey, Long> entry : owed.entrySet()) {
            CollisionTemplate template = templates.get(entry.getKey());
            if (template == null) {
                continue;
            }
            target += template.demandFor(key, entry.getValue());
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

    int activeBatch() {
        return activeBatch;
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

    IItemHandler itemHandlerForSide() {
        return machineView;
    }

    MatrixEnergyBuffer energyStorage() {
        return energy;
    }

    @Override
    public void clearContent() {
        super.clearContent();
        clearHandler(coreSlot);
    }

    void dropBuffers(Level level, BlockPos pos) {
        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), coreSlot.getStackInSlot(0));
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
        tag.putLong(SPEED_KEY, craftSpeed);
        tag.putInt(PROGRESS_KEY, progress);
        tag.putInt(BATCH_KEY, activeBatch);
        CompoundTag addons = new CompoundTag();
        writeAddonToNbt(addons);
        tag.put(ADDONS_KEY, addons);
        tag.put(
                ADDON_LIST_KEY,
                AddonEntry.CODEC
                        .listOf()
                        .encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), addonEntries)
                        .getOrThrow());
        tag.put(CORE_KEY, coreSlot.serializeNBT(registries));
        tag.putBoolean(LAYERED_KEY, OritechConfig.layeredExtenders.get());
        tag.putBoolean(BLOCKED_KEY, outputBlocked);
        tag.putBoolean(POWER_FAULT_KEY, powerFault);
        tag.putBoolean(ENERGY_FAULT_KEY, energyFault);
        tag.putString(STALL_KEY, stall.name());
        tag.putString(REFUSAL_KEY, refusal.name());
        tag.putInt(REFUSAL_TICKS_KEY, refusalTicks);
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
        if (tag.contains(ADDONS_KEY)) {
            loadAddonNbtData(tag.getCompound(ADDONS_KEY));
        }
        if (tag.contains(ADDON_LIST_KEY)) {
            setAddonEntries(AddonEntry.CODEC
                    .listOf()
                    .parse(registries.createSerializationContext(NbtOps.INSTANCE), tag.get(ADDON_LIST_KEY))
                    .result()
                    .orElseGet(List::of));
        }
        coreSlot.deserializeNBT(registries, tag.getCompound(CORE_KEY));
        layeredExtenders = tag.getBoolean(LAYERED_KEY);
        addonRevision++;
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
        craftSpeed = tag.getLong(SPEED_KEY);
        progress = tag.getInt(PROGRESS_KEY);
        activeBatch = Math.max(1, tag.getInt(BATCH_KEY));
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
        stall = EnumNames.byName(Stall.values(), tag.getString(STALL_KEY), Stall.NONE);
        refusal = EnumNames.byName(Refusal.values(), tag.getString(REFUSAL_KEY), Refusal.NONE);
        refusalTicks = refusal == Refusal.NONE
                ? 0
                : Math.max(1, Math.min(REFUSAL_MEMORY_TICKS, tag.getInt(REFUSAL_TICKS_KEY)));
        markScanNeeded();
    }

    private ListTag saveTemplates(HolderLookup.Provider registries) {
        return CollisionTemplate.save(templates, registries);
    }

    private void loadTemplates(ListTag list, HolderLookup.Provider registries) {
        CollisionTemplate.load(templates, list, registries, true);
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
