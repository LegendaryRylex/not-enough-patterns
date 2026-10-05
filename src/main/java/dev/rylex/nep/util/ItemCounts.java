package dev.rylex.nep.util;

import appeng.api.stacks.AEItemKey;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

public final class ItemCounts {
    private ItemCounts() {}

    private static final String KEY_TAG = "Key";
    private static final String LEGACY_ID_TAG = "Id";
    private static final String COUNT_TAG = "Count";

    public static void save(ValueOutput output, String key, Map<AEItemKey, Long> map) {
        ValueOutput.ValueOutputList list = output.childrenList(key);
        for (Map.Entry<AEItemKey, Long> entry : map.entrySet()) {
            ValueOutput entryOutput = list.addChild();
            putKey(entryOutput, entry.getKey());
            entryOutput.putLong(COUNT_TAG, entry.getValue());
        }
    }

    public static void load(Map<AEItemKey, Long> map, ValueInput input, String key) {
        map.clear();
        for (ValueInput entryInput : input.childrenListOrEmpty(key)) {
            long count = entryInput.getLongOr(COUNT_TAG, 0L);
            AEItemKey itemKey = key(entryInput);
            if (count > 0 && itemKey != null) {
                map.merge(itemKey, count, Long::sum);
            }
        }
    }

    /**
     * Reads the item key an entry was written with, falling back to the bare item id written before matrix
     * bookkeeping carried data components.
     */
    @Nullable
    public static AEItemKey key(ValueInput entryInput) {
        AEItemKey stored = entryInput.read(KEY_TAG, AEItemKey.CODEC).orElse(null);
        if (stored != null) {
            return stored;
        }
        Item item = item(entryInput.getStringOr(LEGACY_ID_TAG, ""));
        return item == null ? null : AEItemKey.of(item);
    }

    public static void putKey(ValueOutput entryOutput, AEItemKey key) {
        entryOutput.store(KEY_TAG, AEItemKey.CODEC, key);
    }

    @Nullable
    public static Item item(String id) {
        Identifier location = Identifier.tryParse(id);
        return location == null
                ? null
                : BuiltInRegistries.ITEM.getOptional(location).orElse(null);
    }
}
