package dev.rylex.nep.compat.oritech;

import com.mojang.serialization.MapCodec;
import dev.rylex.nep.machine.MatrixStatus;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jetbrains.annotations.Nullable;

public class ParticleCollisionMatrixBlock extends ParticleCollisionBlock<ParticleCollisionMatrixBlockEntity> {

    public static final MapCodec<ParticleCollisionMatrixBlock> CODEC = simpleCodec(ParticleCollisionMatrixBlock::new);

    public static final EnumProperty<MatrixStatus> STATUS = MatrixStatus.PROPERTY;

    public ParticleCollisionMatrixBlock(Properties properties) {
        super(
                properties,
                ParticleCollisionMatrixBlockEntity.class,
                NepOritechContent.MATRIX_BLOCK_ENTITY::get,
                ParticleCollisionMatrixBlockEntity::serverTick);
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

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ParticleCollisionMatrixBlockEntity(NepOritechContent.MATRIX_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    int comparatorOutput(ParticleCollisionMatrixBlockEntity entity) {
        return entity.comparatorOutput();
    }
}
