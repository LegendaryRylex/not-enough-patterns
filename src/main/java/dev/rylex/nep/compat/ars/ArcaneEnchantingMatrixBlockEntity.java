package dev.rylex.nep.compat.ars;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.core.definitions.AEItems;
import com.hollingsworth.arsnouveau.common.crafting.recipes.EnchantingApparatusRecipe;
import com.hollingsworth.arsnouveau.common.crafting.recipes.ImbuementRecipe;
import dev.rylex.nep.Nep;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.compat.ars.arseng.ArsEngSource;
import dev.rylex.nep.machine.BufferedMatrixBlockEntity;
import dev.rylex.nep.machine.CraftedOutputs;
import dev.rylex.nep.machine.MachineItemView;
import dev.rylex.nep.machine.ManualCraftHost;
import dev.rylex.nep.machine.ManualCraftOutcome;
import dev.rylex.nep.machine.ManualCraftResult;
import dev.rylex.nep.machine.ManualRequirement;
import dev.rylex.nep.machine.ManualStaging;
import dev.rylex.nep.machine.MatrixGridNode;
import dev.rylex.nep.machine.MatrixStatus;
import dev.rylex.nep.pattern.ApparatusPattern;
import dev.rylex.nep.pattern.ImbuementPattern;
import dev.rylex.nep.pattern.encoding.IngredientMatching;
import dev.rylex.nep.util.ItemCounts;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
import net.minecraft.server.level.ServerLevel;
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
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ArcaneEnchantingMatrixBlockEntity extends BufferedMatrixBlockEntity implements ManualCraftHost {

    static final int INPUT_SLOTS = 24;
    static final int OUTPUT_SLOTS = 9;

    private static final boolean ARSENG_LOADED = ModList.get().isLoaded(ArsEngSource.MOD_ID);

    private static final ResourceLocation RETURNING = Nep.id("returning");

    private static final int RESTOCK_INTERVAL = 10;
    private static final int SOURCE_INTERVAL = 20;
    private static final int RUNNING_GRACE_TICKS = 10;
    private static final int REFUSAL_MEMORY_TICKS = 200;

    private static final String INPUT_KEY = "Input";
    private static final String OUTPUT_KEY = "Output";
    private static final String OWED_KEY = "Owed";
    private static final String RETURN_KEY = "ToReturn";
    private static final String RETURN_DIRS_KEY = "ReturnDirs";
    private static final String TEMPLATE_KEY = "Template";
    private static final String NODE_KEY = "Node";
    private static final String SOURCE_KEY = "Source";
    private static final String CATALYSTS_KEY = "Catalysts";
    private static final String ACCELERATE_KEY = "Accelerate";
    private static final String DAMPEN_KEY = "Dampen";
    private static final String ACTIVE_KEY = "Active";
    private static final String ACTIVE_KIND_KEY = "ActiveKind";
    private static final String ACTIVE_RECIPE_KEY = "ActiveRecipe";
    private static final String ACTIVE_TICKS_KEY = "ActiveTicks";
    private static final String CLAIMED_KEY = "Claimed";
    private static final String PROGRESS_KEY = "Progress";
    private static final String MISSING_KEY = "Missing";
    private static final String BLOCKED_KEY = "Blocked";
    private static final String POWER_FAULT_KEY = "PowerFault";
    private static final String STALL_KEY = "Stall";
    private static final String REFUSAL_KEY = "Refusal";
    private static final String REFUSAL_TICKS_KEY = "RefusalTicks";

    private final MatrixSourceStore source = new MatrixSourceStore(this::onSourceChanged);
    private final CatalystShelf catalysts = new CatalystShelf(this::setChanged);

    private final ItemStackHandler accelerateSlot = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return ArcaneEnchantingMatrixUpgrades.isAccelerate(stack);
        }

        @Override
        public int getSlotLimit(int slot) {
            return ArcaneEnchantingMatrixUpgrades.maxAccelerate();
        }
    };

    private final ItemStackHandler dampenSlot = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return ArcaneEnchantingMatrixUpgrades.isDampen(stack);
        }

        @Override
        public int getSlotLimit(int slot) {
            return ArcaneEnchantingMatrixUpgrades.maxDampen();
        }
    };

    private final Map<AEItemKey, Template> templates = new HashMap<>();
    private final IItemHandler outputView = new OutputView();
    private final IItemHandler machineView =
            MachineItemView.demandLimited(inputBuffer, outputView, this::manualDemandFor);

    private Kind activeKind = Kind.APPARATUS;

    @Nullable
    private ResourceLocation activeRecipe;

    private int activeTicks;
    private int progress;
    private Stall stall = Stall.NONE;
    private Refusal refusal = Refusal.NONE;
    private int refusalTicks;

    enum Kind {
        APPARATUS,
        IMBUEMENT
    }

    enum Stall {
        NONE,
        INGREDIENTS,
        CATALYSTS,
        CATALYST_SHELF_FULL,
        SOURCE,
        OUTPUT_FULL,
        NO_RECIPE;

        private static Stall worse(Stall first, Stall second) {
            return first.ordinal() >= second.ordinal() ? first : second;
        }
    }

    enum Refusal {
        NONE,
        NOT_AN_ARS_PATTERN,
        NO_ITEM_OUTPUT,
        UNKNOWN_RECIPE,
        MIXED_RECIPES,
        ITEMS_ONLY,
        CATALYSTS_IN_PATTERN,
        TOO_MANY_INPUTS,
        BUFFER_FULL
    }

    private record Template(
            Kind kind,
            ResourceLocation recipe,
            List<AEKey> keys,
            List<Long> counts,
            ItemStack result,
            int sourceCost) {}

    public ArcaneEnchantingMatrixBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(
                type,
                pos,
                state,
                INPUT_SLOTS,
                OUTPUT_SLOTS,
                NepArsContent.MATRIX_ITEM.get(),
                NepConfig.arsMatrixIdleMeDrain(),
                NepConfig.arsMatrixChannels(),
                "Arcane enchanting matrix");
    }

    public static void serverTick(
            Level level, BlockPos pos, BlockState state, ArcaneEnchantingMatrixBlockEntity matrix) {
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
        if (!NepConfig.arsMatrix()) {
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
            releaseCatalysts(level);
        }
        if (Math.floorMod(phase, SOURCE_INTERVAL) == 0 && level instanceof ServerLevel server) {
            topUpSource(server);
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
        hash = 31 * hash + Boolean.hashCode(outputBlocked);
        hash = 31 * hash + Boolean.hashCode(powerFault);
        hash = 31 * hash + missingInputs.hashCode();
        hash = 31 * hash + stall.ordinal();
        hash = 31 * hash + refusal.ordinal();
        hash = 31 * hash + source.getSource() / 500;
        hash = 31 * hash + Long.hashCode(pendingJobs());
        hash = 31 * hash + nextCraftTicks();
        hash = 31 * hash + Long.hashCode(sourceNeeded());
        return hash;
    }

    private void onSourceChanged() {
        markScanNeeded();
        setChanged();
    }

    int accelerateCount() {
        ItemStack stack = accelerateSlot.getStackInSlot(0);
        return ArcaneEnchantingMatrixUpgrades.isAccelerate(stack)
                ? Math.min(stack.getCount(), ArcaneEnchantingMatrixUpgrades.maxAccelerate())
                : 0;
    }

    int dampenCount() {
        ItemStack stack = dampenSlot.getStackInSlot(0);
        return ArcaneEnchantingMatrixUpgrades.isDampen(stack)
                ? Math.min(stack.getCount(), ArcaneEnchantingMatrixUpgrades.maxDampen())
                : 0;
    }

    private int baseCraftTicks(Kind kind) {
        return kind == Kind.APPARATUS
                ? NepConfig.arsMatrixApparatusCraftTicks()
                : NepConfig.arsMatrixImbuementCraftTicks();
    }

    int craftTicksFor(Kind kind) {
        return ArcaneEnchantingMatrixUpgrades.craftTicks(baseCraftTicks(kind), accelerateCount());
    }

    private int sourceCostOf(Template template) {
        return ArcaneEnchantingMatrixUpgrades.sourceCost(template.sourceCost(), dampenCount());
    }

    private boolean drawActivePower() {
        double extra = NepConfig.arsMatrixMeDrain() - NepConfig.arsMatrixIdleMeDrain();
        if (extra <= 0) {
            power.clearStarved();
            return true;
        }
        power.extractPower(extra);
        return !power.isStarved();
    }

    long sourceNeeded() {
        AEItemKey paidFor = AEItemKey.of(activeResult);
        long needed = 0;
        for (Map.Entry<AEItemKey, Long> entry : owed.entrySet()) {
            Template template = templates.get(entry.getKey());
            if (template != null) {
                long unpaid = entry.getValue() - (entry.getKey().equals(paidFor) ? 1 : 0);
                needed += (long) sourceCostOf(template) * Math.max(0, unpaid);
            }
        }
        return Math.min(MatrixSourceStore.CAPACITY, needed);
    }

    private void topUpSource(ServerLevel level) {
        long wanted = Math.min(source.room(), sourceNeeded() - source.getSource());
        if (wanted <= 0) {
            return;
        }
        int drawn = SourceJarDraw.draw(level, getBlockPos(), NepConfig.arsMatrixSourceJarRange(), (int) wanted);
        source.add(drawn);
        long missing = wanted - drawn;
        if (missing > 0 && ARSENG_LOADED && NepConfig.arsMatrixMeSource()) {
            source.add((int) power.extractFromNetwork(ArsEngSource.key(), missing));
        }
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
            Stall attempt = start(level, template);
            if (attempt == Stall.NONE) {
                setStall(Stall.NONE);
                return true;
            }
            reason = Stall.worse(reason, attempt);
        }
        setStall(owed.isEmpty() ? Stall.NONE : reason);
        return false;
    }

    private Stall start(Level level, Template template) {
        List<Ingredient> needs = catalystNeeds(level, template);
        if (needs == null) {
            return Stall.NO_RECIPE;
        }
        ItemStack result = template.result().copy();
        if (result.isEmpty()) {
            return Stall.NO_RECIPE;
        }
        if (!fitsInOutput(result)) {
            return Stall.OUTPUT_FULL;
        }
        int[] slotLeft = new int[inputBuffer.getSlots()];
        for (int slot = 0; slot < slotLeft.length; slot++) {
            slotLeft[slot] = inputBuffer.getStackInSlot(slot).getCount();
        }
        List<int[]> takes = planConsumption(template, slotLeft);
        if (takes == null) {
            return Stall.INGREDIENTS;
        }
        List<int[]> catalystTakes = List.of();
        boolean shelving = !needs.isEmpty() && !catalysts.holds(template.recipe());
        if (shelving) {
            if (catalysts.freeSlots() < needs.size()) {
                return Stall.CATALYST_SHELF_FULL;
            }
            catalystTakes = planCatalysts(needs, slotLeft);
            if (catalystTakes == null) {
                return Stall.CATALYSTS;
            }
        }
        int cost = sourceCostOf(template);
        if (source.getSource() < cost) {
            return Stall.SOURCE;
        }

        if (shelving) {
            List<ItemStack> set = new ArrayList<>(catalystTakes.size());
            for (int[] take : catalystTakes) {
                set.add(inputBuffer.extractItem(take[0], take[1], false));
            }
            catalysts.shelve(template.recipe(), set);
        }
        claimedItems.clear();
        for (int[] take : takes) {
            ItemStack taken = inputBuffer.extractItem(take[0], take[1], false);
            if (!taken.isEmpty()) {
                claimedItems.add(taken);
            }
        }
        source.spend(cost);
        activeResult = result;
        activeKind = template.kind();
        activeRecipe = template.recipe();
        activeTicks = craftTicksFor(template.kind());
        progress = 0;
        setChanged();
        if (NepConfig.debugLogging()) {
            Nep.LOGGER.info(
                    "Arcane enchanting matrix {} started {} x{} from {} for {} source",
                    getBlockPos(),
                    activeResult.getItem(),
                    activeResult.getCount(),
                    template.recipe(),
                    cost);
        }
        return Stall.NONE;
    }

    @Nullable
    private List<int[]> planConsumption(Template template, int[] slotLeft) {
        List<int[]> takes = new ArrayList<>();
        for (int i = 0; i < template.keys().size(); i++) {
            if (!(template.keys().get(i) instanceof AEItemKey key)) {
                return null;
            }
            long need = template.counts().get(i);
            for (int slot = 0; slot < slotLeft.length && need > 0; slot++) {
                if (slotLeft[slot] <= 0 || !key.matches(inputBuffer.getStackInSlot(slot))) {
                    continue;
                }
                int take = (int) Math.min(need, slotLeft[slot]);
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

    @Nullable
    private List<int[]> planCatalysts(List<Ingredient> needs, int[] slotLeft) {
        List<int[]> takes = new ArrayList<>(needs.size());
        for (Ingredient need : needs) {
            boolean found = false;
            for (int slot = 0; slot < slotLeft.length; slot++) {
                if (slotLeft[slot] > 0 && need.test(inputBuffer.getStackInSlot(slot))) {
                    slotLeft[slot]--;
                    takes.add(new int[] {slot, 1});
                    found = true;
                    break;
                }
            }
            if (!found) {
                return null;
            }
        }
        return takes;
    }

    /** Empty for a recipe that borrows nothing, and null once the recipe the template names is gone. */
    @Nullable
    private List<Ingredient> catalystNeeds(Level level, Template template) {
        if (template.kind() == Kind.APPARATUS) {
            return ArsRecipeResolver.apparatusById(level, template.recipe()) == null ? null : List.of();
        }
        RecipeHolder<ImbuementRecipe> holder = ArsRecipeResolver.imbuementById(level, template.recipe());
        return holder == null ? null : List.copyOf(holder.value().getPedestalItems());
    }

    private void releaseCatalysts(Level level) {
        for (ResourceLocation recipe : catalysts.heldRecipes()) {
            if (stillNeedsCatalysts(level, recipe)) {
                continue;
            }
            List<ItemStack> leftovers = new ArrayList<>();
            for (ItemStack stack : catalysts.release(recipe)) {
                AEItemKey key = AEItemKey.of(stack);
                long stored = key == null ? 0 : power.dumpToNetwork(key, stack.getCount());
                if (stored < stack.getCount()) {
                    leftovers.add(stack.copyWithCount((int) (stack.getCount() - stored)));
                }
            }
            catalysts.restore(RETURNING, leftovers);
            markScanNeeded();
        }
    }

    private boolean stillNeedsCatalysts(Level level, ResourceLocation recipe) {
        if (RETURNING.equals(recipe)) {
            return false;
        }
        if (recipe.equals(activeRecipe) && !activeResult.isEmpty()) {
            return true;
        }
        for (Map.Entry<AEItemKey, Template> entry : templates.entrySet()) {
            if (recipe.equals(entry.getValue().recipe()) && owed.getOrDefault(entry.getKey(), 0L) > 0) {
                return true;
            }
        }
        RecipeHolder<ImbuementRecipe> holder = ArsRecipeResolver.imbuementById(level, recipe);
        AEItemKey result = holder == null ? null : AEItemKey.of(holder.value().getResultItem(level.registryAccess()));
        return result != null && power.scheduledOutputs(result) > 0;
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
                    "Arcane enchanting matrix {} made {} x{} ({} still owed)",
                    getBlockPos(),
                    produced,
                    result.getCount(),
                    owed.getOrDefault(produced, 0L));
        }
    }

    private void completeCraft() {
        activeResult = ItemStack.EMPTY;
        activeRecipe = null;
        claimedItems.clear();
        progress = 0;
        activeTicks = 0;
        outputBlocked = false;
        markScanNeeded();
        setChanged();
    }

    boolean readyForPatterns() {
        return NepConfig.arsMatrix();
    }

    boolean pushMatrixPattern(IPatternDetails pattern, KeyCounter[] inputs, Direction ejectionDirection) {
        Level level = getLevel();
        if (level == null || level.isClientSide || !NepConfig.arsMatrix()) {
            return false;
        }
        boolean processing = pattern.getDefinition().getItem() == AEItems.PROCESSING_PATTERN.asItem();
        if (!(pattern instanceof ApparatusPattern) && !(pattern instanceof ImbuementPattern) && !processing) {
            return reject(
                    Refusal.NOT_AN_ARS_PATTERN, "pattern is neither an apparatus, imbuement or processing pattern");
        }
        List<GenericStack> outputs = pattern.getOutputs();
        if (outputs.isEmpty() || !(outputs.get(0).what() instanceof AEItemKey declared)) {
            return reject(Refusal.NO_ITEM_OUTPUT, "pattern has no item output");
        }

        Map<AEItemKey, Long> items = new LinkedHashMap<>();
        for (KeyCounter counter : inputs) {
            for (var entry : counter) {
                if (!(entry.getKey() instanceof AEItemKey itemKey) || entry.getLongValue() <= 0) {
                    return reject(Refusal.ITEMS_ONLY, "this machine takes items only");
                }
                items.merge(itemKey, entry.getLongValue(), Long::sum);
            }
        }

        Template template = null;
        if (!(pattern instanceof ImbuementPattern)) {
            template = apparatusTemplate(pattern, level, items);
        }
        if (template == null && !(pattern instanceof ApparatusPattern)) {
            ArsRecipeResolver.ImbuementPlan plan = ArsRecipeResolver.resolveImbuement(pattern, level);
            if (plan != null && plan.catalystsInPattern()) {
                return reject(
                        Refusal.CATALYSTS_IN_PATTERN,
                        "the pattern carries the imbuement pedestal items, which the Matrix borrows from the network");
            }
            if (plan != null && items.equals(plan.expectedItems())) {
                template = templateOf(Kind.IMBUEMENT, plan.holder().id(), items, plan.result(), plan.sourceCost());
            }
        }
        if (template == null) {
            return reject(Refusal.UNKNOWN_RECIPE, "no apparatus or imbuement recipe matches the pattern and its items");
        }

        AEItemKey producedItem = AEItemKey.of(template.result());
        if (producedItem == null) {
            return reject(Refusal.NO_ITEM_OUTPUT, "recipe produces nothing");
        }
        if (isManualJob(producedItem)) {
            return reject(Refusal.MIXED_RECIPES, "a manual craft of " + producedItem + " is already queued");
        }
        Template existing = templates.get(producedItem);
        if (existing != null && !existing.recipe().equals(template.recipe())) {
            return reject(
                    Refusal.MIXED_RECIPES,
                    "a craft of " + producedItem + " from " + existing.recipe() + " is already queued");
        }
        if (items.size() > MatrixGridNode.TRACKER_SIZE) {
            return reject(
                    Refusal.TOO_MANY_INPUTS,
                    "pattern needs more than " + MatrixGridNode.TRACKER_SIZE + " distinct inputs");
        }
        if (!bufferAll(items)) {
            return reject(Refusal.BUFFER_FULL, "matrix buffer is full");
        }

        CraftedOutputs.expect(producedItem, declared);
        returnDirections.record(producedItem, ejectionDirection);
        templates.put(producedItem, template);
        owed.merge(producedItem, 1L, Long::sum);
        pushingCpus.record();
        clearRefusal();
        markScanNeeded();
        setChanged();
        if (NepConfig.debugLogging()) {
            Nep.LOGGER.info(
                    "Arcane enchanting matrix {} accepted {} for {}", getBlockPos(), template.recipe(), producedItem);
        }
        return true;
    }

    @Nullable
    private Template apparatusTemplate(IPatternDetails pattern, Level level, Map<AEItemKey, Long> items) {
        ArsRecipeResolver.ApparatusPlan plan = ArsRecipeResolver.resolve(pattern, level);
        if (plan == null) {
            return null;
        }
        if (!items.equals(plan.expectedItems())) {
            plan = ArsRecipeResolver.planFromProvided(plan, items, level);
            if (plan == null) {
                return null;
            }
        }
        return templateOf(Kind.APPARATUS, plan.holder().id(), items, plan.result(), plan.sourceCost());
    }

    @Nullable
    private static Template templateOf(
            Kind kind, ResourceLocation recipe, Map<AEItemKey, Long> items, GenericStack result, int sourceCost) {
        ItemStack stack = ArsRecipeResolver.toStack(result);
        if (stack.isEmpty()) {
            return null;
        }
        List<AEKey> keys = new ArrayList<>(items.keySet());
        List<Long> counts = new ArrayList<>(items.values());
        return new Template(kind, recipe, List.copyOf(keys), List.copyOf(counts), stack, Math.max(0, sourceCost));
    }

    @Override
    public ManualCraftOutcome startManualCraft(Player player, ResourceLocation recipe, int batches) {
        Level level = getLevel();
        if (level == null || level.isClientSide) {
            return ManualCraftOutcome.failed(ManualCraftResult.UNKNOWN_RECIPE);
        }
        if (!NepConfig.manualCrafting() || !NepConfig.arsMatrix()) {
            return ManualCraftOutcome.failed(ManualCraftResult.DISABLED);
        }

        RecipeHolder<EnchantingApparatusRecipe> apparatus = ArsRecipeResolver.apparatusById(level, recipe);
        RecipeHolder<ImbuementRecipe> imbuement =
                apparatus == null ? ArsRecipeResolver.imbuementById(level, recipe) : null;
        List<ManualRequirement> requirements;
        if (apparatus != null) {
            requirements = ArsRecipeIngredients.apparatusRequirements(apparatus.value());
        } else if (imbuement != null) {
            requirements = ArsRecipeIngredients.imbuementRequirements(imbuement.value());
        } else {
            return ManualCraftOutcome.failed(ManualCraftResult.UNKNOWN_RECIPE);
        }
        if (requirements == null || requirements.isEmpty()) {
            return ManualCraftOutcome.failed(ManualCraftResult.UNSUPPORTED);
        }

        ManualStaging.Result staged = pullFromPlayer(player, requirements, batches);
        if (staged.status() != ManualCraftResult.STARTED) {
            return ManualCraftOutcome.failed(staged.status());
        }

        Template template;
        if (apparatus != null) {
            ArsRecipeResolver.ApparatusPlan plan = ArsRecipeResolver.planFromItems(apparatus, level, staged.perCraft());
            template = plan == null
                    ? null
                    : templateOf(Kind.APPARATUS, recipe, staged.perCraft(), plan.result(), plan.sourceCost());
        } else {
            ItemStack output = imbuement.value().getResultItem(level.registryAccess());
            GenericStack result = IngredientMatching.resultOf(output);
            template = result == null
                    ? null
                    : templateOf(
                            Kind.IMBUEMENT,
                            recipe,
                            staged.perCraft(),
                            result,
                            imbuement.value().getSource());
        }
        AEItemKey produced = template == null ? null : AEItemKey.of(template.result());
        if (produced == null) {
            return ManualCraftOutcome.failed(ManualCraftResult.UNSUPPORTED);
        }
        Template existing = templates.get(produced);
        if ((existing != null && !recipe.equals(existing.recipe()))
                || (owed.containsKey(produced) && !isManualJob(produced))) {
            return ManualCraftOutcome.failed(ManualCraftResult.BUSY);
        }

        templates.put(produced, template);
        owed.merge(produced, (long) staged.batches(), Long::sum);
        manualOwed.merge(produced, (long) staged.batches(), Long::sum);
        clearRefusal();
        markScanNeeded();
        setChanged();
        if (NepConfig.debugLogging()) {
            Nep.LOGGER.info(
                    "Arcane enchanting matrix {} queued {} manual craft(s) of {} for {}",
                    getBlockPos(),
                    staged.batches(),
                    produced,
                    player.getName().getString());
        }
        return ManualCraftOutcome.started(staged.batches(), template.result());
    }

    private void autoRequest(Level level) {
        if (!NepConfig.arsMatrixAutoRequest()) {
            setMissingInputs(List.of());
            return;
        }
        Map<AEKey, Long> targets = new LinkedHashMap<>();
        for (Map.Entry<AEItemKey, Long> entry : owed.entrySet()) {
            Template template = templates.get(entry.getKey());
            if (template == null || isManualJob(entry.getKey())) {
                continue;
            }
            for (int i = 0; i < template.keys().size(); i++) {
                targets.merge(template.keys().get(i), template.counts().get(i) * entry.getValue(), Long::sum);
            }
        }
        catalystDemand(level).forEach((key, count) -> targets.merge(key, count, Long::sum));
        if (targets.isEmpty()) {
            setMissingInputs(List.of());
            return;
        }
        List<AEKey> keys = new ArrayList<>(targets.keySet());
        keys.sort(Comparator.comparing(AEKey::toString));
        List<Long> counts = new ArrayList<>(keys.size());
        for (AEKey key : keys) {
            counts.add(targets.get(key));
        }
        setMissingInputs(power.restock(level, keys, counts));
    }

    private Map<AEItemKey, Long> catalystDemand(Level level) {
        Map<AEItemKey, Long> demand = new LinkedHashMap<>();
        Set<ResourceLocation> counted = new HashSet<>();
        for (Map.Entry<AEItemKey, Template> entry : templates.entrySet()) {
            Template template = entry.getValue();
            if (template.kind() != Kind.IMBUEMENT
                    || owed.getOrDefault(entry.getKey(), 0L) <= 0
                    || catalysts.holds(template.recipe())
                    || !counted.add(template.recipe())) {
                continue;
            }
            List<Ingredient> needs = catalystNeeds(level, template);
            if (needs == null) {
                continue;
            }
            for (Ingredient need : needs) {
                AEItemKey choice = catalystChoice(need);
                if (choice != null) {
                    demand.merge(choice, 1L, Long::sum);
                }
            }
        }
        return demand;
    }

    @Nullable
    private AEItemKey catalystChoice(Ingredient need) {
        List<AEItemKey> options = IngredientMatching.itemOptions(need);
        if (options.isEmpty()) {
            return null;
        }
        for (AEItemKey option : options) {
            if (bufferedAmount(option) > 0 || power.networkStock(option) > 0) {
                return option;
            }
        }
        for (AEItemKey option : options) {
            if (power.networkCanCraft(option)) {
                return option;
            }
        }
        return options.get(0);
    }

    int manualDemandFor(ItemStack stack) {
        AEItemKey key = AEItemKey.of(stack);
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
        Level level = getLevel();
        if (level != null) {
            target += catalystNeedsMatching(level, stack);
        }
        return (int) Math.max(0, Math.min(Integer.MAX_VALUE, target - bufferedAmount(key)));
    }

    private int catalystNeedsMatching(Level level, ItemStack stack) {
        int matching = 0;
        Set<ResourceLocation> counted = new HashSet<>();
        for (Map.Entry<AEItemKey, Template> entry : templates.entrySet()) {
            Template template = entry.getValue();
            if (template.kind() != Kind.IMBUEMENT
                    || owed.getOrDefault(entry.getKey(), 0L) <= 0
                    || catalysts.holds(template.recipe())
                    || !counted.add(template.recipe())) {
                continue;
            }
            List<Ingredient> needs = catalystNeeds(level, template);
            if (needs != null) {
                for (Ingredient need : needs) {
                    if (need.test(stack)) {
                        matching++;
                    }
                }
            }
        }
        return matching;
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
            Nep.LOGGER.info("Arcane enchanting matrix {} rejected pattern: {}", getBlockPos(), reason);
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
        if (!state.hasProperty(ArcaneEnchantingMatrixBlock.STATUS)) {
            return;
        }
        MatrixStatus desired = desiredStatus();
        if (state.getValue(ArcaneEnchantingMatrixBlock.STATUS) != desired) {
            level.setBlock(
                    getBlockPos(), state.setValue(ArcaneEnchantingMatrixBlock.STATUS, desired), Block.UPDATE_CLIENTS);
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

    int storedSource() {
        return source.getSource();
    }

    int activeTicks() {
        return activeTicks;
    }

    Kind activeKind() {
        return activeKind;
    }

    int nextCraftTicks() {
        for (Map.Entry<AEItemKey, Long> entry : owed.entrySet()) {
            Template template = templates.get(entry.getKey());
            if (entry.getValue() > 0 && template != null) {
                return craftTicksFor(template.kind());
            }
        }
        return 0;
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
                    "Arcane enchanting matrix {} cleared {} pending craft(s) and cancelled {} network job(s)",
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
        if (activeResult.isEmpty()) {
            for (ItemStack stack : catalysts.clear()) {
                if (!player.getInventory().add(stack)) {
                    player.drop(stack, false);
                }
            }
        }
        setChanged();
    }

    IItemHandler itemHandlerForSide() {
        return machineView;
    }

    MatrixSourceStore sourceStorage() {
        return source;
    }

    IItemHandler getAccelerateSlot() {
        return accelerateSlot;
    }

    IItemHandler getDampenSlot() {
        return dampenSlot;
    }

    IItemHandler getCatalystShelf() {
        return catalysts.items();
    }

    void dropBuffers(Level level, BlockPos pos) {
        List<ItemStack> drops = new ArrayList<>();
        for (ItemStackHandler handler : List.of(inputBuffer, outputBuffer, accelerateSlot, dampenSlot)) {
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                drops.add(handler.getStackInSlot(slot));
            }
        }
        drops.addAll(claimedItems);
        drops.addAll(catalysts.clear());
        for (ItemStack stack : drops) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
        }
        clearContent();
    }

    @Override
    public void clearContent() {
        super.clearContent();
        catalysts.clear();
        clearHandler(accelerateSlot);
        clearHandler(dampenSlot);
        templates.clear();
        activeRecipe = null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put(INPUT_KEY, inputBuffer.serializeNBT(registries));
        tag.put(OUTPUT_KEY, outputBuffer.serializeNBT(registries));
        tag.putInt(SOURCE_KEY, source.getSource());
        tag.put(CATALYSTS_KEY, catalysts.save(registries));
        tag.put(ACCELERATE_KEY, accelerateSlot.serializeNBT(registries));
        tag.put(DAMPEN_KEY, dampenSlot.serializeNBT(registries));
        tag.put(OWED_KEY, ItemCounts.save(owed, registries));
        tag.put(RETURN_KEY, ItemCounts.save(toReturn, registries));
        tag.put(RETURN_DIRS_KEY, returnDirections.save(registries));
        tag.put(TEMPLATE_KEY, saveTemplates(registries));
        if (!activeResult.isEmpty()) {
            tag.put(ACTIVE_KEY, activeResult.save(registries));
        }
        if (activeRecipe != null) {
            tag.putString(ACTIVE_RECIPE_KEY, activeRecipe.toString());
        }
        tag.putString(ACTIVE_KIND_KEY, activeKind.name());
        tag.putInt(ACTIVE_TICKS_KEY, activeTicks);
        ListTag claimed = new ListTag();
        for (ItemStack stack : claimedItems) {
            if (!stack.isEmpty()) {
                claimed.add(stack.save(registries));
            }
        }
        tag.put(CLAIMED_KEY, claimed);
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
        source.setSource(tag.getInt(SOURCE_KEY));
        catalysts.load(tag.getCompound(CATALYSTS_KEY), registries);
        loadBuffer(accelerateSlot, registries, tag.getCompound(ACCELERATE_KEY));
        loadBuffer(dampenSlot, registries, tag.getCompound(DAMPEN_KEY));
        ItemCounts.load(owed, tag, OWED_KEY, registries);
        ItemCounts.load(toReturn, tag, RETURN_KEY, registries);
        returnDirections.load(tag, RETURN_DIRS_KEY, "ReturnDir", toReturn.keySet(), registries);
        loadTemplates(tag.getList(TEMPLATE_KEY, Tag.TAG_COMPOUND), registries);
        activeResult = tag.contains(ACTIVE_KEY)
                ? ItemStack.parse(registries, tag.getCompound(ACTIVE_KEY)).orElse(ItemStack.EMPTY)
                : ItemStack.EMPTY;
        activeRecipe =
                tag.contains(ACTIVE_RECIPE_KEY) ? ResourceLocation.tryParse(tag.getString(ACTIVE_RECIPE_KEY)) : null;
        activeKind = byName(Kind.values(), tag.getString(ACTIVE_KIND_KEY), Kind.APPARATUS);
        activeTicks = tag.getInt(ACTIVE_TICKS_KEY);
        claimedItems.clear();
        ListTag claimed = tag.getList(CLAIMED_KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < claimed.size(); i++) {
            ItemStack.parse(registries, claimed.getCompound(i)).ifPresent(claimedItems::add);
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
        for (Map.Entry<AEItemKey, Template> entry : templates.entrySet()) {
            Template template = entry.getValue();
            CompoundTag tag = new CompoundTag();
            ItemCounts.putKey(tag, entry.getKey(), registries);
            tag.putString("Kind", template.kind().name());
            tag.putString("Recipe", template.recipe().toString());
            tag.put("Result", template.result().save(registries));
            tag.putInt("SourceCost", template.sourceCost());
            ListTag stacks = new ListTag();
            for (int i = 0; i < template.keys().size(); i++) {
                stacks.add(GenericStack.writeTag(
                        registries,
                        new GenericStack(
                                template.keys().get(i), template.counts().get(i))));
            }
            tag.put("Stacks", stacks);
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
            ItemStack result =
                    ItemStack.parse(registries, tag.getCompound("Result")).orElse(ItemStack.EMPTY);
            if (item == null || recipe == null || result.isEmpty()) {
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
                                byName(Kind.values(), tag.getString("Kind"), Kind.APPARATUS),
                                recipe,
                                List.copyOf(keys),
                                List.copyOf(counts),
                                result,
                                tag.getInt("SourceCost")));
            }
        }
    }

    @Override
    public Component getDisplayName() {
        return NepArsContent.MATRIX.get().getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory inventory, Player player) {
        return new ArcaneEnchantingMatrixMenu(windowId, inventory, getBlockPos());
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
