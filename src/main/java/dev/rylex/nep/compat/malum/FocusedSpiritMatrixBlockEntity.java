package dev.rylex.nep.compat.malum;

import appeng.api.config.Actionable;
import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.core.definitions.AEItems;
import com.sammy.malum.common.data.map.ImpetusDataMap;
import com.sammy.malum.common.recipe.RuneworkingRecipe;
import com.sammy.malum.common.recipe.SpiritFocusingRecipe;
import com.sammy.malum.common.recipe.SpiritInfusionRecipe;
import com.sammy.malum.registry.common.MalumDataMaps;
import dev.rylex.nep.Nep;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.machine.BufferedMatrixBlockEntity;
import dev.rylex.nep.machine.MachineItemView;
import dev.rylex.nep.machine.ManualCraftHost;
import dev.rylex.nep.machine.ManualCraftOutcome;
import dev.rylex.nep.machine.ManualCraftResult;
import dev.rylex.nep.machine.ManualRequirement;
import dev.rylex.nep.machine.ManualStaging;
import dev.rylex.nep.machine.MatrixGridNode;
import dev.rylex.nep.machine.MatrixStatus;
import dev.rylex.nep.machine.OverstackedItemHandler;
import dev.rylex.nep.pattern.RuneworkingPattern;
import dev.rylex.nep.pattern.SpiritFocusingPattern;
import dev.rylex.nep.pattern.SpiritInfusionPattern;
import dev.rylex.nep.util.ItemCounts;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
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
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FocusedSpiritMatrixBlockEntity extends BufferedMatrixBlockEntity implements ManualCraftHost {

    static final int INPUT_SLOTS = 12;
    static final int OUTPUT_SLOTS = 9;
    static final int SPIRIT_SLOTS = SpiritBank.SLOTS;
    static final int UPGRADE_SLOTS = 1;
    static final int CATALYZER_SLOTS = 1;
    static final int IMPETUS_SLOTS = 1;

    private static final int INPUT_HANDLER = 0;
    private static final int SPIRIT_HANDLER = 1;

    private static final int RESTOCK_INTERVAL = 10;
    private static final int RUNNING_GRACE_TICKS = 10;
    private static final int REFUSAL_MEMORY_TICKS = 200;

    private static final String INPUT_KEY = "Input";
    private static final String OUTPUT_KEY = "Output";
    private static final String OWED_KEY = "Owed";
    private static final String RETURN_KEY = "ToReturn";
    private static final String RETURN_DIRS_KEY = "ReturnDirs";
    private static final String TEMPLATE_KEY = "Template";
    private static final String NODE_KEY = "Node";
    private static final String UPGRADE_KEY = "Upgrade";
    private static final String CATALYZER_KEY = "Catalyzer";
    private static final String IMPETUS_KEY = "Impetus";
    private static final String ACTIVE_PROCESS_KEY = "ActiveProcess";
    private static final String ACTIVE_TICKS_KEY = "ActiveTicks";
    private static final String SPIRITS_KEY = "Spirits";
    private static final String SPIRIT_RESTOCK_KEY = "SpiritRestock";
    private static final String ACTIVE_KEY = "Active";
    private static final String CLAIMED_KEY = "Claimed";
    private static final String PROGRESS_KEY = "Progress";
    private static final String CHAINING_KEY = "ChainedCrafts";
    private static final String MISSING_KEY = "Missing";
    private static final String BLOCKED_KEY = "Blocked";
    private static final String POWER_FAULT_KEY = "PowerFault";
    private static final String STALL_KEY = "Stall";
    private static final String REFUSAL_KEY = "Refusal";
    private static final String REFUSAL_TICKS_KEY = "RefusalTicks";
    private static final String PRESERVATION_KEY = "PreservationTicks";

    private final ItemStackHandler upgradeSlot = new ItemStackHandler(UPGRADE_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            refreshUpgrades();
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return FocusedSpiritMatrixUpgrades.obelisksEnabled() && FocusedSpiritMatrixUpgrades.isObelisk(stack);
        }

        @Override
        public int getSlotLimit(int slot) {
            return FocusedSpiritMatrixUpgrades.maxObelisks();
        }
    };

    private final ItemStackHandler catalyzerSlot = new ItemStackHandler(CATALYZER_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            refreshUpgrades();
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return FocusedSpiritMatrixUpgrades.catalyzersEnabled() && FocusedSpiritMatrixUpgrades.isCatalyzer(stack);
        }

        @Override
        public int getSlotLimit(int slot) {
            return FocusedSpiritMatrixUpgrades.maxCatalyzers();
        }
    };

    private final ItemStackHandler impetusSlot = new ItemStackHandler(IMPETUS_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            markScanNeeded();
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return FocusedSpiritMatrixUpgrades.focusingEnabled() && FocusedSpiritMatrixUpgrades.isImpetus(stack);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }
    };

    private final SpiritBank spirits = new SpiritBank(this::onSpiritsChanged);

    private final Map<AEItemKey, Template> templates = new HashMap<>();
    private final IItemHandler outputView = new OutputView();
    private final IItemHandler machineView =
            MachineItemView.demandLimited(inputBuffer, outputView, this::manualDemandFor);

    private int upgradeCount;
    private int catalyzerCount;
    private int chainedCrafts;
    private int progress;
    private int activeCraftTicks;
    private int activeDurabilityCost;
    private Process activeProcess = Process.INFUSION;
    private boolean spiritRestock = true;
    private Stall stall = Stall.NONE;
    private Refusal refusal = Refusal.NONE;
    private int refusalTicks;
    private int preservationTicks;

    enum Process {
        INFUSION,
        FOCUSING,
        RUNEWORKING
    }

    enum Stall {
        NONE,
        INGREDIENTS,
        NO_IMPETUS,
        OUTPUT_FULL,
        NO_RECIPE,
        FOCUSING_DISABLED,
        RUNEWORKING_DISABLED;

        private static Stall worse(Stall first, Stall second) {
            return first.ordinal() >= second.ordinal() ? first : second;
        }
    }

    enum Refusal {
        NONE,
        WRONG_PATTERN,
        NO_ITEM_OUTPUT,
        UNKNOWN_RECIPE,
        MISMATCHED_INPUTS,
        MIXED_RECIPES,
        ITEMS_ONLY,
        TOO_MANY_INPUTS,
        NO_IMPETUS,
        FOCUSING_DISABLED,
        RUNEWORKING_DISABLED,
        BUFFER_FULL
    }

    private record Template(List<AEKey> keys, List<Long> counts, ResourceLocation recipe, Process process) {}

    private record ManualPlan(
            Process process, ItemStack result, List<ManualRequirement> requirements, boolean carriesData) {}

    record RestockPlan(List<AEKey> keys, List<Long> targets, Map<AEKey, Long> needs) {}

    public FocusedSpiritMatrixBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(
                type,
                pos,
                state,
                INPUT_SLOTS,
                OUTPUT_SLOTS,
                NepMalumContent.MATRIX_ITEM.get(),
                NepConfig.malumFocusedSpiritMatrixIdleMeDrain(),
                NepConfig.malumFocusedSpiritMatrixChannels(),
                "Focused Spirit Matrix");
        power.provideStorage(new SpiritBankStorage(this));
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FocusedSpiritMatrixBlockEntity matrix) {
        matrix.tick(level);
    }

    private void tick(Level level) {
        if (runningGrace > 0) {
            runningGrace--;
        }
        if (refusalTicks > 0 && --refusalTicks == 0) {
            refusal = Refusal.NONE;
        }
        if (preservationTicks > 0) {
            preservationTicks--;
        }
        flushOutput(level);
        if (!NepConfig.malumFocusedSpiritMatrix()) {
            setPowerFault(false);
            refreshComparator(level);
            refreshVisualState(level);
            syncIfChanged(level);
            return;
        }
        power.create(level, getBlockPos());

        if (!activeResult.isEmpty() && !activeCraftStillValid()) {
            abortActiveCraft(level);
        }

        long phase = level.getGameTime() + getBlockPos().hashCode();
        if (Math.floorMod(phase, RESTOCK_INTERVAL) == 0) {
            autoRequest(level);
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
            } else if (drawActivePower()) {
                progress++;
                runningGrace = RUNNING_GRACE_TICKS;
                if (progress >= craftTicks()) {
                    finishCraft(level);
                }
            }
        }
        setPowerFault(!power.hasUsablePower());

        refreshComparator(level);
        refreshVisualState(level);
        syncIfChanged(level);
    }

    private void onSpiritsChanged() {
        markScanNeeded();
        setChanged();
        power.requestStorageUpdate();
    }

    IItemHandler getSpiritBank() {
        return spirits;
    }

    boolean spiritRestock() {
        return spiritRestock;
    }

    void toggleSpiritRestock() {
        spiritRestock = !spiritRestock;
        setChanged();
    }

    boolean isOwnActionSource(IActionSource source) {
        return power.isOwnActionSource(source);
    }

    private long spiritAmount(AEItemKey key) {
        return spirits.amountOf(key.toStack());
    }

    private long committed(AEKey key) {
        long total = 0;
        for (Map.Entry<AEItemKey, Long> entry : owed.entrySet()) {
            Template template = templates.get(entry.getKey());
            if (template == null) {
                continue;
            }
            int index = template.keys().indexOf(key);
            if (index >= 0) {
                total += template.counts().get(index) * entry.getValue();
            }
        }
        return total;
    }

    private long claimedAmount(AEItemKey key) {
        long total = 0;
        for (ItemStack stack : claimedItems) {
            if (!stack.isEmpty() && key.matches(stack)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    /**
     * A craft's spirits sit in the bank from the moment its pattern lands until the craft starts, so they stop counting
     * as available the instant they are owed.
     */
    long freeSpirits(AEItemKey key) {
        long banked = spiritAmount(key);
        long spare = banked + claimedAmount(key) - committed(key);
        return Math.max(0, Math.min(banked, spare));
    }

    void forEachFreeSpirit(java.util.function.ObjLongConsumer<AEKey> out) {
        Set<AEItemKey> seen = new HashSet<>();
        for (int slot = 0; slot < spirits.getSlots(); slot++) {
            AEItemKey key = AEItemKey.of(spirits.getStackInSlot(slot));
            if (key == null || !seen.add(key)) {
                continue;
            }
            long free = freeSpirits(key);
            if (free > 0) {
                out.accept(key, free);
            }
        }
    }

    long bankSpirits(AEItemKey key, long amount, Actionable mode) {
        int slot = spirits.slotFor(key.toStack());
        if (slot < 0) {
            return 0;
        }
        long room = spirits.getSlotLimit(slot) - spirits.getStackInSlot(slot).getCount();
        int accepted = (int) Math.max(0, Math.min(amount, room));
        if (accepted > 0 && mode == Actionable.MODULATE) {
            ItemStack leftover = spirits.insertItem(slot, SpiritBankStorage.stackOf(key, accepted), false);
            accepted -= leftover.getCount();
        }
        return accepted;
    }

    boolean banksSpirit(AEItemKey key) {
        return spirits.isBanked(key.toStack());
    }

    long takeSpirits(AEItemKey key, long amount) {
        int slot = spirits.slotFor(key.toStack());
        if (slot < 0) {
            return 0;
        }
        return spirits.extractItem(slot, (int) Math.min(amount, Integer.MAX_VALUE), false)
                .getCount();
    }

    private void refreshUpgrades() {
        ItemStack stack = upgradeSlot.getStackInSlot(0);
        upgradeCount = FocusedSpiritMatrixUpgrades.obelisksEnabled() && FocusedSpiritMatrixUpgrades.isObelisk(stack)
                ? Math.min(stack.getCount(), FocusedSpiritMatrixUpgrades.maxObelisks())
                : 0;
        ItemStack catalyzers = catalyzerSlot.getStackInSlot(0);
        catalyzerCount =
                FocusedSpiritMatrixUpgrades.catalyzersEnabled() && FocusedSpiritMatrixUpgrades.isCatalyzer(catalyzers)
                        ? Math.min(catalyzers.getCount(), FocusedSpiritMatrixUpgrades.maxCatalyzers())
                        : 0;
    }

    int craftTicks() {
        if (!activeResult.isEmpty()) {
            return Math.max(1, activeCraftTicks);
        }
        return FocusedSpiritMatrixUpgrades.craftTicks(upgradeCount);
    }

    int catalyzerCount() {
        return catalyzerCount;
    }

    IItemHandler getCatalyzerSlot() {
        return catalyzerSlot;
    }

    IItemHandler getImpetusSlot() {
        return impetusSlot;
    }

    boolean hasImpetus() {
        return FocusedSpiritMatrixUpgrades.isImpetus(impetusSlot.getStackInSlot(0));
    }

    Process activeProcess() {
        return activeProcess;
    }

    /** Durability is spent once a craft finishes, so only an impetus already worn through stops the Matrix. */
    private boolean impetusReady() {
        ItemStack impetus = impetusSlot.getStackInSlot(0);
        if (!FocusedSpiritMatrixUpgrades.isImpetus(impetus)) {
            return false;
        }
        return !NepConfig.malumFocusedSpiritMatrixConsumeImpetusDurability()
                || impetus.getDamageValue() < impetus.getMaxDamage();
    }

    private boolean activeCraftStillValid() {
        return activeProcess != Process.FOCUSING || impetusReady();
    }

    private void abortActiveCraft(Level level) {
        for (ItemStack claimed : claimedItems) {
            ItemStack left = ItemHandlerHelper.insertItem(
                    spirits.isBanked(claimed) ? spirits : inputBuffer, claimed.copy(), false);
            AEItemKey key = AEItemKey.of(left);
            if (key != null) {
                left.shrink((int) power.dumpToNetwork(key, left.getCount()));
            }
            if (!left.isEmpty()) {
                Containers.dropItemStack(
                        level,
                        getBlockPos().getX(),
                        getBlockPos().getY(),
                        getBlockPos().getZ(),
                        left);
            }
        }
        claimedItems.clear();
        activeResult = ItemStack.EMPTY;
        activeCraftTicks = 0;
        activeDurabilityCost = 0;
        progress = 0;
        chainedCrafts = 0;
        outputBlocked = false;
        setStall(Stall.NO_IMPETUS);
        markScanNeeded();
        setChanged();
        if (NepConfig.debugLogging()) {
            Nep.LOGGER.info("Focused Spirit Matrix {} voided its craft: the impetus left mid-focusing", getBlockPos());
        }
    }

    void preserveImpetus(int ticks) {
        preservationTicks = Math.max(preservationTicks, ticks);
        setChanged();
    }

    private void spendImpetus(Level level, int durabilityCost) {
        if (preservationTicks > 0
                || !NepConfig.malumFocusedSpiritMatrixConsumeImpetusDurability()
                || durabilityCost <= 0) {
            return;
        }
        ItemStack impetus = impetusSlot.getStackInSlot(0);
        if (!FocusedSpiritMatrixUpgrades.isImpetus(impetus)) {
            return;
        }
        impetus.setDamageValue(impetus.getDamageValue() + durabilityCost);
        restoreImpetus(level, impetus);
        if (impetus.getDamageValue() >= impetus.getMaxDamage()) {
            impetusSlot.setStackInSlot(0, fracturedVariant(impetus));
        }
        setChanged();
    }

    private static ItemStack fracturedVariant(ItemStack impetus) {
        ImpetusDataMap fractured = impetus.getItemHolder().getData(MalumDataMaps.FRACTURED_IMPETUS_VARIANT);
        return fractured == null
                ? new ItemStack(NepMalumContent.FRACTURED_MATRIX_IMPETUS.get())
                : fractured.otherImpetus().value().getDefaultInstance();
    }

    private void restoreImpetus(Level level, ItemStack impetus) {
        int repairs = FocusedSpiritMatrixUpgrades.rollChance(
                FocusedSpiritMatrixUpgrades.restorationChance(catalyzerCount), level.getRandom());
        if (repairs <= 0) {
            return;
        }
        int mended = Math.max(1, Math.round(impetus.getMaxDamage() * 0.01F)) * repairs;
        impetus.setDamageValue(Math.max(impetus.getDamageValue() - mended, 0));
    }

    int upgradeCount() {
        return upgradeCount;
    }

    IItemHandler getUpgradeSlot() {
        return upgradeSlot;
    }

    private boolean drawActivePower() {
        double extra = NepConfig.malumFocusedSpiritMatrixMeDrain() - NepConfig.malumFocusedSpiritMatrixIdleMeDrain();
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
            Stall attempt = claimIngredients(level, wanted, template);
            if (attempt == Stall.NONE) {
                setStall(Stall.NONE);
                return true;
            }
            reason = Stall.worse(reason, attempt);
        }
        setStall(owed.isEmpty() ? Stall.NONE : reason);
        return false;
    }

    private Stall claimIngredients(Level level, AEItemKey wanted, Template template) {
        ItemStack result;
        int craftTicks;
        int durabilityCost = 0;
        if (template.process() == Process.FOCUSING) {
            if (!NepConfig.malumFocusedSpiritMatrixFocusing()) {
                return Stall.FOCUSING_DISABLED;
            }
            RecipeHolder<SpiritFocusingRecipe> holder = SpiritFocusingResolver.byId(level, template.recipe());
            if (holder == null) {
                return Stall.NO_RECIPE;
            }
            result = holder.value().output.copy();
            durabilityCost = holder.value().durabilityCost;
            if (!impetusReady()) {
                return Stall.NO_IMPETUS;
            }
            craftTicks = FocusedSpiritMatrixUpgrades.focusingTicks(holder.value().time, catalyzerCount);
        } else if (template.process() == Process.RUNEWORKING) {
            if (!NepConfig.malumFocusedSpiritMatrixRuneworking()) {
                return Stall.RUNEWORKING_DISABLED;
            }
            RecipeHolder<RuneworkingRecipe> holder = RuneworkingResolver.byId(level, template.recipe());
            if (holder == null) {
                return Stall.NO_RECIPE;
            }
            result = holder.value().output.copy();
            craftTicks = FocusedSpiritMatrixUpgrades.craftTicks(upgradeCount);
        } else {
            RecipeHolder<SpiritInfusionRecipe> holder = SpiritInfusionResolver.byId(level, template.recipe());
            if (holder == null) {
                return Stall.NO_RECIPE;
            }
            result = MalumRecipeIngredients.infusionResult(holder.value(), level, template.keys());
            craftTicks = FocusedSpiritMatrixUpgrades.craftTicks(upgradeCount);
        }
        if (result.isEmpty() || !wanted.matches(result)) {
            return Stall.NO_RECIPE;
        }
        if (!fitsInOutput(result)) {
            return Stall.OUTPUT_FULL;
        }
        List<int[]> takes = planConsumption(template);
        if (takes == null) {
            return Stall.INGREDIENTS;
        }

        claimedItems.clear();
        for (int[] take : takes) {
            ItemStack taken = handler(take[0]).extractItem(take[1], take[2], false);
            if (!taken.isEmpty()) {
                claimedItems.add(taken);
            }
        }
        activeResult = result;
        activeProcess = template.process();
        activeCraftTicks = craftTicks;
        activeDurabilityCost = durabilityCost;
        progress = chainedStart(template.process(), craftTicks);
        setChanged();
        if (NepConfig.debugLogging()) {
            Nep.LOGGER.info(
                    "Focused Spirit Matrix {} started {} {} x{} over {} tick(s)",
                    getBlockPos(),
                    verbOf(activeProcess),
                    activeResult.getItem(),
                    activeResult.getCount(),
                    craftTicks);
        }
        return Stall.NONE;
    }

    private static String verbOf(Process process) {
        return switch (process) {
            case FOCUSING -> "focusing";
            case RUNEWORKING -> "shaping";
            case INFUSION -> "infusing";
        };
    }

    private int chainedStart(Process process, int craftTicks) {
        if (process != Process.FOCUSING || chainedCrafts <= 0) {
            chainedCrafts = 0;
            return 0;
        }
        chainedCrafts--;
        return Math.max(0, craftTicks - NepConfig.CHAIN_FOCUSING_REMAINDER_TICKS);
    }

    private ItemStackHandler handler(int index) {
        return index == SPIRIT_HANDLER ? spirits : inputBuffer;
    }

    /**
     * The input buffer is drained before the bank, so a recipe whose extra ingredient is a spirit shard spends what
     * was staged for it rather than the bank's stock.
     */
    @Nullable
    private List<int[]> planConsumption(Template template) {
        int[][] slotLeft = new int[2][];
        slotLeft[INPUT_HANDLER] = countsOf(inputBuffer);
        slotLeft[SPIRIT_HANDLER] = countsOf(spirits);
        List<int[]> takes = new ArrayList<>();
        for (int index = 0; index < template.keys().size(); index++) {
            if (!(template.keys().get(index) instanceof AEItemKey key)) {
                return null;
            }
            long need = template.counts().get(index);
            need = drawFrom(INPUT_HANDLER, slotLeft, takes, key, need);
            need = drawFrom(SPIRIT_HANDLER, slotLeft, takes, key, need);
            if (need > 0) {
                return null;
            }
        }
        return takes;
    }

    private long drawFrom(int from, int[][] slotLeft, List<int[]> takes, AEItemKey key, long wanted) {
        ItemStackHandler source = handler(from);
        long need = wanted;
        for (int slot = 0; slot < slotLeft[from].length && need > 0; slot++) {
            if (slotLeft[from][slot] <= 0 || !key.matches(source.getStackInSlot(slot))) {
                continue;
            }
            int take = (int) Math.min(need, slotLeft[from][slot]);
            need -= take;
            slotLeft[from][slot] -= take;
            takes.add(new int[] {from, slot, take});
        }
        return need;
    }

    private static int[] countsOf(ItemStackHandler handler) {
        int[] counts = new int[handler.getSlots()];
        for (int slot = 0; slot < counts.length; slot++) {
            counts[slot] = handler.getStackInSlot(slot).getCount();
        }
        return counts;
    }

    private void finishCraft(Level level) {
        ItemStack result = activeResult.copy();
        if (!fitsInOutput(result)) {
            outputBlocked = true;
            return;
        }
        outputBlocked = false;
        ItemHandlerHelper.insertItem(outputBuffer, result.copy(), false);
        int bonus = 0;
        if (activeProcess == Process.FOCUSING) {
            bonus = insertFortuneBonus(level, result);
            spendImpetus(level, activeDurabilityCost);
            chainedCrafts = FocusedSpiritMatrixUpgrades.rollChance(
                    FocusedSpiritMatrixUpgrades.chainFocusingChance(catalyzerCount), level.getRandom());
        }
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
                toReturn.merge(produced, (long) result.getCount() + bonus, Long::sum);
            }
        }
        activeResult = ItemStack.EMPTY;
        claimedItems.clear();
        progress = 0;
        activeCraftTicks = 0;
        activeDurabilityCost = 0;
        outputBlocked = false;
        markScanNeeded();
        setChanged();
        flushOutput(level);
        if (NepConfig.debugLogging()) {
            Nep.LOGGER.info(
                    "Focused Spirit Matrix {} infused {} x{} ({} still owed)",
                    getBlockPos(),
                    produced,
                    result.getCount(),
                    owed.getOrDefault(produced, 0L));
        }
    }

    private int insertFortuneBonus(Level level, ItemStack result) {
        int rolls = FocusedSpiritMatrixUpgrades.rollChance(
                FocusedSpiritMatrixUpgrades.fortuneChance(catalyzerCount), level.getRandom());
        int inserted = 0;
        for (int roll = 0; roll < rolls; roll++) {
            ItemStack left = ItemHandlerHelper.insertItem(outputBuffer, result.copy(), false);
            inserted += result.getCount() - left.getCount();
        }
        return inserted;
    }

    @Override
    public ManualCraftOutcome startManualCraft(Player player, ResourceLocation recipe, int batches) {
        Level level = getLevel();
        if (level == null || level.isClientSide) {
            return ManualCraftOutcome.failed(ManualCraftResult.UNKNOWN_RECIPE);
        }
        if (!NepConfig.manualCrafting() || !NepConfig.malumFocusedSpiritMatrix()) {
            return ManualCraftOutcome.failed(ManualCraftResult.DISABLED);
        }
        RecipeHolder<SpiritInfusionRecipe> infusion = SpiritInfusionResolver.byId(level, recipe);
        RecipeHolder<RuneworkingRecipe> runeworking = RuneworkingResolver.byId(level, recipe);
        RecipeHolder<SpiritFocusingRecipe> focusing = SpiritFocusingResolver.byId(level, recipe);
        if (runeworking != null && !NepConfig.malumFocusedSpiritMatrixRuneworking()) {
            return ManualCraftOutcome.failed(ManualCraftResult.DISABLED);
        }
        if (focusing != null && !NepConfig.malumFocusedSpiritMatrixFocusing()) {
            return ManualCraftOutcome.failed(ManualCraftResult.DISABLED);
        }
        if (focusing != null && !impetusReady()) {
            return ManualCraftOutcome.failed(ManualCraftResult.NO_IMPETUS);
        }
        ManualPlan plan = manualPlan(infusion, runeworking, focusing);
        if (plan == null) {
            return ManualCraftOutcome.failed(
                    infusion == null && runeworking == null && focusing == null
                            ? ManualCraftResult.UNKNOWN_RECIPE
                            : ManualCraftResult.UNSUPPORTED);
        }
        ItemStack result = plan.result();
        AEItemKey produced = AEItemKey.of(result);
        Template existing = templates.get(produced);
        if (existing != null && !recipe.equals(existing.recipe())) {
            return ManualCraftOutcome.failed(ManualCraftResult.BUSY);
        }
        if (owed.containsKey(produced) && (plan.carriesData() || !isManualJob(produced))) {
            return ManualCraftOutcome.failed(ManualCraftResult.BUSY);
        }

        int wanted = plan.carriesData() ? 1 : batches;
        ManualStaging.Result staged = ManualStaging.pull(player, plan.requirements(), wanted, this::stageAll);
        if (staged.status() != ManualCraftResult.STARTED) {
            return ManualCraftOutcome.failed(staged.status());
        }
        if (infusion != null) {
            result = MalumRecipeIngredients.infusionResult(
                    infusion.value(), level, List.copyOf(staged.perCraft().keySet()));
            produced = AEItemKey.of(result);
        }

        captureTemplate(produced, staged.perCraft(), recipe, plan.process());
        owed.merge(produced, (long) staged.batches(), Long::sum);
        manualOwed.merge(produced, (long) staged.batches(), Long::sum);
        clearRefusal();
        markScanNeeded();
        setChanged();
        if (NepConfig.debugLogging()) {
            Nep.LOGGER.info(
                    "Focused Spirit Matrix {} queued {} manual {} craft(s) of {} for {}",
                    getBlockPos(),
                    staged.batches(),
                    verbOf(plan.process()),
                    produced,
                    player.getName().getString());
        }
        return ManualCraftOutcome.started(staged.batches(), result);
    }

    @Nullable
    private static ManualPlan manualPlan(
            @Nullable RecipeHolder<SpiritInfusionRecipe> infusion,
            @Nullable RecipeHolder<RuneworkingRecipe> runeworking,
            @Nullable RecipeHolder<SpiritFocusingRecipe> focusing) {
        if (infusion != null) {
            List<ManualRequirement> requirements = MalumRecipeIngredients.manualSpiritInfusion(infusion.value());
            return requirements == null
                    ? null
                    : new ManualPlan(
                            Process.INFUSION,
                            infusion.value().result.copy(),
                            requirements,
                            infusion.value().carryOverComponentData);
        }
        if (runeworking != null) {
            List<ManualRequirement> requirements = MalumRecipeIngredients.manualRuneworking(runeworking.value());
            return requirements == null
                    ? null
                    : new ManualPlan(
                            Process.RUNEWORKING, runeworking.value().output.copy(), requirements, false);
        }
        if (focusing != null) {
            List<ManualRequirement> requirements = MalumRecipeIngredients.manualSpiritFocusing(focusing.value());
            return requirements == null
                    ? null
                    : new ManualPlan(Process.FOCUSING, focusing.value().output.copy(), requirements, false);
        }
        return null;
    }

    boolean readyForPatterns() {
        return NepConfig.malumFocusedSpiritMatrix();
    }

    boolean pushMatrixPattern(IPatternDetails pattern, KeyCounter[] inputs, Direction ejectionDirection) {
        Level level = getLevel();
        if (level == null || level.isClientSide || !NepConfig.malumFocusedSpiritMatrix()) {
            return false;
        }
        boolean processing = pattern.getDefinition().getItem() == AEItems.PROCESSING_PATTERN.asItem();
        boolean infusionPattern = pattern instanceof SpiritInfusionPattern;
        boolean focusingPattern = pattern instanceof SpiritFocusingPattern;
        boolean runeworkingPattern = pattern instanceof RuneworkingPattern;
        if (!infusionPattern && !focusingPattern && !runeworkingPattern && !processing) {
            return reject(
                    Refusal.WRONG_PATTERN,
                    "pattern is not a spirit infusion, focusing, runeworking or processing pattern");
        }
        boolean focusingEnabled = NepConfig.malumFocusedSpiritMatrixFocusing();
        if (focusingPattern && !focusingEnabled) {
            return reject(Refusal.FOCUSING_DISABLED, "spirit focusing is disabled for the Matrix");
        }
        boolean runeworkingEnabled = NepConfig.malumFocusedSpiritMatrixRuneworking();
        if (runeworkingPattern && !runeworkingEnabled) {
            return reject(Refusal.RUNEWORKING_DISABLED, "runeworking is disabled for the Matrix");
        }

        if (!focusingPattern && !runeworkingPattern) {
            SpiritInfusionResolver.Plan infusion = SpiritInfusionResolver.resolve(pattern, level);
            if (infusion != null) {
                return stagePush(
                        pattern,
                        inputs,
                        ejectionDirection,
                        Process.INFUSION,
                        infusion.holder().id(),
                        infusion.expectedItems());
            }
        }
        if (!infusionPattern && !focusingPattern) {
            RuneworkingResolver.Plan runeworking = RuneworkingResolver.resolve(pattern, level);
            if (runeworking != null) {
                if (!runeworkingEnabled) {
                    return reject(Refusal.RUNEWORKING_DISABLED, "runeworking is disabled for the Matrix");
                }
                return stagePush(
                        pattern,
                        inputs,
                        ejectionDirection,
                        Process.RUNEWORKING,
                        runeworking.holder().id(),
                        runeworking.expectedItems());
            }
        }
        if (!infusionPattern && !runeworkingPattern) {
            SpiritFocusingResolver.Plan focusing = SpiritFocusingResolver.resolve(pattern, level);
            if (focusing != null) {
                if (!focusingEnabled) {
                    return reject(Refusal.FOCUSING_DISABLED, "spirit focusing is disabled for the Matrix");
                }
                if (!impetusReady()) {
                    return reject(Refusal.NO_IMPETUS, "focusing needs a whole Matrix Impetus in the impetus slot");
                }
                return stagePush(
                        pattern,
                        inputs,
                        ejectionDirection,
                        Process.FOCUSING,
                        focusing.holder().id(),
                        focusing.expectedItems());
            }
        }
        return reject(Refusal.UNKNOWN_RECIPE, "no spirit infusion, focusing or runeworking recipe matches the pattern");
    }

    private boolean stagePush(
            IPatternDetails pattern,
            KeyCounter[] inputs,
            Direction ejectionDirection,
            Process process,
            ResourceLocation recipe,
            Map<AEItemKey, Long> expectedItems) {
        List<GenericStack> outputs = pattern.getOutputs();
        if (outputs.isEmpty() || !(outputs.get(0).what() instanceof AEItemKey outputKey)) {
            return reject(Refusal.NO_ITEM_OUTPUT, "pattern has no item output");
        }
        AEItemKey producedItem = outputKey;
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
                    return reject(Refusal.ITEMS_ONLY, "the Matrix takes items only");
                }
                items.merge(itemKey, entry.getLongValue(), Long::sum);
            }
        }
        if (items.size() > MatrixGridNode.TRACKER_SIZE) {
            return reject(
                    Refusal.TOO_MANY_INPUTS,
                    "pattern needs more than " + MatrixGridNode.TRACKER_SIZE + " distinct inputs");
        }
        if (!items.equals(expectedItems)) {
            return reject(
                    Refusal.MISMATCHED_INPUTS,
                    "pushed items " + items + " do not match the recipe's inputs " + expectedItems);
        }
        if (!stageAll(items)) {
            return reject(Refusal.BUFFER_FULL, "matrix buffer is full");
        }

        returnDirections.record(producedItem, ejectionDirection);
        captureTemplate(producedItem, items, recipe, process);
        owed.merge(producedItem, 1L, Long::sum);
        pushingCpus.record();
        clearRefusal();
        markScanNeeded();
        setChanged();
        if (NepConfig.debugLogging()) {
            Nep.LOGGER.info("Focused Spirit Matrix {} accepted pattern for {}", getBlockPos(), outputKey);
        }
        return true;
    }

    private boolean stageAll(Map<AEItemKey, Long> items) {
        List<ItemStack> toInput = new ArrayList<>();
        List<ItemStack> toBank = new ArrayList<>();
        AEItemKey jokerClaim = jokerClaim(items);
        for (Map.Entry<AEItemKey, Long> entry : items.entrySet()) {
            AEItemKey key = entry.getKey();
            List<ItemStack> target = banks(key, jokerClaim) ? toBank : toInput;
            long count = entry.getValue();
            int maxStack = key.toStack().getMaxStackSize();
            while (count > 0) {
                int chunk = (int) Math.min(count, maxStack);
                target.add(SpiritBankStorage.stackOf(key, chunk));
                count -= chunk;
            }
        }
        if (!fits(inputBuffer, toInput) || !fits(spirits, toBank)) {
            return false;
        }
        for (ItemStack stack : toInput) {
            ItemHandlerHelper.insertItem(inputBuffer, stack, false);
        }
        for (ItemStack stack : toBank) {
            ItemHandlerHelper.insertItem(spirits, stack, false);
        }
        return true;
    }

    @Nullable
    private AEItemKey jokerClaim(Map<AEItemKey, Long> items) {
        ItemStack held = spirits.getStackInSlot(SpiritBank.JOKER_SLOT);
        if (!held.isEmpty()) {
            return AEItemKey.of(held);
        }
        AEItemKey best = null;
        long bestCount = 0;
        for (Map.Entry<AEItemKey, Long> entry : items.entrySet()) {
            ItemStack stack = entry.getKey().toStack();
            if (!SpiritBank.isSpirit(stack) || SpiritBank.hasDedicatedSlot(stack)) {
                continue;
            }
            boolean better = best == null
                    || entry.getValue() > bestCount
                    || (entry.getValue() == bestCount
                            && entry.getKey().toString().compareTo(best.toString()) < 0);
            if (better) {
                best = entry.getKey();
                bestCount = entry.getValue();
            }
        }
        return best;
    }

    private static boolean banks(AEItemKey key, @Nullable AEItemKey jokerClaim) {
        ItemStack stack = key.toStack();
        return SpiritBank.hasDedicatedSlot(stack) || (SpiritBank.isSpirit(stack) && key.equals(jokerClaim));
    }

    private static boolean fits(ItemStackHandler handler, List<ItemStack> stacks) {
        if (stacks.isEmpty()) {
            return true;
        }
        ItemStackHandler probe = new OverstackedItemHandler(handler.getSlots()) {
            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                return handler.isItemValid(slot, stack);
            }

            @Override
            public int getSlotLimit(int slot) {
                return handler.getSlotLimit(slot);
            }
        };
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            probe.setStackInSlot(slot, handler.getStackInSlot(slot).copy());
        }
        for (ItemStack stack : stacks) {
            if (!ItemHandlerHelper.insertItem(probe, stack.copy(), false).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private void captureTemplate(
            AEItemKey output, Map<AEItemKey, Long> items, ResourceLocation recipe, Process process) {
        List<AEKey> keys = new ArrayList<>(items.size());
        List<Long> counts = new ArrayList<>(items.size());
        for (Map.Entry<AEItemKey, Long> entry : items.entrySet()) {
            keys.add(entry.getKey());
            counts.add(entry.getValue());
        }
        templates.put(output, new Template(List.copyOf(keys), List.copyOf(counts), recipe, process));
    }

    private void autoRequest(Level level) {
        RestockPlan plan = restockPlan();
        if (plan == null) {
            setMissingInputs(List.of());
            return;
        }
        List<GenericStack> unmet = new ArrayList<>();
        for (GenericStack stack : power.restock(level, plan.keys(), plan.targets())) {
            Long need = plan.needs().get(stack.what());
            if (need == null) {
                continue;
            }
            long shortfall = need - bufferedAmount(stack.what());
            if (shortfall > 0) {
                unmet.add(new GenericStack(stack.what(), shortfall));
            }
        }
        setMissingInputs(unmet);
    }

    /** Spirits are named by the bank's fixed layout rather than by what it holds, so a slot drained to zero still restocks. */
    @Nullable
    RestockPlan restockPlan() {
        boolean pullIngredients = NepConfig.malumFocusedSpiritMatrixAutoRequest() && !owed.isEmpty();
        if (!pullIngredients && !spiritRestock) {
            return null;
        }
        List<AEKey> keys = new ArrayList<>();
        List<Long> targets = new ArrayList<>();
        if (pullIngredients) {
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
        }
        int ingredientKeys = keys.size();
        Map<AEKey, Long> needs = new HashMap<>();
        for (int i = 0; i < ingredientKeys; i++) {
            needs.put(keys.get(i), targets.get(i));
        }
        if (spiritRestock) {
            long stock = NepConfig.malumFocusedSpiritMatrixSpiritStock();
            for (int slot = 0; slot < SpiritBank.SLOTS; slot++) {
                if (slot == SpiritBank.JOKER_SLOT) {
                    continue;
                }
                AEItemKey key = AEItemKey.of(SpiritBank.spiritFor(slot));
                int existing = keys.indexOf(key);
                if (existing >= 0) {
                    targets.set(existing, Math.max(targets.get(existing), stock));
                } else {
                    keys.add(key);
                    targets.add(stock);
                }
            }
        }
        if (keys.isEmpty()) {
            return null;
        }
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < keys.size(); i++) {
            order.add(i);
        }
        order.sort(Comparator.comparingInt((Integer i) -> i < ingredientKeys ? 0 : 1)
                .thenComparing(i -> keys.get(i).toString()));
        List<AEKey> stableKeys = new ArrayList<>(keys.size());
        List<Long> stableTargets = new ArrayList<>(targets.size());
        for (int i : order) {
            stableKeys.add(keys.get(i));
            stableTargets.add(targets.get(i));
        }
        return new RestockPlan(stableKeys, stableTargets, Map.copyOf(needs));
    }

    int manualDemandFor(ItemStack stack) {
        AEItemKey key = AEItemKey.of(stack);
        if (key == null || spirits.isBanked(stack)) {
            return 0;
        }
        return reportedDemandFor(key);
    }

    @Override
    public long bufferedAmount(AEKey key) {
        long total = super.bufferedAmount(key);
        return key instanceof AEItemKey itemKey ? total + spiritAmount(itemKey) : total;
    }

    @Override
    public long acceptCrafted(AEKey what, long amount, Actionable mode) {
        if (what instanceof AEItemKey key && spirits.isBanked(key.toStack())) {
            return amount - bankSpirits(key, amount, mode);
        }
        return super.acceptCrafted(what, amount, mode);
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
            Nep.LOGGER.info("Focused Spirit Matrix {} rejected pattern: {}", getBlockPos(), reason);
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

    private void setStall(Stall reason) {
        if (stall != reason) {
            stall = reason;
            setChanged();
        }
    }

    private void refreshVisualState(Level level) {
        BlockState state = getBlockState();
        if (!state.hasProperty(FocusedSpiritMatrixBlock.STATUS)) {
            return;
        }
        MatrixStatus desired = desiredStatus();
        if (state.getValue(FocusedSpiritMatrixBlock.STATUS) != desired) {
            level.setBlock(
                    getBlockPos(), state.setValue(FocusedSpiritMatrixBlock.STATUS, desired), Block.UPDATE_CLIENTS);
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
    protected int readoutSignature() {
        int hash = activeResult.isEmpty() ? 0 : activeResult.getItem().hashCode();
        hash = 31 * hash + Math.round(craftProgress() * 128.0F);
        hash = 31 * hash + Boolean.hashCode(outputBlocked);
        hash = 31 * hash + Boolean.hashCode(powerFault);
        hash = 31 * hash + missingInputs.hashCode();
        hash = 31 * hash + stall.ordinal();
        hash = 31 * hash + refusal.ordinal();
        hash = 31 * hash + Long.hashCode(pendingJobs());
        hash = 31 * hash + upgradeCount;
        hash = 31 * hash + catalyzerCount;
        hash = 31 * hash + activeProcess.ordinal();
        hash = 31 * hash + activeCraftTicks;
        hash = 31 * hash + impetusSlot.getStackInSlot(0).getDamageValue();
        hash = 31 * hash + Boolean.hashCode(hasImpetus());
        hash = 31 * hash + Boolean.hashCode(spiritRestock);
        for (int slot = 0; slot < spirits.getSlots(); slot++) {
            ItemStack stack = spirits.getStackInSlot(slot);
            hash = 31 * hash + (stack.isEmpty() ? 0 : stack.getItem().hashCode() * 31 + stack.getCount());
        }
        return hash;
    }

    @Override
    public float craftProgress() {
        if (activeResult.isEmpty()) {
            return 0.0F;
        }
        int craftTicks = craftTicks();
        return craftTicks <= 0 ? 1.0F : Math.min(1.0F, progress / (float) craftTicks);
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
                    "Focused Spirit Matrix {} cleared {} pending craft(s) and cancelled {} network job(s)",
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

    void dropBuffers(Level level, BlockPos pos) {
        for (int slot = 0; slot < inputBuffer.getSlots(); slot++) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), inputBuffer.getStackInSlot(slot));
        }
        for (int slot = 0; slot < outputBuffer.getSlots(); slot++) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), outputBuffer.getStackInSlot(slot));
        }
        for (int slot = 0; slot < spirits.getSlots(); slot++) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), spirits.getStackInSlot(slot));
        }
        for (int slot = 0; slot < upgradeSlot.getSlots(); slot++) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), upgradeSlot.getStackInSlot(slot));
        }
        for (int slot = 0; slot < catalyzerSlot.getSlots(); slot++) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), catalyzerSlot.getStackInSlot(slot));
        }
        for (int slot = 0; slot < impetusSlot.getSlots(); slot++) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), impetusSlot.getStackInSlot(slot));
        }
        for (ItemStack stack : claimedItems) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
        }
        clearContent();
    }

    @Override
    public void clearContent() {
        super.clearContent();
        clearHandler(upgradeSlot);
        clearHandler(catalyzerSlot);
        clearHandler(impetusSlot);
        clearHandler(spirits);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put(INPUT_KEY, inputBuffer.serializeNBT(registries));
        tag.put(OUTPUT_KEY, outputBuffer.serializeNBT(registries));
        tag.put(UPGRADE_KEY, upgradeSlot.serializeNBT(registries));
        tag.put(CATALYZER_KEY, catalyzerSlot.serializeNBT(registries));
        tag.put(IMPETUS_KEY, impetusSlot.serializeNBT(registries));
        tag.putString(ACTIVE_PROCESS_KEY, activeProcess.name());
        tag.putInt(ACTIVE_TICKS_KEY, activeCraftTicks);
        tag.putInt("ActiveDurability", activeDurabilityCost);
        tag.put(SPIRITS_KEY, spirits.serializeNBT(registries));
        tag.putBoolean(SPIRIT_RESTOCK_KEY, spiritRestock);
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
                claimed.add(OverstackedItemHandler.saveStack(stack, registries));
            }
        }
        tag.put(CLAIMED_KEY, claimed);
        tag.putInt(PROGRESS_KEY, progress);
        tag.putInt(CHAINING_KEY, chainedCrafts);
        tag.putBoolean(BLOCKED_KEY, outputBlocked);
        tag.putBoolean(POWER_FAULT_KEY, powerFault);
        tag.putString(STALL_KEY, stall.name());
        tag.putString(REFUSAL_KEY, refusal.name());
        tag.putInt(REFUSAL_TICKS_KEY, refusalTicks);
        tag.putInt(PRESERVATION_KEY, preservationTicks);
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
        upgradeSlot.deserializeNBT(registries, tag.getCompound(UPGRADE_KEY));
        catalyzerSlot.deserializeNBT(registries, tag.getCompound(CATALYZER_KEY));
        impetusSlot.deserializeNBT(registries, tag.getCompound(IMPETUS_KEY));
        activeProcess = byName(Process.values(), tag.getString(ACTIVE_PROCESS_KEY), Process.INFUSION);
        activeCraftTicks = tag.getInt(ACTIVE_TICKS_KEY);
        activeDurabilityCost = tag.getInt("ActiveDurability");
        loadBuffer(spirits, registries, tag.getCompound(SPIRITS_KEY));
        spiritRestock = !tag.contains(SPIRIT_RESTOCK_KEY) || tag.getBoolean(SPIRIT_RESTOCK_KEY);
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
            OverstackedItemHandler.parseStack(registries, claimed.getCompound(i))
                    .ifPresent(claimedItems::add);
        }
        progress = tag.getInt(PROGRESS_KEY);
        chainedCrafts = tag.getInt(CHAINING_KEY);
        outputBlocked = tag.getBoolean(BLOCKED_KEY);
        powerFault = tag.getBoolean(POWER_FAULT_KEY);
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
        preservationTicks = Math.max(0, tag.getInt(PRESERVATION_KEY));
        refreshUpgrades();
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
            tag.putString("Process", entry.getValue().process().name());
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
                Process process = byName(Process.values(), tag.getString("Process"), Process.INFUSION);
                templates.put(item, new Template(List.copyOf(keys), List.copyOf(counts), recipe, process));
            }
        }
    }

    @Override
    public Component getDisplayName() {
        return NepMalumContent.MATRIX.get().getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory inventory, Player player) {
        return new FocusedSpiritMatrixMenu(windowId, inventory, getBlockPos());
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
