package dev.rylex.nep.compat.occultism;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import dev.rylex.nep.Nep;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.compat.occultism.BoundSpiritTemplates.Template;
import dev.rylex.nep.compat.occultism.OccultismMatrixRecipes.Craft;
import dev.rylex.nep.compat.occultism.OccultismMatrixRecipes.Kind;
import dev.rylex.nep.compat.occultism.OccultismMatrixRecipes.Match;
import dev.rylex.nep.machine.BufferedMatrixBlockEntity;
import dev.rylex.nep.machine.CraftedOutputs;
import dev.rylex.nep.machine.MachineItemView;
import dev.rylex.nep.machine.ManualCraftHost;
import dev.rylex.nep.machine.ManualCraftOutcome;
import dev.rylex.nep.machine.ManualCraftResult;
import dev.rylex.nep.machine.ManualRequirement;
import dev.rylex.nep.machine.ManualStaging;
import dev.rylex.nep.machine.MatrixStatus;
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
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BoundSpiritMatrixBlockEntity extends BufferedMatrixBlockEntity implements ManualCraftHost {

    static final int INPUT_SLOTS = 21;
    static final int OUTPUT_SLOTS = 9;
    static final int CRUSHER_SLOT = 0;
    static final int CRYSTALLIZER_SLOT = 1;

    private static final int RESTOCK_INTERVAL = 10;
    private static final int RUNNING_GRACE_TICKS = 10;
    private static final int REFUSAL_MEMORY_TICKS = 200;

    private static final String INPUT_KEY = "Input";
    private static final String OUTPUT_KEY = "Output";
    private static final String BOOKS_KEY = "Books";
    private static final String OWED_KEY = "Owed";
    private static final String RETURN_KEY = "ToReturn";
    private static final String RETURN_DIRS_KEY = "ReturnDirs";
    private static final String TEMPLATE_KEY = "Template";
    private static final String NODE_KEY = "Node";
    private static final String ACTIVE_KEY = "Active";
    private static final String CLAIMED_KEY = "Claimed";
    private static final String PROGRESS_KEY = "Progress";
    private static final String TICKS_KEY = "Ticks";
    private static final String OPERATIONS_KEY = "Operations";
    private static final String MISSING_KEY = "Missing";
    private static final String BLOCKED_KEY = "Blocked";
    private static final String POWER_FAULT_KEY = "PowerFault";
    private static final String STALL_KEY = "Stall";
    private static final String REFUSAL_KEY = "Refusal";
    private static final String REFUSAL_TICKS_KEY = "RefusalTicks";
    private static final String ACTIVE_KIND_KEY = "ActiveKind";
    private static final String ACTIVE_OUTPUT_KEY = "ActiveOutput";

    private final Map<AEItemKey, Template> templates = new HashMap<>();
    private final IItemHandler outputView = new OutputView();
    private final IItemHandler machineView =
            MachineItemView.demandLimited(inputBuffer, outputView, this::manualDemandFor);
    private final ItemStackHandler books = new ItemStackHandler(2) {
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            BoundSpirit spirit = stack.get(NepOccultismContent.BOUND_SPIRIT.get());
            return stack.is(NepOccultismContent.SPIRIT_INFUSED_BOOK.get())
                    && spirit != null
                    && spirit.work() == workOf(slot);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            markScanNeeded();
            setChanged();
        }
    };

    private int progress;
    private int activeTicks;
    private int activeOperations;
    private Kind activeKind = Kind.SPIRIT_FIRE;

    @Nullable
    private AEItemKey activeOutput;

    private Stall stall = Stall.NONE;
    private Refusal refusal = Refusal.NONE;
    private int refusalTicks;

    enum Stall {
        NONE,
        INGREDIENTS,
        NO_SPIRIT,
        OUTPUT_FULL,
        NO_RECIPE;

        private static Stall worse(Stall first, Stall second) {
            return first.ordinal() >= second.ordinal() ? first : second;
        }
    }

    enum Refusal {
        NONE,
        NO_ITEM_OUTPUT,
        ONE_INPUT_ONLY,
        ITEMS_ONLY,
        UNKNOWN_RECIPE,
        NEEDS_CRUSHER,
        NEEDS_CRYSTALLIZER,
        SPIRIT_TOO_WEAK,
        MIXED_RECIPES,
        BUFFER_FULL
    }

    public BoundSpiritMatrixBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(
                type,
                pos,
                state,
                INPUT_SLOTS,
                OUTPUT_SLOTS,
                NepOccultismContent.MATRIX_ITEM.get(),
                NepConfig.occultismMatrixIdleMeDrain(),
                NepConfig.occultismMatrixChannels(),
                "Bound spirit matrix");
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BoundSpiritMatrixBlockEntity matrix) {
        matrix.tick(level);
    }

    private static BoundSpirit.Work workOf(int slot) {
        return slot == CRUSHER_SLOT ? BoundSpirit.Work.CRUSHER : BoundSpirit.Work.CRYSTALLIZER;
    }

    @Nullable
    BoundSpirit spirit(BoundSpirit.Work work) {
        return books.getStackInSlot(work == BoundSpirit.Work.CRUSHER ? CRUSHER_SLOT : CRYSTALLIZER_SLOT)
                .get(NepOccultismContent.BOUND_SPIRIT.get());
    }

    ItemStackHandler books() {
        return books;
    }

    private void tick(Level level) {
        if (runningGrace > 0) {
            runningGrace--;
        }
        if (refusalTicks > 0 && --refusalTicks == 0) {
            refusal = Refusal.NONE;
        }
        flushOutput(level);
        if (!NepConfig.occultismMatrix()) {
            setPowerFault(false);
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
            } else if (drawActivePower()) {
                progress++;
                runningGrace = RUNNING_GRACE_TICKS;
                if (progress >= activeTicks) {
                    finishCraft(level);
                }
            }
        }
        setPowerFault(!power.hasUsablePower());
        refreshComparator(level);
        refreshVisualState(level);
        syncIfChanged(level);
    }

    @Override
    protected int readoutSignature() {
        int hash = activeResult.isEmpty() ? 0 : activeResult.getItem().hashCode();
        hash = 31 * hash + activeKind.ordinal();
        hash = 31 * hash + Math.round(craftProgress() * 128.0F);
        hash = 31 * hash + activeTicks;
        hash = 31 * hash + Boolean.hashCode(outputBlocked);
        hash = 31 * hash + Boolean.hashCode(powerFault);
        hash = 31 * hash + missingInputs.hashCode();
        hash = 31 * hash + stall.ordinal();
        hash = 31 * hash + refusal.ordinal();
        hash = 31 * hash + Long.hashCode(pendingJobs());
        hash = 31 * hash + books.getStackInSlot(CRUSHER_SLOT).hashCode();
        hash = 31 * hash + books.getStackInSlot(CRYSTALLIZER_SLOT).hashCode();
        return hash;
    }

    private boolean drawActivePower() {
        double extra = NepConfig.occultismMatrixMeDrain() - NepConfig.occultismMatrixIdleMeDrain();
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
        BoundSpirit spirit =
                template.kind().work() == null ? null : spirit(template.kind().work());
        if (template.kind().work() != null && (spirit == null || spirit.tier().level() < template.tier())) {
            return Stall.NO_SPIRIT;
        }
        int slot = slotHolding(template.input());
        if (slot < 0) {
            return Stall.INGREDIENTS;
        }
        int operations = (int) Math.min(
                Math.min(OccultismMatrixRecipes.operations(spirit), owed.getOrDefault(wanted, 0L)),
                bufferedAmount(template.input()));
        Craft craft = OccultismMatrixRecipes.craft(
                level, template.kind(), template.recipe(), template.input().toStack(), spirit, operations);
        if (craft == null) {
            return Stall.NO_RECIPE;
        }
        if (!fitsInOutput(craft.result())) {
            return Stall.OUTPUT_FULL;
        }
        claimedItems.clear();
        int left = operations;
        for (int index = 0; index < inputBuffer.getSlots() && left > 0; index++) {
            if (template.input().matches(inputBuffer.getStackInSlot(index))) {
                ItemStack taken = inputBuffer.extractItem(index, left, false);
                left -= taken.getCount();
                claimedItems.add(taken);
            }
        }
        activeOperations = operations - left;
        activeResult = craft.result().copy();
        activeKind = template.kind();
        activeOutput = wanted;
        activeTicks = craft.ticks();
        progress = 0;
        setChanged();
        if (NepConfig.debugLogging()) {
            Nep.LOGGER.info(
                    "Bound spirit matrix {} started {} {} x{} over {} ticks",
                    getBlockPos(),
                    activeKind.id(),
                    activeResult.getItem(),
                    activeResult.getCount(),
                    activeTicks);
        }
        return Stall.NONE;
    }

    private int slotHolding(AEItemKey input) {
        for (int slot = 0; slot < inputBuffer.getSlots(); slot++) {
            if (input.matches(inputBuffer.getStackInSlot(slot))) {
                return slot;
            }
        }
        return -1;
    }

    private void setStall(Stall reason) {
        if (stall != reason) {
            stall = reason;
            setChanged();
        }
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
        AEItemKey owedFor = activeOutput != null ? activeOutput : produced;
        if (owedFor != null && owed.getOrDefault(owedFor, 0L) > 0) {
            boolean manual = isManualJob(owedFor);
            decrement(owed, owedFor, Math.max(1, activeOperations));
            if (manual) {
                decrement(manualOwed, owedFor, Math.max(1, activeOperations));
            }
            if (!owed.containsKey(owedFor)) {
                templates.remove(owedFor);
            }
            if (owed.isEmpty()) {
                pushingCpus.clear();
            }
            if (produced != null && !manual) {
                toReturn.merge(produced, (long) result.getCount(), Long::sum);
                if (!produced.equals(owedFor)) {
                    CraftedOutputs.expect(produced, owedFor);
                    Direction direction = returnDirections.directionFor(owedFor);
                    if (direction != null) {
                        returnDirections.record(produced, direction);
                    }
                }
            }
        }
        completeCraft();
        flushOutput(level);
        if (NepConfig.debugLogging()) {
            Nep.LOGGER.info(
                    "Bound spirit matrix {} made {} x{} ({} still owed)",
                    getBlockPos(),
                    produced,
                    result.getCount(),
                    owedFor == null ? 0L : owed.getOrDefault(owedFor, 0L));
        }
    }

    private void completeCraft() {
        activeResult = ItemStack.EMPTY;
        activeOutput = null;
        claimedItems.clear();
        progress = 0;
        activeTicks = 0;
        activeOperations = 0;
        outputBlocked = false;
        markScanNeeded();
        setChanged();
    }

    boolean readyForPatterns() {
        return NepConfig.occultismMatrix();
    }

    boolean pushMatrixPattern(IPatternDetails pattern, KeyCounter[] inputs, Direction ejectionDirection) {
        Level level = getLevel();
        if (level == null || level.isClientSide || !NepConfig.occultismMatrix()) {
            return false;
        }
        List<GenericStack> outputs = pattern.getOutputs();
        if (outputs.isEmpty() || !(outputs.get(0).what() instanceof AEItemKey outputKey)) {
            return reject(Refusal.NO_ITEM_OUTPUT, "pattern has no item output");
        }

        AEItemKey inputKey = null;
        long batch = 0;
        for (KeyCounter counter : inputs) {
            for (var entry : counter) {
                if (!(entry.getKey() instanceof AEItemKey itemKey) || entry.getLongValue() <= 0) {
                    return reject(Refusal.ITEMS_ONLY, "this machine takes items only");
                }
                if (inputKey != null && !inputKey.equals(itemKey)) {
                    return reject(Refusal.ONE_INPUT_ONLY, "every recipe this machine runs takes a single input");
                }
                inputKey = itemKey;
                batch += entry.getLongValue();
            }
        }
        if (inputKey == null) {
            return reject(Refusal.ITEMS_ONLY, "pattern pushed no items");
        }

        ItemStack input = inputKey.toStack();
        BoundSpirit crusher = spirit(BoundSpirit.Work.CRUSHER);
        BoundSpirit crystallizer = spirit(BoundSpirit.Work.CRYSTALLIZER);
        Match match = OccultismMatrixRecipes.find(level, input, outputKey.getItem(), crusher, crystallizer);
        if (match == null) {
            BoundSpirit.Work needed = OccultismMatrixRecipes.workNeeded(level, input, outputKey.getItem());
            if (needed == null) {
                return reject(Refusal.UNKNOWN_RECIPE, "no Occultism recipe turns " + inputKey + " into " + outputKey);
            }
            if (spirit(needed) != null) {
                return reject(Refusal.SPIRIT_TOO_WEAK, "the bound " + needed.getSerializedName() + " is too weak");
            }
            return reject(
                    needed == BoundSpirit.Work.CRUSHER ? Refusal.NEEDS_CRUSHER : Refusal.NEEDS_CRYSTALLIZER,
                    "no " + needed.getSerializedName() + " book is bound");
        }

        BoundSpirit spirit =
                match.kind().work() == null ? null : spirit(match.kind().work());
        Craft craft = OccultismMatrixRecipes.craft(level, match.kind(), match.recipe(), input, spirit, 1);
        if (craft == null) {
            return reject(Refusal.UNKNOWN_RECIPE, "recipe " + match.recipe() + " made nothing");
        }
        if (isManualJob(outputKey)) {
            return reject(Refusal.MIXED_RECIPES, "a manual craft of " + outputKey + " is already queued");
        }
        if ((long) craft.result().getCount() * batch < outputs.get(0).amount()) {
            return reject(
                    Refusal.SPIRIT_TOO_WEAK,
                    "pattern expects " + outputs.get(0).amount() + " but the spirit makes "
                            + craft.result().getCount() * batch);
        }

        Template existing = templates.get(outputKey);
        if (existing != null && !existing.recipe().equals(match.recipe())) {
            return reject(
                    Refusal.MIXED_RECIPES,
                    "a craft of " + outputKey + " from " + existing.recipe() + " is already queued");
        }
        if (!bufferAll(Map.of(inputKey, batch))) {
            return reject(Refusal.BUFFER_FULL, "matrix buffer is full");
        }

        int tier = spirit == null ? 0 : spirit.tier().level();
        returnDirections.record(outputKey, ejectionDirection);
        templates.put(
                outputKey,
                new Template(
                        match.kind(),
                        inputKey,
                        match.recipe(),
                        Math.max(tier, existing == null ? 0 : existing.tier())));
        owed.merge(outputKey, batch, Long::sum);
        pushingCpus.record();
        clearRefusal();
        markScanNeeded();
        setChanged();
        if (NepConfig.debugLogging()) {
            Nep.LOGGER.info(
                    "Bound spirit matrix {} accepted {} pattern for {} x{}",
                    getBlockPos(),
                    match.kind().id(),
                    outputKey,
                    batch);
        }
        return true;
    }

    private void autoRequest(Level level) {
        if (!NepConfig.occultismMatrixAutoRequest()) {
            setMissingInputs(List.of());
            return;
        }
        Map<AEItemKey, Long> wanted = new HashMap<>();
        for (Map.Entry<AEItemKey, Long> entry : owed.entrySet()) {
            Template template = templates.get(entry.getKey());
            if (template != null && !isManualJob(entry.getKey())) {
                wanted.merge(template.input(), entry.getValue(), Long::sum);
            }
        }
        if (wanted.isEmpty()) {
            setMissingInputs(List.of());
            return;
        }
        List<AEItemKey> sorted = new ArrayList<>(wanted.keySet());
        sorted.sort(Comparator.comparing(Object::toString));
        List<AEKey> keys = new ArrayList<>(sorted);
        List<Long> targets = new ArrayList<>();
        for (AEItemKey key : sorted) {
            targets.add(wanted.get(key));
        }
        setMissingInputs(power.restock(level, keys, targets));
    }

    @Override
    public ManualCraftOutcome startManualCraft(Player player, ResourceLocation recipe, int batches) {
        Level level = getLevel();
        if (level == null || level.isClientSide) {
            return ManualCraftOutcome.failed(ManualCraftResult.UNKNOWN_RECIPE);
        }
        if (!NepConfig.manualCrafting() || !NepConfig.occultismMatrix()) {
            return ManualCraftOutcome.failed(ManualCraftResult.DISABLED);
        }
        Kind kind = OccultismMatrixRecipes.kindOf(level, recipe);
        Ingredient ingredient = OccultismMatrixRecipes.ingredient(level, recipe);
        if (kind == null || ingredient == null || ingredient.isEmpty()) {
            return ManualCraftOutcome.failed(ManualCraftResult.UNKNOWN_RECIPE);
        }
        BoundSpirit spirit = kind.work() == null ? null : spirit(kind.work());
        if (kind.work() != null && spirit == null) {
            return ManualCraftOutcome.failed(ManualCraftResult.NO_SPIRIT);
        }

        ItemStack sample = ingredient.test(player.getMainHandItem())
                ? player.getMainHandItem().copyWithCount(1)
                : null;
        if (sample == null) {
            for (ItemStack stack : player.getInventory().items) {
                if (!stack.isEmpty() && ingredient.test(stack)) {
                    sample = stack.copyWithCount(1);
                    break;
                }
            }
        }
        if (sample == null) {
            return ManualCraftOutcome.failed(ManualCraftResult.MISSING_ITEMS);
        }
        Craft craft = OccultismMatrixRecipes.craft(level, kind, recipe, sample, spirit, 1);
        if (craft == null) {
            return ManualCraftOutcome.failed(
                    spirit == null ? ManualCraftResult.UNSUPPORTED : ManualCraftResult.NO_SPIRIT);
        }
        AEItemKey input = AEItemKey.of(sample);
        AEItemKey produced = AEItemKey.of(craft.result());
        Template existing = templates.get(produced);
        if (existing != null && (!recipe.equals(existing.recipe()) || !input.equals(existing.input()))) {
            return ManualCraftOutcome.failed(ManualCraftResult.BUSY);
        }
        if (owed.containsKey(produced) && !isManualJob(produced)) {
            return ManualCraftOutcome.failed(ManualCraftResult.BUSY);
        }

        ManualStaging.Result staged =
                pullFromPlayer(player, List.of(new ManualRequirement(Ingredient.of(sample), 1, true)), batches);
        if (staged.status() != ManualCraftResult.STARTED) {
            return ManualCraftOutcome.failed(staged.status());
        }
        int tier = spirit == null ? 0 : spirit.tier().level();
        templates.put(produced, new Template(kind, input, recipe, tier));
        owed.merge(produced, (long) staged.batches(), Long::sum);
        manualOwed.merge(produced, (long) staged.batches(), Long::sum);
        clearRefusal();
        markScanNeeded();
        setChanged();
        if (NepConfig.debugLogging()) {
            Nep.LOGGER.info(
                    "Bound spirit matrix {} queued {} manual craft(s) of {} for {}",
                    getBlockPos(),
                    staged.batches(),
                    produced,
                    player.getName().getString());
        }
        return ManualCraftOutcome.started(staged.batches(), craft.result());
    }

    int manualDemandFor(ItemStack stack) {
        AEItemKey key = AEItemKey.of(stack);
        if (key == null) {
            return 0;
        }
        long target = 0;
        for (Map.Entry<AEItemKey, Long> entry : owed.entrySet()) {
            Template template = templates.get(entry.getKey());
            if (template != null && template.input().equals(key)) {
                target += entry.getValue();
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
            Nep.LOGGER.info("Bound spirit matrix {} rejected pattern: {}", getBlockPos(), reason);
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
        if (!state.hasProperty(BoundSpiritMatrixBlock.STATUS)) {
            return;
        }
        MatrixStatus desired = desiredStatus();
        if (state.getValue(BoundSpiritMatrixBlock.STATUS) != desired) {
            level.setBlock(getBlockPos(), state.setValue(BoundSpiritMatrixBlock.STATUS, desired), Block.UPDATE_CLIENTS);
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
        return Math.min(1.0F, activeTicks <= 0 ? 1.0F : progress / (float) activeTicks);
    }

    int craftTicks() {
        return activeTicks;
    }

    Kind activeKind() {
        return activeKind;
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
                    "Bound spirit matrix {} cleared {} pending craft(s) and cancelled {} network job(s)",
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
        for (int slot = 0; slot < books.getSlots(); slot++) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), books.getStackInSlot(slot));
            books.setStackInSlot(slot, ItemStack.EMPTY);
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
        tag.put(BOOKS_KEY, books.serializeNBT(registries));
        tag.put(OWED_KEY, ItemCounts.save(owed, registries));
        tag.put(RETURN_KEY, ItemCounts.save(toReturn, registries));
        tag.put(RETURN_DIRS_KEY, returnDirections.save(registries));
        tag.put(TEMPLATE_KEY, BoundSpiritTemplates.save(templates, registries));
        if (!activeResult.isEmpty()) {
            tag.put(ACTIVE_KEY, activeResult.save(registries));
        }
        if (activeOutput != null) {
            CompoundTag output = new CompoundTag();
            ItemCounts.putKey(output, activeOutput, registries);
            tag.put(ACTIVE_OUTPUT_KEY, output);
        }
        ListTag claimed = new ListTag();
        for (ItemStack stack : claimedItems) {
            if (!stack.isEmpty()) {
                claimed.add(stack.save(registries));
            }
        }
        tag.put(CLAIMED_KEY, claimed);
        tag.putInt(PROGRESS_KEY, progress);
        tag.putInt(TICKS_KEY, activeTicks);
        tag.putInt(OPERATIONS_KEY, activeOperations);
        tag.putBoolean(BLOCKED_KEY, outputBlocked);
        tag.putBoolean(POWER_FAULT_KEY, powerFault);
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
        if (tag.contains(BOOKS_KEY)) {
            books.deserializeNBT(registries, tag.getCompound(BOOKS_KEY));
        }
        ItemCounts.load(owed, tag, OWED_KEY, registries);
        ItemCounts.load(toReturn, tag, RETURN_KEY, registries);
        returnDirections.load(tag, RETURN_DIRS_KEY, "ReturnDir", toReturn.keySet(), registries);
        BoundSpiritTemplates.load(templates, tag.getList(TEMPLATE_KEY, Tag.TAG_COMPOUND), registries);
        activeResult = tag.contains(ACTIVE_KEY)
                ? ItemStack.parse(registries, tag.getCompound(ACTIVE_KEY)).orElse(ItemStack.EMPTY)
                : ItemStack.EMPTY;
        activeOutput = tag.contains(ACTIVE_OUTPUT_KEY)
                ? ItemCounts.key(tag.getCompound(ACTIVE_OUTPUT_KEY), ItemCounts.LEGACY_ID_TAG, registries)
                : null;
        claimedItems.clear();
        ListTag claimed = tag.getList(CLAIMED_KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < claimed.size(); i++) {
            ItemStack.parse(registries, claimed.getCompound(i)).ifPresent(claimedItems::add);
        }
        progress = tag.getInt(PROGRESS_KEY);
        activeTicks = tag.getInt(TICKS_KEY);
        activeOperations = tag.getInt(OPERATIONS_KEY);
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
        stall = EnumNames.byName(Stall.values(), tag.getString(STALL_KEY), Stall.NONE);
        refusal = EnumNames.byName(Refusal.values(), tag.getString(REFUSAL_KEY), Refusal.NONE);
        refusalTicks = refusal == Refusal.NONE
                ? 0
                : Math.max(1, Math.min(REFUSAL_MEMORY_TICKS, tag.getInt(REFUSAL_TICKS_KEY)));
        activeKind = EnumNames.byName(Kind.values(), tag.getString(ACTIVE_KIND_KEY), Kind.SPIRIT_FIRE);
        markScanNeeded();
    }

    @Override
    public Component getDisplayName() {
        return NepOccultismContent.MATRIX.get().getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory inventory, Player player) {
        return new BoundSpiritMatrixMenu(windowId, inventory, getBlockPos());
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
