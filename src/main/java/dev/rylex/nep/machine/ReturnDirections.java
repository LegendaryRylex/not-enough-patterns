package dev.rylex.nep.machine;

import appeng.api.stacks.AEItemKey;
import dev.rylex.nep.util.ItemCounts;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jetbrains.annotations.Nullable;

public final class ReturnDirections {

    private final Map<AEItemKey, Direction> directions = new HashMap<>();
    private final Map<Direction, BlockCapabilityCache<ResourceHandler<ItemResource>, Direction>> targets =
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
    public ResourceHandler<ItemResource> targetFor(Level level, BlockPos pos, AEItemKey key) {
        Direction direction = directions.get(key);
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
        for (Map.Entry<AEItemKey, Direction> entry : directions.entrySet()) {
            ValueOutput entryOutput = list.addChild();
            ItemCounts.putKey(entryOutput, entry.getKey());
            entryOutput.putInt("Dir", entry.getValue().get3DDataValue());
        }
    }

    public void load(ValueInput input, String key) {
        directions.clear();
        for (ValueInput entryInput : input.childrenListOrEmpty(key)) {
            AEItemKey itemKey = ItemCounts.key(entryInput);
            if (itemKey != null) {
                directions.put(itemKey, Direction.from3DDataValue(entryInput.getIntOr("Dir", 0)));
            }
        }
    }
}
