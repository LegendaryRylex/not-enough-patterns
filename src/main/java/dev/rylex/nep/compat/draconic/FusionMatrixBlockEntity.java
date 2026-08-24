package dev.rylex.nep.compat.draconic;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import com.brandon3055.brandonscore.api.TechLevel;
import com.brandon3055.brandonscore.api.power.IOPStorage;
import com.brandon3055.draconicevolution.api.crafting.IFusionRecipe;
import dev.rylex.nep.Nep;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.compat.draconic.FusionMatrixBlock.MatrixStatus;
import dev.rylex.nep.machine.BufferedMatrixBlockEntity;
import dev.rylex.nep.machine.MachineItemView;
import dev.rylex.nep.machine.MatrixEnergyBuffer;
import dev.rylex.nep.machine.MatrixGridNode;
import dev.rylex.nep.machine.OverstackedItemHandler;
import dev.rylex.nep.pattern.FusionCraftingPattern;
import dev.rylex.nep.util.ItemCounts;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FusionMatrixBlockEntity extends BufferedMatrixBlockEntity {

    static final int INPUT_SLOTS = 18;
    static final int OUTPUT_SLOTS = 9;
    static final int UPGRADE_SLOTS = 1;

    private static final int RESTOCK_INTERVAL = 10;
    private static final int RUNNING_GRACE_TICKS = 10;
    private static final int ENERGY_STARVED_TICKS = 40;
    private static final int ENERGY_SYNC_STEPS = 256;
    private static final int REFUSAL_MEMORY_TICKS = 200;

    private static final String INPUT_KEY = "Input";
    private static final String OUTPUT_KEY = "Output";
    private static final String UPGRADE_KEY = "Upgrade";
    private static final String OWED_KEY = "Owed";
    private static final String RETURN_KEY = "ToReturn";
    private static final String RETURN_DIRS_KEY = "ReturnDirs";
    private static final String TEMPLATE_KEY = "Template";
    private static final String NODE_KEY = "Node";
    private static final String ENERGY_KEY = "Energy";
    private static final String ACTIVE_KEY = "Active";
    private static final String CLAIMED_KEY = "Claimed";
    private static final String CHARGED_KEY = "Charged";
    private static final String COST_KEY = "Cost";
    private static final String PROGRESS_KEY = "Progress";
    private static final String MISSING_KEY = "Missing";
    private static final String BLOCKED_KEY = "Blocked";
    private static final String POWER_FAULT_KEY = "PowerFault";
    private static final String ENERGY_FAULT_KEY = "EnergyFault";
    private static final String SWAP_GRACE_KEY = "SwapGrace";
    private static final String STALL_KEY = "Stall";
    private static final String REFUSAL_KEY = "Refusal";
    private static final String REFUSAL_TICKS_KEY = "RefusalTicks";

    private final ItemStackHandler upgradeSlot = new ItemStackHandler(UPGRADE_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            refreshUpgrades();
            armCoreSwapGrace();
            refreshCapacity();
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return FusionMatrixUpgrades.maxCores() > 0 && FusionMatrixUpgrades.tierOf(stack) != null;
        }

        @Override
        public int getSlotLimit(int slot) {
            return FusionMatrixUpgrades.maxCores();
        }
    };

    private final MatrixEnergyBuffer energy = new MatrixEnergyBuffer(
            NepConfig.draconicFusionMatrixCapacity(), NepConfig.draconicFusionMatrixChargeRate());

    private final FusionMatrixOpStorage opStorage = new FusionMatrixOpStorage(energy);

    private final Map<Item, Template> templates = new HashMap<>();
    private final IItemHandler outputView = new OutputView();
    private final IItemHandler machineView =
            MachineItemView.demandLimited(inputBuffer, outputView, this::manualDemandFor);

    private long charged;
    private long chargeCost;
    private int progress;

    @Nullable
    private TechLevel upgradeTier;

    private int upgradeCount;

    private int coreSwapGrace;

    private boolean energyFault;
    private int energyIdleTicks;
    private Stall stall = Stall.NONE;
    private Refusal refusal = Refusal.NONE;
    private int refusalTicks;

    enum Stall {
        NONE,
        INGREDIENTS,
        OUTPUT_FULL,
        TIER,
        NO_RECIPE;

        private static Stall worse(Stall first, Stall second) {
            return first.ordinal() >= second.ordinal() ? first : second;
        }
    }

    enum Refusal {
        NONE,
        NOT_A_FUSION_PATTERN,
        NO_ITEM_OUTPUT,
        UNKNOWN_RECIPE,
        TIER_TOO_HIGH,
        ITEMS_ONLY,
        TOO_MANY_INPUTS,
        BUFFER_FULL,
        MIXED_RECIPES
    }

    private record Template(
            List<AEKey> keys,
            List<Long> counts,
            List<GenericStack> retained,
            @Nullable ResourceLocation recipe) {}

    public FusionMatrixBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(
                type,
                pos,
                state,
                INPUT_SLOTS,
                OUTPUT_SLOTS,
                NepDraconicContent.MATRIX_ITEM.get(),
                NepConfig.draconicFusionMatrixIdleMeDrain(),
                NepConfig.draconicFusionMatrixChannels(),
                "Fusion matrix");
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FusionMatrixBlockEntity matrix) {
        matrix.tick(level);
    }

    private void tick(Level level) {
        if (runningGrace > 0) {
            runningGrace--;
        }
        if (coreSwapGrace > 0) {
            coreSwapGrace--;
        }
        if (refusalTicks > 0 && --refusalTicks == 0) {
            refusal = Refusal.NONE;
        }
        flushOutput(level);
        refreshCapacity();
        if (!NepConfig.draconicFusionMatrix()) {
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
            } else if (charged < chargeCost) {
                chargeUp();
            } else if (drawActivePower()) {
                progress++;
                runningGrace = RUNNING_GRACE_TICKS;
                if (progress >= effectiveCraftTicks()) {
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
        hash = 31 * hash + upgradeCount;
        hash = 31 * hash + (upgradeTier == null ? 0 : upgradeTier.index + 1);
        hash = 31 * hash + energyLevel();
        hash = 31 * hash + Long.hashCode(energy.capacity());
        hash = 31 * hash + Long.hashCode(pendingJobs());
        return hash;
    }

    private int energyLevel() {
        long capacity = energy.capacity();
        long stored = energy.stored();
        if (capacity <= 0L || stored <= 0L) {
            return 0;
        }
        return (int) Math.min(ENERGY_SYNC_STEPS, stored / Math.max(1L, capacity / ENERGY_SYNC_STEPS));
    }

    private void refreshUpgrades() {
        ItemStack stack = upgradeSlot.getStackInSlot(0);
        TechLevel tier = FusionMatrixUpgrades.tierOf(stack);
        upgradeTier = tier;
        upgradeCount = tier == null ? 0 : Math.min(stack.getCount(), FusionMatrixUpgrades.maxCores());
    }

    private void armCoreSwapGrace() {
        coreSwapGrace = NepConfig.draconicFusionMatrixCoreSwapGrace();
    }

    private void refreshCapacity() {
        long target = FusionMatrixUpgrades.capacity(upgradeTier, upgradeCount);
        long stored = energy.stored();
        long held = coreSwapGrace > 0 ? Math.max(target, stored) : target;
        if (energy.capacity() != held) {
            energy.setCapacity(held);
            setChanged();
        }
        if (stored > held) {
            setChanged();
        }
        if (coreSwapGrace > 0 && stored <= target) {
            coreSwapGrace = 0;
            setChanged();
        }
    }

    boolean isHoldingSwappedEnergy() {
        return coreSwapGrace > 0;
    }

    private int effectiveCraftTicks() {
        return FusionMatrixUpgrades.craftTicks(upgradeTier, upgradeCount);
    }

    private void chargeUp() {
        long wanted = Math.min(NepConfig.draconicFusionMatrixChargeRate(), chargeCost - charged);
        if (wanted <= 0) {
            return;
        }
        long drawn = Math.min(wanted, energy.stored());
        if (drawn <= 0) {
            return;
        }
        energy.spend(drawn);
        charged += drawn;
        energyIdleTicks = 0;
        runningGrace = RUNNING_GRACE_TICKS;
        setChanged();
    }

    private boolean drawActivePower() {
        double extra = NepConfig.draconicFusionMatrixMeDrain() - NepConfig.draconicFusionMatrixIdleMeDrain();
        if (extra <= 0) {
            power.clearStarved();
            return true;
        }
        power.extractPower(extra);
        return !power.isStarved();
    }

    @Nullable
    private RecipeHolder<IFusionRecipe> recipeFor(Level level, Item output) {
        Template template = templates.get(output);
        if (template != null && template.recipe() != null) {
            RecipeHolder<IFusionRecipe> holder = FusionRecipeResolver.resolveById(level, template.recipe());
            if (holder != null) {
                return holder;
            }
        }
        return FusionRecipeResolver.resolveByOutputItem(level, output);
    }

    private boolean beginCraft(Level level) {
        Stall reason = Stall.NONE;
        for (Item wanted : List.copyOf(owed.keySet())) {
            if (owed.getOrDefault(wanted, 0L) <= 0) {
                continue;
            }
            RecipeHolder<IFusionRecipe> holder = recipeFor(level, wanted);
            if (holder == null) {
                reason = Stall.worse(reason, Stall.NO_RECIPE);
                continue;
            }
            Stall attempt = claimIngredients(level, holder);
            if (attempt == Stall.NONE) {
                setStall(Stall.NONE);
                return true;
            }
            reason = Stall.worse(reason, attempt);
        }
        setStall(owed.isEmpty() ? Stall.NONE : reason);
        return false;
    }

    private Stall claimIngredients(Level level, RecipeHolder<IFusionRecipe> holder) {
        IFusionRecipe recipe = holder.value();
        if (recipe.getRecipeTier().index > maximumTier().index) {
            return Stall.TIER;
        }
        ItemStack result = FusionResults.expectedResult(recipe, level);
        if (result.isEmpty()) {
            return Stall.NO_RECIPE;
        }
        if (!fitsInOutput(withRetained(result, templates.get(result.getItem())))) {
            return Stall.OUTPUT_FULL;
        }
        List<DraconicRecipeIngredients.Demand> demands = DraconicRecipeIngredients.demandOf(recipe);
        if (demands == null) {
            return Stall.NO_RECIPE;
        }
        List<int[]> takes = planConsumption(demands);
        if (takes == null) {
            return Stall.INGREDIENTS;
        }

        claimedItems.clear();
        ItemStack catalyst = ItemStack.EMPTY;
        for (int[] take : takes) {
            ItemStack taken = inputBuffer.extractItem(take[0], take[1], false);
            if (!taken.isEmpty()) {
                claimedItems.add(taken);
                if (take[2] == 0) {
                    catalyst = taken;
                }
            }
        }
        activeResult = FusionResults.assemble(recipe, level, catalyst.copy());
        if (activeResult.isEmpty()) {
            activeResult = result.copy();
        }
        chargeCost = chargeCostOf(recipe);
        charged = 0;
        progress = 0;
        setChanged();
        if (NepConfig.debugLogging()) {
            Nep.LOGGER.info(
                    "Fusion matrix {} started fusing {} x{} for {} FE",
                    getBlockPos(),
                    activeResult.getItem(),
                    activeResult.getCount(),
                    chargeCost);
        }
        return Stall.NONE;
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

    private long chargeCostOf(IFusionRecipe recipe) {
        return FusionMatrixUpgrades.chargeCost(recipe.getEnergyCost(), upgradeTier, upgradeCount);
    }

    static TechLevel maximumTier() {
        String configured = NepConfig.draconicFusionMatrixMaximumTier();
        for (TechLevel level : TechLevel.VALUES) {
            if (level.getSerializedName().equalsIgnoreCase(configured)) {
                return level;
            }
        }
        return TechLevel.CHAOTIC;
    }

    @Nullable
    private List<int[]> planConsumption(List<DraconicRecipeIngredients.Demand> demands) {
        int[] slotLeft = new int[inputBuffer.getSlots()];
        for (int slot = 0; slot < slotLeft.length; slot++) {
            slotLeft[slot] = inputBuffer.getStackInSlot(slot).getCount();
        }
        List<int[]> takes = new ArrayList<>();
        for (int index = 0; index < demands.size(); index++) {
            DraconicRecipeIngredients.Demand demand = demands.get(index);
            int need = demand.count();
            for (int slot = 0; slot < slotLeft.length && need > 0; slot++) {
                if (slotLeft[slot] <= 0 || !demand.ingredient().test(inputBuffer.getStackInSlot(slot))) {
                    continue;
                }
                int take = Math.min(need, slotLeft[slot]);
                need -= take;
                slotLeft[slot] -= take;
                if (demand.consume()) {
                    takes.add(new int[] {slot, take, index});
                }
            }
            if (need > 0) {
                return null;
            }
        }
        return takes;
    }

    private void finishCraft(Level level) {
        ItemStack result = activeResult.copy();
        Template template = templates.get(result.getItem());
        if (!fitsInOutput(withRetained(result, template))) {
            outputBlocked = true;
            return;
        }
        outputBlocked = false;
        ItemHandlerHelper.insertItem(outputBuffer, result.copy(), false);
        Item produced = result.getItem();
        if (owed.getOrDefault(produced, 0L) > 0) {
            returnRetained(template);
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
                    "Fusion matrix {} fused {} x{} ({} still owed)",
                    getBlockPos(),
                    produced,
                    result.getCount(),
                    owed.getOrDefault(produced, 0L));
        }
    }

    private void completeCraft() {
        activeResult = ItemStack.EMPTY;
        claimedItems.clear();
        charged = 0;
        chargeCost = 0;
        progress = 0;
        outputBlocked = false;
        markScanNeeded();
        setChanged();
    }

    private boolean fitsInOutput(List<ItemStack> stacks) {
        ItemStackHandler probe = new OverstackedItemHandler(outputBuffer.getSlots());
        for (int slot = 0; slot < outputBuffer.getSlots(); slot++) {
            probe.setStackInSlot(slot, outputBuffer.getStackInSlot(slot).copy());
        }
        for (ItemStack stack : stacks) {
            if (!ItemHandlerHelper.insertItem(probe, stack.copy(), false).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static List<ItemStack> withRetained(ItemStack result, @Nullable Template template) {
        if (template == null || template.retained().isEmpty()) {
            return List.of(result);
        }
        List<ItemStack> stacks = new ArrayList<>();
        stacks.add(result);
        for (GenericStack kept : template.retained()) {
            if (kept.what() instanceof AEItemKey key) {
                stacks.add(key.toStack((int) kept.amount()));
            }
        }
        return stacks;
    }

    private void returnRetained(@Nullable Template template) {
        if (template == null || template.retained().isEmpty()) {
            return;
        }
        for (GenericStack kept : template.retained()) {
            if (!(kept.what() instanceof AEItemKey key)) {
                continue;
            }
            int wanted = (int) kept.amount();
            for (int slot = 0; slot < inputBuffer.getSlots() && wanted > 0; slot++) {
                ItemStack held = inputBuffer.getStackInSlot(slot);
                if (held.isEmpty() || !key.matches(held)) {
                    continue;
                }
                ItemStack taken = inputBuffer.extractItem(slot, wanted, false);
                if (taken.isEmpty()) {
                    continue;
                }
                ItemStack leftover = ItemHandlerHelper.insertItem(outputBuffer, taken.copy(), false);
                int moved = taken.getCount() - leftover.getCount();
                if (moved > 0) {
                    toReturn.merge(key.getItem(), (long) moved, Long::sum);
                    wanted -= moved;
                }
                if (!leftover.isEmpty()) {
                    ItemHandlerHelper.insertItem(inputBuffer, leftover, false);
                }
            }
        }
    }

    boolean readyForPatterns() {
        return NepConfig.draconicFusionMatrix();
    }

    boolean pushMatrixPattern(IPatternDetails pattern, KeyCounter[] inputs, Direction ejectionDirection) {
        Level level = getLevel();
        if (level == null || level.isClientSide || !NepConfig.draconicFusionMatrix()) {
            return false;
        }
        if (!(pattern instanceof FusionCraftingPattern fusion)) {
            return reject(Refusal.NOT_A_FUSION_PATTERN, "pattern is not a fusion pattern");
        }
        List<GenericStack> outputs = pattern.getOutputs();
        if (outputs.isEmpty() || !(outputs.get(0).what() instanceof AEItemKey outputKey)) {
            return reject(Refusal.NO_ITEM_OUTPUT, "pattern has no item output");
        }
        RecipeHolder<IFusionRecipe> holder = FusionRecipeResolver.resolveById(level, fusion.recipe());
        if (holder == null) {
            return reject(Refusal.UNKNOWN_RECIPE, "no fusion recipe with id " + fusion.recipe());
        }
        if (holder.value().getRecipeTier().index > maximumTier().index) {
            return reject(
                    Refusal.TIER_TOO_HIGH,
                    "recipe tier " + holder.value().getRecipeTier().getSerializedName()
                            + " is above the Matrix's configured maximum of "
                            + maximumTier().getSerializedName());
        }

        Item producedItem = outputKey.getItem();
        Template existing = templates.get(producedItem);
        if (existing != null && !fusion.recipe().equals(existing.recipe())) {
            return reject(
                    Refusal.MIXED_RECIPES,
                    "a craft of " + producedItem + " from " + existing.recipe() + " is already queued");
        }

        Map<AEItemKey, Long> items = new HashMap<>();
        for (KeyCounter counter : inputs) {
            for (var entry : counter) {
                if (!(entry.getKey() instanceof AEItemKey itemKey) || entry.getLongValue() <= 0) {
                    return reject(Refusal.ITEMS_ONLY, "fusion crafting takes items only");
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
        for (GenericStack kept : fusion.retained()) {
            if (kept.what() instanceof AEItemKey keptKey) {
                returnDirections.record(keptKey.getItem(), ejectionDirection);
            }
        }
        captureTemplate(producedItem, items, fusion.retained(), fusion.recipe());
        owed.merge(producedItem, 1L, Long::sum);
        pushingCpus.record();
        clearRefusal();
        markScanNeeded();
        setChanged();
        if (NepConfig.debugLogging()) {
            Nep.LOGGER.info("Fusion matrix {} accepted pattern for {}", getBlockPos(), outputKey);
        }
        return true;
    }

    private void captureTemplate(
            Item output, Map<AEItemKey, Long> items, List<GenericStack> retained, ResourceLocation recipe) {
        List<AEKey> keys = new ArrayList<>(items.size());
        List<Long> counts = new ArrayList<>(items.size());
        for (Map.Entry<AEItemKey, Long> entry : items.entrySet()) {
            keys.add(entry.getKey());
            counts.add(entry.getValue());
        }
        templates.put(output, new Template(List.copyOf(keys), List.copyOf(counts), List.copyOf(retained), recipe));
    }

    private void autoRequest(Level level) {
        if (!NepConfig.draconicFusionMatrixAutoRequest()) {
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
                long target = demandFor(template, i, entry.getValue());
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

    private static long demandFor(Template template, int index, long queued) {
        long perCraft = template.counts().get(index);
        return isFullyRetained(template, template.keys().get(index), perCraft) ? perCraft : perCraft * queued;
    }

    private static boolean isFullyRetained(Template template, AEKey key, long perCraft) {
        long kept = 0;
        for (GenericStack stack : template.retained()) {
            if (stack.what().equals(key)) {
                kept += stack.amount();
            }
        }
        return kept > 0 && kept == perCraft;
    }

    int manualDemandFor(ItemStack stack) {
        AEItemKey key = AEItemKey.of(stack);
        if (key == null) {
            return 0;
        }
        return reportedDemandFor(key) + keptDemandFor(stack);
    }

    private int keptDemandFor(ItemStack stack) {
        Level level = getLevel();
        if (level == null || stack.isEmpty() || owed.isEmpty()) {
            return 0;
        }
        int needed = 0;
        for (Item wanted : owed.keySet()) {
            RecipeHolder<IFusionRecipe> holder = recipeFor(level, wanted);
            if (holder == null) {
                continue;
            }
            int keptHere = 0;
            for (IFusionRecipe.IFusionIngredient ingredient : holder.value().fusionIngredients()) {
                if (!ingredient.consume() && ingredient.get().test(stack)) {
                    keptHere++;
                }
            }
            needed = Math.max(needed, keptHere);
        }
        if (needed == 0) {
            return 0;
        }
        int held = 0;
        for (int slot = 0; slot < inputBuffer.getSlots(); slot++) {
            ItemStack staged = inputBuffer.getStackInSlot(slot);
            if (!staged.isEmpty() && ItemStack.isSameItemSameComponents(staged, stack)) {
                held += staged.getCount();
            }
        }
        return Math.max(0, needed - held);
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
                target += demandFor(template, index, entry.getValue());
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
            Nep.LOGGER.info("Fusion matrix {} rejected pattern: {}", getBlockPos(), reason);
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
        if (!state.hasProperty(FusionMatrixBlock.STATUS)) {
            return;
        }
        MatrixStatus desired = desiredStatus();
        if (state.getValue(FusionMatrixBlock.STATUS) != desired) {
            level.setBlock(getBlockPos(), state.setValue(FusionMatrixBlock.STATUS, desired), Block.UPDATE_CLIENTS);
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
        if (charged < chargeCost) {
            return chargeCost <= 0 ? 0.0F : 0.5F * (float) ((double) charged / chargeCost);
        }
        int craftTicks = effectiveCraftTicks();
        return 0.5F + 0.5F * Math.min(1.0F, craftTicks <= 0 ? 1.0F : progress / (float) craftTicks);
    }

    boolean isCharging() {
        return !activeResult.isEmpty() && charged < chargeCost;
    }

    long chargedEnergy() {
        return charged;
    }

    long chargeCost() {
        return chargeCost;
    }

    long storedEnergy() {
        return energy.stored();
    }

    long energyCapacity() {
        return energy.capacity();
    }

    @Nullable
    TechLevel upgradeTier() {
        return upgradeTier;
    }

    int upgradeCount() {
        return upgradeCount;
    }

    int craftTicks() {
        return effectiveCraftTicks();
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

    Set<AEItemKey> retainedInputKeys() {
        if (templates.isEmpty()) {
            return Set.of();
        }
        Map<AEItemKey, Boolean> fullyRetained = new HashMap<>();
        for (Template template : templates.values()) {
            Map<AEKey, Long> kept = new HashMap<>();
            for (GenericStack stack : template.retained()) {
                kept.merge(stack.what(), stack.amount(), Long::sum);
            }
            for (int i = 0; i < template.keys().size(); i++) {
                if (!(template.keys().get(i) instanceof AEItemKey key)) {
                    continue;
                }
                fullyRetained.merge(
                        key, Objects.equals(kept.get(key), template.counts().get(i)), Boolean::logicalAnd);
            }
        }
        Set<AEItemKey> keys = new HashSet<>();
        fullyRetained.forEach((key, retained) -> {
            if (retained) {
                keys.add(key);
            }
        });
        return keys;
    }

    void clearPending() {
        clearRefusal();
        if (owed.isEmpty() && templates.isEmpty()) {
            return;
        }
        int cancelled = pushingCpus.cancelJobsFor(power.grid(), Set.copyOf(templates.keySet()));
        if (NepConfig.debugLogging()) {
            Nep.LOGGER.info(
                    "Fusion matrix {} cleared {} pending craft(s) and cancelled {} network job(s)",
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

    MatrixEnergyBuffer energyStorage() {
        return energy;
    }

    IOPStorage opStorage() {
        return opStorage;
    }

    IItemHandler getUpgradeSlot() {
        return upgradeSlot;
    }

    void dropBuffers(Level level, BlockPos pos) {
        for (int slot = 0; slot < inputBuffer.getSlots(); slot++) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), inputBuffer.getStackInSlot(slot));
        }
        for (int slot = 0; slot < outputBuffer.getSlots(); slot++) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), outputBuffer.getStackInSlot(slot));
        }
        for (int slot = 0; slot < upgradeSlot.getSlots(); slot++) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), upgradeSlot.getStackInSlot(slot));
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
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put(INPUT_KEY, inputBuffer.serializeNBT(registries));
        tag.put(OUTPUT_KEY, outputBuffer.serializeNBT(registries));
        tag.put(UPGRADE_KEY, upgradeSlot.serializeNBT(registries));
        tag.put(ENERGY_KEY, energy.save());
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
        tag.putLong(CHARGED_KEY, charged);
        tag.putLong(COST_KEY, chargeCost);
        tag.putInt(PROGRESS_KEY, progress);
        tag.putBoolean(BLOCKED_KEY, outputBlocked);
        tag.putBoolean(POWER_FAULT_KEY, powerFault);
        tag.putBoolean(ENERGY_FAULT_KEY, energyFault);
        tag.putInt(SWAP_GRACE_KEY, coreSwapGrace);
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
        inputBuffer.deserializeNBT(registries, tag.getCompound(INPUT_KEY));
        outputBuffer.deserializeNBT(registries, tag.getCompound(OUTPUT_KEY));
        upgradeSlot.deserializeNBT(registries, tag.getCompound(UPGRADE_KEY));
        energy.load(tag.getCompound(ENERGY_KEY));
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
        charged = tag.getLong(CHARGED_KEY);
        chargeCost = tag.getLong(COST_KEY);
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
        coreSwapGrace = tag.getInt(SWAP_GRACE_KEY);
        stall = byName(Stall.values(), tag.getString(STALL_KEY), Stall.NONE);
        refusal = byName(Refusal.values(), tag.getString(REFUSAL_KEY), Refusal.NONE);
        refusalTicks = refusal == Refusal.NONE
                ? 0
                : Math.max(1, Math.min(REFUSAL_MEMORY_TICKS, tag.getInt(REFUSAL_TICKS_KEY)));
        refreshUpgrades();
        refreshCapacity();
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
            ListTag kept = new ListTag();
            for (GenericStack stack : entry.getValue().retained()) {
                kept.add(GenericStack.writeTag(registries, stack));
            }
            tag.put("Retained", kept);
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
            List<GenericStack> retained = new ArrayList<>();
            ListTag kept = tag.getList("Retained", Tag.TAG_COMPOUND);
            for (int s = 0; s < kept.size(); s++) {
                GenericStack stack = GenericStack.readTag(registries, kept.getCompound(s));
                if (stack != null) {
                    retained.add(stack);
                }
            }
            if (!keys.isEmpty()) {
                templates.put(
                        item,
                        new Template(
                                List.copyOf(keys),
                                List.copyOf(counts),
                                List.copyOf(retained),
                                ResourceLocation.tryParse(tag.getString("Recipe"))));
            }
        }
    }

    @Override
    public Component getDisplayName() {
        return NepDraconicContent.MATRIX.get().getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory inventory, Player player) {
        return new FusionMatrixMenu(windowId, inventory, getBlockPos());
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
