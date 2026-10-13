package dev.rylex.nep.compat.oritech;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import dev.rylex.nep.Nep;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.compat.oritech.ParticleCollisionControllerBlock.ControllerStatus;
import dev.rylex.nep.compat.oritech.oritechthings.OritechThingsCompat;
import dev.rylex.nep.machine.BufferedMatrixBlockEntity;
import dev.rylex.nep.machine.MachineItemView;
import dev.rylex.nep.machine.MatrixGridNode;
import dev.rylex.nep.pattern.ParticleCollisionPattern;
import dev.rylex.nep.pattern.encoding.EncodedIngredients;
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
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.entity.accelerator.AcceleratorControllerBlockEntity;
import rearth.oritech.block.entity.accelerator.AcceleratorParticleLogic;
import rearth.oritech.init.recipes.OritechRecipe;

public class ParticleCollisionControllerBlockEntity extends BufferedMatrixBlockEntity {

    static final int INPUT_SLOTS = 27;
    static final int OUTPUT_SLOTS = 9;
    static final int LINKS = 2;

    private static final int FIRST = 0;
    private static final int SECOND = 1;

    private static final int RESTOCK_INTERVAL = 10;
    private static final int RUNNING_GRACE_TICKS = 10;
    private static final int REFUSAL_MEMORY_TICKS = 200;
    private static final int LAUNCH_TIMEOUT_TICKS = 40;
    private static final int STALLED_SPEED_TICKS = 200;
    private static final int RETRY_COOLDOWN_TICKS = 200;
    private static final int PROGRESS_STEPS = 64;

    private static final String INPUT_KEY = "Input";
    private static final String OUTPUT_KEY = "Output";
    private static final String OWED_KEY = "Owed";
    private static final String RETURN_KEY = "ToReturn";
    private static final String RETURN_DIRS_KEY = "ReturnDirs";
    private static final String TEMPLATE_KEY = "Template";
    private static final String NODE_KEY = "Node";
    private static final String ACTIVE_KEY = "Active";
    private static final String CLAIMED_KEY = "Claimed";
    private static final String IN_FLIGHT_KEY = "InFlight";
    private static final String MISSING_KEY = "Missing";
    private static final String BLOCKED_KEY = "Blocked";
    private static final String POWER_FAULT_KEY = "PowerFault";
    private static final String STALL_KEY = "Stall";
    private static final String REFUSAL_KEY = "Refusal";
    private static final String REFUSAL_TICKS_KEY = "RefusalTicks";
    private static final String PHASE_KEY = "Phase";
    private static final String PHASE_TICKS_KEY = "PhaseTicks";
    private static final String RECIPE_KEY = "Recipe";
    private static final String REQUIRED_KEY = "RequiredSpeed";
    private static final String TOP_SPEED_KEY = "TopSpeed";
    private static final String SPEED_KEY = "Speed";
    private static final String SPEED_TICKS_KEY = "SpeedTicks";
    private static final String COOLDOWN_KEY = "Cooldown";
    private static final String LINKS_KEY = "Links";
    private static final String FAST_LINK_KEY = "FastLink";
    private static final String FIELD_HELD_KEY = "FieldHeld";
    private static final String FIELD_LIMIT_KEY = "FieldLimit";

    private final Map<AEItemKey, Template> templates = new HashMap<>();
    private final List<BlockPos> links = new ArrayList<>(LINKS);
    private final List<ItemStack> inFlight = new ArrayList<>(LINKS);
    private final IItemHandler outputView = new OutputView();
    private final IItemHandler machineView =
            MachineItemView.demandLimited(inputBuffer, outputView, this::manualDemandFor);

    private Phase phase = Phase.IDLE;
    private int phaseTicks;
    private int speedTicks;
    private int cooldown;
    private long requiredSpeed;
    private long topSpeed;
    private long speed;
    private long peakSpeed;
    private int fastLink;
    private boolean fieldHeld;
    private long fieldLimit = Long.MAX_VALUE;

