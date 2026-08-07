package dev.rylex.nep.compat.compactcrafting;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class MiniaturizationControllerBlock extends BaseEntityBlock {

    public static final MapCodec<MiniaturizationControllerBlock> CODEC =
            simpleCodec(MiniaturizationControllerBlock::new);

    public static final EnumProperty<ControllerStatus> STATUS = EnumProperty.create("status", ControllerStatus.class);

    public MiniaturizationControllerBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(STATUS, ControllerStatus.IDLE));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STATUS);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MiniaturizationControllerBlockEntity(
                NepCompactCraftingContent.CONTROLLER_BLOCK_ENTITY.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(
                type,
                NepCompactCraftingContent.CONTROLLER_BLOCK_ENTITY.get(),
                MiniaturizationControllerBlockEntity::serverTick);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof MiniaturizationControllerBlockEntity controller
                ? controller.comparatorOutput()
                : 0;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())
                && level.getBlockEntity(pos) instanceof MiniaturizationControllerBlockEntity controller) {
            controller.dropBuffers(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide
                && level.getBlockEntity(pos) instanceof MiniaturizationControllerBlockEntity controller) {
            player.openMenu(controller, buffer -> buffer.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    public enum ControllerStatus implements StringRepresentable {
        IDLE("idle"),
        RUNNING("running"),
        STALLED("stalled");

        private final String name;

        ControllerStatus(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
