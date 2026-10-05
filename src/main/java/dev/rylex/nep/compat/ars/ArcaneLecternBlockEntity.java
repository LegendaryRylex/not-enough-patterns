package dev.rylex.nep.compat.ars;

import appeng.api.config.Actionable;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.MEStorage;
import dev.rylex.nep.NepConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ArcaneLecternBlockEntity extends BlockEntity implements IInWorldGridNodeHost, IActionHost {

    private static final String NODE_KEY = "node";

    private static final IGridNodeListener<ArcaneLecternBlockEntity> LISTENER = new Listener();

    private static final int ONLINE_CHECK_DELAY = 20;
    private static final float OPEN_RATE = 0.05F;
    private static final int FLIP_CHANCE = 60;
    private static final float FLIP_PULL = 0.4F;
    private static final float FLIP_LIMIT = 0.2F;
    private static final float FLIP_DAMPING = 0.9F;

    private final IManagedGridNode mainNode;
    private final IActionSource source;
    private final NetworkView view = new NetworkView();

    private AEItemKey[] keys = new AEItemKey[0];
    private long[] amounts = new long[0];
    private long snapshotTick = Long.MIN_VALUE;

    int time;
    float open;
    float oOpen;
    float flip;
    float oFlip;
    private float flipTarget;
    private float flipSpeed;

    public ArcaneLecternBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.mainNode = GridHelper.createManagedNode(this, LISTENER)
                .setInWorldNode(true)
                .setTagName("proxy")
                .setVisualRepresentation(NepArsContent.ARCANE_LECTERN_ITEM.get());
        this.source = IActionSource.ofMachine(this);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide() && mainNode.getNode() == null) {
            mainNode.setIdlePowerUsage(NepConfig.arsArcaneLecternIdleMeDrain());
            mainNode.create(level, getBlockPos());
            level.scheduleTick(getBlockPos(), getBlockState().getBlock(), ONLINE_CHECK_DELAY);
            ArcaneLecterns.add(this);
        }
    }

    private void scheduleOnlineCheck() {
        if (level != null && !level.isClientSide() && level.isLoaded(getBlockPos())) {
            level.scheduleTick(getBlockPos(), getBlockState().getBlock(), 1);
        }
    }

    void showOnline() {
        if (level == null || level.isClientSide() || isRemoved()) {
            return;
        }
        BlockState current = getBlockState();
        boolean online = mainNode.isActive();
        if (current.hasProperty(ArcaneLecternBlock.ONLINE) && current.getValue(ArcaneLecternBlock.ONLINE) != online) {
            level.setBlock(getBlockPos(), current.setValue(ArcaneLecternBlock.ONLINE, online), Block.UPDATE_CLIENTS);
        }
    }

    void clientTick(Level level) {
        oOpen = open;
        oFlip = flip;
        time++;
        boolean online = getBlockState().getValue(ArcaneLecternBlock.ONLINE);
        open = Mth.clamp(open + (online ? OPEN_RATE : -OPEN_RATE), 0.0F, 1.0F);
        if (online && level.random.nextInt(FLIP_CHANCE) == 0) {
            float was = flipTarget;
            do {
                flipTarget += level.random.nextInt(4) - level.random.nextInt(4);
            } while (was == flipTarget);
        }
        float pull = Mth.clamp((flipTarget - flip) * FLIP_PULL, -FLIP_LIMIT, FLIP_LIMIT);
        flipSpeed += (pull - flipSpeed) * FLIP_DAMPING;
        flip += flipSpeed;
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        ArcaneLecterns.remove(this);
        mainNode.destroy();
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        ArcaneLecterns.remove(this);
        mainNode.destroy();
    }

    ItemStack takeOne(Ingredient ingredient, Predicate<ItemStack> accepts) {
        MEStorage storage = storage();
        if (storage == null) {
            return ItemStack.EMPTY;
        }
        for (ItemStack candidate : ingredient.getItems()) {
            AEItemKey key = AEItemKey.of(candidate);
            if (key == null || !accepts.test(key.toStack())) {
                continue;
            }
            if (storage.extract(key, 1, Actionable.MODULATE, source) == 1) {
                snapshotTick = Long.MIN_VALUE;
                return key.toStack();
            }
        }
        return ItemStack.EMPTY;
    }

    public IItemHandler itemHandler() {
        return view;
    }

    @Nullable
    @Override
    public IGridNode getGridNode(Direction dir) {
        return mainNode.getNode();
    }

    @Override
    public IGridNode getActionableNode() {
        return mainNode.getNode();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        CompoundTag node = new CompoundTag();
        mainNode.saveToNBT(node);
        tag.put(NODE_KEY, node);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains(NODE_KEY)) {
            mainNode.loadFromNBT(tag.getCompound(NODE_KEY));
        }
    }

    private void refresh() {
        if (level == null || level.isClientSide()) {
            return;
        }
        long now = level.getGameTime();
        if (now == snapshotTick) {
            return;
        }
        snapshotTick = now;

        MEStorage storage = storage();
        if (storage == null) {
            keys = new AEItemKey[0];
            amounts = new long[0];
            return;
        }

        int cap = NepConfig.arsArcaneLecternMaxTypes();
        List<AEItemKey> foundKeys = new ArrayList<>();
        List<Long> foundAmounts = new ArrayList<>();
        for (var entry : storage.getAvailableStacks()) {
            if (entry.getKey() instanceof AEItemKey key && entry.getLongValue() > 0) {
                foundKeys.add(key);
                foundAmounts.add(entry.getLongValue());
                if (foundKeys.size() >= cap) {
                    break;
                }
            }
        }

        keys = foundKeys.toArray(new AEItemKey[0]);
        amounts = new long[foundAmounts.size()];
        for (int i = 0; i < amounts.length; i++) {
            amounts[i] = foundAmounts.get(i);
        }
    }

    @Nullable
    private MEStorage storage() {
        if (!NepConfig.arsArcaneLectern() || !mainNode.isActive()) {
            return null;
        }
        IGrid grid = mainNode.getGrid();
        return grid == null ? null : grid.getStorageService().getInventory();
    }

    private final class NetworkView implements IItemHandler {

        @Override
        public int getSlots() {
            refresh();
            return keys.length;
        }

        /** Reports the whole network amount, the oversized-slot convention Ars's Item Detector and storage drawers read. */
        @NotNull
        @Override
        public ItemStack getStackInSlot(int slot) {
            refresh();
            if (slot < 0 || slot >= keys.length) {
                return ItemStack.EMPTY;
            }
            return keys[slot].toStack((int) Math.min(amounts[slot], Integer.MAX_VALUE));
        }

        @NotNull
        @Override
        public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            return stack;
        }

        @NotNull
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            refresh();
            if (slot < 0 || slot >= keys.length || amount <= 0) {
                return ItemStack.EMPTY;
            }
            AEItemKey key = keys[slot];
            int limit = Math.min(amount, key.getItem().getDefaultMaxStackSize());
            if (limit <= 0) {
                return ItemStack.EMPTY;
            }
            if (simulate) {
                return key.toStack((int) Math.min(limit, amounts[slot]));
            }
            MEStorage storage = storage();
            if (storage == null) {
                return ItemStack.EMPTY;
            }
            long extracted = storage.extract(key, limit, Actionable.MODULATE, source);
            if (extracted <= 0) {
                return ItemStack.EMPTY;
            }
            snapshotTick = Long.MIN_VALUE;
            return key.toStack((int) extracted);
        }

        @Override
        public int getSlotLimit(int slot) {
            return Integer.MAX_VALUE;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return false;
        }
    }

    private static final class Listener implements IGridNodeListener<ArcaneLecternBlockEntity> {
        @Override
        public void onSaveChanges(ArcaneLecternBlockEntity host, IGridNode node) {
            host.setChanged();
        }

        @Override
        public void onStateChanged(ArcaneLecternBlockEntity host, IGridNode node, State state) {
            host.snapshotTick = Long.MIN_VALUE;
            host.scheduleOnlineCheck();
        }
    }
}
