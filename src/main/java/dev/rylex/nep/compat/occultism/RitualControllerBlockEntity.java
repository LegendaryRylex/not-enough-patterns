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
import dev.rylex.nep.machine.MatrixBuffer;
import dev.rylex.nep.machine.MatrixGridNode;
import dev.rylex.nep.pattern.RitualPattern;
import dev.rylex.nep.util.EnumNames;
import dev.rylex.nep.util.ItemCounts;
import dev.rylex.nep.util.Recipes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
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
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
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

    private final SpiritBookSlots book = new SpiritBookSlots(
            1,
            (slot, stack) -> stack.is(NepOccultismContent.SPIRIT_INFUSED_BOOK.get())
                    && stack.get(NepOccultismContent.BOUND_SPIRIT.get()) != null,
            () -> {
                markScanNeeded();
                setChanged();
            });

    private final Map<AEItemKey, Template> templates = new HashMap<>();
    private final List<Placed> placed = new ArrayList<>();
    private final List<ItemStack> extras = new ArrayList<>();
    private final ResourceHandler<ItemResource> outputView = new OutputView();
    private final ResourceHandler<ItemResource> machineView =
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
    private Identifier activeRecipe;

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
                || !golden.itemStackHandler.getResource(0).isEmpty()) {
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
        ItemStack result = recipe.getResult();
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
            units.add(takeFromInput(slot, 1));
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
            if (!bowl.itemStackHandler.getResource(0).isEmpty()) {
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
            setBowl(bowl, unit.copy());
            placed.add(new Placed(bowl.getBlockPos().immutable(), unit.copy()));
        }
        ItemStack activation = units.get(0).copy();
        FakePlayer ritualist = FakePlayerFactory.get((ServerLevel) level, PROFILE);
        if (!recipe.getRitual().isValid(level, goldenPos, golden, ritualist, activation, recipe.getIngredients())) {
            discardPlaced(level);
            return Stall.CONDITIONS;
        }
        setBowl(golden, activation.copy());
        if (!golden.startRitual(ritualist, activation, holder)) {
            takeFromBowl(golden, 1);
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
        activeRecipe = Recipes.idOf(holder);
        received = ItemStack.EMPTY;
        speedCarry = 0;
        ritualTime = 0;
        ritualDuration = recipe.getDuration();
        enterPhase(Phase.RUNNING);
        runningGrace = RUNNING_GRACE_TICKS;
        setChanged();
        if (NepConfig.debugLogging()) {
            Nep.LOGGER.info("Ritual controller {} started {} at {}", getBlockPos(), Recipes.idOf(holder), goldenPos);
        }
        return Stall.NONE;
    }

    @Nullable
    private int[] pickFromBuffer(List<Predicate<ItemStack>> requirements) {
        int[] left = new int[inputBuffer.size()];
        for (int slot = 0; slot < left.length; slot++) {
            left[slot] = inputBuffer.getAmountAsInt(slot);
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
            if (left[slot] > 0 && requirements.get(index).test(inputBuffer.stackAt(slot))) {
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
        if (current == null || !Recipes.idOf(current).equals(activeRecipe)) {
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
                ItemStack leftover = insertInto(outputBuffer, rest);
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
            insertInto(outputBuffer, received.copy());
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
                    && ItemStack.isSameItemSameComponents(bowlStack(bowl), entry.stack())) {
                restore(level, takeFromBowl(bowl, entry.stack().getCount()));
            }
        }
        placed.clear();
    }

    private void discardPlaced(Level level) {
        for (Placed entry : placed) {
            if (level.getBlockEntity(entry.bowl()) instanceof SacrificialBowlBlockEntity bowl
                    && ItemStack.isSameItemSameComponents(bowlStack(bowl), entry.stack())) {
                setBowl(bowl, ItemStack.EMPTY);
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
        ItemStack leftover = insertInto(inputBuffer, stack.copy());
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
        return book.stackAt(0).get(NepOccultismContent.BOUND_SPIRIT.get());
    }

    SpiritBookSlots book() {
        return book;
    }

    @Nullable
    private RecipeHolder<RitualRecipe> recipeFor(Level level, AEItemKey output) {
        Template template = templates.get(output);
        return template == null ? null : OccultismRituals.byId(level, template.recipe());
    }

    boolean readyForPatterns() {
        Level level = getLevel();
        return NepConfig.occultismRitualController() && level != null && !level.isClientSide() && spirit() != null;
    }

    boolean pushControllerPattern(IPatternDetails pattern, KeyCounter[] inputs, Direction ejectionDirection) {
        Level level = getLevel();
        if (level == null || level.isClientSide() || !NepConfig.occultismRitualController()) {
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

    private void captureTemplate(AEItemKey output, Map<AEItemKey, Long> items, Identifier recipe) {
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

    int manualDemandFor(ItemResource resource) {
        AEItemKey key = AEItemKey.of(resource);
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
        hash = 31 * hash + book.stackAt(0).hashCode();
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

    @Override
    public void clearContent() {
        super.clearContent();
        book.put(0, ItemStack.EMPTY);
    }

    private ItemStack takeFromInput(int slot, int amount) {
        ItemResource resource = inputBuffer.getResource(slot);
        try (Transaction tx = Transaction.openRoot()) {
            int taken = inputBuffer.extract(slot, resource, amount, tx);
            if (taken <= 0) {
                return ItemStack.EMPTY;
            }
            tx.commit();
            return resource.toStack(taken);
        }
    }

    private static ItemStack insertInto(MatrixBuffer buffer, ItemStack stack) {
        int inserted = ResourceHandlerUtil.insertStacking(buffer, ItemResource.of(stack), stack.getCount(), null);
        return stack.copyWithCount(stack.getCount() - inserted);
    }

    private static ItemStack bowlStack(SacrificialBowlBlockEntity bowl) {
        return bowl.itemStackHandler.getResource(0).toStack(bowl.itemStackHandler.getAmountAsInt(0));
    }

    private static void setBowl(SacrificialBowlBlockEntity bowl, ItemStack stack) {
        if (stack.isEmpty()) {
            bowl.itemStackHandler.set(0, ItemResource.EMPTY, 0);
        } else {
            bowl.itemStackHandler.set(0, ItemResource.of(stack), stack.getCount());
        }
    }

    private static ItemStack takeFromBowl(SacrificialBowlBlockEntity bowl, int amount) {
        ItemResource resource = bowl.itemStackHandler.getResource(0);
        try (Transaction tx = Transaction.openRoot()) {
            int taken = bowl.itemStackHandler.extract(0, resource, amount, tx);
            if (taken <= 0) {
                return ItemStack.EMPTY;
            }
            tx.commit();
            return resource.toStack(taken);
        }
    }

    void dropBuffers(Level level, BlockPos pos) {
        for (int slot = 0; slot < inputBuffer.size(); slot++) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), inputBuffer.stackAt(slot));
        }
        for (int slot = 0; slot < outputBuffer.size(); slot++) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), outputBuffer.stackAt(slot));
        }
        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), book.stackAt(0));
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
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        inputBuffer.serialize(output.child(INPUT_KEY));
        outputBuffer.serialize(output.child(OUTPUT_KEY));
        book.serialize(output.child(BOOK_KEY));
        ItemCounts.save(output, OWED_KEY, owed);
        ItemCounts.save(output, RETURN_KEY, toReturn);
        returnDirections.save(output, RETURN_DIRS_KEY);
        RitualControllerPersistence.saveTemplates(templates, output, TEMPLATE_KEY);
        if (!activeResult.isEmpty()) {
            output.store(ACTIVE_KEY, ItemStack.CODEC, activeResult);
        }
        if (activeDeclared != null) {
            output.store(DECLARED_KEY, AEKey.CODEC, activeDeclared);
        }
        if (!received.isEmpty()) {
            output.store(RECEIVED_KEY, ItemStack.CODEC, received);
        }
        RitualControllerPersistence.savePlaced(placed, output, PLACED_KEY);
        ValueOutput.TypedOutputList<ItemStack> extraList = output.list(EXTRAS_KEY, ItemStack.CODEC);
        for (ItemStack stack : extras) {
            if (!stack.isEmpty()) {
                extraList.add(stack);
            }
        }
        output.putBoolean(BLOCKED_KEY, outputBlocked);
        output.putBoolean(POWER_FAULT_KEY, powerFault);
        output.putString(STALL_KEY, stall.name());
        output.putString(REFUSAL_KEY, refusal.name());
        output.putInt(REFUSAL_TICKS_KEY, refusalTicks);
        output.putString(PHASE_KEY, phase.name());
        output.putInt(COOLDOWN_KEY, cooldown);
        output.putBoolean(REPAIR_KEY, repairEnabled);
        output.putBoolean(REPAIR_JOB_KEY, repairJob);
        output.putDouble(SPEED_CARRY_KEY, speedCarry);
        output.putInt(TIME_KEY, ritualTime);
        output.putInt(DURATION_KEY, ritualDuration);
        if (bowlPos != null) {
            output.putLong(BOWL_KEY, bowlPos.asLong());
        }
        if (activeRecipe != null) {
            output.putString(RECIPE_KEY, activeRecipe.toString());
        }
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
        book.deserialize(input.childOrEmpty(BOOK_KEY));
        ItemCounts.load(owed, input, OWED_KEY);
        ItemCounts.load(toReturn, input, RETURN_KEY);
        returnDirections.load(input, RETURN_DIRS_KEY);
        RitualControllerPersistence.loadTemplates(templates, input, TEMPLATE_KEY);
        activeResult = input.read(ACTIVE_KEY, ItemStack.CODEC).orElse(ItemStack.EMPTY);
        activeDeclared = input.read(DECLARED_KEY, AEKey.CODEC).orElse(null) instanceof AEItemKey key ? key : null;
        received = input.read(RECEIVED_KEY, ItemStack.CODEC).orElse(ItemStack.EMPTY);
        RitualControllerPersistence.loadPlaced(placed, input, PLACED_KEY);
        extras.clear();
        for (ItemStack stack : input.listOrEmpty(EXTRAS_KEY, ItemStack.CODEC)) {
            extras.add(stack);
        }
        outputBlocked = input.getBooleanOr(BLOCKED_KEY, false);
        powerFault = input.getBooleanOr(POWER_FAULT_KEY, false);
        missingInputs.clear();
        for (GenericStack stack : input.listOrEmpty(MISSING_KEY, GenericStack.CODEC)) {
            missingInputs.add(stack);
        }
        pushingCpus.load(input);
        power.load(input.childOrEmpty(NODE_KEY));
        stall = EnumNames.byName(Stall.values(), input.getStringOr(STALL_KEY, ""), Stall.NONE);
        refusal = EnumNames.byName(Refusal.values(), input.getStringOr(REFUSAL_KEY, ""), Refusal.NONE);
        refusalTicks = refusal == Refusal.NONE
                ? 0
                : Math.max(1, Math.min(REFUSAL_MEMORY_TICKS, input.getIntOr(REFUSAL_TICKS_KEY, 0)));
        phase = EnumNames.byName(Phase.values(), input.getStringOr(PHASE_KEY, ""), Phase.IDLE);
        cooldown = input.getIntOr(COOLDOWN_KEY, 0);
        repairEnabled = input.getBooleanOr(REPAIR_KEY, false);
        repairJob = input.getBooleanOr(REPAIR_JOB_KEY, false);
        speedCarry = input.getDoubleOr(SPEED_CARRY_KEY, 0.0);
        ritualTime = input.getIntOr(TIME_KEY, 0);
        ritualDuration = input.getIntOr(DURATION_KEY, 0);
        bowlPos = input.getLong(BOWL_KEY).map(BlockPos::of).orElse(null);
        activeRecipe = input.getString(RECIPE_KEY).map(Identifier::tryParse).orElse(null);
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
            if (resource.isEmpty() || toReturn.getOrDefault(AEItemKey.of(resource), 0L) > 0) {
                return 0;
            }
            return outputBuffer.extract(index, resource, amount, transaction);
        }
    }
}
