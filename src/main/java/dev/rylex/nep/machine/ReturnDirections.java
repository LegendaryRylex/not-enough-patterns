package dev.rylex.nep.machine;

import appeng.api.stacks.AEItemKey;
import dev.rylex.nep.util.ItemCounts;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

public final class ReturnDirections {

    private final Map<AEItemKey, Direction> directions = new HashMap<>();
    private final Map<Direction, BlockCapabilityCache<IItemHandler, Direction>> targets =
            new EnumMap<>(Direction.class);

    public void record(AEItemKey key, Direction direction) {
        directions.put(key, direction);
    }

    public void forget(AEItemKey key) {
        directions.remove(key);
    }

    @Nullable
    public Direction directionFor(AEItemKey key) {
        return directions.get(key);
    }

    public boolean isEmpty() {
        return directions.isEmpty();
    }

    @Nullable
    public IItemHandler targetFor(Level level, BlockPos pos, AEItemKey key) {
        Direction direction = directions.get(key);
        if (direction == null || !(level instanceof ServerLevel serverLevel)) {
            return null;
        }
        return targets.computeIfAbsent(
                        direction,
                        dir -> BlockCapabilityCache.create(
                                Capabilities.ItemHandler.BLOCK, serverLevel, pos.relative(dir), dir.getOpposite()))
                .getCapability();
    }

    public ListTag save(HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Map.Entry<AEItemKey, Direction> entry : directions.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            ItemCounts.putKey(entryTag, entry.getKey(), registries);
            entryTag.putInt("Dir", entry.getValue().get3DDataValue());
            list.add(entryTag);
        }
        return list;
    }

    public void load(
            CompoundTag tag,
            String key,
            String legacyKey,
            Collection<AEItemKey> legacyKeys,
            HolderLookup.Provider registries) {
        directions.clear();
        if (tag.contains(key, Tag.TAG_LIST)) {
            ListTag list = tag.getList(key, Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entryTag = list.getCompound(i);
                AEItemKey itemKey = ItemCounts.key(entryTag, registries);
                if (itemKey != null) {
                    directions.put(itemKey, Direction.from3DDataValue(entryTag.getInt("Dir")));
                }
            }
        } else if (tag.contains(legacyKey)) {
            Direction legacy = Direction.from3DDataValue(tag.getInt(legacyKey));
            for (AEItemKey itemKey : legacyKeys) {
                directions.put(itemKey, legacy);
            }
        }
    }
}
