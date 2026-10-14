package dev.rylex.nep.machine;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import org.jetbrains.annotations.Nullable;

/** Components another mod stamps on an item at random whenever a stack is built, so two otherwise equal keys differ. */
public final class IncidentalComponents {

    private static final Set<DataComponentType<?>> TYPES = ConcurrentHashMap.newKeySet();

    private IncidentalComponents() {}

    public static void register(DataComponentType<?> type) {
        TYPES.add(type);
    }

    public static boolean carries(AEKey key) {
        if (TYPES.isEmpty() || !(key instanceof AEItemKey itemKey)) {
            return false;
        }
        for (DataComponentType<?> type : TYPES) {
            if (itemKey.get(type) != null) {
                return true;
            }
        }
        return false;
    }

    public static boolean equivalent(@Nullable AEKey a, @Nullable AEKey b) {
        if (!(a instanceof AEItemKey first)
                || !(b instanceof AEItemKey second)
                || first.getItem() != second.getItem()) {
            return false;
        }
        if (first.equals(second)) {
            return true;
        }
        return !TYPES.isEmpty() && essential(first).equals(essential(second));
    }

    @Nullable
    public static AEKey match(Iterable<AEKey> candidates, AEKey actual) {
        if (!carries(actual)) {
            return null;
        }
        for (AEKey candidate : candidates) {
            if (equivalent(candidate, actual)) {
                return candidate;
            }
        }
        return null;
    }

    private static DataComponentPatch essential(AEItemKey key) {
        return key.getReadOnlyStack().getComponentsPatch().forget(TYPES::contains);
    }
}
