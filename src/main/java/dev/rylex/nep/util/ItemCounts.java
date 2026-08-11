package dev.rylex.nep.util;

import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

public final class ItemCounts {
    private ItemCounts() {}

    public static void save(ValueOutput output, String key, Map<Item, Long> map) {
        ValueOutput.ValueOutputList list = output.childrenList(key);
        for (Map.Entry<Item, Long> entry : map.entrySet()) {
            ValueOutput entryOutput = list.addChild();
            entryOutput.putString(
                    "Id", BuiltInRegistries.ITEM.getKey(entry.getKey()).toString());
            entryOutput.putLong("Count", entry.getValue());
        }
    }

    public static void load(Map<Item, Long> map, ValueInput input, String key) {
        map.clear();
        for (ValueInput entryInput : input.childrenListOrEmpty(key)) {
            long count = entryInput.getLongOr("Count", 0L);
            Item item = item(entryInput.getStringOr("Id", ""));
            if (count > 0 && item != null) {
                map.merge(item, count, Long::sum);
            }
        }
    }

    @Nullable
    public static Item item(String id) {
        Identifier location = Identifier.tryParse(id);
        return location == null
                ? null
                : BuiltInRegistries.ITEM.getOptional(location).orElse(null);
    }
}
