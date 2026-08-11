package dev.rylex.nep.machine;

import dev.rylex.nep.util.ItemCounts;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jetbrains.annotations.Nullable;

public final class ReturnDirections {

    private final Map<Item, Direction> directions = new HashMap<>();
    private final Map<Direction, BlockCapabilityCache<ResourceHandler<ItemResource>, Direction>> targets =
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
    public ResourceHandler<ItemResource> targetFor(Level level, BlockPos pos, Item item) {
        Direction direction = directions.get(item);
        if (direction == null || !(level instanceof ServerLevel serverLevel)) {
            return null;
        }
        return targets.computeIfAbsent(
                        direction,
                        dir -> BlockCapabilityCache.create(
                                Capabilities.Item.BLOCK, serverLevel, pos.relative(dir), dir.getOpposite()))
                .getCapability();
    }

    public void save(ValueOutput output, String key) {
        ValueOutput.ValueOutputList list = output.childrenList(key);
        for (Map.Entry<Item, Direction> entry : directions.entrySet()) {
            ValueOutput entryOutput = list.addChild();
            entryOutput.putString(
                    "Id", BuiltInRegistries.ITEM.getKey(entry.getKey()).toString());
            entryOutput.putInt("Dir", entry.getValue().get3DDataValue());
        }
    }

    public void load(ValueInput input, String key) {
        directions.clear();
        for (ValueInput entryInput : input.childrenListOrEmpty(key)) {
            Item item = ItemCounts.item(entryInput.getStringOr("Id", ""));
            if (item != null) {
                directions.put(item, Direction.from3DDataValue(entryInput.getIntOr("Dir", 0)));
            }
        }
    }
}