    @Nullable
    private ResourceLocation activeRecipe;

    private Stall stall = Stall.NONE;
    private Refusal refusal = Refusal.NONE;
    private int refusalTicks;

    enum Phase {
        IDLE,
        LAUNCHING,
        ACCELERATING,
        COLLIDING
    }

    enum Stall {
        NONE,
        INGREDIENTS,
        OUTPUT_FULL,
        NOT_ACCELERATING,
        PARTNER_BUSY,
        ACCELERATOR_BUSY,
        UNLOADED,
        NO_RING,
        NO_COLLISION,
        PARTICLE_LOST,
        TOO_SLOW,
        MISSING_ACCELERATOR,
        NO_LINKS,
        NO_RECIPE;

        private static Stall worse(Stall first, Stall second) {
            return first.ordinal() >= second.ordinal() ? first : second;
        }
    }

    enum Refusal {
        NONE,
        NOT_A_COLLISION_PATTERN,
        NO_ITEM_OUTPUT,
        UNKNOWN_RECIPE,
        MIXED_RECIPES,
        ITEMS_ONLY,
        TOO_MANY_INPUTS,
        TOO_FAST,
        BUFFER_FULL
    }

    private record Template(
            List<AEKey> keys, List<Long> counts, @Nullable ResourceLocation recipe) {}

