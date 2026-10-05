package dev.rylex.nep.compat.compactcrafting;

import dev.compactmods.crafting.api.components.IRecipeBlockComponent;
import dev.compactmods.crafting.api.recipe.layers.IRecipeLayer;
import dev.compactmods.crafting.recipes.MiniaturizationRecipe;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

final class MiniaturizationLayout {
    private MiniaturizationLayout() {}

    record Placement(BlockPos pos, BlockState state) {}

    static BlockPos anchorFor(MiniaturizationRecipe recipe, BlockPos center) {
        AABB dimensions = recipe.getDimensions();
        return new BlockPos(
                center.getX() - (span(dimensions.getXsize()) - 1) / 2,
                center.getY() - (span(dimensions.getYsize()) - 1) / 2,
                center.getZ() - (span(dimensions.getZsize()) - 1) / 2);
    }

    @Nullable
    static List<Placement> plan(MiniaturizationRecipe recipe, BlockPos anchor) {
        List<Placement> placements = new ArrayList<>();
        for (int layerIndex = 0; layerIndex < recipe.getNumberLayers(); layerIndex++) {
            IRecipeLayer layer = recipe.getLayer(layerIndex).orElse(null);
            if (layer == null) {
                continue;
            }
            for (String component : layer.getComponents()) {
                if (component == null || MiniaturizationComponents.isEmpty(recipe, component)) {
                    continue;
                }
                IRecipeBlockComponent block =
                        recipe.getComponents().getBlock(component).orElse(null);
                if (block == null) {
                    return null;
                }
                BlockState state = stateFor(block);
                if (state == null) {
                    return null;
                }
                List<BlockPos> offsets = layer.getPositionsForComponent(component)
                        .map(BlockPos::immutable)
                        .toList();
                for (BlockPos offset : offsets) {
                    placements.add(new Placement(anchor.offset(offset.getX(), layerIndex, offset.getZ()), state));
                }
            }
        }
        return placements.isEmpty() ? null : List.copyOf(placements);
    }

    static boolean isClear(Level level, AABB bounds) {
        for (BlockPos pos : blocksIn(bounds)) {
            if (!level.isEmptyBlock(pos)) {
                return false;
            }
        }
        return true;
    }

    static Iterable<BlockPos> blocksIn(AABB bounds) {
        return BlockPos.betweenClosed(
                BlockPos.containing(bounds.minX, bounds.minY, bounds.minZ),
                BlockPos.containing(bounds.maxX - 1.0, bounds.maxY - 1.0, bounds.maxZ - 1.0));
    }

    @Nullable
    private static BlockState stateFor(IRecipeBlockComponent component) {
        BlockState rendered = component.getRenderState();
        if (rendered != null && component.matches(rendered)) {
            return rendered;
        }
        for (BlockState candidate : component.getBlock().getStateDefinition().getPossibleStates()) {
            if (component.matches(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static int span(double size) {
        return Math.max(1, (int) Math.ceil(size));
    }
}
