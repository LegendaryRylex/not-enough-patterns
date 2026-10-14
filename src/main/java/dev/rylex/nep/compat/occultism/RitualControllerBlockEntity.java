package dev.rylex.nep.compat.occultism;

import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.IGrid;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import com.klikli_dev.occultism.common.blockentity.GoldenSacrificialBowlBlockEntity;
import com.klikli_dev.occultism.common.blockentity.SacrificialBowlBlockEntity;
import com.klikli_dev.occultism.crafting.recipe.RitualRecipe;
import com.mojang.authlib.GameProfile;
import dev.rylex.nep.Nep;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.compat.occultism.RitualControllerBlock.ControllerStatus;
import dev.rylex.nep.compat.occultism.RitualControllerPersistence.Placed;
import dev.rylex.nep.compat.occultism.RitualControllerPersistence.Template;
import dev.rylex.nep.machine.BufferedMatrixBlockEntity;
import dev.rylex.nep.machine.CraftedOutputs;
import dev.rylex.nep.machine.DemandTally;
import dev.rylex.nep.machine.IncidentalComponents;
import dev.rylex.nep.machine.MachineItemView;
import dev.rylex.nep.machine.MatrixGridNode;
import dev.rylex.nep.pattern.RitualPattern;
import dev.rylex.nep.util.EnumNames;
import dev.rylex.nep.util.ItemCounts;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RitualControllerBlockEntity extends BufferedMatrixBlockEntity {

    static final int INPUT_SLOTS = 21;
    static final int OUTPUT_SLOTS = 9;

    private static final int RESTOCK_INTERVAL = 10;
    private static final int REPAIR_INTERVAL = 100;
    private static final int RUNNING_GRACE_TICKS = 10;
    private static final int REFUSAL_MEMORY_TICKS = 200;
    private static final int RETRY_COOLDOWN_TICKS = 100;
    private static final int BOWL_RESCAN_TICKS = 100;
    private static final int PROGRESS_STEPS = 64;
    private static final int SEARCH_BUDGET = 20_000;
    private static final double SWEEP_RADIUS = 1.5;

    private static final GameProfile PROFILE =
            new GameProfile(UUID.fromString("6e9f3a52-7c1d-4b8e-9a2f-1d5c0b7e4a91"), "[NEP Ritual Controller]");

    private static final String INPUT_KEY = "Input";
    private static final String OUTPUT_KEY = "Output";
    private static final String BOOK_KEY = "Book";
    private static final String OWED_KEY = "Owed";
    private static final String RETURN_KEY = "ToReturn";
    private static final String RETURN_DIRS_KEY = "ReturnDirs";
    private static final String TEMPLATE_KEY = "Template";
    private static final String NODE_KEY = "Node";
    private static final String ACTIVE_KEY = "Active";
    private static final String DECLARED_KEY = "Declared";
    private static final String MISSING_KEY = "Missing";
    private static final String BLOCKED_KEY = "Blocked";
    private static final String POWER_FAULT_KEY = "PowerFault";
    private static final String STALL_KEY = "Stall";
    private static final String REFUSAL_KEY = "Refusal";
    private static final String REFUSAL_TICKS_KEY = "RefusalTicks";
    private static final String PHASE_KEY = "Phase";
    private static final String RECIPE_KEY = "Recipe";
    private static final String COOLDOWN_KEY = "Cooldown";
    private static final String BOWL_KEY = "Bowl";
    private static final String PLACED_KEY = "Placed";
    private static final String RECEIVED_KEY = "Received";
    private static final String EXTRAS_KEY = "Extras";
    private static final String REPAIR_JOB_KEY = "RepairJob";
    private static final String REPAIR_KEY = "Repair";
    private static final String SPEED_CARRY_KEY = "SpeedCarry";
    private static final String TIME_KEY = "RitualTime";
    private static final String DURATION_KEY = "RitualDuration";

    private final ItemStackHandler book = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return stack.is(NepOccultismContent.SPIRIT_INFUSED_BOOK.get())
                    && stack.get(NepOccultismContent.BOUND_SPIRIT.get()) != null;
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

    private final Map<AEItemKey, Template> templates = new HashMap<>();
    private final List<Placed> placed = new ArrayList<>();
    private final List<ItemStack> extras = new ArrayList<>();
    private final IItemHandler outputView = new OutputView();
    private final IItemHandler machineView =
            MachineItemView.demandLimited(inputBuffer, outputView, this::manualDemandFor);

    private Phase phase = Phase.IDLE;
    private int cooldown;
    private int bowlRescan;
    private boolean repairEnabled;
    private boolean repairJob;
    private double speedCarry;
    private int ritualTime;
    private int ritualDuration;

    @Nullable
    private BlockPos bowlPos;

    @Nullable
    private ResourceLocation activeRecipe;

    @Nullable
    private AEItemKey activeDeclared;

    private ItemStack received = ItemStack.EMPTY;

    private Stall stall = Stall.NONE;
    private Refusal refusal = Refusal.NONE;
    private int refusalTicks;

    enum Phase {
        IDLE,
        RUNNING
    }

    enum Stall {
        NONE,
        INGREDIENTS,
        OUTPUT_FULL,
        BOWL_BUSY,
        CONDITIONS,
        INTERRUPTED,
        BOWLS_OCCUPIED,
        TOO_FEW_BOWLS,
        WRONG_PENTACLE,
        TIER_TOO_LOW,
        UNLOADED,
        NO_BOWL,
        NO_BOOK,
        NO_RECIPE;

        private static Stall worse(Stall first, Stall second) {
            return first.ordinal() >= second.ordinal() ? first : second;
        }
    }

    enum Refusal {
        NONE,
        NOT_A_RITUAL_PATTERN,
        NO_ITEM_OUTPUT,
        UNKNOWN_RECIPE,
        NO_BOOK,
        TIER_TOO_LOW,
        MIXED_RECIPES,
        ITEMS_ONLY,
        TOO_MANY_INPUTS,
        BUFFER_FULL
    }

    public RitualControllerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(
                type,
                pos,
                state,
                INPUT_SLOTS,
                OUTPUT_SLOTS,
                NepOccultismContent.CONTROLLER_ITEM.get(),
                NepConfig.occultismRitualControllerIdleMeDrain(),
                NepConfig.occultismRitualControllerChannels(),
                "Ritual controller");
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, RitualControllerBlockEntity controller) {
        controller.tick(level);
    }

    private void tick(Level level) {
        if (runningGrace > 0) {
            runningGrace--;
        }
        if (refusalTicks > 0 && --refusalTicks == 0) {
            refusal = Refusal.NONE;
        }
        if (cooldown > 0) {
            cooldown--;
        }
        flushOutput(level);
        if (!NepConfig.occultismRitualController()) {
            setPowerFault(false);
            refreshComparator(level);
            refreshVisualState(level);
            syncIfChanged(level);
            return;
        }
        power.create(level, getBlockPos());
        dumpExtras();

        long beat = level.getGameTime() + getBlockPos().hashCode();
        if (Math.floorMod(beat, RESTOCK_INTERVAL) == 0) {
            if (owed.isEmpty()) {
                setMissingInputs(List.of());
            } else {
                autoRequest(level);
            }
        }
        if (owed.isEmpty() && phase == Phase.IDLE && !repairEnabled) {
            setStall(Stall.NONE);
        }

        if (phase == Phase.RUNNING) {
            monitor(level);
        } else if (power.isPowered()) {
            power.clearStarved();
            if (cooldown == 0 && (scanNeeded || Math.floorMod(beat, RESTOCK_INTERVAL) == 0)) {
                scanNeeded = false;
                if (!owed.isEmpty()) {
                    tryBegin(level);
                } else if (Math.floorMod(beat, REPAIR_INTERVAL) == 0) {
                    tryRepair(level);
                }
            }
        }
        setPowerFault(!power.hasUsablePower());

        refreshComparator(level);
        refreshVisualState(level);
        syncIfChanged(level);
    }

    private void tryBegin(Level level) {
        GoldenSacrificialBowlBlockEntity golden = bowl(level);
        BoundSpirit spirit = spirit();
        Stall base = baseStall(golden, spirit);
        if (base != Stall.NONE) {
            setStall(base);
            return;
        }
        Stall reason = Stall.NONE;
        for (AEItemKey wanted : List.copyOf(owed.keySet())) {
            if (owed.getOrDefault(wanted, 0L) <= 0) {
                continue;
            }
            RecipeHolder<RitualRecipe> holder = recipeFor(level, wanted);
            if (holder == null) {
                reason = Stall.worse(reason, Stall.NO_RECIPE);
                continue;
            }
            Stall attempt = beginPattern(level, golden, spirit, holder, wanted);
            if (attempt == Stall.NONE) {
                setStall(Stall.NONE);
                return;
            }
            reason = Stall.worse(reason, attempt);
        }
        setStall(reason);
    }

    private Stall baseStall(@Nullable GoldenSacrificialBowlBlockEntity golden, @Nullable BoundSpirit spirit) {
        if (spirit == null) {
            return Stall.NO_BOOK;
        }
        if (golden == null) {
            return Stall.NO_BOWL;
        }
        if (golden.getCurrentRitualRecipe() != null
                || !golden.itemStackHandler.getStackInSlot(0).isEmpty()) {
            return Stall.BOWL_BUSY;
        }
        return Stall.NONE;
    }

    private Stall beginPattern(
            Level level,
            GoldenSacrificialBowlBlockEntity golden,
            BoundSpirit spirit,
            RecipeHolder<RitualRecipe> holder,
            AEItemKey declared) {
        RitualRecipe recipe = holder.value();
        if (OccultismRituals.tierOf(recipe) > spirit.tier().level()) {
            return Stall.TIER_TOO_LOW;
        }
        ItemStack result = recipe.getResultItem(level.registryAccess());
        if (!fitsInOutput(result)) {
            return Stall.OUTPUT_FULL;
        }
        Stall site = siteStall(level, golden, recipe);
        if (site != Stall.NONE) {
            return site;
        }
        int[] slots = pickFromBuffer(OccultismRituals.requirements(level, recipe));
        if (slots == null) {
            return Stall.INGREDIENTS;
        }
        List<ItemStack> units = new ArrayList<>(slots.length);
        for (int slot : slots) {
            units.add(inputBuffer.extractItem(slot, 1, false));
        }
        Stall started = start(level, golden, holder, units);
        if (started != Stall.NONE) {
            for (ItemStack unit : units) {
                restore(level, unit);
            }
            return started;
        }
        repairJob = false;
        activeDeclared = declared;
        activeResult = declared.toStack((int) Math.max(1, result.getCount()));
        return Stall.NONE;
    }

    private Stall siteStall(Level level, GoldenSacrificialBowlBlockEntity golden, RitualRecipe recipe) {
        if (recipe.getPentacle() == null || recipe.getPentacle().validate(level, golden.getBlockPos()) == null) {
            return Stall.WRONG_PENTACLE;
        }
        List<SacrificialBowlBlockEntity> bowls = recipe.getRitual().getSacrificialBowls(level, golden.getBlockPos());
        for (SacrificialBowlBlockEntity bowl : bowls) {
            if (!bowl.itemStackHandler.getStackInSlot(0).isEmpty()) {
                return Stall.BOWLS_OCCUPIED;
            }
        }
        return bowls.size() < recipe.getIngredients().size() ? Stall.TOO_FEW_BOWLS : Stall.NONE;
    }

    /** Starts the ritual from {@code units}, ordered as {@link OccultismRituals#requirements}; on any failure every unit is still in {@code units}. */
    private Stall start(
            Level level,
            GoldenSacrificialBowlBlockEntity golden,
            RecipeHolder<RitualRecipe> holder,
            List<ItemStack> units) {
        RitualRecipe recipe = holder.value();
        BlockPos goldenPos = golden.getBlockPos();
        List<SacrificialBowlBlockEntity> bowls = recipe.getRitual().getSacrificialBowls(level, goldenPos);
        int ingredients = recipe.getIngredients().size();
        if (bowls.size() < ingredients || units.size() < 1 + ingredients) {
            return Stall.TOO_FEW_BOWLS;
        }
        placed.clear();
        for (int index = 0; index < ingredients; index++) {
            ItemStack unit = units.get(1 + index);
            SacrificialBowlBlockEntity bowl = bowls.get(index);
            bowl.itemStackHandler.setStackInSlot(0, unit.copy());
            placed.add(new Placed(bowl.getBlockPos().immutable(), unit.copy()));
        }
        ItemStack activation = units.get(0).copy();
        FakePlayer ritualist = FakePlayerFactory.get((ServerLevel) level, PROFILE);
        if (!recipe.getRitual().isValid(level, goldenPos, golden, ritualist, activation, recipe.getIngredients())) {
            discardPlaced(level);
            return Stall.CONDITIONS;
        }
        golden.itemStackHandler.insertItem(0, activation.copy(), false);
        if (!golden.startRitual(ritualist, activation, holder)) {
            golden.itemStackHandler.extractItem(0, 1, false);
            discardDropped(level, goldenPos, activation);
            discardPlaced(level);
            return Stall.CONDITIONS;
        }
        if (recipe.requiresSacrifice()) {
            golden.notifySacrifice(null);
            ItemStack leftover = SacrificeStandIns.leftover(units.get(units.size() - 1));
            if (!leftover.isEmpty()) {
                extras.add(leftover);
            }
        }
        units.clear();
        bowlPos = goldenPos.immutable();
        RitualClaims.claim(level, bowlPos, this);
        activeRecipe = holder.id();
        received = ItemStack.EMPTY;
        speedCarry = 0;
        ritualTime = 0;
        ritualDuration = recipe.getDuration();
        enterPhase(Phase.RUNNING);
        runningGrace = RUNNING_GRACE_TICKS;
        setChanged();
        if (NepConfig.debugLogging()) {
            Nep.LOGGER.info("Ritual controller {} started {} at {}", getBlockPos(), holder.id(), goldenPos);
        }
        return Stall.NONE;
    }

    @Nullable
    private int[] pickFromBuffer(List<Predicate<ItemStack>> requirements) {
        int[] left = new int[inputBuffer.getSlots()];
        for (int slot = 0; slot < left.length; slot++) {
            left[slot] = inputBuffer.getStackInSlot(slot).getCount();
        }
        int[] chosen = new int[requirements.size()];
        int[] budget = {SEARCH_BUDGET};
        return pick(requirements, 0, left, chosen, budget) ? chosen : null;
    }

    private boolean pick(List<Predicate<ItemStack>> requirements, int index, int[] left, int[] chosen, int[] budget) {
        if (index == requirements.size()) {
            return true;
        }
        for (int slot = 0; slot < left.length; slot++) {
            if (--budget[0] < 0) {
                return false;
            }
            if (left[slot] > 0 && requirements.get(index).test(inputBuffer.getStackInSlot(slot))) {
                left[slot]--;
                chosen[index] = slot;
                if (pick(requirements, index + 1, left, chosen, budget)) {
                    return true;
                }
                left[slot]++;
            }
        }
        return false;
    }

    private void monitor(Level level) {
        if (!received.isEmpty()) {
            finishRitual(level);
            return;
        }
        if (bowlPos == null) {
            abandon(level, Stall.NO_BOWL);
            return;
        }
        if (!level.isLoaded(bowlPos)) {
            setStall(Stall.UNLOADED);
            return;
        }
        GoldenSacrificialBowlBlockEntity golden = goldenAt(level, bowlPos);
        RecipeHolder<RitualRecipe> current = golden == null ? null : golden.getCurrentRitualRecipe();
        if (current == null || !current.id().equals(activeRecipe)) {
            Nep.LOGGER.warn(
                    "Ritual controller {} lost its ritual {} at {} before it finished",
                    getBlockPos(),
                    activeRecipe,
                    bowlPos);
            sweep(level, bowlPos);
            takeBackPlaced(level);
            abandon(level, Stall.INTERRUPTED);
            return;
        }
        RitualClaims.claim(level, bowlPos, this);
        runningGrace = RUNNING_GRACE_TICKS;
        drawActivePower();
        hasten(golden, current.value());
        if (stall == Stall.UNLOADED) {
            setStall(Stall.NONE);
        }
    }

    private void hasten(GoldenSacrificialBowlBlockEntity golden, RitualRecipe recipe) {
        BoundSpirit spirit = spirit();
        double bonus = spirit == null
                ? 0
                : (spirit.tier().level() - 1) * NepConfig.occultismRitualControllerTierSpeed() / 100.0;
        int now = golden.currentTime;
        if (now > ritualTime && bonus > 0 && golden.sacrificeFulfilled() && golden.itemUseFulfilled()) {
            speedCarry += (now - ritualTime) * bonus;
            int extra = (int) speedCarry;
            if (extra > 0) {
                speedCarry -= extra;
                golden.currentTime = Math.min(recipe.getDuration(), now + extra);
                golden.setChanged();
            }
        }
        ritualTime = golden.currentTime;
        ritualDuration = recipe.getDuration();
    }

    boolean receiveResult(BlockPos bowl, ItemStack result) {
        if (phase != Phase.RUNNING || !bowl.equals(bowlPos) || !received.isEmpty()) {
            return false;
        }
        received = result;
        setChanged();
        return true;
    }

    private void finishRitual(Level level) {
        if (repairJob) {
            AEItemKey key = AEItemKey.of(received);
            long stored = key == null ? 0 : power.dumpToNetwork(key, received.getCount());
            ItemStack rest = received.copyWithCount(received.getCount() - (int) stored);
            if (!rest.isEmpty()) {
                ItemStack leftover = ItemHandlerHelper.insertItem(outputBuffer, rest, false);
                if (!leftover.isEmpty()) {
                    extras.add(leftover);
                }
            }
        } else {
            if (!fitsInOutput(received)) {
                outputBlocked = true;
                setStall(Stall.OUTPUT_FULL);
                return;
            }
            ItemHandlerHelper.insertItem(outputBuffer, received.copy(), false);
            AEItemKey actual = AEItemKey.of(received);
            if (actual != null && activeDeclared != null) {
                if (!actual.equals(activeDeclared)) {
                    CraftedOutputs.expect(actual, activeDeclared);
                    Direction direction = returnDirections.directionFor(activeDeclared);
                    if (direction != null) {
                        returnDirections.record(actual, direction);
                    }
                }
                if (owed.getOrDefault(activeDeclared, 0L) > 0) {
                    decrement(owed, activeDeclared, 1);
                    if (!owed.containsKey(activeDeclared)) {
                        templates.remove(activeDeclared);
                    }
                    if (owed.isEmpty()) {
                        pushingCpus.clear();
                    }
                    toReturn.merge(actual, (long) received.getCount(), Long::sum);
                }
            }
        }
        if (NepConfig.debugLogging()) {
            Nep.LOGGER.info("Ritual controller {} finished {} with {}", getBlockPos(), activeRecipe, received);
        }
        placed.clear();
        resetRitual(level);
        flushOutput(level);
    }

    private void abandon(Level level, Stall reason) {
        if (NepConfig.debugLogging()) {
            Nep.LOGGER.info("Ritual controller {} abandoned {}: {}", getBlockPos(), activeRecipe, reason);
        }
        placed.clear();
        resetRitual(level);
        cooldown = RETRY_COOLDOWN_TICKS;
        setStall(reason);
    }

    private void takeBackPlaced(Level level) {
        for (Placed entry : placed) {
            if (level.isLoaded(entry.bowl())
                    && level.getBlockEntity(entry.bowl()) instanceof SacrificialBowlBlockEntity bowl
                    && ItemStack.isSameItemSameComponents(bowl.itemStackHandler.getStackInSlot(0), entry.stack())) {
                restore(
                        level,
                        bowl.itemStackHandler.extractItem(0, entry.stack().getCount(), false));
            }
        }
        placed.clear();
    }

    private void discardPlaced(Level level) {
        for (Placed entry : placed) {
            if (level.getBlockEntity(entry.bowl()) instanceof SacrificialBowlBlockEntity bowl
                    && ItemStack.isSameItemSameComponents(bowl.itemStackHandler.getStackInSlot(0), entry.stack())) {
                bowl.itemStackHandler.setStackInSlot(0, ItemStack.EMPTY);
            }
        }
        placed.clear();
    }

    private static void discardDropped(Level level, BlockPos around, ItemStack dropped) {
        AABB area = new AABB(around).inflate(SWEEP_RADIUS);
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, area, ItemEntity::isAlive)) {
            if (ItemStack.isSameItemSameComponents(item.getItem(), dropped)) {
                item.discard();
                return;
            }
        }
    }

    private void sweep(Level level, BlockPos around) {
        AABB area = new AABB(around).inflate(SWEEP_RADIUS);
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, area, ItemEntity::isAlive)) {
            restore(level, item.getItem().copy());
            item.discard();
        }
    }

    private void restore(Level level, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        if (repairJob) {
            extras.add(stack);
            return;
        }
        ItemStack leftover = ItemHandlerHelper.insertItem(inputBuffer, stack.copy(), false);
        if (!leftover.isEmpty()) {
            extras.add(leftover);
        }
    }

    private void dumpExtras() {
        if (extras.isEmpty() || power.grid() == null) {
            return;
        }
        List<ItemStack> kept = new ArrayList<>();
        boolean changed = false;
        for (ItemStack stack : extras) {
            AEItemKey key = AEItemKey.of(stack);
            long stored = key == null ? 0 : power.dumpToNetwork(key, stack.getCount());
            changed |= stored > 0;
            if (stored < stack.getCount()) {
                kept.add(stack.copyWithCount(stack.getCount() - (int) stored));
            }
        }
        if (changed) {
            extras.clear();
            extras.addAll(kept);
            setChanged();
        }
    }

    private void resetRitual(Level level) {
        if (bowlPos != null) {
            RitualClaims.release(level, bowlPos, this);
        }
        activeResult = ItemStack.EMPTY;
        activeRecipe = null;
        activeDeclared = null;
        received = ItemStack.EMPTY;
        repairJob = false;
        speedCarry = 0;
        ritualTime = 0;
        ritualDuration = 0;
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
    }

    private void drawActivePower() {
        double extra = NepConfig.occultismRitualControllerMeDrain() - NepConfig.occultismRitualControllerIdleMeDrain();
        if (extra <= 0) {
            power.clearStarved();
            return;
        }
        power.extractPower(extra);
    }

    private void tryRepair(Level level) {
        if (!repairEnabled || !NepConfig.occultismRitualControllerRepair()) {
            return;
        }
        GoldenSacrificialBowlBlockEntity golden = bowl(level);
        BoundSpirit spirit = spirit();
        if (baseStall(golden, spirit) != Stall.NONE) {
            return;
        }
        IGrid grid = power.grid();
        if (grid == null) {
            return;
        }
        KeyCounter stock = grid.getStorageService().getCachedInventory();
        for (RecipeHolder<RitualRecipe> holder : OccultismRituals.all(level)) {
            RitualRecipe recipe = holder.value();
            if (!OccultismRituals.repairs(recipe)
                    || OccultismRituals.tierOf(recipe) > spirit.tier().level()) {
                continue;
            }
            List<AEItemKey> keys = repairKeys(recipe, stock);
            if (keys == null || siteStall(level, golden, recipe) != Stall.NONE) {
                continue;
            }
            List<ItemStack> units = new ArrayList<>(keys.size());
            boolean complete = true;
            for (AEItemKey key : keys) {
                long taken = power.extractFromNetwork(key, 1);
                if (taken < 1) {
                    complete = false;
                    break;
                }
                units.add(key.toStack(1));
            }
            repairJob = true;
            if (!complete || start(level, golden, holder, units) != Stall.NONE) {
                extras.addAll(units);
                repairJob = false;
                continue;
            }
            activeDeclared = null;
            activeResult = keys.get(0).toStack(1);
            return;
        }
    }

    @Nullable
    private static List<AEItemKey> repairKeys(RitualRecipe recipe, KeyCounter stock) {
        Map<AEItemKey, Long> claimed = new HashMap<>();
        List<AEItemKey> keys = new ArrayList<>();
        AEItemKey damaged = null;
        for (var entry : stock) {
            if (entry.getKey() instanceof AEItemKey key
                    && key.isDamaged()
                    && entry.getLongValue() > 0
                    && recipe.getActivationItem().test(key.toStack())) {
                damaged = key;
                break;
            }
        }
        if (damaged == null) {
            return null;
        }
        keys.add(damaged);
        claimed.put(damaged, 1L);
        for (Ingredient ingredient : recipe.getIngredients()) {
            AEItemKey found = null;
            for (var entry : stock) {
                if (entry.getKey() instanceof AEItemKey key
                        && !key.isDamaged()
                        && entry.getLongValue() > claimed.getOrDefault(key, 0L)
                        && ingredient.test(key.toStack())) {
                    found = key;
                    break;
                }
            }
            if (found == null) {
                return null;
            }
            claimed.merge(found, 1L, Long::sum);
            keys.add(found);
        }
        return keys;
    }

    @Nullable
    private GoldenSacrificialBowlBlockEntity bowl(Level level) {
        if (bowlPos != null) {
            GoldenSacrificialBowlBlockEntity cached = goldenAt(level, bowlPos);
            if (cached != null && (phase == Phase.RUNNING || --bowlRescan > 0)) {
                return cached;
            }
            if (phase == Phase.RUNNING) {
                return null;
            }
        }
        bowlRescan = BOWL_RESCAN_TICKS / RESTOCK_INTERVAL;
        int range = NepConfig.occultismRitualControllerBowlRange();
        GoldenSacrificialBowlBlockEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        BlockPos origin = getBlockPos();
        for (int chunkX = (origin.getX() - range) >> 4; chunkX <= (origin.getX() + range) >> 4; chunkX++) {
            for (int chunkZ = (origin.getZ() - range) >> 4; chunkZ <= (origin.getZ() + range) >> 4; chunkZ++) {
                if (!(level.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false) instanceof LevelChunk chunk)) {
                    continue;
                }
                for (BlockEntity entity : chunk.getBlockEntities().values()) {
                    if (!(entity instanceof GoldenSacrificialBowlBlockEntity candidate)) {
                        continue;
                    }
                    BlockPos pos = candidate.getBlockPos();
                    if (Math.abs(pos.getX() - origin.getX()) > range
                            || Math.abs(pos.getY() - origin.getY()) > range
                            || Math.abs(pos.getZ() - origin.getZ()) > range) {
                        continue;
                    }
                    double distance = pos.distSqr(origin);
                    if (distance < bestDistance
                            || distance == bestDistance && best != null && scanOrderBefore(pos, best.getBlockPos())) {
                        best = candidate;
                        bestDistance = distance;
                    }
                }
            }
        }
        BlockPos found = best == null ? null : best.getBlockPos().immutable();
        if (found == null ? bowlPos != null : !found.equals(bowlPos)) {
            bowlPos = found;
            setChanged();
        }
        return best;
    }

    private static boolean scanOrderBefore(BlockPos a, BlockPos b) {
        if (a.getZ() != b.getZ()) {
            return a.getZ() < b.getZ();
        }
        if (a.getY() != b.getY()) {
            return a.getY() < b.getY();
        }
        return a.getX() < b.getX();
    }

    @Nullable
    private static GoldenSacrificialBowlBlockEntity goldenAt(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof GoldenSacrificialBowlBlockEntity golden ? golden : null;
    }

    @Nullable
    BoundSpirit spirit() {
        return book.getStackInSlot(0).get(NepOccultismContent.BOUND_SPIRIT.get());
    }

    ItemStackHandler book() {
        return book;
    }

    @Nullable
    private RecipeHolder<RitualRecipe> recipeFor(Level level, AEItemKey output) {
        Template template = templates.get(output);
        return template == null ? null : OccultismRituals.byId(level, template.recipe());
    }

    boolean readyForPatterns() {
        Level level = getLevel();
        return NepConfig.occultismRitualController() && level != null && !level.isClientSide && spirit() != null;
    }

    boolean pushControllerPattern(IPatternDetails pattern, KeyCounter[] inputs, Direction ejectionDirection) {
        Level level = getLevel();
        if (level == null || level.isClientSide || !NepConfig.occultismRitualController()) {
            return false;
        }
        if (!(pattern instanceof RitualPattern ritual)) {
            return reject(Refusal.NOT_A_RITUAL_PATTERN, "pattern is not a ritual pattern");
        }
        List<GenericStack> outputs = pattern.getOutputs();
        if (outputs.isEmpty() || !(outputs.get(0).what() instanceof AEItemKey producedItem)) {
            return reject(Refusal.NO_ITEM_OUTPUT, "pattern has no item output");
        }
        RecipeHolder<RitualRecipe> holder = OccultismRituals.byId(level, ritual.recipe());
        if (holder == null || !OccultismRituals.patternable(holder.value(), level)) {
            return reject(Refusal.UNKNOWN_RECIPE, "no craft ritual matches " + ritual.recipe());
        }
        BoundSpirit spirit = spirit();
        if (spirit == null) {
            return reject(Refusal.NO_BOOK, "no spirit-infused book is bound into the controller");
        }
        if (OccultismRituals.tierOf(holder.value()) > spirit.tier().level()) {
            return reject(Refusal.TIER_TOO_LOW, ritual.recipe() + " needs a stronger spirit than " + spirit.tier());
        }
        Template existing = templates.get(producedItem);
        if (existing != null && !ritual.recipe().equals(existing.recipe())) {
            return reject(
                    Refusal.MIXED_RECIPES,
                    "a ritual for " + producedItem + " from " + existing.recipe() + " is already queued");
        }

        Map<AEItemKey, Long> items = new HashMap<>();
        for (KeyCounter counter : inputs) {
            for (var entry : counter) {
                if (!(entry.getKey() instanceof AEItemKey itemKey) || entry.getLongValue() <= 0) {
                    return reject(Refusal.ITEMS_ONLY, "rituals take items only");
                }
                items.merge(itemKey, entry.getLongValue(), Long::sum);
            }
        }
        List<GenericStack> provided = new ArrayList<>(items.size());
        items.forEach((key, amount) -> provided.add(new GenericStack(key, amount)));
        if (OccultismRituals.plan(level, holder.value(), provided, outputs.get(0)) == null) {
            return reject(Refusal.UNKNOWN_RECIPE, "pushed items " + items + " do not fill " + ritual.recipe());
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
        captureTemplate(producedItem, items, ritual.recipe());
        owed.merge(producedItem, 1L, Long::sum);
        pushingCpus.record();
        clearRefusal();
        markScanNeeded();
        setChanged();
        if (NepConfig.debugLogging()) {
            Nep.LOGGER.info("Ritual controller {} accepted a ritual for {}", getBlockPos(), producedItem);
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
            Nep.LOGGER.info("Ritual controller {} rejected pattern: {}", getBlockPos(), reason);
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

    private void captureTemplate(AEItemKey output, Map<AEItemKey, Long> items, ResourceLocation recipe) {
        List<AEKey> keys = new ArrayList<>(items.size());
        List<Long> counts = new ArrayList<>(items.size());
        for (Map.Entry<AEItemKey, Long> entry : items.entrySet()) {
            keys.add(entry.getKey());
            counts.add(entry.getValue());
        }
        templates.put(output, new Template(List.copyOf(keys), List.copyOf(counts), recipe));
    }

    private void autoRequest(Level level) {
        if (!NepConfig.occultismRitualControllerAutoRequest()) {
            setMissingInputs(List.of());
            return;
        }
        DemandTally tally = new DemandTally();
        for (Map.Entry<AEItemKey, Long> entry : owed.entrySet()) {
            Template template = templates.get(entry.getKey());
            if (template == null) {
                continue;
            }
            for (int i = 0; i < template.keys().size(); i++) {
                tally.add(template.keys().get(i), template.counts().get(i) * entry.getValue());
            }
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
            Template template = templates.get(entry.getKey());
            if (template == null) {
                continue;
            }
            for (int index = 0; index < template.keys().size(); index++) {
                if (IncidentalComponents.equivalent(template.keys().get(index), key)) {
                    target += template.counts().get(index) * entry.getValue();
                }
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
    protected int readoutSignature() {
        int hash = activeResult.isEmpty() ? 0 : activeResult.getItem().hashCode();
        hash = 31 * hash + Math.round(craftProgress() * PROGRESS_STEPS);
        hash = 31 * hash + Boolean.hashCode(outputBlocked);
        hash = 31 * hash + Boolean.hashCode(powerFault);
        hash = 31 * hash + missingInputs.hashCode();
        hash = 31 * hash + stall.ordinal();
        hash = 31 * hash + refusal.ordinal();
        hash = 31 * hash + phase.ordinal();
        hash = 31 * hash + Boolean.hashCode(repairEnabled);
        hash = 31 * hash + Boolean.hashCode(repairJob);
        hash = 31 * hash + (bowlPos == null ? 0 : bowlPos.hashCode());
        hash = 31 * hash + Long.hashCode(pendingJobs());
        hash = 31 * hash + book.getStackInSlot(0).hashCode();
        return hash;
    }

    private void refreshVisualState(Level level) {
        BlockState state = getBlockState();
        if (!state.hasProperty(RitualControllerBlock.STATUS)) {
            return;
        }
        ControllerStatus desired = desiredStatus();
        if (state.getValue(RitualControllerBlock.STATUS) != desired) {
            level.setBlock(getBlockPos(), state.setValue(RitualControllerBlock.STATUS, desired), Block.UPDATE_CLIENTS);
        }
    }

    private ControllerStatus desiredStatus() {
        if (stall != Stall.NONE || refusal != Refusal.NONE) {
            return ControllerStatus.STALLED;
        }
        if (runningGrace > 0 || phase != Phase.IDLE) {
            return ControllerStatus.RUNNING;
        }
        return ControllerStatus.IDLE;
    }

    @Override
    public float craftProgress() {
        if (phase == Phase.IDLE || ritualDuration <= 0) {
            return 0.0F;
        }
        return Math.min(1.0F, ritualTime / (float) ritualDuration);
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

    boolean repairEnabled() {
        return repairEnabled;
    }

    boolean repairing() {
        return repairJob && phase == Phase.RUNNING;
    }

    @Nullable
    BlockPos bowlPos() {
        return bowlPos;
    }

    private void setStall(Stall reason) {
        if (stall != reason) {
            stall = reason;
            setChanged();
        }
    }

    void toggleRepair() {
        repairEnabled = !repairEnabled;
        markScanNeeded();
        setChanged();
    }

    void clearPending() {
        clearRefusal();
        if (owed.isEmpty() && templates.isEmpty()) {
            return;
        }
        int cancelled = pushingCpus.cancelJobsFor(power.grid(), Set.copyOf(templates.keySet()));
        if (NepConfig.debugLogging()) {
            Nep.LOGGER.info(
                    "Ritual controller {} cleared {} pending ritual(s) and cancelled {} network job(s)",
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
        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), book.getStackInSlot(0));
        book.setStackInSlot(0, ItemStack.EMPTY);
        for (ItemStack stack : extras) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
        }
        extras.clear();
        if (!received.isEmpty()) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), received);
        }
        if (bowlPos != null) {
            RitualClaims.release(level, bowlPos, this);
        }
        clearContent();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put(INPUT_KEY, inputBuffer.serializeNBT(registries));
        tag.put(OUTPUT_KEY, outputBuffer.serializeNBT(registries));
        tag.put(BOOK_KEY, book.serializeNBT(registries));
        tag.put(OWED_KEY, ItemCounts.save(owed, registries));
        tag.put(RETURN_KEY, ItemCounts.save(toReturn, registries));
        tag.put(RETURN_DIRS_KEY, returnDirections.save(registries));
        tag.put(TEMPLATE_KEY, RitualControllerPersistence.saveTemplates(templates, registries));
        if (!activeResult.isEmpty()) {
            tag.put(ACTIVE_KEY, activeResult.save(registries));
        }
        if (activeDeclared != null) {
            tag.put(DECLARED_KEY, activeDeclared.toTagGeneric(registries));
        }
        if (!received.isEmpty()) {
            tag.put(RECEIVED_KEY, received.save(registries));
        }
        tag.put(PLACED_KEY, RitualControllerPersistence.savePlaced(placed, registries));
        ListTag extraList = new ListTag();
        for (ItemStack stack : extras) {
            if (!stack.isEmpty()) {
                extraList.add(stack.save(registries));
            }
        }
        tag.put(EXTRAS_KEY, extraList);
        tag.putBoolean(BLOCKED_KEY, outputBlocked);
        tag.putBoolean(POWER_FAULT_KEY, powerFault);
        tag.putString(STALL_KEY, stall.name());
        tag.putString(REFUSAL_KEY, refusal.name());
        tag.putInt(REFUSAL_TICKS_KEY, refusalTicks);
        tag.putString(PHASE_KEY, phase.name());
        tag.putInt(COOLDOWN_KEY, cooldown);
        tag.putBoolean(REPAIR_KEY, repairEnabled);
        tag.putBoolean(REPAIR_JOB_KEY, repairJob);
        tag.putDouble(SPEED_CARRY_KEY, speedCarry);
        tag.putInt(TIME_KEY, ritualTime);
        tag.putInt(DURATION_KEY, ritualDuration);
        if (bowlPos != null) {
            tag.putLong(BOWL_KEY, bowlPos.asLong());
        }
        if (activeRecipe != null) {
            tag.putString(RECIPE_KEY, activeRecipe.toString());
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
        if (tag.contains(BOOK_KEY)) {
            loadBuffer(book, registries, tag.getCompound(BOOK_KEY));
        }
        ItemCounts.load(owed, tag, OWED_KEY, registries);
        ItemCounts.load(toReturn, tag, RETURN_KEY, registries);
        returnDirections.load(tag, RETURN_DIRS_KEY, "ReturnDir", toReturn.keySet(), registries);
        RitualControllerPersistence.loadTemplates(templates, tag.getList(TEMPLATE_KEY, Tag.TAG_COMPOUND), registries);
        activeResult = tag.contains(ACTIVE_KEY)
                ? ItemStack.parse(registries, tag.getCompound(ACTIVE_KEY)).orElse(ItemStack.EMPTY)
                : ItemStack.EMPTY;
        activeDeclared = tag.contains(DECLARED_KEY)
                        && AEKey.fromTagGeneric(registries, tag.getCompound(DECLARED_KEY)) instanceof AEItemKey key
                ? key
                : null;
        received = tag.contains(RECEIVED_KEY)
                ? ItemStack.parse(registries, tag.getCompound(RECEIVED_KEY)).orElse(ItemStack.EMPTY)
                : ItemStack.EMPTY;
        RitualControllerPersistence.loadPlaced(placed, tag.getList(PLACED_KEY, Tag.TAG_COMPOUND), registries);
        extras.clear();
        ListTag extraList = tag.getList(EXTRAS_KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < extraList.size(); i++) {
            ItemStack.parse(registries, extraList.getCompound(i)).ifPresent(extras::add);
        }
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
        phase = EnumNames.byName(Phase.values(), tag.getString(PHASE_KEY), Phase.IDLE);
        cooldown = tag.getInt(COOLDOWN_KEY);
        repairEnabled = tag.getBoolean(REPAIR_KEY);
        repairJob = tag.getBoolean(REPAIR_JOB_KEY);
        speedCarry = tag.getDouble(SPEED_CARRY_KEY);
        ritualTime = tag.getInt(TIME_KEY);
        ritualDuration = tag.getInt(DURATION_KEY);
        bowlPos = tag.contains(BOWL_KEY) ? BlockPos.of(tag.getLong(BOWL_KEY)) : null;
        activeRecipe = tag.contains(RECIPE_KEY) ? ResourceLocation.tryParse(tag.getString(RECIPE_KEY)) : null;
        markScanNeeded();
    }

    @Override
    public Component getDisplayName() {
        return NepOccultismContent.CONTROLLER.get().getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory inventory, Player player) {
        return new RitualControllerMenu(windowId, inventory, getBlockPos());
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
