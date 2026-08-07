package dev.rylex.nep.compat.compactcrafting;

import dev.compactmods.crafting.api.field.IMiniaturizationField;
import dev.compactmods.crafting.api.field.MiniaturizationFieldSize;
import dev.compactmods.crafting.api.projector.FieldProjectorProperties;
import dev.compactmods.crafting.data.CCAttachments;
import dev.compactmods.crafting.field.ActiveWorldFields;
import dev.compactmods.crafting.projector.FieldProjectorBlock;
import dev.compactmods.crafting.recipes.MiniaturizationRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

final class FieldBinding {
    private FieldBinding() {}

    @Nullable
    static BlockPos centerFromNeighbours(BlockGetter level, BlockPos pos) {
        for (Direction side : Direction.values()) {
            BlockPos projector = pos.relative(side);
            BlockState state = level.getBlockState(projector);
            if (!(state.getBlock() instanceof FieldProjectorBlock) || !FieldProjectorBlock.isActive(state)) {
                continue;
            }
            MiniaturizationFieldSize size = state.getValue(FieldProjectorProperties.SIZE);
            Direction facing = state.getValue(FieldProjectorProperties.FACING);
            return size.getCenterFromProjector(projector, facing);
        }
        return null;
    }

    @Nullable
    static IMiniaturizationField<MiniaturizationRecipe> adjacentField(Level level, BlockPos pos) {
        BlockPos center = centerFromNeighbours(level, pos);
        return center == null ? null : fieldAt(level, center);
    }

    @Nullable
    static IMiniaturizationField<MiniaturizationRecipe> fieldAt(Level level, BlockPos center) {
        ActiveWorldFields fields = level.getData(CCAttachments.ACTIVE_FIELDS);
        return fields.get(center).orElse(null);
    }
}
