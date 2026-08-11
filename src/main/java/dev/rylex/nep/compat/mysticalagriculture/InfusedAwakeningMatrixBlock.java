package dev.rylex.nep.compat.mysticalagriculture;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

public class InfusedAwakeningMatrixBlock extends BaseEntityBlock {

    public static final MapCodec<InfusedAwakeningMatrixBlock> CODEC = simpleCodec(InfusedAwakeningMatrixBlock::new);

    public static final EnumProperty<MatrixStatus> STATUS = EnumProperty.create("status", MatrixStatus.class);

    public InfusedAwakeningMatrixBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(STATUS, MatrixStatus.IDLE));
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
        return new InfusedAwakeningMatrixBlockEntity(NepMysticalContent.MATRIX_BLOCK_ENTITY.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(
                type, NepMysticalContent.MATRIX_BLOCK_ENTITY.get(), InfusedAwakeningMatrixBlockEntity::serverTick);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction fromDirection) {
        return level.getBlockEntity(pos) instanceof InfusedAwakeningMatrixBlockEntity matrix
                ? matrix.comparatorOutput()
                : 0;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof InfusedAwakeningMatrixBlockEntity matrix) {
            player.openMenu(matrix, buffer -> buffer.writeBlockPos(pos));
        }
        return InteractionResult.SUCCESS;
    }

    public enum MatrixStatus implements StringRepresentable {
        IDLE("idle"),
        RUNNING("running"),
        STALLED("stalled");

        private final String name;

        MatrixStatus(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
