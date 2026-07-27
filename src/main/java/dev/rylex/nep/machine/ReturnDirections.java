package dev.rylex.nep.machine;

import dev.rylex.nep.util.ItemCounts;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

public final class ReturnDirections {

    private final Map<Item, Direction> directions = new HashMap<>();
    private final Map<Direction, BlockCapabilityCache<IItemHandler, Direction>> targets =
            new EnumMap<>(Direction.class);

    public void record(Item item, Direction direction) {
        directions.put(item, direction);
    }

    public void forget(Item item) {
        directions.remove(item);
    }

    @Nullable
    public Direction directionFor(Item item) {
        return directions.get(item);
    }

    public boolean isEmpty() {
        return directions.isEmpty();
    }

    @Nullable
    public IItemHandler targetFor(Level level, BlockPos pos, Item item) {
        Direction direction = directions.get(item);
        if (direction == null || !(level instanceof ServerLevel serverLevel)) {
            return null;
        }
        return targets.computeIfAbsent(
                        direction,
                        dir -> BlockCapabilityCache.create(
                                Capabilities.ItemHandler.BLOCK, serverLevel, pos.relative(dir), dir.getOpposite()))
                .getCapability();
    }

    public ListTag save() {
        ListTag list = new ListTag();
        for (Map.Entry<Item, Direction> entry : directions.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putString(
                    "Id",
                    net.minecraft.core.registries.BuiltInRegistries.ITEM
                            .getKey(entry.getKey())
                            .toString());
            entryTag.putInt("Dir", entry.getValue().get3DDataValue());
            list.add(entryTag);
        }
        return list;
    }

    public void load(CompoundTag tag, String key, String legacyKey, Collection<Item> legacyItems) {
        directions.clear();
        if (tag.contains(key, Tag.TAG_LIST)) {
            ListTag list = tag.getList(key, Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entryTag = list.getCompound(i);
                Item item = ItemCounts.item(entryTag.getString("Id"));
                if (item != null) {
                    directions.put(item, Direction.from3DDataValue(entryTag.getInt("Dir")));
                }
            }
        } else if (tag.contains(legacyKey)) {
            Direction legacy = Direction.from3DDataValue(tag.getInt(legacyKey));
            for (Item item : legacyItems) {
                directions.put(item, legacy);
            }
        }
    }
}
