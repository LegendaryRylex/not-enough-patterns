package dev.rylex.nep.compat.mysticalagriculture;

import appeng.api.config.Actionable;
import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import com.blakebr0.mysticalagriculture.api.crafting.IAwakeningRecipe;
import com.blakebr0.mysticalagriculture.api.crafting.IInfusionRecipe;
import com.blakebr0.mysticalagriculture.util.RecipeIngredientCache;
import dev.rylex.nep.Nep;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.compat.mysticalagriculture.InfusedAwakeningMatrixBlock.MatrixStatus;
import dev.rylex.nep.machine.BufferedMatrixBlockEntity;
import dev.rylex.nep.machine.MachineItemView;
import dev.rylex.nep.machine.MatrixGridNode;
import dev.rylex.nep.machine.OverstackedItemHandler;
import dev.rylex.nep.pattern.AwakeningPattern;
import dev.rylex.nep.pattern.InfusionPattern;
import dev.rylex.nep.util.ItemCounts;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
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

public class InfusedAwakeningMatrixBlockEntity extends BufferedMatrixBlockEntity {

    static final int INPUT_SLOTS = 18;
    static final int OUTPUT_SLOTS = 9;
    static final int TANKS = 4;

    private static final int GRID_SIZE = 9;
    private static final int RESTOCK_INTERVAL = 10;
    private static final int RUNNING_GRACE_TICKS = 10;
    private static final int REFUSAL_MEMORY_TICKS = 200;

    private static final String INPUT_KEY = "Input";
    private static final String OUTPUT_KEY = "Output";
    private static final String TANKS_KEY = "Tanks";
    private static final String OWED_KEY = "Owed";
    private static final String RETURN_KEY = "ToReturn";
    private static final String RETURN_DIRS_KEY = "ReturnDirs";
    private static final String TEMPLATE_KEY = "Template";
    private static final String NODE_KEY = "Node";
    private static final String ACTIVE_KEY = "Active";
    private static final String CLAIMED_KEY = "Claimed";
    private static final String CLAIMED_ESSENCE_KEY = "ClaimedEssence";
    private static final String PROGRESS_KEY = "Progress";
    private static final String MISSING_KEY = "Missing";
    private static final String BLOCKED_KEY = "Blocked";
    private static final String POWER_FAULT_KEY = "PowerFault";
    private static final String STALL_KEY = "Stall";
    private static final String REFUSAL_KEY = "Refusal";
    private static final String REFUSAL_TICKS_KEY = "RefusalTicks";

    private final EssenceTank[] tanks = new EssenceTank[TANKS];

    private final Map<Item, Template> templates = new HashMap<>();
    private final IItemHandler outputView = new OutputView();
    private final IItemHandler machineView =
            MachineItemView.demandLimited(inputBuffer, outputView, this::manualDemandFor);

    private final List<GenericStack> claimedEssences = new ArrayList<>();
    private boolean drainNeeded = true;
    private int progress;

    private Stall stall = Stall.NONE;
    private Refusal refusal = Refusal.NONE;
    private int refusalTicks;

    enum Kind {
        INFUSION,
        AWAKENING
    }

    enum Stall {
        NONE,
        INGREDIENTS,
        ESSENCE,
        OUTPUT_FULL,
        NO_RECIPE;

        private static Stall worse(Stall first, Stall second) {
            return first.ordinal() >= second.ordinal() ? first : second;
        }
    }

    enum Refusal {
        NONE,
        NOT_A_MYSTICAL_PATTERN,
        NO_ITEM_OUTPUT,
        UNKNOWN_RECIPE,
        MISMATCHED_INPUTS,
        MIXED_RECIPES,
        MODULE_DISABLED,
        ITEMS_ONLY,
        TOO_MANY_INPUTS,
        BUFFER_FULL
    }

    private record Template(
            Kind kind,
            List<AEKey> keys,
            List<Long> counts,
            List<AEKey> essenceKeys,
            List<Long> essenceCounts,
            @Nullable ResourceLocation recipe) {}

    public InfusedAwakeningMatrixBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(
                type,
                pos,
                state,
                INPUT_SLOTS,
                OUTPUT_SLOTS,
                NepMysticalContent.MATRIX_ITEM.get(),
                NepConfig.mysticalInfusedAwakeningMatrixIdleMeDrain(),
                NepConfig.mysticalInfusedAwakeningMatrixChannels(),
                "Infused awakening matrix");
        for (int tank = 0; tank < TANKS; tank++) {
            tanks[tank] = new EssenceTank();
        }
    }

    public static void serverTick(
            Level level, BlockPos pos, BlockState state, InfusedAwakeningMatrixBlockEntity matrix) {
        matrix.tick(level);
    }

    @Override
    public void markScanNeeded() {
        super.markScanNeeded();
        drainNeeded = true;
    }

    private void tick(Level level) {
        if (runningGrace > 0) {
            runningGrace--;
        }
        if (refusalTicks > 0 && --refusalTicks == 0) {
            refusal = Refusal.NONE;
        }
        flushOutput(level);
        if (!NepConfig.mysticalInfusedAwakeningMatrix()) {
            setPowerFault(false);
            refreshComparator(level);
            refreshVisualState(level);
            syncIfChanged(level);
            return;
        }
        power.create(level, getBlockPos());
        drainEssencesToTanks();

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
                if (progress >= NepConfig.mysticalInfusedAwakeningMatrixCraftTicks()) {
                    finishCraft(level);
                }
            }
        }
        setPowerFault(!power.hasUsablePower());

        refreshComparator(level);
        refreshVisualState(level);
        syncIfChanged(level);
    }

    private boolean drawActivePower() {
        double extra = NepConfig.mysticalInfusedAwakeningMatrixMeDrain()
                - NepConfig.mysticalInfusedAwakeningMatrixIdleMeDrain();
        if (extra <= 0) {
            power.clearStarved();
            return true;
        }
        power.extractPower(extra);
        return !power.isStarved();
    }

    private void drainEssencesToTanks() {
        if (!drainNeeded) {
            return;
        }
        drainNeeded = false;
        for (int slot = 0; slot < inputBuffer.getSlots(); slot++) {
            ItemStack stack = inputBuffer.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            AEItemKey key = AEItemKey.of(stack);
            if (key == null || !isTankItem(stack, key)) {
                continue;
            }
            long spare = bufferedItemAmount(key) - ingredientNeed(key);
            if (spare <= 0) {
                continue;
            }
            long moved = insertIntoTanks(key, Math.min(stack.getCount(), spare), false);
            if (moved > 0) {
                inputBuffer.extractItem(slot, (int) moved, false);
            }
        }
    }

    private long bufferedItemAmount(AEItemKey key) {
        long total = 0;
        for (int slot = 0; slot < inputBuffer.getSlots(); slot++) {
            ItemStack stack = inputBuffer.getStackInSlot(slot);
            if (!stack.isEmpty() && key.matches(stack)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private long ingredientNeed(AEItemKey key) {
        long need = 0;
        for (Map.Entry<Item, Long> entry : owed.entrySet()) {
            Template template = templates.get(entry.getKey());
            if (template == null) {
                continue;
            }
            int index = template.keys().indexOf(key);
            if (index >= 0) {
                need += template.counts().get(index) * entry.getValue();
            }
        }
        return need;
    }

    private boolean isTankItem(ItemStack stack, AEItemKey key) {
        if (RecipeIngredientCache.INSTANCE.isValidVesselItem(stack)) {
            return true;
        }
        for (Template template : templates.values()) {
            if (template.essenceKeys().contains(key)) {
                return true;
            }
        }
        return false;
    }

    private boolean isBufferIngredient(AEItemKey key) {
        for (Template template : templates.values()) {
            if (template.keys().contains(key)) {
                return true;
            }
        }
        return false;
    }

    private long insertIntoTanks(AEItemKey key, long amount, boolean simulate) {
        if (simulate) {
            return insertInto(snapshotTanks(), key, amount);
        }
        long inserted = insertInto(tanks, key, amount);
        if (inserted > 0) {
            setChanged();
        }
        return inserted;
    }

    private static long insertInto(EssenceTank[] target, AEItemKey key, long amount) {
        long inserted = 0;
        for (EssenceTank tank : target) {
            if (inserted >= amount || tank.isEmpty()) {
                continue;
            }
            inserted += tank.insert(key, amount - inserted, false);
        }
        for (EssenceTank tank : target) {
            if (inserted >= amount || !tank.isEmpty()) {
                continue;
            }
            inserted += tank.insert(key, amount - inserted, false);
        }
        return inserted;
    }

    private EssenceTank[] snapshotTanks() {
        EssenceTank[] copy = new EssenceTank[tanks.length];
        for (int tank = 0; tank < tanks.length; tank++) {
            copy[tank] = tanks[tank].copy();
        }
        return copy;
    }

    private long tankAmount(AEItemKey key) {
        long total = 0;
        for (EssenceTank tank : tanks) {
            if (tank.holds(key)) {
                total += tank.amount();
            }
        }
        return total;
    }

    private long tankRoom(AEItemKey key) {
        long room = 0;
        for (EssenceTank tank : tanks) {
            room += tank.room(key);
        }
        return room;
    }

    private void takeFromTanks(AEItemKey key, long amount) {
        long remaining = amount;
        for (EssenceTank tank : tanks) {
            if (remaining <= 0 || !tank.holds(key)) {
                continue;
            }
            remaining -= tank.extract(remaining, false);
        }
        setChanged();
    }

    @Nullable
    private RecipeHolder<IInfusionRecipe> infusionFor(Level level, Item output, @Nullable Template template) {
        if (template != null && template.recipe() != null) {
            RecipeHolder<IInfusionRecipe> holder = MysticalRecipeResolver.infusionById(level, template.recipe());
            if (holder != null) {
                return holder;
            }
        }
        return MysticalRecipeResolver.infusionByOutputItem(level, output);
    }

    @Nullable
    private RecipeHolder<IAwakeningRecipe> awakeningFor(Level level, Item output, @Nullable Template template) {
        if (template != null && template.recipe() != null) {
            RecipeHolder<IAwakeningRecipe> holder = MysticalRecipeResolver.awakeningById(level, template.recipe());
            if (holder != null) {
                return holder;
            }
        }
        return MysticalRecipeResolver.awakeningByOutputItem(level, output);
    }

    private boolean beginCraft(Level level) {
        Stall reason = Stall.NONE;
        for (Item wanted : List.copyOf(owed.keySet())) {
            if (owed.getOrDefault(wanted, 0L) <= 0) {
                continue;
            }
            Template template = templates.get(wanted);
            Stall attempt = template != null && template.kind() == Kind.AWAKENING
                    ? claimAwakening(level, wanted, template)
                    : claimInfusion(level, wanted, template);
            if (attempt == Stall.NONE) {
                setStall(Stall.NONE);
                return true;
            }
            reason = Stall.worse(reason, attempt);
        }
        setStall(owed.isEmpty() ? Stall.NONE : reason);
        return false;
    }

    private Stall claimInfusion(Level level, Item wanted, @Nullable Template template) {
        RecipeHolder<IInfusionRecipe> holder = infusionFor(level, wanted, template);
        if (holder == null) {
            return Stall.NO_RECIPE;
        }
        List<int[]> takes = planConsumption(MysticalRecipeIngredients.infusionDemands(holder.value()));
        if (takes == null) {
            return Stall.INGREDIENTS;
        }
        List<ItemStack> staged = peek(takes);
        ItemStack result = resultOf(holder.value().assemble(gridOf(staged), level.registryAccess()), holder, level);
        if (result.getItem() != wanted) {
            return Stall.NO_RECIPE;
        }
        if (!fitsInOutput(result)) {
            return Stall.OUTPUT_FULL;
        }
        return start(result, extract(takes), List.of());
    }

    private Stall claimAwakening(Level level, Item wanted, @Nullable Template template) {
        RecipeHolder<IAwakeningRecipe> holder = awakeningFor(level, wanted, template);
        if (holder == null) {
            return Stall.NO_RECIPE;
        }
        List<GenericStack> essences = new ArrayList<>();
        for (ItemStack essence : holder.value().getEssences()) {
            AEItemKey key = AEItemKey.of(essence);
            if (key == null) {
                return Stall.NO_RECIPE;
            }
            essences.add(new GenericStack(key, essence.getCount()));
        }
        Map<AEItemKey, Long> wantedEssence = new HashMap<>();
        for (GenericStack essence : essences) {
            wantedEssence.merge((AEItemKey) essence.what(), essence.amount(), Long::sum);
        }
        for (Map.Entry<AEItemKey, Long> entry : wantedEssence.entrySet()) {
            if (tankAmount(entry.getKey()) < entry.getValue()) {
                return Stall.ESSENCE;
            }
        }
        List<int[]> takes = planConsumption(MysticalRecipeIngredients.awakeningDemands(holder.value()));
        if (takes == null) {
            return Stall.INGREDIENTS;
        }
        List<ItemStack> grid = new ArrayList<>(peek(takes));
        for (GenericStack essence : essences) {
            grid.add(InfusionAltarCraftingMachine.toStack(essence));
        }
        ItemStack result = resultOf(holder.value().assemble(gridOf(grid), level.registryAccess()), holder, level);
        if (result.getItem() != wanted) {
            return Stall.NO_RECIPE;
        }
        if (!fitsInOutput(result)) {
            return Stall.OUTPUT_FULL;
        }
        List<ItemStack> taken = extract(takes);
        for (Map.Entry<AEItemKey, Long> entry : wantedEssence.entrySet()) {
            takeFromTanks(entry.getKey(), entry.getValue());
        }
        return start(result, taken, essences);
    }

    private static ItemStack resultOf(ItemStack assembled, RecipeHolder<?> holder, Level level) {
        return assembled.isEmpty()
                ? holder.value().getResultItem(level.registryAccess()).copy()
                : assembled;
    }

    private Stall start(ItemStack result, List<ItemStack> taken, List<GenericStack> essences) {
        claimedItems.clear();
        claimedItems.addAll(taken);
        claimedEssences.clear();
        claimedEssences.addAll(essences);
        activeResult = result;
        progress = 0;
        setChanged();
        if (NepConfig.debugLogging()) {
            Nep.LOGGER.info(
                    "Infused awakening matrix {} started crafting {} x{}",
                    getBlockPos(),
                    activeResult.getItem(),
                    activeResult.getCount());
        }
        return Stall.NONE;
    }

    private static CraftingInput gridOf(List<ItemStack> stacks) {
        if (stacks.size() != GRID_SIZE) {
            return CraftingInput.of(1, 1, List.of(stacks.isEmpty() ? ItemStack.EMPTY : stacks.get(0)));
        }
        NonNullList<ItemStack> grid = NonNullList.withSize(GRID_SIZE, ItemStack.EMPTY);
        for (int slot = 0; slot < GRID_SIZE; slot++) {
            grid.set(slot, stacks.get(slot));
        }
        return CraftingInput.of(3, 3, grid);
    }

    private List<ItemStack> peek(List<int[]> takes) {
        List<ItemStack> staged = new ArrayList<>(takes.size());
        for (int[] take : takes) {
            ItemStack stack = inputBuffer.getStackInSlot(take[0]);
            if (!stack.isEmpty()) {
                staged.add(stack.copyWithCount(take[1]));
            }
        }
        return staged;
    }

    private List<ItemStack> extract(List<int[]> takes) {
        List<ItemStack> taken = new ArrayList<>(takes.size());
        for (int[] take : takes) {
            ItemStack stack = inputBuffer.extractItem(take[0], take[1], false);
            if (!stack.isEmpty()) {
                taken.add(stack);
            }
        }
        return taken;
    }

    @Nullable
    private List<int[]> planConsumption(List<MysticalRecipeIngredients.Demand> demands) {
        int[] slotLeft = new int[inputBuffer.getSlots()];
        for (int slot = 0; slot < slotLeft.length; slot++) {
            slotLeft[slot] = inputBuffer.getStackInSlot(slot).getCount();
        }
        List<int[]> takes = new ArrayList<>();
        for (MysticalRecipeIngredients.Demand demand : demands) {
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
        activeResult = ItemStack.EMPTY;
        claimedItems.clear();
        claimedEssences.clear();
        progress = 0;
        markScanNeeded();
        setChanged();
        flushOutput(level);
        if (NepConfig.debugLogging()) {
            Nep.LOGGER.info(
                    "Infused awakening matrix {} crafted {} x{} ({} still owed)",
                    getBlockPos(),
                    produced,
                    result.getCount(),
                    owed.getOrDefault(produced, 0L));
        }
    }

    boolean readyForPatterns() {
        return NepConfig.mysticalInfusedAwakeningMatrix();
    }

    boolean pushMatrixPattern(IPatternDetails pattern, KeyCounter[] inputs, Direction ejectionDirection) {
        Level level = getLevel();
        if (level == null || level.isClientSide || !NepConfig.mysticalInfusedAwakeningMatrix()) {
            return false;
        }

        Kind kind;
        ResourceLocation recipe;
        List<GenericStack> essenceStacks;
        Map<AEItemKey, Long> expected;
        if (pattern instanceof InfusionPattern infusion) {
            if (!NepConfig.mysticalInfusion()) {
                return reject(Refusal.MODULE_DISABLED, "the infusion module is disabled");
            }
            MysticalRecipeResolver.InfusionPlan plan = MysticalRecipeResolver.resolveInfusion(pattern, level);
            if (plan == null) {
                return reject(Refusal.UNKNOWN_RECIPE, "no infusion recipe matches pattern " + infusion.recipe());
            }
            kind = Kind.INFUSION;
            recipe = infusion.recipe();
            essenceStacks = List.of();
            expected = plan.expectedItems();
        } else if (pattern instanceof AwakeningPattern awakening) {
            if (!NepConfig.mysticalAwakening()) {
                return reject(Refusal.MODULE_DISABLED, "the awakening module is disabled");
            }
            MysticalRecipeResolver.AwakeningPlan plan = MysticalRecipeResolver.resolveAwakening(pattern, level);
            if (plan == null) {
                return reject(Refusal.UNKNOWN_RECIPE, "no awakening recipe matches pattern " + awakening.recipe());
            }
            kind = Kind.AWAKENING;
            recipe = awakening.recipe();
            essenceStacks = plan.essences();
            expected = plan.expectedItems();
        } else {
            return reject(Refusal.NOT_A_MYSTICAL_PATTERN, "pattern is not an infusion or awakening pattern");
        }

        List<GenericStack> outputs = pattern.getOutputs();
        if (outputs.isEmpty() || !(outputs.get(0).what() instanceof AEItemKey outputKey)) {
            return reject(Refusal.NO_ITEM_OUTPUT, "pattern has no item output");
        }
        Item producedItem = outputKey.getItem();
        Template existing = templates.get(producedItem);
        if (existing != null && (existing.kind() != kind || !recipe.equals(existing.recipe()))) {
            return reject(
                    Refusal.MIXED_RECIPES,
                    "a craft of " + producedItem + " from " + existing.recipe() + " is already queued");
        }

        Map<AEItemKey, Long> items = new HashMap<>();
        for (KeyCounter counter : inputs) {
            for (var entry : counter) {
                if (!(entry.getKey() instanceof AEItemKey itemKey) || entry.getLongValue() <= 0) {
                    return reject(Refusal.ITEMS_ONLY, "infusion and awakening take items only");
                }
                items.merge(itemKey, entry.getLongValue(), Long::sum);
            }
        }
        if (items.size() > MatrixGridNode.TRACKER_SIZE) {
            return reject(
                    Refusal.TOO_MANY_INPUTS,
                    "pattern needs more than " + MatrixGridNode.TRACKER_SIZE + " distinct inputs");
        }
        if (!items.equals(expected)) {
            return reject(
                    Refusal.MISMATCHED_INPUTS,
                    "pushed items " + items + " do not match the recipe's inputs " + expected);
        }

        Map<AEItemKey, Long> essenceShare = new LinkedHashMap<>();
        for (GenericStack essence : essenceStacks) {
            essenceShare.merge((AEItemKey) essence.what(), essence.amount(), Long::sum);
        }
        Map<AEItemKey, Long> bufferShare = new HashMap<>(items);
        for (Map.Entry<AEItemKey, Long> entry : essenceShare.entrySet()) {
            long left = bufferShare.getOrDefault(entry.getKey(), 0L) - entry.getValue();
            if (left > 0) {
                bufferShare.put(entry.getKey(), left);
            } else {
                bufferShare.remove(entry.getKey());
            }
        }
        Map<AEItemKey, Long> staged = new LinkedHashMap<>(bufferShare);
        Map<AEItemKey, Long> tankShare = planTankShare(essenceShare, staged);
        if (!fitsInBuffer(staged)) {
            return reject(Refusal.BUFFER_FULL, "matrix buffer is full");
        }
        for (Map.Entry<AEItemKey, Long> entry : tankShare.entrySet()) {
            insertIntoTanks(entry.getKey(), entry.getValue(), false);
        }
        bufferItems(staged);

        returnDirections.record(producedItem, ejectionDirection);
        captureTemplate(producedItem, kind, bufferShare, essenceShare, recipe);
        owed.merge(producedItem, 1L, Long::sum);
        pushingCpus.record();
        clearRefusal();
        markScanNeeded();
        setChanged();
        if (NepConfig.debugLogging()) {
            Nep.LOGGER.info("Infused awakening matrix {} accepted {} pattern for {}", getBlockPos(), kind, outputKey);
        }
        return true;
    }

    private Map<AEItemKey, Long> planTankShare(Map<AEItemKey, Long> essences, Map<AEItemKey, Long> staged) {
        EssenceTank[] probe = snapshotTanks();
        Map<AEItemKey, Long> tankShare = new LinkedHashMap<>();
        for (Map.Entry<AEItemKey, Long> entry : essences.entrySet()) {
            long banked = insertInto(probe, entry.getKey(), entry.getValue());
            if (banked > 0) {
                tankShare.put(entry.getKey(), banked);
            }
            long overflow = entry.getValue() - banked;
            if (overflow > 0) {
                staged.merge(entry.getKey(), overflow, Long::sum);
            }
        }
        return tankShare;
    }

    private static List<ItemStack> stacksOf(Map<AEItemKey, Long> items) {
        List<ItemStack> stacks = new ArrayList<>();
        for (Map.Entry<AEItemKey, Long> entry : items.entrySet()) {
            long count = entry.getValue();
            while (count > 0) {
                int chunk = (int) Math.min(count, OverstackedItemHandler.SLOT_LIMIT);
                stacks.add(entry.getKey().toStack(chunk));
                count -= chunk;
            }
        }
        return stacks;
    }

    private boolean fitsInBuffer(Map<AEItemKey, Long> items) {
        ItemStackHandler probe = new OverstackedItemHandler(inputBuffer.getSlots());
        for (int slot = 0; slot < inputBuffer.getSlots(); slot++) {
            probe.setStackInSlot(slot, inputBuffer.getStackInSlot(slot).copy());
        }
        for (ItemStack stack : stacksOf(items)) {
            if (!ItemHandlerHelper.insertItem(probe, stack.copy(), false).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private void bufferItems(Map<AEItemKey, Long> items) {
        for (ItemStack stack : stacksOf(items)) {
            ItemHandlerHelper.insertItem(inputBuffer, stack, false);
        }
    }

    private void captureTemplate(
            Item output,
            Kind kind,
            Map<AEItemKey, Long> items,
            Map<AEItemKey, Long> essences,
            ResourceLocation recipe) {
        List<AEKey> keys = new ArrayList<>(items.size());
        List<Long> counts = new ArrayList<>(items.size());
        for (Map.Entry<AEItemKey, Long> entry : items.entrySet()) {
            keys.add(entry.getKey());
            counts.add(entry.getValue());
        }
        List<AEKey> essenceKeys = new ArrayList<>(essences.size());
        List<Long> essenceCounts = new ArrayList<>(essences.size());
        for (Map.Entry<AEItemKey, Long> entry : essences.entrySet()) {
            essenceKeys.add(entry.getKey());
            essenceCounts.add(entry.getValue());
        }
        templates.put(
                output,
                new Template(
                        kind,
                        List.copyOf(keys),
                        List.copyOf(counts),
                        List.copyOf(essenceKeys),
                        List.copyOf(essenceCounts),
                        recipe));
    }

    private void autoRequest(Level level) {
        if (!NepConfig.mysticalInfusedAwakeningMatrixAutoRequest()) {
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
                merge(keys, targets, template.keys().get(i), template.counts().get(i) * entry.getValue());
            }
            for (int i = 0; i < template.essenceKeys().size(); i++) {
                long wanted = template.essenceCounts().get(i) * entry.getValue();
                merge(keys, targets, template.essenceKeys().get(i), Math.min(wanted, EssenceTank.capacity()));
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

    private static void merge(List<AEKey> keys, List<Long> targets, AEKey key, long target) {
        int existing = keys.indexOf(key);
        if (existing >= 0) {
            targets.set(existing, targets.get(existing) + target);
        } else {
            keys.add(key);
            targets.add(target);
        }
    }

    int manualDemandFor(ItemStack stack) {
        AEItemKey key = AEItemKey.of(stack);
        if (key == null) {
            return 0;
        }
        long demand = reportedDemandFor(key);
        if (RecipeIngredientCache.INSTANCE.isValidVesselItem(stack) && !isBufferIngredient(key)) {
            demand = Math.max(demand, tankRoom(key));
        }
        return (int) Math.max(0, Math.min(Integer.MAX_VALUE, demand));
    }

    private long reportedDemandFor(AEKey key) {
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
            int essence = template.essenceKeys().indexOf(key);
            if (essence >= 0) {
                target += Math.min(template.essenceCounts().get(essence) * entry.getValue(), EssenceTank.capacity());
            }
        }
        return Math.max(0, target - bufferedAmount(key));
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
            for (ItemStack stack : claimedItems) {
                if (!stack.isEmpty() && itemKey.matches(stack)) {
                    total += stack.getCount();
                }
            }
            for (GenericStack essence : claimedEssences) {
                if (itemKey.equals(essence.what())) {
                    total += essence.amount();
                }
            }
            total += tankAmount(itemKey);
        }
        return total;
    }

    @Override
    public long acceptCrafted(AEKey what, long amount, Actionable mode) {
        if (!(what instanceof AEItemKey key)) {
            return amount;
        }
        boolean simulate = mode == Actionable.SIMULATE;
        long remaining = amount;
        if (!isBufferIngredient(key) && (RecipeIngredientCache.INSTANCE.isValidVesselItem(key.toStack(1)))) {
            remaining -= insertIntoTanks(key, remaining, simulate);
        }
        if (remaining <= 0) {
            return 0;
        }
        ItemStackHandler target = inputBuffer;
        if (simulate) {
            target = new OverstackedItemHandler(inputBuffer.getSlots());
            for (int slot = 0; slot < inputBuffer.getSlots(); slot++) {
                target.setStackInSlot(slot, inputBuffer.getStackInSlot(slot).copy());
            }
        }
        int maxStack = OverstackedItemHandler.SLOT_LIMIT;
        while (remaining > 0) {
            int chunk = (int) Math.min(remaining, maxStack);
            ItemStack leftover = ItemHandlerHelper.insertItem(target, key.toStack(chunk), false);
            int inserted = chunk - leftover.getCount();
            if (inserted <= 0) {
                break;
            }
            remaining -= inserted;
        }
        if (!simulate && remaining < amount) {
            markScanNeeded();
            setChanged();
        }
        return remaining;
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
            Nep.LOGGER.info("Infused awakening matrix {} rejected pattern: {}", getBlockPos(), reason);
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
        if (!state.hasProperty(InfusedAwakeningMatrixBlock.STATUS)) {
            return;
        }
        MatrixStatus desired = desiredStatus();
        if (state.getValue(InfusedAwakeningMatrixBlock.STATUS) != desired) {
            level.setBlock(
                    getBlockPos(), state.setValue(InfusedAwakeningMatrixBlock.STATUS, desired), Block.UPDATE_CLIENTS);
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
        for (EssenceTank tank : tanks) {
            hash = 31 * hash + (tank.isEmpty() ? 0 : tank.essence().hashCode());
            hash = 31 * hash + Long.hashCode(tank.amount());
        }
        return hash;
    }

    @Override
    public float craftProgress() {
        if (activeResult.isEmpty()) {
            return 0.0F;
        }
        int craftTicks = NepConfig.mysticalInfusedAwakeningMatrixCraftTicks();
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

    EssenceTank tank(int index) {
        return tanks[index];
    }

    void clearPending() {
        clearRefusal();
        if (owed.isEmpty() && templates.isEmpty()) {
            return;
        }
        int cancelled = pushingCpus.cancelJobsFor(power.grid(), Set.copyOf(templates.keySet()));
        if (NepConfig.debugLogging()) {
            Nep.LOGGER.info(
                    "Infused awakening matrix {} cleared {} pending craft(s) and cancelled {} network job(s)",
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

    private void setStall(Stall reason) {
        if (stall != reason) {
            stall = reason;
            setChanged();
        }
    }

    void clearBufferTo(Player player) {
        emptyHandlerTo(player, inputBuffer);
        emptyHandlerTo(player, outputBuffer);
        for (EssenceTank tank : tanks) {
            giveTankTo(player, tank);
        }
        setChanged();
    }

    private static void giveTankTo(Player player, EssenceTank tank) {
        while (!tank.isEmpty()) {
            AEItemKey key = tank.essence();
            int chunk = (int) Math.min(tank.amount(), key.toStack(1).getMaxStackSize());
            tank.extract(chunk, false);
            ItemStack stack = key.toStack(chunk);
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
        }
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
        for (GenericStack essence : claimedEssences) {
            if (!(essence.what() instanceof AEItemKey key)) {
                continue;
            }
            long count = essence.amount();
            while (count > 0) {
                int chunk = (int) Math.min(count, key.toStack(1).getMaxStackSize());
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), key.toStack(chunk));
                count -= chunk;
            }
        }
        for (EssenceTank tank : tanks) {
            while (!tank.isEmpty()) {
                AEItemKey key = tank.essence();
                int chunk = (int) Math.min(tank.amount(), key.toStack(1).getMaxStackSize());
                tank.extract(chunk, false);
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), key.toStack(chunk));
            }
        }
        clearContent();
    }

    @Override
    public void clearContent() {
        super.clearContent();
        claimedEssences.clear();
        for (EssenceTank tank : tanks) {
            tank.clear();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put(INPUT_KEY, inputBuffer.serializeNBT(registries));
        tag.put(OUTPUT_KEY, outputBuffer.serializeNBT(registries));
        ListTag tankList = new ListTag();
        for (EssenceTank tank : tanks) {
            tankList.add(tank.save(registries));
        }
        tag.put(TANKS_KEY, tankList);
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
        ListTag claimedEssence = new ListTag();
        for (GenericStack stack : claimedEssences) {
            claimedEssence.add(GenericStack.writeTag(registries, stack));
        }
        tag.put(CLAIMED_ESSENCE_KEY, claimedEssence);
        tag.putInt(PROGRESS_KEY, progress);
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
        ListTag tankList = tag.getList(TANKS_KEY, Tag.TAG_COMPOUND);
        for (int index = 0; index < tanks.length; index++) {
            tanks[index].load(index < tankList.size() ? tankList.getCompound(index) : new CompoundTag(), registries);
        }
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
        claimedEssences.clear();
        ListTag claimedEssence = tag.getList(CLAIMED_ESSENCE_KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < claimedEssence.size(); i++) {
            GenericStack stack = GenericStack.readTag(registries, claimedEssence.getCompound(i));
            if (stack != null) {
                claimedEssences.add(stack);
            }
        }
        progress = tag.getInt(PROGRESS_KEY);
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
            tag.putString("Kind", entry.getValue().kind().name());
            tag.put(
                    "Stacks",
                    saveStacks(
                            registries,
                            entry.getValue().keys(),
                            entry.getValue().counts()));
            tag.put(
                    "Essences",
                    saveStacks(
                            registries,
                            entry.getValue().essenceKeys(),
                            entry.getValue().essenceCounts()));
            if (entry.getValue().recipe() != null) {
                tag.putString("Recipe", entry.getValue().recipe().toString());
            }
            list.add(tag);
        }
        return list;
    }

    private static ListTag saveStacks(HolderLookup.Provider registries, List<AEKey> keys, List<Long> counts) {
        ListTag stacks = new ListTag();
        for (int i = 0; i < keys.size(); i++) {
            stacks.add(GenericStack.writeTag(registries, new GenericStack(keys.get(i), counts.get(i))));
        }
        return stacks;
    }

    private void loadTemplates(ListTag list, HolderLookup.Provider registries) {
        templates.clear();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            Item item = ItemCounts.item(tag.getString("Id"));
            if (item == null) {
                continue;
            }
            List<AEKey> keys = new ArrayList<>();
            List<Long> counts = new ArrayList<>();
            readStacks(registries, tag.getList("Stacks", Tag.TAG_COMPOUND), keys, counts);
            List<AEKey> essenceKeys = new ArrayList<>();
            List<Long> essenceCounts = new ArrayList<>();
            readStacks(registries, tag.getList("Essences", Tag.TAG_COMPOUND), essenceKeys, essenceCounts);
            if (keys.isEmpty() && essenceKeys.isEmpty()) {
                continue;
            }
            templates.put(
                    item,
                    new Template(
                            byName(Kind.values(), tag.getString("Kind"), Kind.INFUSION),
                            List.copyOf(keys),
                            List.copyOf(counts),
                            List.copyOf(essenceKeys),
                            List.copyOf(essenceCounts),
                            ResourceLocation.tryParse(tag.getString("Recipe"))));
        }
    }

    private static void readStacks(
            HolderLookup.Provider registries, ListTag list, List<AEKey> keys, List<Long> counts) {
        for (int i = 0; i < list.size(); i++) {
            GenericStack stack = GenericStack.readTag(registries, list.getCompound(i));
            if (stack != null) {
                keys.add(stack.what());
                counts.add(stack.amount());
            }
        }
    }

    @Override
    public Component getDisplayName() {
        return NepMysticalContent.MATRIX.get().getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory inventory, Player player) {
        return new InfusedAwakeningMatrixMenu(windowId, inventory, getBlockPos());
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
