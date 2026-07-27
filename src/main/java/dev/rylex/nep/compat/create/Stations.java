package dev.rylex.nep.compat.create;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

final class Stations {
    private Stations() {}

    static boolean deployerFacesDown(BlockState state) {
        return state.getBlock() == AllBlocks.DEPLOYER.get()
                && state.getValue(DirectionalKineticBlock.FACING) == Direction.DOWN;
    }

    static boolean isUnpowered(Level level, BlockPos pos, StationKind kind) {
        if (!kind.needsRotation()) {
            return false;
        }
        return !(level.getBlockEntity(pos) instanceof KineticBlockEntity kinetic) || kinetic.getSpeed() == 0;
    }

    private static final Map<Block, StationKind> KINDS = Map.of(
            AllBlocks.DEPLOYER.get(), StationKind.DEPLOYER,
            AllBlocks.MECHANICAL_PRESS.get(), StationKind.PRESS,
            AllBlocks.SPOUT.get(), StationKind.SPOUT,
            AllBlocks.MECHANICAL_SAW.get(), StationKind.SAW);

    private static final Map<StationKind, ItemStack> ICONS = new EnumMap<>(StationKind.class);

    static {
        KINDS.forEach((block, kind) -> ICONS.put(kind, new ItemStack(block)));
    }

    static ItemStack icon(StationKind kind) {
        return ICONS.getOrDefault(kind, ItemStack.EMPTY);
    }

    static StationKind detect(BlockState state) {
        return KINDS.getOrDefault(state.getBlock(), StationKind.UNKNOWN);
    }

    static StationKind kindOf(Set<ItemLike> machines) {
        for (ItemLike machine : machines) {
            StationKind kind = kindOfItem(machine.asItem());
            if (kind != StationKind.UNKNOWN) {
                return kind;
            }
        }
        return StationKind.UNKNOWN;
    }

    private static StationKind kindOfItem(Item item) {
        for (Map.Entry<Block, StationKind> entry : KINDS.entrySet()) {
            if (entry.getKey().asItem() == item) {
                return entry.getValue();
            }
        }
        return StationKind.UNKNOWN;
    }
}
