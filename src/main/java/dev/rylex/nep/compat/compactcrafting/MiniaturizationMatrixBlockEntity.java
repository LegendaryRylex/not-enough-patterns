package dev.rylex.nep.compat.compactcrafting;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import dev.compactmods.crafting.api.field.MiniaturizationFieldSize;
import dev.compactmods.crafting.recipes.MiniaturizationRecipe;
import dev.rylex.nep.Nep;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.compat.compactcrafting.MiniaturizationMatrixBlock.MatrixStatus;
import dev.rylex.nep.machine.BufferedMatrixBlockEntity;
import dev.rylex.nep.machine.MachineItemView;
import dev.rylex.nep.machine.MatrixGridNode;
import dev.rylex.nep.machine.RedstoneMode;
import dev.rylex.nep.pattern.MiniaturizationPattern;
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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
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

public class MiniaturizationMatrixBlockEntity extends BufferedMatrixBlockEntity {

    static final int INPUT_SLOTS = 27;
    static final int OUTPUT_SLOTS = 9;

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
    private static final String ACTIVE_KEY = "Active";
    private static final String CLAIMED_KEY = "Claimed";
    private static final String PROGRESS_KEY = "Progress";
    private static final String CRAFT_TICKS_KEY = "CraftTicks";
    private static final String REDSTONE_MODE_KEY = "RedstoneMode";
    private static final String MISSING_KEY = "Missing";
    private static final String BLOCKED_KEY = "Blocked";
    private static final String POWER_FAULT_KEY = "PowerFault";
    private static final String STALL_KEY = "Stall";
    private static final String REFUSAL_KEY = "Refusal";
    private static final String REFUSAL_TICKS_KEY = "RefusalTicks";

    private final Map<Item, Template> templates = new HashMap<>();
    private final IItemHandler outputView = new OutputView();
    private final IItemHandler machineView =
            MachineItemView.demandLimited(inputBuffer, outputView, this::manualDemandFor);

    private int progress;
    private int craftTicks;

    private Stall stall = Stall.NONE;
    private Refusal refusal = Refusal.NONE;
    private int refusalTicks;

    enum Stall {
        NONE,
        INGREDIENTS,
        OUTPUT_FULL,
        FIELD_TOO_LARGE,
        NO_RECIPE;

        private static Stall worse(Stall first, Stall second) {
            return first.ordinal() >= second.ordinal() ? first : second;
        }
    }

    enum Refusal {
        NONE,
        NOT_A_MINIATURIZATION_PATTERN,
        NO_ITEM_OUTPUT,
        UNKNOWN_RECIPE,
        MIXED_RECIPES,
        FIELD_TOO_LARGE,
        ITEMS_ONLY,
        TOO_MANY_INPUTS,
        BUFFER_FULL
    }

    private record Template(
            List<AEKey> keys, List<Long> counts, @Nullable ResourceLocation recipe) {}

