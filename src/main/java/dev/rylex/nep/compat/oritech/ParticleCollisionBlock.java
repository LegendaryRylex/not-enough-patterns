package dev.rylex.nep.compat.oritech;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

abstract class ParticleCollisionBlock<T extends BlockEntity & MenuProvider> extends BaseEntityBlock {

    private final Class<T> entityClass;
    private final Supplier<BlockEntityType<T>> entityType;
    private final BlockEntityTicker<T> serverTicker;

    ParticleCollisionBlock(
            Properties properties,
            Class<T> entityClass,
            Supplier<BlockEntityType<T>> entityType,
            BlockEntityTicker<T> serverTicker) {
        super(properties);
        this.entityClass = entityClass;
        this.entityType = entityType;
        this.serverTicker = serverTicker;
    }

    abstract int comparatorOutput(T entity);

    abstract void releaseContents(T entity, Level level, BlockPos pos);

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public <E extends BlockEntity> BlockEntityTicker<E> getTicker(
            Level level, BlockState state, BlockEntityType<E> type) {
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(type, entityType.get(), serverTicker);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        BlockEntity entity = level.getBlockEntity(pos);
        return entityClass.isInstance(entity) ? comparatorOutput(entityClass.cast(entity)) : 0;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity entity = level.getBlockEntity(pos);
            if (entityClass.isInstance(entity)) {
                releaseContents(entityClass.cast(entity), level, pos);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            BlockEntity entity = level.getBlockEntity(pos);
            if (entityClass.isInstance(entity)) {
                player.openMenu(entityClass.cast(entity), buffer -> buffer.writeBlockPos(pos));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
