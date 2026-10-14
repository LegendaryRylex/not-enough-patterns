package dev.rylex.nep.compat.occultism;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import com.klikli_dev.occultism.common.entity.spirit.SpiritEntity;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.machine.ReturnDirections;
import dev.rylex.nep.util.ItemCounts;
import dev.rylex.nep.util.OfferedItems;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

public class SpiritProviderBlockEntity extends BlockEntity {

    /** Owner stamped on an offered item so no player can pick it up; spirits ignore the owner. */
    private static final UUID NO_PLAYER = UUID.fromString("6e657000-0000-4000-8000-73706972746f");

    private static final int WORK_INTERVAL = 10;
    private static final int SPIRIT_CACHE_TICKS = 20;
    private static final int OFFER_TIMEOUT = 600;
    private static final double BOWL_FLOOR = 5.0 / 16.0;

    private static final String QUEUE_KEY = "Queue";
    private static final String RETURNS_KEY = "Returns";
    private static final String OUTSTANDING_KEY = "Outstanding";
    private static final String OFFERED_KEY = "Offered";
    private static final String OFFERED_AT_KEY = "OfferedAt";
    private static final String RETURN_DIRS_KEY = "ReturnDirs";
    private static final String LAST_RETURN_KEY = "LastReturn";

    private final Deque<ItemStack> queue = new ArrayDeque<>();
    private final List<ItemStack> returns = new ArrayList<>();
    private final Map<AEItemKey, Long> outstanding = new HashMap<>();
    private final ReturnDirections returnDirections = new ReturnDirections();

    @Nullable
    private Direction lastReturn;

    @Nullable
    private UUID offered;

    private long offeredAt;
    private List<SpiritEntity> spirits = List.of();
    private long spiritsAt = Long.MIN_VALUE;

