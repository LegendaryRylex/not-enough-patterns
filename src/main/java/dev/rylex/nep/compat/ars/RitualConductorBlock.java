package dev.rylex.nep.compat.ars;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class RitualConductorBlock extends BaseEntityBlock {

    public static final MapCodec<RitualConductorBlock> CODEC = simpleCodec(RitualConductorBlock::new);

    public static final BooleanProperty CONDUCTING = BooleanProperty.create("conducting");

    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(2, 0, 2, 14, 2, 14),
            Block.box(5, 2, 5, 11, 5, 11),
            Block.box(6, 5, 6, 10, 13, 10),
            Block.box(2.5, 8, 2.5, 13.5, 10.5, 13.5),
            Block.box(2, 13, 2, 14, 16, 14));

    public RitualConductorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(CONDUCTING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CONDUCTING);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RitualConductorBlockEntity(NepArsContent.RITUAL_CONDUCTOR_BLOCK_ENTITY.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(
                type, NepArsContent.RITUAL_CONDUCTOR_BLOCK_ENTITY.get(), (ticking, pos, blockState, conductor) -> {
                    if (ticking instanceof ServerLevel serverLevel) {
                        conductor.serverTick(serverLevel);
                    }
                });
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (player.isShiftKeyDown()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        return open(level, pos, player)
                ? ItemInteractionResult.SUCCESS
                : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return open(level, pos, player) ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

    private static boolean open(Level level, BlockPos pos, Player player) {
        if (!(level.getBlockEntity(pos) instanceof RitualConductorBlockEntity conductor)) {
            return false;
        }
        if (!level.isClientSide()) {
            player.openMenu(conductor, buffer -> buffer.writeBlockPos(pos));
        }
        return true;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof RitualConductorBlockEntity conductor
                ? conductor.state().signal()
                : 0;
    }
}
