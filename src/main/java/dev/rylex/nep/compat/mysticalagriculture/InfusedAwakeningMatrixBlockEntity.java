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
import dev.rylex.nep.machine.RedstoneMode;
import dev.rylex.nep.pattern.AwakeningPattern;
import dev.rylex.nep.pattern.InfusionPattern;
import dev.rylex.nep.pattern.encoding.IngredientMatching;
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
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
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
    private static final String REDSTONE_MODE_KEY = "RedstoneMode";
    private static final String MISSING_KEY = "Missing";
    private static final String BLOCKED_KEY = "Blocked";
    private static final String POWER_FAULT_KEY = "PowerFault";
    private static final String STALL_KEY = "Stall";
    private static final String REFUSAL_KEY = "Refusal";
    private static final String REFUSAL_TICKS_KEY = "RefusalTicks";

    private final EssenceTank[] tanks = new EssenceTank[TANKS];

    private final Map<Item, Template> templates = new HashMap<>();
    private final ResourceHandler<ItemResource> outputView = new OutputView();
    private final ResourceHandler<ItemResource> machineView =
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
            @Nullable Identifier recipe) {}

    public InfusedAwakeningMatrixBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(
                type,
                pos,
                state,
                INPUT_SLOTS,
                OUTPUT_SLOTS,
                NepMysticalContent.MATRIX_ITEM.get(),
                NepConfig.mysticalInfusedAwakeningMatrixIdleMeDrain(),
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
        for (int slot = 0; slot < inputBuffer.size(); slot++) {
            ItemStack stack = inputBuffer.stackAt(slot);
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
                try (Transaction tx = Transaction.openRoot()) {
                    inputBuffer.extract(slot, inputBuffer.getResource(slot), (int) moved, tx);
                    tx.commit();
                }
            }
        }
    }

    private long bufferedItemAmount(AEItemKey key) {
        long total = 0;
        for (int slot = 0; slot < inputBuffer.size(); slot++) {
            ItemStack stack = inputBuffer.stackAt(slot);
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
        if (RecipeIngredientCache.INSTANCE.isValidVesselItem(ItemResource.of(stack))) {
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
        ItemStack result = resultOf(holder.value().assemble(gridOf(staged)), holder.value());
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
        Map<AEItemKey, Long> wantedEssence = new HashMap<>();
        for (SizedIngredient essence : holder.value().getEssenceIngredients()) {
            AEItemKey key = stockedEssence(essence, wantedEssence);
            if (key == null) {
                return Stall.ESSENCE;
            }
            essences.add(new GenericStack(key, essence.count()));
            wantedEssence.merge(key, (long) essence.count(), Long::sum);
        }
        List<int[]> takes = planConsumption(MysticalRecipeIngredients.awakeningDemands(holder.value()));
        if (takes == null) {
            return Stall.INGREDIENTS;
        }
        List<ItemStack> grid = new ArrayList<>(peek(takes));
        for (GenericStack essence : essences) {
            grid.add(InfusionAltarCraftingMachine.toStack(essence));
        }
        ItemStack result = resultOf(holder.value().assemble(gridOf(grid)), holder.value());
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

    /**
     * An essence ingredient can list several acceptable items, so the tanks decide which one this craft uses. Amounts
     * already promised to earlier vessels of the same craft are subtracted first, or two vessels wanting the same
     * essence would both read the whole tank as available.
     */
    @Nullable
    private AEItemKey stockedEssence(SizedIngredient essence, Map<AEItemKey, Long> claimed) {
        for (AEItemKey key : IngredientMatching.itemOptions(essence.ingredient(), level)) {
            if (tankAmount(key) - claimed.getOrDefault(key, 0L) >= essence.count()) {
                return key;
            }
        }
        return null;
    }

    private static ItemStack resultOf(ItemStack assembled, Recipe<CraftingInput> recipe) {
        return assembled.isEmpty() ? MysticalRecipeResolver.resultOf(recipe).copy() : assembled;
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
            ItemStack stack = inputBuffer.stackAt(take[0]);
            if (!stack.isEmpty()) {
                staged.add(stack.copyWithCount(take[1]));
            }
        }
        return staged;
    }

    private List<ItemStack> extract(List<int[]> takes) {
        List<ItemStack> taken = new ArrayList<>(takes.size());
        try (Transaction tx = Transaction.openRoot()) {
            for (int[] take : takes) {
                ItemResource resource = inputBuffer.getResource(take[0]);
                if (resource.isEmpty()) {
                    continue;
                }
                int extracted = inputBuffer.extract(take[0], resource, take[1], tx);
                if (extracted > 0) {
                    taken.add(resource.toStack(extracted));
                }
            }
            tx.commit();
        }
        return taken;
    }

    @Nullable
    private List<int[]> planConsumption(List<MysticalRecipeIngredients.Demand> demands) {
        int[] slotLeft = new int[inputBuffer.size()];
        for (int slot = 0; slot < slotLeft.length; slot++) {
            slotLeft[slot] = inputBuffer.stackAt(slot).getCount();
        }
        List<int[]> takes = new ArrayList<>();
        for (MysticalRecipeIngredients.Demand demand : demands) {
            int need = demand.count();
            for (int slot = 0; slot < slotLeft.length && need > 0; slot++) {
                if (slotLeft[slot] <= 0 || !demand.ingredient().test(inputBuffer.stackAt(slot))) {
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
        ResourceHandlerUtil.insertStacking(outputBuffer, ItemResource.of(result), result.getCount(), null);
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
        if (level == null || level.isClientSide() || !NepConfig.mysticalInfusedAwakeningMatrix()) {
            return false;
        }

        Kind kind;
        Identifier recipe;
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

    private boolean fitsInBuffer(Map<AEItemKey, Long> items) {
        return insertIntoBuffer(items, false);
    }

    private void bufferItems(Map<AEItemKey, Long> items) {
        insertIntoBuffer(items, true);
    }

    private boolean insertIntoBuffer(Map<AEItemKey, Long> items, boolean commit) {
        try (Transaction tx = Transaction.openRoot()) {
            boolean fits = true;
            for (Map.Entry<AEItemKey, Long> entry : items.entrySet()) {
                int want = clampToInt(entry.getValue());
                if (ResourceHandlerUtil.insertStacking(inputBuffer, resourceOf(entry.getKey()), want, tx) != want) {
                    fits = false;
                    if (!commit) {
                        return false;
                    }
                }
            }
            if (commit) {
                tx.commit();
            }
            return fits;
        }
    }

    private void captureTemplate(
            Item output, Kind kind, Map<AEItemKey, Long> items, Map<AEItemKey, Long> essences, Identifier recipe) {
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

    int manualDemandFor(ItemResource resource) {
        AEItemKey key = AEItemKey.of(resource.toStack(1));
        if (key == null) {
            return 0;
        }
        long demand = reportedDemandFor(key);
        if (RecipeIngredientCache.INSTANCE.isValidVesselItem(resource) && !isBufferIngredient(key)) {
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
            for (int slot = 0; slot < inputBuffer.size(); slot++) {
                ItemStack stack = inputBuffer.stackAt(slot);
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
        if (!isBufferIngredient(key) && RecipeIngredientCache.INSTANCE.isValidVesselItem(resourceOf(key))) {
            remaining -= insertIntoTanks(key, remaining, simulate);
        }
        if (remaining <= 0) {
            return 0;
        }
        try (Transaction tx = Transaction.openRoot()) {
            remaining -= ResourceHandlerUtil.insertStacking(inputBuffer, resourceOf(key), clampToInt(remaining), tx);
            if (!simulate) {
                tx.commit();
            }
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
        hash = 31 * hash + redstoneMode.ordinal();
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

    ResourceHandler<ItemResource> itemHandlerForSide() {
        return machineView;
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
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        inputBuffer.serialize(output.child(INPUT_KEY));
        outputBuffer.serialize(output.child(OUTPUT_KEY));
        ValueOutput.ValueOutputList tankList = output.childrenList(TANKS_KEY);
        for (EssenceTank tank : tanks) {
            tank.save(tankList.addChild());
        }
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
        ValueOutput.TypedOutputList<GenericStack> claimedEssence = output.list(CLAIMED_ESSENCE_KEY, GenericStack.CODEC);
        for (GenericStack stack : claimedEssences) {
            claimedEssence.add(stack);
        }
        output.putInt(PROGRESS_KEY, progress);
        output.putString(REDSTONE_MODE_KEY, redstoneMode.name());
        output.putBoolean(BLOCKED_KEY, outputBlocked);
        output.putBoolean(POWER_FAULT_KEY, powerFault);
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
        int tankIndex = 0;
        for (ValueInput tankInput : input.childrenListOrEmpty(TANKS_KEY)) {
            if (tankIndex >= tanks.length) {
                break;
            }
            tanks[tankIndex++].load(tankInput);
        }
        while (tankIndex < tanks.length) {
            tanks[tankIndex++].clear();
        }
        ItemCounts.load(owed, input, OWED_KEY);
        ItemCounts.load(toReturn, input, RETURN_KEY);
        returnDirections.load(input, RETURN_DIRS_KEY);
        loadTemplates(input);
        activeResult = input.read(ACTIVE_KEY, ItemStack.CODEC).orElse(ItemStack.EMPTY);
        claimedItems.clear();
        for (ItemStack stack : input.listOrEmpty(CLAIMED_KEY, ItemStack.CODEC)) {
            claimedItems.add(stack);
        }
        claimedEssences.clear();
        for (GenericStack stack : input.listOrEmpty(CLAIMED_ESSENCE_KEY, GenericStack.CODEC)) {
            claimedEssences.add(stack);
        }
        progress = input.getIntOr(PROGRESS_KEY, 0);
        redstoneMode = RedstoneMode.byName(input.getStringOr(REDSTONE_MODE_KEY, ""));
        outputBlocked = input.getBooleanOr(BLOCKED_KEY, false);
        powerFault = input.getBooleanOr(POWER_FAULT_KEY, false);
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
        for (Map.Entry<Item, Template> entry : templates.entrySet()) {
            ValueOutput entryOutput = list.addChild();
            entryOutput.putString(
                    "Id", BuiltInRegistries.ITEM.getKey(entry.getKey()).toString());
            entryOutput.putString("Kind", entry.getValue().kind().name());
            saveStacks(
                    entryOutput,
                    "Stacks",
                    entry.getValue().keys(),
                    entry.getValue().counts());
            saveStacks(
                    entryOutput,
                    "Essences",
                    entry.getValue().essenceKeys(),
                    entry.getValue().essenceCounts());
            if (entry.getValue().recipe() != null) {
                entryOutput.putString("Recipe", entry.getValue().recipe().toString());
            }
        }
    }

    private static void saveStacks(ValueOutput output, String key, List<AEKey> keys, List<Long> counts) {
        ValueOutput.TypedOutputList<GenericStack> stacks = output.list(key, GenericStack.CODEC);
        for (int i = 0; i < keys.size(); i++) {
            stacks.add(new GenericStack(keys.get(i), counts.get(i)));
        }
    }

    private void loadTemplates(ValueInput input) {
        templates.clear();
        for (ValueInput entryInput : input.childrenListOrEmpty(TEMPLATE_KEY)) {
            Item item = ItemCounts.item(entryInput.getStringOr("Id", ""));
            if (item == null) {
                continue;
            }
            List<AEKey> keys = new ArrayList<>();
            List<Long> counts = new ArrayList<>();
            readStacks(entryInput, "Stacks", keys, counts);
            List<AEKey> essenceKeys = new ArrayList<>();
            List<Long> essenceCounts = new ArrayList<>();
            readStacks(entryInput, "Essences", essenceKeys, essenceCounts);
            if (keys.isEmpty() && essenceKeys.isEmpty()) {
                continue;
            }
            templates.put(
                    item,
                    new Template(
                            byName(Kind.values(), entryInput.getStringOr("Kind", ""), Kind.INFUSION),
                            List.copyOf(keys),
                            List.copyOf(counts),
                            List.copyOf(essenceKeys),
                            List.copyOf(essenceCounts),
                            entryInput
                                    .getString("Recipe")
                                    .map(Identifier::tryParse)
                                    .orElse(null)));
        }
    }

    private static void readStacks(ValueInput input, String key, List<AEKey> keys, List<Long> counts) {
        for (GenericStack stack : input.listOrEmpty(key, GenericStack.CODEC)) {
            keys.add(stack.what());
            counts.add(stack.amount());
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
            if (resource.isEmpty() || toReturn.getOrDefault(resource.getItem(), 0L) > 0) {
                return 0;
            }
            return outputBuffer.extract(index, resource, amount, transaction);
        }
    }
}