    public SpiritProviderBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        SpiritProviders.add(this);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        SpiritProviders.remove(this);
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        SpiritProviders.remove(this);
    }

    boolean push(IPatternDetails details, KeyCounter[] inputs, Direction ejection) {
        if (level == null || level.isClientSide() || !NepConfig.occultismSpiritProvider()) {
            return false;
        }
        if (load() >= NepConfig.occultismSpiritProviderQueue()) {
            return false;
        }
        List<ItemStack> stacks = new ArrayList<>();
        for (KeyCounter counter : inputs) {
            for (var entry : counter) {
                if (!(entry.getKey() instanceof AEItemKey key) || entry.getLongValue() <= 0) {
                    return false;
                }
                long remaining = entry.getLongValue();
                while (remaining > 0) {
                    int count = (int) Math.min(remaining, key.getMaxStackSize());
                    stacks.add(key.toStack(count));
                    remaining -= count;
                }
            }
        }
        if (stacks.isEmpty() || !servable(stacks)) {
            return false;
        }
        queue.addAll(stacks);
        for (GenericStack output : details.getOutputs()) {
            if (output.what() instanceof AEItemKey key) {
                returnDirections.record(key, ejection);
            }
        }
        lastReturn = ejection;
        setChanged();
        return true;
    }

    boolean claims(ItemStack input) {
        Long owed = outstanding.get(AEItemKey.of(input.getItem()));
        return owed != null && owed > 0;
    }

    void capture(ItemStack input, ItemStack result) {
        AEItemKey key = AEItemKey.of(input.getItem());
        settle(key, input.getCount());
        returns.add(result.copy());
        setChanged();
    }

    private void settle(AEItemKey key, int count) {
        long left = outstanding.getOrDefault(key, 0L) - count;
        if (left > 0) {
            outstanding.put(key, left);
        } else {
            outstanding.remove(key);
        }
    }

    void serverTick(ServerLevel level) {
        if (level.getGameTime() % WORK_INTERVAL != 0) {
            return;
        }
        flushReturns(level);
        if (!NepConfig.occultismSpiritProvider()) {
            return;
        }
        if (offered != null) {
            watchOffer(level);
        }
        if (offered == null && !queue.isEmpty()) {
            offerNext(level);
        }
    }

    void dropContents(Level level, BlockPos pos) {
        reclaimOffer(level);
        for (ItemStack stack : queue) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
        }
        for (ItemStack stack : returns) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
        }
        queue.clear();
        returns.clear();
        outstanding.clear();
    }

    private int load() {
        return queue.size() + returns.size() + outstanding.size();
    }

    private boolean servable(List<ItemStack> stacks) {
        List<SpiritEntity> available = spirits();
        for (ItemStack stack : stacks) {
            if (available.stream().noneMatch(spirit -> WorkingSpirits.accepts(spirit, stack))) {
                return false;
            }
        }
        return true;
    }

    private List<SpiritEntity> spirits() {
        if (level == null) {
            return List.of();
        }
        long now = level.getGameTime();
        if (spiritsAt == Long.MIN_VALUE || now - spiritsAt >= SPIRIT_CACHE_TICKS) {
            spirits = WorkingSpirits.serving(level, getBlockPos(), NepConfig.occultismSpiritProviderRange());
            spiritsAt = now;
        }
        return spirits;
    }

    private void offerNext(ServerLevel level) {
        ItemStack next = queue.peekFirst();
        boolean taker = spirits().stream()
                .anyMatch(spirit ->
                        spirit.isAlive() && WorkingSpirits.idle(spirit) && WorkingSpirits.accepts(spirit, next));
        if (!taker) {
            return;
        }
        queue.pollFirst();
        Vec3 top = Vec3.upFromBottomCenterOf(getBlockPos(), BOWL_FLOOR);
        ItemEntity item = new ItemEntity(level, top.x, top.y, top.z, next, 0, 0, 0);
        item.setUnlimitedLifetime();
        item.setTarget(NO_PLAYER);
        OfferedItems.mark(item);
        level.addFreshEntity(item);
        offered = item.getUUID();
        offeredAt = level.getGameTime();
        outstanding.merge(AEItemKey.of(next.getItem()), (long) next.getCount(), Long::sum);
        setChanged();
    }

    private void watchOffer(ServerLevel level) {
        Entity entity = level.getEntity(offered);
        if (!(entity instanceof ItemEntity item) || !item.isAlive()) {
            offered = null;
            setChanged();
            return;
        }
        if (level.getGameTime() - offeredAt >= OFFER_TIMEOUT) {
            reclaimOffer(level);
        }
    }

    private void reclaimOffer(Level level) {
        if (offered == null || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (serverLevel.getEntity(offered) instanceof ItemEntity item && item.isAlive()) {
            ItemStack stack = item.getItem().copy();
            item.discard();
            AEItemKey key = AEItemKey.of(stack.getItem());
            settle(key, stack.getCount());
            queue.addFirst(stack);
        }
        offered = null;
        setChanged();
    }

    private void flushReturns(ServerLevel level) {
        if (returns.isEmpty()) {
            return;
        }
        boolean changed = false;
        for (Iterator<ItemStack> it = returns.iterator(); it.hasNext(); ) {
            ItemStack stack = it.next();
            IItemHandler target = returnTarget(level, stack);
            if (target == null) {
                continue;
            }
            ItemStack rest = ItemHandlerHelper.insertItemStacked(target, stack, false);
            if (rest.getCount() != stack.getCount()) {
                changed = true;
            }
            if (rest.isEmpty()) {
                it.remove();
            } else {
                stack.setCount(rest.getCount());
            }
        }
        if (changed) {
            setChanged();
        }
    }

    @Nullable
    private IItemHandler returnTarget(ServerLevel level, ItemStack stack) {
        AEItemKey key = AEItemKey.of(stack);
        IItemHandler target = key == null ? null : returnDirections.targetFor(level, getBlockPos(), key);
        if (target != null || lastReturn == null) {
            return target;
        }
        return level.getCapability(
                Capabilities.ItemHandler.BLOCK, getBlockPos().relative(lastReturn), lastReturn.getOpposite());
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put(QUEUE_KEY, saveStacks(queue, registries));
        tag.put(RETURNS_KEY, saveStacks(returns, registries));
        tag.put(OUTSTANDING_KEY, ItemCounts.save(outstanding, registries));
        tag.put(RETURN_DIRS_KEY, returnDirections.save(registries));
        if (lastReturn != null) {
            tag.putInt(LAST_RETURN_KEY, lastReturn.get3DDataValue());
        }
        if (offered != null) {
            tag.putUUID(OFFERED_KEY, offered);
            tag.putLong(OFFERED_AT_KEY, offeredAt);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        queue.clear();
        queue.addAll(loadStacks(tag.getList(QUEUE_KEY, Tag.TAG_COMPOUND), registries));
        returns.clear();
        returns.addAll(loadStacks(tag.getList(RETURNS_KEY, Tag.TAG_COMPOUND), registries));
        ItemCounts.load(outstanding, tag, OUTSTANDING_KEY, registries);
        returnDirections.load(tag, RETURN_DIRS_KEY, LAST_RETURN_KEY, List.of(), registries);
        lastReturn = tag.contains(LAST_RETURN_KEY) ? Direction.from3DDataValue(tag.getInt(LAST_RETURN_KEY)) : null;
        offered = tag.hasUUID(OFFERED_KEY) ? tag.getUUID(OFFERED_KEY) : null;
        offeredAt = tag.getLong(OFFERED_AT_KEY);
    }

    private static ListTag saveStacks(Iterable<ItemStack> stacks, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) {
                list.add(stack.save(registries));
            }
        }
        return list;
    }

    private static List<ItemStack> loadStacks(ListTag list, HolderLookup.Provider registries) {
        List<ItemStack> stacks = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            ItemStack stack = ItemStack.parseOptional(registries, list.getCompound(i));
            if (!stack.isEmpty()) {
                stacks.add(stack);
            }
        }
        return stacks;
    }
}
