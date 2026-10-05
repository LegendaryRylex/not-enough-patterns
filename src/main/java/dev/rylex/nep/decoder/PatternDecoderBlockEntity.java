package dev.rylex.nep.decoder;

import appeng.api.networking.IInWorldGridNodeHost;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class PatternDecoderBlockEntity extends BlockEntity implements MenuProvider {

    public static final int SLOTS = DecoderModule.values().length;

    private static final String MODULES_KEY = "Modules";

    private final ModuleSlots modules = new ModuleSlots() {
        @Override
        protected void onContentsChanged(int slot) {
            held = held();
            setChanged();
        }

        @Override
        protected void onLoad() {
            held = held();
        }
    };

    private final DecoderGridNode node = new DecoderGridNode(this);

    private int held;

    public PatternDecoderBlockEntity(BlockPos pos, BlockState state) {
        super(DecoderContent.PATTERN_DECODER_BLOCK_ENTITY.get(), pos, state);
    }

    public IInWorldGridNodeHost gridNodeHost() {
        return node;
    }

    public ModuleSlots moduleSlots() {
        return modules;
    }

    int modules() {
        return held;
    }

    public DecoderStatus status() {
        return node.status();
    }

    public int networkModules() {
        return status() == DecoderStatus.ONLINE ? PatternDecoding.modulesOn(node.grid()) : 0;
    }

    public void serverTick(Level level) {
        node.create(level, worldPosition);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        node.destroy();
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        node.destroy();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        node.save(tag);
        tag.put(MODULES_KEY, modules.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        node.load(tag);
        if (tag.contains(MODULES_KEY)) {
            modules.deserializeNBT(registries, tag.getCompound(MODULES_KEY));
        }
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory inventory, Player player) {
        return new PatternDecoderMenu(windowId, inventory, worldPosition);
    }

    @Nullable
    public static PatternDecoderBlockEntity at(@Nullable Level level, BlockPos pos) {
        return level != null && level.getBlockEntity(pos) instanceof PatternDecoderBlockEntity decoder ? decoder : null;
    }
}
