package dev.rylex.nep.decoder;

import appeng.api.networking.IInWorldGridNodeHost;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

public class PatternDecoderBlockEntity extends BlockEntity implements MenuProvider {

    public static final int SLOTS = DecoderModule.values().length;

    private static final String MODULES_KEY = "Modules";

    private final ModuleSlots modules = new ModuleSlots() {
        @Override
        protected void onContentsChanged(int index, ItemStack previousContents) {
            held = held();
            setChanged();
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
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null) {
            for (int slot = 0; slot < modules.size(); slot++) {
                ItemStack stack = modules.getResource(slot).toStack(modules.getAmountAsInt(slot));
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
            }
        }
        super.preRemoveSideEffects(pos, state);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        node.save(output);
        modules.serialize(output.child(MODULES_KEY));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        node.load(input);
        modules.deserialize(input.childOrEmpty(MODULES_KEY));
        held = modules.held();
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