    public MiniaturizationMatrixBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(
                type,
                pos,
                state,
                INPUT_SLOTS,
                OUTPUT_SLOTS,
                NepCompactCraftingContent.MATRIX_ITEM.get(),
                NepConfig.compactCraftingMatrixIdleMeDrain(),
                "Miniaturization matrix");
    }

    public static void serverTick(
            Level level, BlockPos pos, BlockState state, MiniaturizationMatrixBlockEntity matrix) {
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
        if (!NepConfig.compactCraftingMiniaturizationMatrix()) {
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
                if (progress >= Math.max(1, craftTicks)) {
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
        hash = 31 * hash + Math.round(craftProgress() * 128.0F);
        hash = 31 * hash + Boolean.hashCode(outputBlocked);
        hash = 31 * hash + Boolean.hashCode(powerFault);
        hash = 31 * hash + missingInputs.hashCode();
        hash = 31 * hash + stall.ordinal();
        hash = 31 * hash + refusal.ordinal();
        hash = 31 * hash + craftTicks;
        hash = 31 * hash + Long.hashCode(pendingJobs());
        hash = 31 * hash + redstoneMode.ordinal();
        return hash;
    }

    private boolean drawActivePower() {
        double extra = NepConfig.compactCraftingMatrixMeDrain() - NepConfig.compactCraftingMatrixIdleMeDrain();
        if (extra <= 0) {
            power.clearStarved();
            return true;
        }
        power.extractPower(extra);
        return !power.isStarved();
    }

    @Nullable
    private RecipeHolder<MiniaturizationRecipe> recipeFor(Level level, Item output) {
        Template template = templates.get(output);
        if (template != null && template.recipe() != null) {
            RecipeHolder<MiniaturizationRecipe> holder =
                    MiniaturizationRecipeResolver.resolveById(level, template.recipe());
            if (holder != null) {
                return holder;
            }
        }
        return MiniaturizationRecipeResolver.resolveByOutputItem(level, output);
    }

    private boolean beginCraft(Level level) {
        Stall reason = Stall.NONE;
        for (Item wanted : List.copyOf(owed.keySet())) {
            if (owed.getOrDefault(wanted, 0L) <= 0) {
                continue;
            }
            RecipeHolder<MiniaturizationRecipe> holder = recipeFor(level, wanted);
            if (holder == null) {
                reason = Stall.worse(reason, Stall.NO_RECIPE);
                continue;
            }
            Stall attempt = claimIngredients(holder);
            if (attempt == Stall.NONE) {
                setStall(Stall.NONE);
                return true;
            }
            reason = Stall.worse(reason, attempt);
        }
        setStall(owed.isEmpty() ? Stall.NONE : reason);
        return false;
    }

    private Stall claimIngredients(RecipeHolder<MiniaturizationRecipe> holder) {
        MiniaturizationRecipe recipe = holder.value();
        if (!fitsFieldCeiling(recipe)) {
            return Stall.FIELD_TOO_LARGE;
        }
        GenericStack output = MiniaturizationRecipeIngredients.singleOutput(recipe);
        if (output == null || !(output.what() instanceof AEItemKey key)) {
            return Stall.NO_RECIPE;
        }
        ItemStack result = key.toStack((int) output.amount());
        if (!fitsInOutput(result)) {
            return Stall.OUTPUT_FULL;
        }
        List<MiniaturizationRecipeIngredients.Demand> demands = MiniaturizationRecipeIngredients.demandOf(recipe);
        if (demands == null) {
            return Stall.NO_RECIPE;
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
        craftTicks = craftTicksFor(recipe);
        progress = 0;
        setChanged();
        if (NepConfig.debugLogging()) {
            Nep.LOGGER.info(
                    "Miniaturization matrix {} started crafting {} x{} over {} ticks",
                    getBlockPos(),
                    activeResult.getItem(),
                    activeResult.getCount(),
                    craftTicks);
        }
        return Stall.NONE;
    }

    private static int craftTicksFor(MiniaturizationRecipe recipe) {
        long scaled =
                (long) Math.max(1, recipe.getCraftingTime()) * NepConfig.compactCraftingMatrixCraftTimePercent() / 100L;
        return (int) Mth.clamp(scaled, 1L, Integer.MAX_VALUE);
    }

    static boolean fitsFieldCeiling(MiniaturizationRecipe recipe) {
        return recipe.fitsInFieldSize(maximumFieldSize());
    }

    static MiniaturizationFieldSize maximumFieldSize() {
        String configured = NepConfig.compactCraftingMatrixMaximumFieldSize();
        for (MiniaturizationFieldSize size : MiniaturizationFieldSize.VALID_SIZES) {
            if (size.getSerializedName().equalsIgnoreCase(configured)) {
                return size;
            }
        }
        return MiniaturizationFieldSize.maximum();
    }

    private void setStall(Stall reason) {
        if (stall != reason) {
            stall = reason;
            setChanged();
        }
    }

    @Nullable
    private List<int[]> planConsumption(List<MiniaturizationRecipeIngredients.Demand> demands) {
        int[] slotLeft = new int[inputBuffer.getSlots()];
        for (int slot = 0; slot < slotLeft.length; slot++) {
            slotLeft[slot] = inputBuffer.getStackInSlot(slot).getCount();
        }
        List<int[]> takes = new ArrayList<>();
        for (MiniaturizationRecipeIngredients.Demand demand : demands) {
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

    private void finishCraft(Level level) {
        ItemStack result = activeResult.copy();
        if (!fitsInOutput(result)) {
            outputBlocked = true;
            return;
        }
        outputBlocked = false;
        ItemHandlerHelper.insertItem(outputBuffer, result.copy(), false);
        Item produced = result.getItem();
        if (owed.getOrDefault(produced, 0L) > 0) {
            decrement(owed, produced, 1);
            if (!owed.containsKey(produced)) {
                templates.remove(produced);
            }
            if (owed.isEmpty()) {
                pushingCpus.clear();
            }
            toReturn.merge(produced, (long) result.getCount(), Long::sum);
        }
        completeCraft();
        flushOutput(level);
        if (NepConfig.debugLogging()) {
            Nep.LOGGER.info(
                    "Miniaturization matrix {} crafted {} x{} ({} still owed)",
                    getBlockPos(),
                    produced,
                    result.getCount(),
                    owed.getOrDefault(produced, 0L));
        }
    }

    private void completeCraft() {
        activeResult = ItemStack.EMPTY;
        claimedItems.clear();
        progress = 0;
        craftTicks = 0;
        outputBlocked = false;
        markScanNeeded();
        setChanged();
    }

    boolean readyForPatterns() {
        return NepConfig.compactCraftingMiniaturizationMatrix();
    }

    boolean pushMatrixPattern(IPatternDetails pattern, KeyCounter[] inputs, Direction ejectionDirection) {
        Level level = getLevel();
        if (level == null || level.isClientSide || !NepConfig.compactCraftingMiniaturizationMatrix()) {
            return false;
        }
        if (!(pattern instanceof MiniaturizationPattern miniaturization)) {
            return reject(Refusal.NOT_A_MINIATURIZATION_PATTERN, "pattern is not a miniaturization pattern");
        }
        List<GenericStack> outputs = pattern.getOutputs();
        if (outputs.isEmpty() || !(outputs.get(0).what() instanceof AEItemKey outputKey)) {
            return reject(Refusal.NO_ITEM_OUTPUT, "pattern has no item output");
        }
        RecipeHolder<MiniaturizationRecipe> holder =
                MiniaturizationRecipeResolver.resolveById(level, miniaturization.recipe());
        if (holder == null) {
            return reject(Refusal.UNKNOWN_RECIPE, "no miniaturization recipe with id " + miniaturization.recipe());
        }
        if (!fitsFieldCeiling(holder.value())) {
            return reject(
                    Refusal.FIELD_TOO_LARGE,
                    "recipe needs a "
                            + MiniaturizationRecipeResolver.fieldSizeOf(holder.value())
                                    .getSerializedName()
                            + " field, above the Matrix's configured maximum of "
                            + maximumFieldSize().getSerializedName());
        }
        Item producedItem = outputKey.getItem();
        Template existing = templates.get(producedItem);
        if (existing != null && !miniaturization.recipe().equals(existing.recipe())) {
            return reject(
                    Refusal.MIXED_RECIPES,
                    "a craft of " + producedItem + " from " + existing.recipe() + " is already queued");
        }

        Map<AEItemKey, Long> items = new HashMap<>();
        for (KeyCounter counter : inputs) {
            for (var entry : counter) {
                if (!(entry.getKey() instanceof AEItemKey itemKey) || entry.getLongValue() <= 0) {
                    return reject(Refusal.ITEMS_ONLY, "miniaturization crafting takes items only");
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
        captureTemplate(producedItem, items, miniaturization.recipe());
        owed.merge(producedItem, 1L, Long::sum);
        pushingCpus.record();
        clearRefusal();
        markScanNeeded();
        setChanged();
        if (NepConfig.debugLogging()) {
            Nep.LOGGER.info("Miniaturization matrix {} accepted pattern for {}", getBlockPos(), outputKey);
        }
        return true;
    }

    private void captureTemplate(Item output, Map<AEItemKey, Long> items, ResourceLocation recipe) {
        List<AEKey> keys = new ArrayList<>(items.size());
        List<Long> counts = new ArrayList<>(items.size());
        for (Map.Entry<AEItemKey, Long> entry : items.entrySet()) {
            keys.add(entry.getKey());
            counts.add(entry.getValue());
        }
        templates.put(output, new Template(List.copyOf(keys), List.copyOf(counts), recipe));
    }

    private void autoRequest(Level level) {
        if (!NepConfig.compactCraftingMatrixAutoRequest()) {
            setMissingInputs(List.of());
            return;
        }
        List<AEKey> keys = new ArrayList<>();
        List<Long> targets = new ArrayList<>();
        for (Map.Entry<Item, Long> entry : owed.entrySet()) {
            Template template = templates.get(entry.getKey());
            if (template == null) {
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
        for (Map.Entry<Item, Long> entry : owed.entrySet()) {
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
            Nep.LOGGER.info("Miniaturization matrix {} rejected pattern: {}", getBlockPos(), reason);
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

    @Override
    protected int comparatorOutput() {
        return switch (redstoneMode) {
            case OUTPUT -> RedstoneMode.fullness(outputBuffer);
            case INPUT -> RedstoneMode.fullness(inputBuffer);
            case STATUS -> statusSignal();
        };
    }

    private void refreshVisualState(Level level) {
        BlockState state = getBlockState();
        if (!state.hasProperty(MiniaturizationMatrixBlock.STATUS)) {
            return;
        }
        MatrixStatus desired = desiredStatus();
        if (state.getValue(MiniaturizationMatrixBlock.STATUS) != desired) {
            level.setBlock(
                    getBlockPos(), state.setValue(MiniaturizationMatrixBlock.STATUS, desired), Block.UPDATE_CLIENTS);
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
        if (activeResult.isEmpty() || craftTicks <= 0) {
            return 0.0F;
        }
        return Math.min(1.0F, progress / (float) craftTicks);
    }

    int craftTicks() {
        return craftTicks;
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
                    "Miniaturization matrix {} cleared {} pending craft(s) and cancelled {} network job(s)",
                    getBlockPos(),
                    owed.size(),
                    cancelled);
        }
        owed.clear();
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
        tag.put(OWED_KEY, ItemCounts.save(owed));
        tag.put(RETURN_KEY, ItemCounts.save(toReturn));
        tag.put(RETURN_DIRS_KEY, returnDirections.save());
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
        tag.putInt(PROGRESS_KEY, progress);
        tag.putInt(CRAFT_TICKS_KEY, craftTicks);
        tag.putString(REDSTONE_MODE_KEY, redstoneMode.name());
        tag.putBoolean(BLOCKED_KEY, outputBlocked);
        tag.putBoolean(POWER_FAULT_KEY, powerFault);
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
        ItemCounts.load(owed, tag, OWED_KEY);
        ItemCounts.load(toReturn, tag, RETURN_KEY);
        returnDirections.load(tag, RETURN_DIRS_KEY, "ReturnDir", toReturn.keySet());
        loadTemplates(tag.getList(TEMPLATE_KEY, Tag.TAG_COMPOUND), registries);
        activeResult = tag.contains(ACTIVE_KEY)
                ? ItemStack.parse(registries, tag.getCompound(ACTIVE_KEY)).orElse(ItemStack.EMPTY)
                : ItemStack.EMPTY;
        claimedItems.clear();
        ListTag claimed = tag.getList(CLAIMED_KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < claimed.size(); i++) {
            ItemStack.parse(registries, claimed.getCompound(i)).ifPresent(claimedItems::add);
        }
        progress = tag.getInt(PROGRESS_KEY);
        craftTicks = tag.getInt(CRAFT_TICKS_KEY);
        redstoneMode = RedstoneMode.byName(tag.getString(REDSTONE_MODE_KEY));
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
        for (Map.Entry<Item, Template> entry : templates.entrySet()) {
            CompoundTag tag = new CompoundTag();
            tag.putString("Id", BuiltInRegistries.ITEM.getKey(entry.getKey()).toString());
            ListTag stacks = new ListTag();
            for (int i = 0; i < entry.getValue().keys().size(); i++) {
                stacks.add(GenericStack.writeTag(
                        registries,
                        new GenericStack(
                                entry.getValue().keys().get(i),
                                entry.getValue().counts().get(i))));
            }
            tag.put("Stacks", stacks);
            if (entry.getValue().recipe() != null) {
                tag.putString("Recipe", entry.getValue().recipe().toString());
            }
            list.add(tag);
        }
        return list;
    }

    private void loadTemplates(ListTag list, HolderLookup.Provider registries) {
        templates.clear();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            Item item = ItemCounts.item(tag.getString("Id"));
            if (item == null) {
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
                                List.copyOf(keys),
                                List.copyOf(counts),
                                ResourceLocation.tryParse(tag.getString("Recipe"))));
            }
        }
    }

    @Override
    public Component getDisplayName() {
        return NepCompactCraftingContent.MATRIX.get().getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory inventory, Player player) {
        return new MiniaturizationMatrixMenu(windowId, inventory, getBlockPos());
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
            if (stack.isEmpty() || toReturn.getOrDefault(stack.getItem(), 0L) > 0) {
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