    public ParticleCollisionControllerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(
                type,
                pos,
                state,
                INPUT_SLOTS,
                OUTPUT_SLOTS,
                NepOritechContent.CONTROLLER_ITEM.get(),
                NepConfig.oritechControllerIdleMeDrain(),
                NepConfig.oritechControllerChannels(),
                "Particle collision controller");
    }

    public static void serverTick(
            Level level, BlockPos pos, BlockState state, ParticleCollisionControllerBlockEntity controller) {
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
        observeRing(level);
        if (!NepConfig.oritechParticleCollisionController() || !AcceleratorProbe.available()) {
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

        switch (phase) {
            case IDLE -> {
                if (power.isPowered()) {
                    power.clearStarved();
                    if (cooldown == 0 && (scanNeeded || Math.floorMod(beat, RESTOCK_INTERVAL) == 0)) {
                        scanNeeded = false;
                        tryBegin(level);
                    }
                }
            }
            case LAUNCHING -> awaitLaunch(level);
            case ACCELERATING -> accelerate(level);
            case COLLIDING -> awaitCollision(level);
        }
        setPowerFault(!power.hasUsablePower());

        refreshComparator(level);
        refreshVisualState(level);
        syncIfChanged(level);
    }

    private void observeRing(Level level) {
        if (phase == Phase.IDLE) {
            fastLink = links.size() == LINKS
                            && !OritechThingsCompat.fieldHolds(level, AcceleratorProbe.at(level, links.get(FIRST)))
                            && OritechThingsCompat.fieldHolds(level, AcceleratorProbe.at(level, links.get(SECOND)))
                    ? SECOND
                    : FIRST;
        }
        boolean held = OritechThingsCompat.fieldHolds(level, accelerator(level, FIRST));
        if (held != fieldHeld) {
            fieldHeld = held;
            setChanged();
        }
        long fastest = 0;
        for (BlockPos link : links) {
            AcceleratorControllerBlockEntity accelerator = AcceleratorProbe.at(level, link);
            if (accelerator != null) {
                fastest = Math.max(fastest, AcceleratorProbe.speed(accelerator));
            }
        }
        if (fastest > fieldLimit) {
            fieldLimit = fastest;
            setChanged();
        }
        if (fastest > topSpeed) {
            topSpeed = fastest;
            setChanged();
        }
        speed = phase == Phase.IDLE ? 0 : fastest;
    }

    private void tryBegin(Level level) {
        if (owed.isEmpty()) {
            setStall(Stall.NONE);
            return;
        }
        Stall linkFault = linkFault(level);
        if (linkFault != Stall.NONE) {
            setStall(linkFault);
            return;
        }
        AcceleratorControllerBlockEntity first = accelerator(level, FIRST);
        AcceleratorControllerBlockEntity second = accelerator(level, SECOND);
        if (!AcceleratorProbe.idle(first) || !AcceleratorProbe.idle(second)) {
            setStall(Stall.ACCELERATOR_BUSY);
            return;
        }

        Stall reason = Stall.NONE;
        for (AEItemKey wanted : List.copyOf(owed.keySet())) {
            if (owed.getOrDefault(wanted, 0L) <= 0) {
                continue;
            }
            RecipeHolder<OritechRecipe> holder = recipeFor(level, wanted);
            if (holder == null) {
                reason = Stall.worse(reason, Stall.NO_RECIPE);
                continue;
            }
            Stall attempt = startCollision(holder, first);
            if (attempt == Stall.NONE) {
                setStall(Stall.NONE);
                return;
            }
            reason = Stall.worse(reason, attempt);
        }
        setStall(reason);
    }

    private Stall startCollision(RecipeHolder<OritechRecipe> holder, AcceleratorControllerBlockEntity first) {
        OritechRecipe recipe = holder.value();
        EncodedIngredients ingredients = OritechRecipeIngredients.collision(recipe);
        if (ingredients == null || !(ingredients.outputs().get(0).what() instanceof AEItemKey resultKey)) {
            return Stall.NO_RECIPE;
        }
        if (ParticleEnergy.launchSpeedFor(recipe.getTime()) > speedLimit()) {
            return Stall.TOO_SLOW;
        }
        ItemStack result = resultKey.toStack(1);
        if (!fitsInOutput(result)) {
            return Stall.OUTPUT_FULL;
        }
        int[] slots = pickParticles(recipe.getInputs());
        if (slots == null) {
            return Stall.INGREDIENTS;
        }

        ItemStack fast = inputBuffer.extractItem(slots[FIRST], 1, false);
        ItemStack slow = inputBuffer.extractItem(slots[SECOND], 1, false);
        claimedItems.clear();
        claimedItems.add(slow);
        AcceleratorProbe.load(first, fast);
        inFlight.add(fast.copy());

        activeResult = result;
        activeRecipe = holder.id();
        requiredSpeed = recipe.getTime();
        speedTicks = 0;
        enterPhase(Phase.LAUNCHING);
        setChanged();
        if (NepConfig.debugLogging()) {
            Nep.LOGGER.info(
                    "Particle collision controller {} launched {} from {} toward {} at speed {}",
                    getBlockPos(),
                    fast,
                    first.getBlockPos(),
                    holder.id(),
                    requiredSpeed);
        }
        return Stall.NONE;
    }

    @Nullable
    private int[] pickParticles(List<Ingredient> particles) {
        if (particles.size() != LINKS) {
            return null;
        }
        int[] left = new int[inputBuffer.getSlots()];
        for (int slot = 0; slot < left.length; slot++) {
            left[slot] = inputBuffer.getStackInSlot(slot).getCount();
        }
        int[] picked = new int[LINKS];
        for (int particle = 0; particle < LINKS; particle++) {
            picked[particle] = -1;
            for (int slot = 0; slot < left.length; slot++) {
                if (left[slot] > 0 && particles.get(particle).test(inputBuffer.getStackInSlot(slot))) {
                    left[slot]--;
                    picked[particle] = slot;
                    break;
                }
            }
            if (picked[particle] < 0) {
                return null;
            }
        }
        return picked;
    }

    private void awaitLaunch(Level level) {
        AcceleratorControllerBlockEntity first = accelerator(level, FIRST);
        if (first == null) {
            waitOrLose(level, FIRST);
            return;
        }
        if (AcceleratorProbe.live(first) != null) {
            enterPhase(Phase.ACCELERATING);
            return;
        }
        if (first.inventory.getItem(AcceleratorProbe.INPUT_SLOT).isEmpty()) {
            loseCraft(level, Stall.PARTICLE_LOST, "the first particle vanished as it launched");
            return;
        }
        if (++phaseTicks > LAUNCH_TIMEOUT_TICKS) {
            restore(level, AcceleratorProbe.unload(first));
            abandon(level, Stall.NO_RING);
        }
    }

    private void accelerate(Level level) {
        AcceleratorControllerBlockEntity first = accelerator(level, FIRST);
        if (first == null) {
            waitOrLose(level, FIRST);
            return;
        }
        AcceleratorParticleLogic.ActiveParticle particle = AcceleratorProbe.live(first);
        if (particle == null) {
            AcceleratorParticleLogic.ActiveParticle lost = first.getParticle();
            loseCraft(
                    level,
                    Stall.PARTICLE_LOST,
                    "the first particle left the ring at speed " + (lost == null ? "unknown" : lost.velocity));
            return;
        }
        runningGrace = RUNNING_GRACE_TICKS;
        drawActivePower();
        if (particle.velocity > peakSpeed) {
            peakSpeed = particle.velocity;
            speedTicks = 0;
            if (stall == Stall.NOT_ACCELERATING) {
                setStall(Stall.NONE);
            }
        } else if (++speedTicks > STALLED_SPEED_TICKS) {
            setStall(Stall.NOT_ACCELERATING);
        }
        if (particle.velocity < ParticleEnergy.launchSpeedFor(requiredSpeed)) {
            return;
        }
        AcceleratorControllerBlockEntity second = accelerator(level, SECOND);
        if (second == null || !AcceleratorProbe.idle(second) || claimedItems.isEmpty()) {
            setStall(Stall.PARTNER_BUSY);
            return;
        }
        ItemStack slow = claimedItems.remove(0);
        AcceleratorProbe.load(second, slow);
        inFlight.add(slow.copy());
        claimedItems.clear();
        setStall(Stall.NONE);
        enterPhase(Phase.COLLIDING);
    }

    private void awaitCollision(Level level) {
        AcceleratorControllerBlockEntity first = accelerator(level, FIRST);
        AcceleratorControllerBlockEntity second = accelerator(level, SECOND);
        if (first == null || second == null) {
            waitOrLose(level, first == null ? FIRST : SECOND);
            return;
        }
        runningGrace = RUNNING_GRACE_TICKS;
        ItemStack collected = collectResult(first);
        if (collected.isEmpty()) {
            collected = collectResult(second);
        }
        if (!collected.isEmpty()) {
            if (ItemStack.isSameItemSameComponents(collected, activeResult)) {
                finishCraft(level);
            } else {
                Nep.LOGGER.warn(
                        "Particle collision controller {} expected {} but the collision made {}",
                        getBlockPos(),
                        activeResult,
                        collected);
                abandon(level, Stall.NO_RECIPE);
            }
            return;
        }
        boolean fastLive = AcceleratorProbe.live(first) != null;
        boolean slowLive = AcceleratorProbe.live(second) != null;
        boolean slowWaiting =
                !second.inventory.getItem(AcceleratorProbe.INPUT_SLOT).isEmpty();
        boolean resultWaiting = !first.inventory
                        .getItem(AcceleratorProbe.OUTPUT_SLOT)
                        .isEmpty()
                || !second.inventory.getItem(AcceleratorProbe.OUTPUT_SLOT).isEmpty();

        if (!fastLive && !slowLive && !slowWaiting) {
            if (!resultWaiting) {
                loseCraft(level, Stall.PARTICLE_LOST, "the collision produced nothing");
            }
            return;
        }
        if (!fastLive) {
            restore(level, slowWaiting ? AcceleratorProbe.unload(second) : AcceleratorProbe.recall(second));
            loseCraft(level, Stall.PARTICLE_LOST, "the fast particle left the ring before the collision");
            return;
        }
        if (!slowLive && !slowWaiting) {
            restore(level, AcceleratorProbe.recall(first));
            loseCraft(level, Stall.PARTICLE_LOST, "the slow particle left the ring before the collision");
            return;
        }
        drawActivePower();
        int timeout = slowWaiting ? LAUNCH_TIMEOUT_TICKS : NepConfig.oritechControllerCollisionTimeout();
        if (++phaseTicks > timeout) {
            restore(level, AcceleratorProbe.recall(first));
            restore(level, slowWaiting ? AcceleratorProbe.unload(second) : AcceleratorProbe.recall(second));
            abandon(level, slowWaiting ? Stall.NO_RING : Stall.NO_COLLISION);
        }
    }

    private ItemStack collectResult(AcceleratorControllerBlockEntity accelerator) {
        ItemStack waiting = accelerator.inventory.getItem(AcceleratorProbe.OUTPUT_SLOT);
        if (waiting.isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (!fitsInOutput(waiting)) {
            outputBlocked = true;
            return ItemStack.EMPTY;
        }
        ItemStack result = AcceleratorProbe.takeResult(accelerator);
        ItemHandlerHelper.insertItem(outputBuffer, result.copy(), false);
        outputBlocked = false;
        return result;
    }

    private void finishCraft(Level level) {
        AEItemKey produced = AEItemKey.of(activeResult);
        if (produced != null && owed.getOrDefault(produced, 0L) > 0) {
            decrement(owed, produced, 1);
            if (!owed.containsKey(produced)) {
                templates.remove(produced);
            }
            if (owed.isEmpty()) {
                pushingCpus.clear();
            }
            toReturn.merge(produced, 1L, Long::sum);
        }
        if (NepConfig.debugLogging()) {
            Nep.LOGGER.info(
                    "Particle collision controller {} collected {} ({} still owed)",
                    getBlockPos(),
                    produced,
                    produced == null ? 0 : owed.getOrDefault(produced, 0L));
        }
        resetCraft();
        flushOutput(level);
    }

    private void loseCraft(Level level, Stall reason, String detail) {
        Nep.LOGGER.warn("Particle collision controller {} lost a collision: {}", getBlockPos(), detail);
        long ceiling = ParticleEnergy.launchSpeedFor(requiredSpeed) - 1;
        if (topSpeed > ceiling) {
            topSpeed = Math.max(0, ceiling);
        }
        if (fieldHeld && fieldLimit > ceiling) {
            fieldLimit = Math.max(0, ceiling);
        }
        abandon(level, reason);
    }

    private void abandon(Level level, Stall reason) {
        for (ItemStack held : List.copyOf(claimedItems)) {
            restore(level, held);
        }
        claimedItems.clear();
        if (NepConfig.debugLogging()) {
            Nep.LOGGER.info("Particle collision controller {} abandoned {}: {}", getBlockPos(), activeRecipe, reason);
        }
        resetCraft();
        cooldown = RETRY_COOLDOWN_TICKS;
        setStall(reason);
    }

    private void waitOrLose(Level level, int role) {
        int index = role ^ fastLink;
        if (index < links.size() && !level.isLoaded(links.get(index))) {
            setStall(Stall.UNLOADED);
            return;
        }
        Nep.LOGGER.warn(
                "Particle collision controller {} lost a collision: a linked accelerator is gone", getBlockPos());
        abandon(level, Stall.MISSING_ACCELERATOR);
    }

    private void recallActive(Level level) {
        AcceleratorControllerBlockEntity first = accelerator(level, FIRST);
        AcceleratorControllerBlockEntity second = accelerator(level, SECOND);
        if (first != null) {
            restore(
                    level,
                    AcceleratorProbe.live(first) != null
                            ? AcceleratorProbe.recall(first)
                            : AcceleratorProbe.unload(first));
        }
        if (second != null && phase == Phase.COLLIDING) {
            restore(
                    level,
                    AcceleratorProbe.live(second) != null
                            ? AcceleratorProbe.recall(second)
                            : AcceleratorProbe.unload(second));
        }
        abandon(level, Stall.NONE);
    }

    private void restore(Level level, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
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

    private void resetCraft() {
        activeResult = ItemStack.EMPTY;
        activeRecipe = null;
        requiredSpeed = 0;
        speedTicks = 0;
        peakSpeed = 0;
        claimedItems.clear();
        inFlight.clear();
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

    private boolean drawActivePower() {
        double extra = NepConfig.oritechControllerMeDrain() - NepConfig.oritechControllerIdleMeDrain();
        if (extra <= 0) {
            power.clearStarved();
            return true;
        }
        power.extractPower(extra);
        return !power.isStarved();
    }

    private Stall linkFault(Level level) {
        if (links.size() < LINKS) {
            return Stall.NO_LINKS;
        }
        for (BlockPos link : links) {
            if (!level.isLoaded(link)) {
                return Stall.UNLOADED;
            }
            if (AcceleratorProbe.at(level, link) == null) {
                return Stall.MISSING_ACCELERATOR;
            }
        }
        return Stall.NONE;
    }

    @Nullable
    private AcceleratorControllerBlockEntity accelerator(Level level, int role) {
        int index = role ^ fastLink;
        return index < links.size() ? AcceleratorProbe.at(level, links.get(index)) : null;
    }

    @Nullable
    private RecipeHolder<OritechRecipe> recipeFor(Level level, AEItemKey output) {
        Template template = templates.get(output);
        return template == null || template.recipe() == null
                ? null
                : OritechRecipeResolver.collisionById(level, template.recipe());
    }

    boolean readyForPatterns() {
        Level level = getLevel();
        return NepConfig.oritechParticleCollisionController()
                && AcceleratorProbe.available()
                && level != null
                && !level.isClientSide
                && links.size() == LINKS;
    }

    boolean pushControllerPattern(IPatternDetails pattern, KeyCounter[] inputs, Direction ejectionDirection) {
        Level level = getLevel();
        if (level == null || level.isClientSide || !readyForPatterns()) {
            return false;
        }
        if (!(pattern instanceof ParticleCollisionPattern collision)) {
            return reject(Refusal.NOT_A_COLLISION_PATTERN, "pattern is not a particle collision pattern");
        }
        List<GenericStack> outputs = pattern.getOutputs();
        if (outputs.isEmpty() || !(outputs.get(0).what() instanceof AEItemKey producedItem)) {
            return reject(Refusal.NO_ITEM_OUTPUT, "pattern has no item output");
        }
        RecipeHolder<OritechRecipe> holder = OritechRecipeResolver.collisionById(level, collision.recipe());
        OritechRecipeResolver.CollisionPlan plan =
                holder == null ? null : OritechRecipeResolver.planCollision(holder, pattern);
        if (plan == null) {
            return reject(Refusal.UNKNOWN_RECIPE, "no particle collision recipe matches " + collision.recipe());
        }
        if (ParticleEnergy.launchSpeedFor(plan.minimumSpeed()) > speedLimit()) {
            return reject(
                    Refusal.TOO_FAST,
                    "recipe needs speed " + plan.minimumSpeed() + " and the ring has only reached " + speedLimit());
        }
        Template existing = templates.get(producedItem);
        if (existing != null && !collision.recipe().equals(existing.recipe())) {
            return reject(
                    Refusal.MIXED_RECIPES,
                    "a collision for " + producedItem + " from " + existing.recipe() + " is already queued");
        }

        Map<AEItemKey, Long> items = new HashMap<>();
        for (KeyCounter counter : inputs) {
            for (var entry : counter) {
                if (!(entry.getKey() instanceof AEItemKey itemKey) || entry.getLongValue() <= 0) {
                    return reject(Refusal.ITEMS_ONLY, "particle collisions take items only");
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
        captureTemplate(producedItem, plan.expectedItems(), collision.recipe());
        owed.merge(producedItem, plan.batch(), Long::sum);
        pushingCpus.record();
        clearRefusal();
        markScanNeeded();
        setChanged();
        if (NepConfig.debugLogging()) {
            Nep.LOGGER.info(
                    "Particle collision controller {} accepted {} collision(s) for {}",
                    getBlockPos(),
                    plan.batch(),
                    producedItem);
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
            Nep.LOGGER.info("Particle collision controller {} rejected pattern: {}", getBlockPos(), reason);
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
        if (!NepConfig.oritechControllerAutoRequest()) {
            setMissingInputs(List.of());
            return;
        }
        List<AEKey> keys = new ArrayList<>();
        List<Long> targets = new ArrayList<>();
        for (Map.Entry<AEItemKey, Long> entry : owed.entrySet()) {
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

    @Override
    public long bufferedAmount(AEKey key) {
        long total = super.bufferedAmount(key);
        if (key instanceof AEItemKey itemKey) {
            for (ItemStack stack : inFlight) {
                if (itemKey.matches(stack)) {
                    total += stack.getCount();
                }
            }
        }
        return total;
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
        return (int) Math.max(0, Math.min(Integer.MAX_VALUE, target - bufferedAmount(key)));
    }

    @Override
    public void onGridStateChanged() {
        markScanNeeded();
        setChanged();
    }

    boolean applyLinks(List<BlockPos> accelerators) {
        if (phase != Phase.IDLE || accelerators.size() > LINKS) {
            return false;
        }
        links.clear();
        links.addAll(accelerators);
        fastLink = FIRST;
        fieldLimit = Long.MAX_VALUE;
        topSpeed = 0;
        Level level = getLevel();
        if (level != null) {
            for (BlockPos link : links) {
                AcceleratorControllerBlockEntity accelerator = AcceleratorProbe.at(level, link);
                AcceleratorParticleLogic.ActiveParticle last = accelerator == null ? null : accelerator.getParticle();
                if (last != null) {
                    topSpeed = Math.max(topSpeed, last.velocity);
                }
            }
        }
        cooldown = 0;
        markScanNeeded();
        setChanged();
        return true;
    }

    boolean clearLinks() {
        return applyLinks(List.of());
    }

    List<BlockPos> links() {
        return List.copyOf(links);
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
        hash = 31 * hash + Long.hashCode(topSpeed);
        hash = 31 * hash + Boolean.hashCode(fieldHeld);
        hash = 31 * hash + Long.hashCode(fieldLimit);
        hash = 31 * hash + Long.hashCode(requiredSpeed);
        hash = 31 * hash + Long.hashCode(pendingJobs());
        hash = 31 * hash + links.hashCode();
        return hash;
    }

    private void refreshVisualState(Level level) {
        BlockState state = getBlockState();
        if (!state.hasProperty(ParticleCollisionControllerBlock.STATUS)) {
            return;
        }
        ControllerStatus desired = desiredStatus();
        if (state.getValue(ParticleCollisionControllerBlock.STATUS) != desired) {
            level.setBlock(
                    getBlockPos(),
                    state.setValue(ParticleCollisionControllerBlock.STATUS, desired),
                    Block.UPDATE_CLIENTS);
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
        if (phase == Phase.IDLE || requiredSpeed <= 0) {
            return 0.0F;
        }
        if (phase == Phase.COLLIDING) {
            return 1.0F;
        }
        return Math.min(1.0F, speed / (float) ParticleEnergy.launchSpeedFor(requiredSpeed));
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

    long topSpeed() {
        return topSpeed;
    }

    boolean fieldHeld() {
        return fieldHeld;
    }

    long speedLimit() {
        return fieldHeld ? fieldLimit : topSpeed;
    }

    long requiredSpeed() {
        return requiredSpeed;
    }

    long speed() {
        return speed;
    }

    int linkCount() {
        return links.size();
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
        Level level = getLevel();
        if (level != null && phase != Phase.IDLE) {
            recallActive(level);
        }
        if (owed.isEmpty() && templates.isEmpty()) {
            return;
        }
        int cancelled = pushingCpus.cancelJobsFor(power.grid(), Set.copyOf(templates.keySet()));
        if (NepConfig.debugLogging()) {
            Nep.LOGGER.info(
                    "Particle collision controller {} cleared {} pending collision(s) and cancelled {} network job(s)",
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
                claimed.add(stack.save(registries));
            }
        }
        tag.put(CLAIMED_KEY, claimed);
        ListTag launched = new ListTag();
        for (ItemStack stack : inFlight) {
            if (!stack.isEmpty()) {
                launched.add(stack.save(registries));
            }
        }
        tag.put(IN_FLIGHT_KEY, launched);
        tag.putBoolean(BLOCKED_KEY, outputBlocked);
        tag.putBoolean(POWER_FAULT_KEY, powerFault);
        tag.putString(STALL_KEY, stall.name());
        tag.putString(REFUSAL_KEY, refusal.name());
        tag.putInt(REFUSAL_TICKS_KEY, refusalTicks);
        tag.putString(PHASE_KEY, phase.name());
        tag.putInt(PHASE_TICKS_KEY, phaseTicks);
        tag.putInt(SPEED_TICKS_KEY, speedTicks);
        tag.putInt(COOLDOWN_KEY, cooldown);
        tag.putLong(REQUIRED_KEY, requiredSpeed);
        tag.putLong(TOP_SPEED_KEY, topSpeed);
        tag.putInt(FAST_LINK_KEY, fastLink);
        tag.putBoolean(FIELD_HELD_KEY, fieldHeld);
        tag.putLong(FIELD_LIMIT_KEY, fieldLimit);
        tag.putLong(SPEED_KEY, speed);
        tag.putLongArray(LINKS_KEY, links.stream().mapToLong(BlockPos::asLong).toArray());
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
            ItemStack.parse(registries, claimed.getCompound(i)).ifPresent(claimedItems::add);
        }
        inFlight.clear();
        ListTag launched = tag.getList(IN_FLIGHT_KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < launched.size(); i++) {
            ItemStack.parse(registries, launched.getCompound(i)).ifPresent(inFlight::add);
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
        stall = byName(Stall.values(), tag.getString(STALL_KEY), Stall.NONE);
        refusal = byName(Refusal.values(), tag.getString(REFUSAL_KEY), Refusal.NONE);
        refusalTicks = refusal == Refusal.NONE
                ? 0
                : Math.max(1, Math.min(REFUSAL_MEMORY_TICKS, tag.getInt(REFUSAL_TICKS_KEY)));
        phase = byName(Phase.values(), tag.getString(PHASE_KEY), Phase.IDLE);
        phaseTicks = tag.getInt(PHASE_TICKS_KEY);
        speedTicks = tag.getInt(SPEED_TICKS_KEY);
        cooldown = tag.getInt(COOLDOWN_KEY);
        requiredSpeed = tag.getLong(REQUIRED_KEY);
        topSpeed = tag.getLong(TOP_SPEED_KEY);
        fastLink = tag.getInt(FAST_LINK_KEY) == SECOND ? SECOND : FIRST;
        fieldHeld = tag.getBoolean(FIELD_HELD_KEY);
        fieldLimit = tag.contains(FIELD_LIMIT_KEY) ? tag.getLong(FIELD_LIMIT_KEY) : Long.MAX_VALUE;
        speed = tag.getLong(SPEED_KEY);
        links.clear();
        for (long packed : tag.getLongArray(LINKS_KEY)) {
            if (links.size() < LINKS) {
                links.add(BlockPos.of(packed));
            }
        }
        activeRecipe = tag.contains(RECIPE_KEY) ? ResourceLocation.tryParse(tag.getString(RECIPE_KEY)) : null;
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
            AEItemKey item = ItemCounts.key(tag, registries);
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
        return NepOritechContent.CONTROLLER.get().getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory inventory, Player player) {
        return new ParticleCollisionControllerMenu(windowId, inventory, getBlockPos());
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
