package dev.rylex.nep.compat.create;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.rylex.nep.compat.create.newage.CreateNewAgeCompat;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;

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

    private static final boolean NEW_AGE_LOADED = ModList.get().isLoaded(CreateNewAgeCompat.MOD_ID);

    private static final Map<Block, StationKind> KINDS = buildKinds();

    private static Map<Block, StationKind> buildKinds() {
        Map<Block, StationKind> kinds = new LinkedHashMap<>();
        kinds.put(AllBlocks.DEPLOYER.get(), StationKind.DEPLOYER);
        kinds.put(AllBlocks.MECHANICAL_PRESS.get(), StationKind.PRESS);
        kinds.put(AllBlocks.SPOUT.get(), StationKind.SPOUT);
        kinds.put(AllBlocks.MECHANICAL_SAW.get(), StationKind.SAW);
        if (NEW_AGE_LOADED) {
            for (Block energiser : CreateNewAgeCompat.energiserBlocks()) {
                kinds.put(energiser, StationKind.ENERGIZER);
            }
        }
        return Collections.unmodifiableMap(kinds);
    }

    private static final Map<StationKind, ItemStack> ICONS = new EnumMap<>(StationKind.class);

    static {
        KINDS.forEach((block, kind) -> ICONS.putIfAbsent(kind, new ItemStack(block)));
    }

    static ItemStack icon(StationKind kind) {
        return ICONS.getOrDefault(kind, ItemStack.EMPTY);
    }

    static StationKind detect(BlockState state) {
        StationKind kind = KINDS.get(state.getBlock());
        if (kind != null) {
            return kind;
        }
        if (NEW_AGE_LOADED && CreateNewAgeCompat.isEnergiser(state.getBlock())) {
            return StationKind.ENERGIZER;
        }
        return StationKind.UNKNOWN;
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
        if (NEW_AGE_LOADED
                && item instanceof BlockItem blockItem
                && CreateNewAgeCompat.isEnergiser(blockItem.getBlock())) {
            return StationKind.ENERGIZER;
        }
        return StationKind.UNKNOWN;
    }
}
