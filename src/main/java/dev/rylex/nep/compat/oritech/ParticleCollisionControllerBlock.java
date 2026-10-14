package dev.rylex.nep.compat.oritech;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jetbrains.annotations.Nullable;

public class ParticleCollisionControllerBlock extends ParticleCollisionBlock<ParticleCollisionControllerBlockEntity> {

    public static final MapCodec<ParticleCollisionControllerBlock> CODEC =
            simpleCodec(ParticleCollisionControllerBlock::new);

    public static final EnumProperty<ControllerStatus> STATUS = EnumProperty.create("status", ControllerStatus.class);

    public ParticleCollisionControllerBlock(Properties properties) {
        super(
                properties,
                ParticleCollisionControllerBlockEntity.class,
                NepOritechContent.CONTROLLER_BLOCK_ENTITY::get,
                ParticleCollisionControllerBlockEntity::serverTick);
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

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ParticleCollisionControllerBlockEntity(NepOritechContent.CONTROLLER_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    int comparatorOutput(ParticleCollisionControllerBlockEntity entity) {
        return entity.comparatorOutput();
    }

    @Override
    void releaseContents(ParticleCollisionControllerBlockEntity entity, Level level, BlockPos pos) {
        entity.dropBuffers(level, pos);
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
