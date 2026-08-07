package dev.rylex.nep.compat.compactcrafting;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import dev.compactmods.crafting.api.EnumCraftingState;
import dev.compactmods.crafting.api.field.IMiniaturizationField;
import dev.compactmods.crafting.api.field.MiniaturizationFieldSize;
import dev.compactmods.crafting.field.MiniaturizationField;
import dev.compactmods.crafting.recipes.MiniaturizationRecipe;
import dev.rylex.nep.Nep;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.compat.compactcrafting.MiniaturizationControllerBlock.ControllerStatus;
import dev.rylex.nep.machine.BufferedMatrixBlockEntity;
import dev.rylex.nep.machine.MachineItemView;
import dev.rylex.nep.machine.MatrixGridNode;
import dev.rylex.nep.machine.RedstoneMode;
import dev.rylex.nep.pattern.MiniaturizationPattern;
import dev.rylex.nep.util.ItemCounts;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MiniaturizationControllerBlockEntity extends BufferedMatrixBlockEntity {

    static final int INPUT_SLOTS = 27;
    static final int OUTPUT_SLOTS = 9;

    private static final int RESTOCK_INTERVAL = 10;
    private static final int RUNNING_GRACE_TICKS = 10;
    private static final int REFUSAL_MEMORY_TICKS = 200;

    private static final int SCAN_TIMEOUT_TICKS = 60;

    private static final int CATALYST_TIMEOUT_TICKS = 100;

    private static final int COLLECT_TIMEOUT_TICKS = 60;

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
    private static final String PHASE_KEY = "Phase";
    private static final String PHASE_TICKS_KEY = "PhaseTicks";
    private static final String RECIPE_KEY = "Recipe";
    private static final String ANCHOR_KEY = "Anchor";
    private static final String CENTER_KEY = "Center";
    private static final String FIELD_SIZE_KEY = "FieldSize";
    private static final String CATALYST_ID_KEY = "CatalystId";
    private static final String COLLECTED_KEY = "Collected";
    private static final String BUILT_KEY = "Built";

    private final Map<Item, Template> templates = new HashMap<>();
    private final IItemHandler outputView = new OutputView();
    private final IItemHandler machineView =
            MachineItemView.demandLimited(inputBuffer, outputView, this::manualDemandFor);

    private int progress;
    private int craftTicks;

    private Phase phase = Phase.IDLE;
    private int phaseTicks;
    private int collected;
    private int built;

    @Nullable
    private List<MiniaturizationLayout.Placement> plan;

    @Nullable
    private ResourceLocation activeRecipe;

    @Nullable
    private BlockPos anchor;

    @Nullable
    private BlockPos fieldCenter;

    @Nullable
    private MiniaturizationFieldSize fieldSize;

    @Nullable
    private UUID catalystId;

    private Stall stall = Stall.NONE;
    private Refusal refusal = Refusal.NONE;
    private int refusalTicks;

    enum Phase {
        IDLE,
        BUILDING,
        SCANNING,
        CATALYST,
        CRAFTING,
        COLLECTING
    }

    enum Stall {
        NONE,
        INGREDIENTS,
        OUTPUT_FULL,
        NO_FIELD,
        FIELD_DISABLED,
        FIELD_UNLOADED,
        FIELD_BUSY,
        FIELD_OCCUPIED,
        FIELD_TOO_SMALL,
        INSIDE_FIELD,
        NO_MATCH,
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
        ITEMS_ONLY,
        TOO_MANY_INPUTS,
        BUFFER_FULL
    }

    private record Template(
            List<AEKey> keys, List<Long> counts, @Nullable ResourceLocation recipe) {}

    public MiniaturizationControllerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(
                type,
                pos,
                state,
                INPUT_SLOTS,
                OUTPUT_SLOTS,
                NepCompactCraftingContent.CONTROLLER_ITEM.get(),
                NepConfig.compactCraftingControllerIdleMeDrain(),
                "Miniaturization controller");
    }

    public static void serverTick(
            Level level, BlockPos pos, BlockState state, MiniaturizationControllerBlockEntity controller) {
        controller.tick(level);
    }

    private void tick(Level level) {
        if (runningGrace > 0) {
            runningGrace--;
        }
        if (refusalTicks > 0 && --refusalTicks == 0) {
            refusal = Refusal.NONE;
        }
        flushOutput(level);
        if (!NepConfig.compactCraftingMiniaturizationController()) {
            setPowerFault(false);
            refreshComparator(level);
            refreshVisualState(level);
            syncIfChanged(level);
            return;
        }
        power.create(level, getBlockPos());

        long beat = level.getGameTime() + getBlockPos().hashCode();
        if (Math.floorMod(beat, RESTOCK_INTERVAL) == 0) {
            if (owed.isEmpty()) {
                setMissingInputs(List.of());
            } else {
                autoRequest(level);
            }
        }
        if (owed.isEmpty() && phase == Phase.IDLE) {
            setStall(Stall.NONE);
        }

        if (power.isPowered()) {
            IMiniaturizationField<MiniaturizationRecipe> field = FieldBinding.adjacentField(level, getBlockPos());
            switch (phase) {
                case IDLE -> {
                    power.clearStarved();
                    if (scanNeeded || Math.floorMod(beat, RESTOCK_INTERVAL) == 0) {
                        scanNeeded = false;
                        tryBegin(level, field);
                    }
                }
                case BUILDING -> buildLayout(level, field);
                case SCANNING -> awaitMatch(level, field);
                case CATALYST -> awaitCatalyst(level, field);
                case CRAFTING -> awaitCraft(level, field);
                case COLLECTING -> collect(level);
            }
        }
        setPowerFault(!power.hasUsablePower());

        refreshComparator(level);
        refreshVisualState(level);
        syncIfChanged(level);
    }

    private void tryBegin(Level level, @Nullable IMiniaturizationField<MiniaturizationRecipe> field) {
        if (owed.isEmpty()) {
            setStall(Stall.NONE);
            return;
        }
        if (field == null) {
            setStall(Stall.NO_FIELD);
            return;
        }
        if (!field.enabled()) {
            setStall(Stall.FIELD_DISABLED);
            return;
        }
        if (!field.isAreaLoaded()) {
            setStall(Stall.FIELD_UNLOADED);
            return;
        }
        if (field.getBounds().contains(Vec3.atCenterOf(getBlockPos()))) {
            setStall(Stall.INSIDE_FIELD);
            return;
        }
        if (field.getCraftingState() != EnumCraftingState.NOT_MATCHED) {
            setStall(Stall.FIELD_BUSY);
            return;
        }

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
            Stall attempt = startCraft(level, field, holder);
            if (attempt == Stall.NONE) {
                setStall(Stall.NONE);
                return;
            }
            reason = Stall.worse(reason, attempt);
        }
        setStall(reason);
    }

    private Stall startCraft(
            Level level,
            IMiniaturizationField<MiniaturizationRecipe> field,
            RecipeHolder<MiniaturizationRecipe> holder) {
        MiniaturizationRecipe recipe = holder.value();
        if (!recipe.fitsInFieldSize(field.getFieldSize())) {
            return Stall.FIELD_TOO_SMALL;
        }
        GenericStack output = MiniaturizationRecipeIngredients.singleOutput(recipe);
        if (output == null || !(output.what() instanceof AEItemKey key)) {
            return Stall.NO_RECIPE;
        }
        ItemStack result = key.toStack((int) output.amount());
        if (!fitsInOutput(result)) {
            return Stall.OUTPUT_FULL;
        }
        if (recipe.catalyst().isEmpty()) {
            return Stall.NO_RECIPE;
        }
        BlockPos anchorPos = MiniaturizationLayout.anchorFor(recipe, field.getCenter());
        List<MiniaturizationLayout.Placement> layout = MiniaturizationLayout.plan(recipe, anchorPos);
        if (layout == null) {
            return Stall.NO_RECIPE;
        }
        if (!MiniaturizationLayout.isClear(level, field.getBounds())) {
            return Stall.FIELD_OCCUPIED;
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
        activeRecipe = holder.id();
        anchor = anchorPos;
        fieldCenter = field.getCenter();
        fieldSize = field.getFieldSize();
        craftTicks = Math.max(1, recipe.getCraftingTime());
        progress = 0;
        collected = 0;
        catalystId = null;
        plan = layout;
        built = 0;
        enterPhase(Phase.BUILDING);
        setChanged();
        if (NepConfig.debugLogging()) {
            Nep.LOGGER.info(
                    "Miniaturization controller {} started building {} blocks for {} in the field at {}",
                    getBlockPos(),
                    layout.size(),
                    holder.id(),
                    field.getCenter());
        }
        return Stall.NONE;
    }

    private void buildLayout(Level level, @Nullable IMiniaturizationField<MiniaturizationRecipe> field) {
        if (!fieldStillUsable(field)) {
            abortBuild(level, Stall.NO_FIELD);
            return;
        }
        List<MiniaturizationLayout.Placement> layout = activePlan(level);
        if (layout == null) {
            abortBuild(level, Stall.NO_RECIPE);
            return;
        }
        int perTick = Math.max(1, NepConfig.compactCraftingControllerBlocksPerTick());
        for (int placed = 0; placed < perTick && built < layout.size(); placed++) {
            MiniaturizationLayout.Placement placement = layout.get(built);
            BlockState standing = level.getBlockState(placement.pos());
            if (!standing.isAir() && !standing.is(placement.state().getBlock())) {
                abortBuild(level, Stall.FIELD_OCCUPIED);
                return;
            }
            level.setBlock(placement.pos(), placement.state(), Block.UPDATE_ALL);
            built++;
        }
        setChanged();
        if (built >= layout.size()) {
            enterPhase(Phase.SCANNING);
            forceRescan(field);
        }
    }

    private void forceRescan(IMiniaturizationField<MiniaturizationRecipe> field) {
        field.fieldContentsChanged();
        if (field instanceof MiniaturizationField concrete) {
            concrete.doRecipeScan();
        }
    }

    private void awaitMatch(Level level, @Nullable IMiniaturizationField<MiniaturizationRecipe> field) {
        if (!fieldStillUsable(field)) {
            abortBuild(level, Stall.NO_FIELD);
            return;
        }
        switch (field.getCraftingState()) {
            case MATCHED -> {
                RecipeHolder<MiniaturizationRecipe> matched = field.recipeHolder();
                if (matched == null || !matched.id().equals(activeRecipe)) {
                    abortBuild(level, Stall.NO_MATCH);
                    return;
                }
                spawnCatalyst(level, matched.value());
                enterPhase(Phase.CATALYST);
            }
            case CRAFTING -> enterPhase(Phase.CRAFTING);
            case NOT_MATCHED -> {
                if (++phaseTicks > SCAN_TIMEOUT_TICKS) {
                    abortBuild(level, Stall.NO_MATCH);
                }
            }
        }
    }

    private void awaitCatalyst(Level level, @Nullable IMiniaturizationField<MiniaturizationRecipe> field) {
        if (!fieldStillUsable(field)) {
            abortBuild(level, Stall.NO_FIELD);
            return;
        }
        switch (field.getCraftingState()) {
            case CRAFTING -> enterPhase(Phase.CRAFTING);
            case NOT_MATCHED -> abortBuild(level, Stall.NO_MATCH);
            case MATCHED -> {
                if (++phaseTicks > CATALYST_TIMEOUT_TICKS) {
                    abortBuild(level, Stall.NO_MATCH);
                }
            }
        }
    }

    private void awaitCraft(Level level, @Nullable IMiniaturizationField<MiniaturizationRecipe> field) {
        if (field == null) {
            failCraft(level, "the field went away mid-craft");
            return;
        }
        if (field.getCraftingState() != EnumCraftingState.CRAFTING) {
            enterPhase(Phase.COLLECTING);
            return;
        }
        if (!drawActivePower()) {
            return;
        }
        runningGrace = RUNNING_GRACE_TICKS;
        int reported = field.getProgress();
        if (reported != progress) {
            progress = reported;
            setChanged();
        }
    }

    private void collect(Level level) {
        AABB bounds = craftBounds();
        if (bounds == null) {
            failCraft(level, "the craft lost track of its field");
            return;
        }
        reclaimCatalyst(level);
        if (!activeResult.isEmpty()) {
            AEItemKey wanted = AEItemKey.of(activeResult);
            for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, bounds.inflate(0.5))) {
                ItemStack stack = entity.getItem();
                if (stack.isEmpty() || wanted == null || !wanted.matches(stack)) {
                    continue;
                }
                ItemStack leftover = ItemHandlerHelper.insertItem(outputBuffer, stack.copy(), false);
                int moved = stack.getCount() - leftover.getCount();
                if (moved <= 0) {
                    outputBlocked = true;
                    continue;
                }
                outputBlocked = false;
                collected += moved;
                if (leftover.isEmpty()) {
                    entity.discard();
                } else {
                    entity.setItem(leftover);
                }
            }
        }
        if (collected >= activeResult.getCount() || ++phaseTicks > COLLECT_TIMEOUT_TICKS) {
            finishCraft(level);
        }
    }

    private void finishCraft(Level level) {
        if (collected <= 0) {
            failCraft(level, "the field produced nothing this controller could collect");
            return;
        }
        Item produced = activeResult.getItem();
        int amount = collected;
        if (owed.getOrDefault(produced, 0L) > 0) {
            decrement(owed, produced, 1);
            if (!owed.containsKey(produced)) {
                templates.remove(produced);
            }
            if (owed.isEmpty()) {
                pushingCpus.clear();
            }
            toReturn.merge(produced, (long) amount, Long::sum);
        }
        resetCraft();
        flushOutput(level);
        if (NepConfig.debugLogging()) {
            Nep.LOGGER.info(
                    "Miniaturization controller {} collected {} x{} ({} still owed)",
                    getBlockPos(),
                    produced,
                    amount,
                    owed.getOrDefault(produced, 0L));
        }
    }

    private void failCraft(Level level, String reason) {
        reclaimCatalyst(level);
        Nep.LOGGER.warn("Miniaturization controller {} lost a craft: {}", getBlockPos(), reason);
        resetCraft();
        setStall(Stall.NO_FIELD);
    }

    private void abortBuild(Level level, Stall reason) {
        Map<Item, Integer> unrecovered = clearPlacedBlocks(level);
        reclaimCatalyst(level);
        List<ItemStack> returning = List.copyOf(claimedItems);
        claimedItems.clear();
        deductUnrecovered(returning, unrecovered);
        for (ItemStack stack : returning) {
            if (stack.isEmpty()) {
                continue;
            }
            ItemStack leftover = ItemHandlerHelper.insertItem(inputBuffer, stack.copy(), false);
            if (!leftover.isEmpty()) {
                Containers.dropItemStack(
                        level,
                        getBlockPos().getX(),
                        getBlockPos().getY(),
                        getBlockPos().getZ(),
                        leftover);
            }
        }
        if (NepConfig.debugLogging()) {
            Nep.LOGGER.info(
                    "Miniaturization controller {} abandoned its build for {}: {}",
                    getBlockPos(),
                    activeRecipe,
                    reason);
        }
        resetCraft();
        setStall(reason);
    }

    private Map<Item, Integer> clearPlacedBlocks(Level level) {
        List<MiniaturizationLayout.Placement> layout = activePlan(level);
        if (layout == null) {
            return Map.of();
        }
        Map<Item, Integer> unrecovered = new HashMap<>();
        for (MiniaturizationLayout.Placement placement : layout.subList(0, Math.min(built, layout.size()))) {
            if (level.getBlockState(placement.pos()).is(placement.state().getBlock())) {
                level.setBlock(placement.pos(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            } else {
                unrecovered.merge(placement.state().getBlock().asItem(), 1, Integer::sum);
            }
        }
        return unrecovered;
    }

    private static void deductUnrecovered(List<ItemStack> stacks, Map<Item, Integer> unrecovered) {
        for (ItemStack stack : stacks) {
            int lost = Math.min(unrecovered.getOrDefault(stack.getItem(), 0), stack.getCount());
            if (lost > 0) {
                unrecovered.merge(stack.getItem(), -lost, Integer::sum);
                stack.shrink(lost);
            }
        }
    }

    @Nullable
    private List<MiniaturizationLayout.Placement> activePlan(Level level) {
        if (plan != null) {
            return plan;
        }
        BlockPos anchorPos = anchor;
        ResourceLocation recipeId = activeRecipe;
        if (anchorPos == null || recipeId == null) {
            return null;
        }
        RecipeHolder<MiniaturizationRecipe> holder = MiniaturizationRecipeResolver.resolveById(level, recipeId);
        if (holder == null) {
            return null;
        }
        plan = MiniaturizationLayout.plan(holder.value(), anchorPos);
        return plan;
    }

    private void spawnCatalyst(Level level, MiniaturizationRecipe recipe) {
        BlockPos center = fieldCenter;
        ItemStack catalyst = recipe.catalyst().copy();
        if (center == null || catalyst.isEmpty()) {
            return;
        }
        takeClaimed(catalyst.getItem(), catalyst.getCount());
        ItemEntity entity = new ItemEntity(
                level, center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5, catalyst, 0.0, 0.0, 0.0);
        entity.setNoGravity(true);
        entity.setNeverPickUp();
        entity.setUnlimitedLifetime();
        level.addFreshEntity(entity);
        catalystId = entity.getUUID();
        setChanged();
    }

    private void reclaimCatalyst(Level level) {
        UUID id = catalystId;
        if (id == null || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        catalystId = null;
        if (!(serverLevel.getEntity(id) instanceof ItemEntity entity)) {
            return;
        }
        ItemStack stack = entity.getItem().copy();
        entity.discard();
        if (stack.isEmpty()) {
            return;
        }
        ItemStack leftover = ItemHandlerHelper.insertItem(inputBuffer, stack, false);
        if (!leftover.isEmpty()) {
            Containers.dropItemStack(
                    level,
                    getBlockPos().getX(),
                    getBlockPos().getY(),
                    getBlockPos().getZ(),
                    leftover);
        }
    }

    private void takeClaimed(Item item, int count) {
        Iterator<ItemStack> stacks = claimedItems.iterator();
        while (stacks.hasNext() && count > 0) {
            ItemStack stack = stacks.next();
            if (!stack.is(item)) {
                continue;
            }
            int taken = Math.min(count, stack.getCount());
            stack.shrink(taken);
            count -= taken;
            if (stack.isEmpty()) {
                stacks.remove();
            }
        }
    }

    private void resetCraft() {
        activeResult = ItemStack.EMPTY;
        activeRecipe = null;
        anchor = null;
        fieldCenter = null;
        fieldSize = null;
        catalystId = null;
        plan = null;
        built = 0;
        claimedItems.clear();
        progress = 0;
        craftTicks = 0;
        collected = 0;
        outputBlocked = false;
        enterPhase(Phase.IDLE);
        markScanNeeded();
        setChanged();
    }

    private void enterPhase(Phase next) {
        if (phase != next) {
            phase = next;
            setChanged();
        }
        phaseTicks = 0;
    }

    private boolean fieldStillUsable(@Nullable IMiniaturizationField<MiniaturizationRecipe> field) {
        return field != null && field.enabled() && field.isAreaLoaded();
    }

    @Nullable
    private AABB craftBounds() {
        BlockPos center = fieldCenter;
        MiniaturizationFieldSize size = fieldSize;
        return center == null || size == null ? null : size.getBoundsAtPosition(center);
    }

    private boolean drawActivePower() {
        double extra = NepConfig.compactCraftingControllerMeDrain() - NepConfig.compactCraftingControllerIdleMeDrain();
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

    boolean readyForPatterns() {
        Level level = getLevel();
        return NepConfig.compactCraftingMiniaturizationController()
                && level != null
                && !level.isClientSide
                && FieldBinding.adjacentField(level, getBlockPos()) != null;
    }

    boolean pushControllerPattern(IPatternDetails pattern, KeyCounter[] inputs, Direction ejectionDirection) {
        Level level = getLevel();
        if (level == null || level.isClientSide || !NepConfig.compactCraftingMiniaturizationController()) {
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
            return reject(Refusal.BUFFER_FULL, "controller buffer is full");
        }

        returnDirections.record(producedItem, ejectionDirection);
        captureTemplate(producedItem, items, miniaturization.recipe());
        owed.merge(producedItem, 1L, Long::sum);
        pushingCpus.record();
        clearRefusal();
        markScanNeeded();
        setChanged();
        if (NepConfig.debugLogging()) {
            Nep.LOGGER.info("Miniaturization controller {} accepted pattern for {}", getBlockPos(), outputKey);
        }
        return true;
    }

    private boolean reject(Refusal kind, String reason) {
        if (refusal != kind) {
            refusal = kind;
            setChanged();
        }
        refusalTicks = REFUSAL_MEMORY_TICKS;
        if (NepConfig.debugLogging()) {
            Nep.LOGGER.info("Miniaturization controller {} rejected pattern: {}", getBlockPos(), reason);
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
        if (!NepConfig.compactCraftingControllerAutoRequest()) {
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

    @Override
    protected int comparatorOutput() {
        return switch (redstoneMode) {
            case OUTPUT -> RedstoneMode.fullness(outputBuffer);
            case INPUT -> RedstoneMode.fullness(inputBuffer);
            case STATUS -> statusSignal();
        };
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
        hash = 31 * hash + phase.ordinal();
        hash = 31 * hash + craftTicks;
        hash = 31 * hash + Long.hashCode(pendingJobs());
        hash = 31 * hash + redstoneMode.ordinal();
        hash = 31 * hash + (fieldSize == null ? 0 : fieldSize.ordinal());
        return hash;
    }

    private void refreshVisualState(Level level) {
        BlockState state = getBlockState();
        if (!state.hasProperty(MiniaturizationControllerBlock.STATUS)) {
            return;
        }
        ControllerStatus desired = desiredStatus();
        if (state.getValue(MiniaturizationControllerBlock.STATUS) != desired) {
            level.setBlock(
                    getBlockPos(),
                    state.setValue(MiniaturizationControllerBlock.STATUS, desired),
                    Block.UPDATE_CLIENTS);
        }
    }

    private ControllerStatus desiredStatus() {
        if (runningGrace > 0 || phase == Phase.CRAFTING || phase == Phase.BUILDING) {
            return ControllerStatus.RUNNING;
        }
        if (phase != Phase.IDLE || stall != Stall.NONE) {
            return ControllerStatus.STALLED;
        }
        return ControllerStatus.IDLE;
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

    int builtBlocks() {
        return built;
    }

    Phase phase() {
        return phase;
    }

    Stall stall() {
        return stall;
    }

    Refusal refusal() {
        return refusal;
    }

    @Nullable
    MiniaturizationFieldSize boundFieldSize() {
        MiniaturizationFieldSize active = fieldSize;
        if (active != null) {
            return active;
        }
        Level level = getLevel();
        if (level == null) {
            return null;
        }
        IMiniaturizationField<MiniaturizationRecipe> field = FieldBinding.adjacentField(level, getBlockPos());
        return field == null ? null : field.getFieldSize();
    }

    long pendingJobs() {
        long total = 0;
        for (long count : owed.values()) {
            total += count;
        }
        return total;
    }

    private void setStall(Stall reason) {
        if (stall != reason) {
            stall = reason;
            setChanged();
        }
    }

    void clearPending() {
        clearRefusal();
        if (owed.isEmpty() && templates.isEmpty()) {
            return;
        }
        int cancelled = pushingCpus.cancelJobsFor(power.grid(), Set.copyOf(templates.keySet()));
        if (NepConfig.debugLogging()) {
            Nep.LOGGER.info(
                    "Miniaturization controller {} cleared {} pending craft(s) and cancelled {} network job(s)",
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
        Map<Item, Integer> unrecovered = clearPlacedBlocks(level);
        reclaimCatalyst(level);
        for (int slot = 0; slot < inputBuffer.getSlots(); slot++) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), inputBuffer.getStackInSlot(slot));
        }
        for (int slot = 0; slot < outputBuffer.getSlots(); slot++) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), outputBuffer.getStackInSlot(slot));
        }
        deductUnrecovered(claimedItems, unrecovered);
        for (ItemStack stack : claimedItems) {
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
            }
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
        tag.putString(PHASE_KEY, phase.name());
        tag.putInt(PHASE_TICKS_KEY, phaseTicks);
        tag.putInt(COLLECTED_KEY, collected);
        tag.putInt(BUILT_KEY, built);
        if (activeRecipe != null) {
            tag.putString(RECIPE_KEY, activeRecipe.toString());
        }
        if (anchor != null) {
            tag.put(ANCHOR_KEY, NbtUtils.writeBlockPos(anchor));
        }
        if (fieldCenter != null) {
            tag.put(CENTER_KEY, NbtUtils.writeBlockPos(fieldCenter));
        }
        if (fieldSize != null) {
            tag.putString(FIELD_SIZE_KEY, fieldSize.name());
        }
        if (catalystId != null) {
            tag.putUUID(CATALYST_ID_KEY, catalystId);
        }
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
        phase = byName(Phase.values(), tag.getString(PHASE_KEY), Phase.IDLE);
        phaseTicks = tag.getInt(PHASE_TICKS_KEY);
        collected = tag.getInt(COLLECTED_KEY);
        built = tag.getInt(BUILT_KEY);
        plan = null;
        activeRecipe = tag.contains(RECIPE_KEY) ? ResourceLocation.tryParse(tag.getString(RECIPE_KEY)) : null;
        anchor = tag.contains(ANCHOR_KEY)
                ? NbtUtils.readBlockPos(tag, ANCHOR_KEY).orElse(null)
                : null;
        fieldCenter = tag.contains(CENTER_KEY)
                ? NbtUtils.readBlockPos(tag, CENTER_KEY).orElse(null)
                : null;
        fieldSize = tag.contains(FIELD_SIZE_KEY)
                ? byName(MiniaturizationFieldSize.values(), tag.getString(FIELD_SIZE_KEY), null)
                : null;
        catalystId = tag.hasUUID(CATALYST_ID_KEY) ? tag.getUUID(CATALYST_ID_KEY) : null;
        markScanNeeded();
    }

    @Nullable
    private static <E extends Enum<E>> E byName(E[] values, String name, @Nullable E fallback) {
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
        return NepCompactCraftingContent.CONTROLLER.get().getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory inventory, Player player) {
        return new MiniaturizationControllerMenu(windowId, inventory, getBlockPos());
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
